package com.budgettracker.controller;

import com.budgettracker.model.*;
import com.budgettracker.model.Transaction.TxType;
import com.budgettracker.model.Transaction.EntryMode;
import com.budgettracker.service.BudgetService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private BudgetService budgetService;

    private String userId(HttpSession s) { return (String) s.getAttribute("userId"); }

    private String guard(HttpSession s) {
        if (s.getAttribute("userId") == null) return "redirect:/login";
        return null;
    }

    private String fmt(double v) { return String.format("%,.2f", v); }
    private String fmtBalance(double v) { return (v >= 0 ? "+" : "-") + "₹" + fmt(Math.abs(v)); }
    private String pct(double part, double total) {
        if (total <= 0) return "0";
        return String.format("%.1f", (part / total) * 100);
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        String redir = guard(session); if (redir != null) return redir;
        String uid = userId(session);
        int autoPosted = budgetService.postDueRecurring(uid);

        double income  = budgetService.getTotalIncome(uid);
        double expense = budgetService.getTotalExpenses(uid);
        double balance = budgetService.getBalance(uid);

        model.addAttribute("userName",     session.getAttribute("userName"));
        model.addAttribute("fmtIncome",    "₹" + fmt(income));
        model.addAttribute("fmtExpense",   "₹" + fmt(expense));
        model.addAttribute("fmtBalance",   fmtBalance(balance));
        model.addAttribute("balancePos",   balance >= 0);
        model.addAttribute("txCount",      budgetService.getTransactions(uid).size());
        model.addAttribute("bankCount",    budgetService.getBankAccounts(uid).size());
        model.addAttribute("recurCount",   budgetService.getRecurringRules(uid).size());
        model.addAttribute("dueCount",     budgetService.getRecurringRules(uid).stream().filter(RecurringRule::isDue).count());
        model.addAttribute("autoPosted",   autoPosted);
        model.addAttribute("recentTx",     budgetService.getTransactions(uid).stream()
            .sorted((a,b)->b.getDate().compareTo(a.getDate())).limit(5).toList());
        model.addAttribute("banks",        budgetService.getBankAccounts(uid));
        return "dashboard";
    }

    @GetMapping("/add")
    public String addPage(HttpSession session, Model model,
                          @RequestParam(defaultValue="EXPENSE") String type) {
        String redir = guard(session); if (redir != null) return redir;
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("txType",   type);
        model.addAttribute("banks",    budgetService.getBankAccounts(userId(session)));
        model.addAttribute("today",    LocalDate.now().toString());
        model.addAttribute("expCats",  new String[]{
            "Food & Dining","Rent / Housing","Transport","Utilities",
            "Healthcare","Entertainment","Shopping","Education","Other"});
        model.addAttribute("incCats",  new String[]{
            "Salary","Freelance","Investment","Gift / Bonus","Bonus","Other"});
        return "add-transaction";
    }

    @GetMapping("/suggest")
    @ResponseBody
    public Map<String,String> suggest(@RequestParam String desc) {
        String[] s = budgetService.autoSuggest(desc);
        if (s == null) return Map.of();
        return Map.of("category", s[0], "type", s[1]);
    }

    @PostMapping("/add")
    public String addTransaction(HttpSession session,
                                 @RequestParam String txType,
                                 @RequestParam double amount,
                                 @RequestParam String description,
                                 @RequestParam String category,
                                 @RequestParam String date,
                                 @RequestParam(required=false) String bankId,
                                 @RequestParam(defaultValue="MANUAL") String entryMode) {
        String redir = guard(session); if (redir != null) return redir;
        TxType    type = TxType.valueOf(txType);
        EntryMode mode = (bankId != null && !bankId.isEmpty()) ? EntryMode.BANK
                       : "AUTO".equals(entryMode) ? EntryMode.AUTO : EntryMode.MANUAL;
        budgetService.addTransaction(userId(session), type, amount, category,
            description, LocalDate.parse(date), mode,
            (bankId != null && !bankId.isEmpty()) ? bankId : null);
        return "redirect:/dashboard/history?success=Transaction+saved!";
    }

    @GetMapping("/history")
    public String history(HttpSession session, Model model,
                          @RequestParam(required=false) String filter,
                          @RequestParam(required=false) String success) {
        String redir = guard(session); if (redir != null) return redir;
        String uid = userId(session);

        List<Transaction> txs = budgetService.getTransactions(uid);
        if (filter != null && !filter.isEmpty()) {
            String q = filter.toLowerCase();
            txs = txs.stream().filter(t ->
                t.getDescription().toLowerCase().contains(q) ||
                t.getCategory().toLowerCase().contains(q)
            ).toList();
        }
        List<Transaction> sorted = txs.stream()
            .sorted((a,b) -> b.getDate().compareTo(a.getDate())).toList();

        double income  = budgetService.getTotalIncome(uid);
        double expense = budgetService.getTotalExpenses(uid);
        double balance = budgetService.getBalance(uid);

        // Pre-build bank lookup map
        Map<String,String> bankNames = new HashMap<>();
        for (BankAccount b : budgetService.getBankAccounts(uid)) {
            bankNames.put(b.getId(), b.getBankName() + " " + b.getMask());
        }

        // Pre-format each transaction for the template
        List<Map<String,String>> rows = new ArrayList<>();
        for (Transaction t : sorted) {
            Map<String,String> row = new HashMap<>();
            row.put("id",       String.valueOf(t.getId()));
            row.put("date",     t.getFmtDate());
            row.put("desc",     t.getDescription());
            row.put("cat",      t.getCategory());
            row.put("amount",   (t.isIncome() ? "+₹" : "-₹") + fmt(t.getAmount()));
            row.put("amtClass", t.isIncome() ? "amount-inc" : "amount-exp");
            row.put("type",     t.getType().name());
            row.put("typeClass",t.isIncome() ? "badge badge-income" : "badge badge-expense");
            row.put("mode",     t.getMode().name());
            row.put("bank",     t.getBankId() != null ? bankNames.getOrDefault(t.getBankId(),"Bank") : "");
            rows.add(row);
        }

        model.addAttribute("userName",    session.getAttribute("userName"));
        model.addAttribute("rows",        rows);
        model.addAttribute("fmtIncome",   "₹" + fmt(income));
        model.addAttribute("fmtExpense",  "₹" + fmt(expense));
        model.addAttribute("fmtBalance",  fmtBalance(balance));
        model.addAttribute("balancePos",  balance >= 0);
        model.addAttribute("totalCount",  sorted.size());
        model.addAttribute("filter",      filter);
        model.addAttribute("success",     success);
        return "history";
    }

    @PostMapping("/history/delete")
    public String deleteTransaction(HttpSession session, @RequestParam int txId) {
        String redir = guard(session); if (redir != null) return redir;
        budgetService.deleteTransaction(userId(session), txId);
        return "redirect:/dashboard/history?success=Transaction+deleted.";
    }

    @GetMapping("/banks")
    public String banksPage(HttpSession session, Model model,
                            @RequestParam(required=false) String success,
                            @RequestParam(required=false) String error) {
        String redir = guard(session); if (redir != null) return redir;
        String uid = userId(session);
        model.addAttribute("userName",     session.getAttribute("userName"));
        model.addAttribute("banks",        budgetService.getBankAccounts(uid));
        model.addAttribute("success",      success);
        model.addAttribute("error",        error);
        model.addAttribute("indianBanks",  new String[]{
            "State Bank of India","HDFC Bank","ICICI Bank","Axis Bank",
            "Kotak Mahindra Bank","Punjab National Bank","Bank of Baroda",
            "Canara Bank","IDBI Bank","IndusInd Bank","Yes Bank",
            "Federal Bank","IDFC First Bank","RBL Bank","Bandhan Bank",
            "Union Bank of India","UCO Bank","Bank of India","Other"});
        model.addAttribute("accountTypes", new String[]{
            "Savings","Current","Salary","Fixed Deposit","Credit Card","Wallet","Other"});
        return "banks";
    }

    @PostMapping("/banks/add")
    public String addBank(HttpSession session,
                          @RequestParam String bankName,
                          @RequestParam String accountType,
                          @RequestParam String accountNo,
                          @RequestParam String ifsc,
                          @RequestParam String holder,
                          @RequestParam double balance) {
        String redir = guard(session); if (redir != null) return redir;
        if (!ifsc.matches("[A-Za-z]{4}0[A-Za-z0-9]{6}"))
            return "redirect:/dashboard/banks?error=Invalid+IFSC+code.";
        if (!accountNo.matches("\\d{9,18}"))
            return "redirect:/dashboard/banks?error=Account+number+must+be+9-18+digits.";
        budgetService.addBankAccount(userId(session), bankName, accountType,
            accountNo, ifsc.toUpperCase(), holder, balance);
        return "redirect:/dashboard/banks?success=Bank+account+added!";
    }

    @PostMapping("/banks/delete")
    public String deleteBank(HttpSession session, @RequestParam String bankId) {
        String redir = guard(session); if (redir != null) return redir;
        budgetService.deleteBankAccount(userId(session), bankId);
        return "redirect:/dashboard/banks?success=Bank+account+removed.";
    }

    @GetMapping("/recurring")
    public String recurringPage(HttpSession session, Model model,
                                @RequestParam(required=false) String success) {
        String redir = guard(session); if (redir != null) return redir;
        String uid = userId(session);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("rules",    budgetService.getRecurringRules(uid));
        model.addAttribute("success",  success);
        model.addAttribute("today",    LocalDate.now().toString());
        model.addAttribute("expCats",  new String[]{
            "Food & Dining","Rent / Housing","Transport","Utilities",
            "Healthcare","Entertainment","Shopping","Education","Other"});
        model.addAttribute("incCats",  new String[]{
            "Salary","Freelance","Investment","Gift / Bonus","Bonus","Other"});
        return "recurring";
    }

    @PostMapping("/recurring/add")
    public String addRecurring(HttpSession session,
                               @RequestParam String txType,
                               @RequestParam double amount,
                               @RequestParam String description,
                               @RequestParam String category,
                               @RequestParam String frequency,
                               @RequestParam String startDate) {
        String redir = guard(session); if (redir != null) return redir;
        budgetService.addRecurringRule(userId(session),
            TxType.valueOf(txType), amount, category, description,
            RecurringRule.Frequency.valueOf(frequency), LocalDate.parse(startDate));
        return "redirect:/dashboard/recurring?success=Recurring+rule+added!";
    }

    @PostMapping("/recurring/delete")
    public String deleteRecurring(HttpSession session, @RequestParam String ruleId) {
        String redir = guard(session); if (redir != null) return redir;
        budgetService.deleteRecurringRule(userId(session), ruleId);
        return "redirect:/dashboard/recurring?success=Rule+removed.";
    }

    @PostMapping("/recurring/post-all")
    public String postAllRecurring(HttpSession session) {
        String redir = guard(session); if (redir != null) return redir;
        int posted = budgetService.postDueRecurring(userId(session));
        return "redirect:/dashboard/history?success=" + posted + "+recurring+transactions+posted!";
    }

    @GetMapping("/reports")
    public String reportsPage(HttpSession session, Model model) {
        String redir = guard(session); if (redir != null) return redir;
        String uid = userId(session);

        double income  = budgetService.getTotalIncome(uid);
        double expense = budgetService.getTotalExpenses(uid);
        double balance = budgetService.getBalance(uid);
        double savings = income > 0 ? (balance / income) * 100 : 0;

        // Pre-format category maps
        List<Map<String,String>> expRows = new ArrayList<>();
        budgetService.getCategoryExpenses(uid).forEach((cat, amt) -> {
            Map<String,String> r = new HashMap<>();
            r.put("cat",   cat);
            r.put("amt",   "₹" + fmt(amt));
            r.put("pct",   pct(amt, expense) + "%");
            r.put("width", pct(amt, expense));
            expRows.add(r);
        });

        List<Map<String,String>> incRows = new ArrayList<>();
        budgetService.getCategoryIncome(uid).forEach((cat, amt) -> {
            Map<String,String> r = new HashMap<>();
            r.put("cat",   cat);
            r.put("amt",   "₹" + fmt(amt));
            r.put("pct",   pct(amt, income) + "%");
            r.put("width", pct(amt, income));
            incRows.add(r);
        });

        long manual = budgetService.getTransactions(uid).stream()
            .filter(t -> t.getMode()==EntryMode.MANUAL||t.getMode()==EntryMode.AUTO).count();
        long bank   = budgetService.getTransactions(uid).stream()
            .filter(t -> t.getMode()==EntryMode.BANK).count();
        long recur  = budgetService.getTransactions(uid).stream()
            .filter(t -> t.getMode()==EntryMode.RECURRING).count();

        model.addAttribute("userName",     session.getAttribute("userName"));
        model.addAttribute("fmtIncome",    "₹" + fmt(income));
        model.addAttribute("fmtExpense",   "₹" + fmt(expense));
        model.addAttribute("fmtBalance",   fmtBalance(balance));
        model.addAttribute("balancePos",   balance >= 0);
        model.addAttribute("savingsRate",  String.format("%.1f%%", savings));
        model.addAttribute("expRows",      expRows);
        model.addAttribute("incRows",      incRows);
        model.addAttribute("txCount",      budgetService.getTransactions(uid).size());
        model.addAttribute("bankCount",    budgetService.getBankAccounts(uid).size());
        model.addAttribute("recurCount",   budgetService.getRecurringRules(uid).size());
        model.addAttribute("manualCount",  manual);
        model.addAttribute("bankCount2",   bank);
        model.addAttribute("recurCount2",  recur);
        return "reports";
    }
}
