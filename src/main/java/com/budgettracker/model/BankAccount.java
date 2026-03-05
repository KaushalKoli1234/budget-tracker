package com.budgettracker.model;

import java.time.LocalDate;

public class BankAccount {

    private String id;
    private String bankName;
    private String accountType;
    private String accountNo;
    private String ifsc;
    private String holder;
    private double balance;
    private LocalDate addedOn;

    public BankAccount() {}

    public BankAccount(String id, String bankName, String accountType,
                       String accountNo, String ifsc, String holder,
                       double balance, LocalDate addedOn) {
        this.id = id; this.bankName = bankName; this.accountType = accountType;
        this.accountNo = accountNo; this.ifsc = ifsc.toUpperCase();
        this.holder = holder; this.balance = balance; this.addedOn = addedOn;
    }

    public String getId()           { return id; }
    public String getBankName()     { return bankName; }
    public String getAccountType()  { return accountType; }
    public String getAccountNo()    { return accountNo; }
    public String getIfsc()         { return ifsc; }
    public String getHolder()       { return holder; }
    public double getBalance()      { return balance; }
    public LocalDate getAddedOn()   { return addedOn; }

    public void setId(String id)              { this.id = id; }
    public void setBankName(String n)         { this.bankName = n; }
    public void setAccountType(String t)      { this.accountType = t; }
    public void setAccountNo(String n)        { this.accountNo = n; }
    public void setIfsc(String i)             { this.ifsc = i; }
    public void setHolder(String h)           { this.holder = h; }
    public void setBalance(double b)          { this.balance = b; }
    public void setAddedOn(LocalDate d)       { this.addedOn = d; }

    public String getMask() {
        if (accountNo == null || accountNo.length() < 4) return "****";
        return "****" + accountNo.substring(accountNo.length() - 4);
    }

    public String getBalanceFormatted() {
        return String.format("%,.2f", balance);
    }
}
