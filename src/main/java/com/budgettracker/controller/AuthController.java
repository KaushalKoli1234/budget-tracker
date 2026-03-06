package com.budgettracker.controller;

import com.budgettracker.model.User;
import com.budgettracker.service.BudgetService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@Controller
public class AuthController {

    @Autowired
    private BudgetService budgetService;

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("userId") != null) return "redirect:/dashboard";
        return "redirect:/login";
    }

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

    @GetMapping("/signup")
    public String signupPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) return "redirect:/dashboard";
        model.addAttribute("page", "signup");
        return "auth";
    }

    // ── Send OTP ──────────────────────────────────────────────────
    @PostMapping("/send-otp")
    @ResponseBody
    public java.util.Map<String, String> sendOtp(@RequestParam String phone,
                                                  HttpSession session) {
        if (!phone.matches("[0-9]{10}")) {
            return java.util.Map.of("status", "error", "message", "Enter a valid 10-digit mobile number.");
        }
        String otp = String.format("%06d", new Random().nextInt(999999));
        session.setAttribute("pendingOtp",   otp);
        session.setAttribute("pendingPhone", phone);
        // In production: send SMS via Twilio/MSG91
        // For demo: return OTP in response
        System.out.println("OTP for " + phone + " : " + otp);
        return java.util.Map.of("status", "success", "otp", otp,
            "message", "OTP sent! (Demo mode — OTP shown below)");
    }

    // ── Verify OTP ────────────────────────────────────────────────
    @PostMapping("/verify-otp")
    @ResponseBody
    public java.util.Map<String, String> verifyOtp(@RequestParam String otp,
                                                    HttpSession session) {
        String saved = (String) session.getAttribute("pendingOtp");
        if (saved == null)
            return java.util.Map.of("status", "error", "message", "OTP expired. Please resend.");
        if (!saved.equals(otp.trim()))
            return java.util.Map.of("status", "error", "message", "Incorrect OTP. Try again.");
        session.setAttribute("otpVerified", true);
        return java.util.Map.of("status", "success", "message", "Mobile verified!");
    }

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               HttpSession session, Model model) {
        // Check OTP verified
        Boolean verified = (Boolean) session.getAttribute("otpVerified");
        if (verified == null || !verified) {
            model.addAttribute("page",  "signup");
            model.addAttribute("error", "Please verify your mobile number with OTP first.");
            return "auth";
        }
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
            session.removeAttribute("otpVerified");
            session.removeAttribute("pendingOtp");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("page",  "signup");
            model.addAttribute("error", e.getMessage());
            return "auth";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
