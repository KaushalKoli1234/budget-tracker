package com.budgettracker.controller;

import com.budgettracker.model.User;
import com.budgettracker.service.BudgetService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private BudgetService budgetService;

    // ── Landing page → redirect to login ─────────────────────────
    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("userId") != null) return "redirect:/dashboard";
        return "redirect:/login";
    }

    // ── Login page ────────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) return "redirect:/dashboard";
        model.addAttribute("page", "login");
        return "auth";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session, Model model) {
        User user = budgetService.loginUser(email, password);
        if (user == null) {
            model.addAttribute("page",  "login");
            model.addAttribute("error", "Invalid email or password.");
            return "auth";
        }
        session.setAttribute("userId",   user.getId());
        session.setAttribute("userName", user.getName());
        return "redirect:/dashboard";
    }

    // ── Sign-Up page ──────────────────────────────────────────────
    @GetMapping("/signup")
    public String signupPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) return "redirect:/dashboard";
        model.addAttribute("page", "signup");
        return "auth";
    }

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               HttpSession session, Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("page",  "signup");
            model.addAttribute("error", "Passwords do not match.");
            return "auth";
        }
        if (password.length() < 6) {
            model.addAttribute("page",  "signup");
            model.addAttribute("error", "Password must be at least 6 characters.");
            return "auth";
        }
        try {
            User user = budgetService.registerUser(name, email, phone, password);
            session.setAttribute("userId",   user.getId());
            session.setAttribute("userName", user.getName());
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("page",  "signup");
            model.addAttribute("error", e.getMessage());
            return "auth";
        }
    }

    // ── Logout ────────────────────────────────────────────────────
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
