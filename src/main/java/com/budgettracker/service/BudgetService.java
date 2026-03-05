package com.budgettracker.service;

import com.budgettracker.model.*;
import com.budgettracker.model.Transaction.TxType;
import com.budgettracker.model.Transaction.EntryMode;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    // ── In-memory store (keyed by userId) ────────────────────────
    private final Map<String, List<Transaction>>   txMap      = new HashMap<>();
    private final Map<String, List<BankAccount>>   bankMap    = new HashMap<>();
    private final Map<String, List<RecurringRule>> recurMap   = new HashMap<>();
    private final List<User>                       users      = new ArrayList<>();

    // ── Auto-categorization rules ─────────────────────────────────
    private static final Object[][] CAT_RULES = {
        { new String[]{"swiggy","zomato","dominos","kfc","mcdonalds","pizza","burger","restaurant","cafe","dmart","bigbasket","grocery"}, "Food & Dining",   TxType.EXPENSE },
        { new String[]{"rent","housing","flat","apartment","pg","hostel","lease"},                                                        "Rent / Housing",  TxType.EXPENSE },
        { new String[]{"uber","ola","rapido","metro","petrol","diesel","fuel","bpcl","toll","parking"},                                   "Transport",       TxType.EXPENSE },
        { new String[]{"electricity","bescom","water bill","broadband","airtel","jio","internet","phone bill","recharge"},                "Utilities",       TxType.EXPENSE },
        { new String[]{"apollo","medplus","hospital","clinic","doctor","medicine","pharmacy","health"},                                   "Healthcare",      TxType.EXPENSE },
        { new String[]{"netflix","hotstar","spotify","youtube premium","movie","cinema","pvr","gaming"},                                  "Entertainment",   TxType.EXPENSE },
        { new String[]{"flipkart","amazon","myntra","shopping","clothes","shoes","electronics","laptop","mobile"},                        "Shopping",        TxType.EXPENSE },
        { new String[]{"udemy","coursera","college fees","school fees","tuition","books","course"},                                       "Education",       TxType.EXPENSE },
        { new String[]{"salary","payroll","ctc","stipend","wages"},                                                                       "Salary",          TxType.INCOME  },
        { new String[]{"freelance","project payment","client","consulting","invoice"},                                                    "Freelance",       TxType.INCOME  },
        { new String[]{"interest","dividend","mutual fund","stocks","zerodha","groww","returns"},                                         "Investment",      TxType.INCOME  },
        { new String[]{"gift","birthday","bonus","cashback","refund","reward"},                                                           "Gift / Bonus",    TxType.INCOME  },
    };

    public String[] autoSuggest(String description) {
        if (description == null || description.isEmpty()) return null;
        String d = description.toLowerCase();
        for (Object[] rule : CAT_RULES) {
            for (String kw : (String[]) rule[0]) {
                if (d.contains(kw)) {
                    return new String[]{ (String) rule[1], ((TxType) rule[2]).name() };
                }
            }
        }
        return null;
    }

    // ── Password hashing ─────────────────────────────────────────
    public String hashPassword(String pw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return pw; }
    }

    // ── User operations ───────────────────────────────────────────
    public User registerUser(String name, String email, String phone, String password) {
        if (users.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email)))
            throw new IllegalArgumentException("Account already exists with this email.");
        String id = "U" + System.currentTimeMillis();
        User u = new User(id, name, "+91" + phone, email, hashPassword(password), LocalDate.now());
        users.add(u);
        txMap.put(id, new ArrayList<>());
        bankMap.put(id, new ArrayList<>());
        recurMap.put(id, new ArrayList<>());
        return u;
    }

    public User loginUser(String email, String password) {
        String hash = hashPassword(password);
        return users.stream()
            .filter(u -> u.getEmail().equalsIgnoreCase(email) && u.getPasswordHash().equals(hash))
            .findFirst().orElse(null);
    }

    public User getUserById(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }

    // ── Transaction operations ────────────────────────────────────
    public List<Transaction> getTransactions(String userId) {
        return txMap.getOrDefault(userId, new ArrayList<>());
    }

    public Transaction addTransaction(String userId, TxType type, double amount,
                                      String category, String description,
                                      LocalDate date, EntryMode mode, String bankId) {
        List<Transaction> list = txMap.computeIfAbsent(userId, k -> new ArrayList<>());
        int nextId = list.stream().mapToInt(Transaction::getId).max().orElse(0) + 1;
        Transaction tx = new Transaction(nextId, type, amount, category,
                                         description, date, mode, bankId);
        list.add(tx);
        return tx;
    }

    public boolean deleteTransaction(String userId, int txId) {
        List<Transaction> list = txMap.get(userId);
        if (list == null) return false;
        return list.removeIf(t -> t.getId() == txId);
    }

    public double getTotalIncome(String userId) {
        return getTransactions(userId).stream()
            .filter(t -> t.getType() == TxType.INCOME)
            .mapToDouble(Transaction::getAmount).sum();
    }

    public double getTotalExpenses(String userId) {
        return getTransactions(userId).stream()
            .filter(t -> t.getType() == TxType.EXPENSE)
            .mapToDouble(Transaction::getAmount).sum();
    }

    public double getBalance(String userId) {
        return getTotalIncome(userId) - getTotalExpenses(userId);
    }

    // ── Bank account operations ───────────────────────────────────
    public List<BankAccount> getBankAccounts(String userId) {
        return bankMap.getOrDefault(userId, new ArrayList<>());
    }

    public BankAccount addBankAccount(String userId, String bankName, String accountType,
                                      String accountNo, String ifsc, String holder, double balance) {
        List<BankAccount> list = bankMap.computeIfAbsent(userId, k -> new ArrayList<>());
        BankAccount ba = new BankAccount("B" + System.currentTimeMillis(),
            bankName, accountType, accountNo, ifsc, holder, balance, LocalDate.now());
        list.add(ba);
        return ba;
    }

    public boolean deleteBankAccount(String userId, String bankId) {
        List<BankAccount> list = bankMap.get(userId);
        if (list == null) return false;
        return list.removeIf(b -> b.getId().equals(bankId));
    }

    // ── Recurring rule operations ─────────────────────────────────
    public List<RecurringRule> getRecurringRules(String userId) {
        return recurMap.getOrDefault(userId, new ArrayList<>());
    }

    public RecurringRule addRecurringRule(String userId, TxType type, double amount,
                                          String category, String description,
                                          RecurringRule.Frequency frequency, LocalDate nextDue) {
        List<RecurringRule> list = recurMap.computeIfAbsent(userId, k -> new ArrayList<>());
        String id = "R" + String.format("%03d", list.size() + 1);
        RecurringRule rule = new RecurringRule(id, type, amount, category,
                                               description, frequency, nextDue, true);
        list.add(rule);
        return rule;
    }

    public boolean deleteRecurringRule(String userId, String ruleId) {
        List<RecurringRule> list = recurMap.get(userId);
        if (list == null) return false;
        return list.removeIf(r -> r.getId().equals(ruleId));
    }

    public int postDueRecurring(String userId) {
        List<RecurringRule> rules = recurMap.getOrDefault(userId, new ArrayList<>());
        int count = 0;
        for (RecurringRule r : rules) {
            while (r.isDue()) {
                addTransaction(userId, r.getType(), r.getAmount(),
                    r.getCategory(), r.getDescription() + " [Auto]",
                    LocalDate.now(), EntryMode.RECURRING, null);
                r.advanceNextDue();
                count++;
            }
        }
        return count;
    }

    // ── Category summary ──────────────────────────────────────────
    public Map<String, Double> getCategoryExpenses(String userId) {
        Map<String, Double> map = new TreeMap<>();
        getTransactions(userId).stream()
            .filter(t -> t.getType() == TxType.EXPENSE)
            .forEach(t -> map.merge(t.getCategory(), t.getAmount(), Double::sum));
        return map;
    }

    public Map<String, Double> getCategoryIncome(String userId) {
        Map<String, Double> map = new TreeMap<>();
        getTransactions(userId).stream()
            .filter(t -> t.getType() == TxType.INCOME)
            .forEach(t -> map.merge(t.getCategory(), t.getAmount(), Double::sum));
        return map;
    }
}
