# 💰 BudgetTracker — Spring Boot Web App (100% Java)

Personal Finance Manager built entirely in Java using Spring Boot + Thymeleaf.

## Features
- ✨ Sign Up & 🔑 Login with SHA-256 password hashing
- ✏️ Manual transaction entry with 🤖 auto-categorization
- 🏦 Add bank accounts manually (19 Indian banks supported)
- 🔁 Recurring rules (Daily/Weekly/Monthly/Yearly)
- 📋 Transaction history with search & filter
- 📊 Financial reports with category breakdown
- ₹ Indian Rupee support

---

## Run Locally

```bash
# Requires Java 17+ and Maven
mvn spring-boot:run
# Open: http://localhost:8080
```

Or build and run JAR:
```bash
mvn clean package
java -jar target/budget-tracker-1.0.0.jar
```

---

## Deploy on Railway (Free)

### Step 1 — Push to GitHub
```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/budget-tracker.git
git push -u origin main
```

### Step 2 — Deploy on Railway
1. Go to **railway.app** → Sign up with GitHub
2. Click **"New Project"** → **"Deploy from GitHub repo"**
3. Select your `budget-tracker` repo
4. Railway auto-detects Maven → click **Deploy**
5. Go to **Settings → Domains** → Generate Domain
6. Your app is live! 🎉

### Step 3 — Get your live URL
```
https://budget-tracker-production-xxxx.up.railway.app
```

---

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Templates**: Thymeleaf (HTML)
- **Build**: Maven
- **Deploy**: Railway (free)
- **Storage**: In-memory (session-based)
