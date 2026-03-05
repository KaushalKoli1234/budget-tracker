package com.budgettracker.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Transaction {

    public enum TxType    { INCOME, EXPENSE }
    public enum EntryMode { MANUAL, AUTO, BANK, RECURRING }

    private int       id;
    private TxType    type;
    private double    amount;
    private String    category;
    private String    description;
    private LocalDate date;
    private EntryMode mode;
    private String    bankId;

    public Transaction() {}

    public Transaction(int id, TxType type, double amount, String category,
                       String description, LocalDate date, EntryMode mode, String bankId) {
        this.id = id; this.type = type; this.amount = amount;
        this.category = category; this.description = description;
        this.date = date; this.mode = mode; this.bankId = bankId;
    }

    public int       getId()          { return id; }
    public TxType    getType()        { return type; }
    public double    getAmount()      { return amount; }
    public String    getCategory()    { return category; }
    public String    getDescription() { return description; }
    public LocalDate getDate()        { return date; }
    public EntryMode getMode()        { return mode; }
    public String    getBankId()      { return bankId; }

    public void setId(int id)                { this.id = id; }
    public void setType(TxType t)            { this.type = t; }
    public void setAmount(double a)          { this.amount = a; }
    public void setCategory(String c)        { this.category = c; }
    public void setDescription(String d)     { this.description = d; }
    public void setDate(LocalDate d)         { this.date = d; }
    public void setMode(EntryMode m)         { this.mode = m; }
    public void setBankId(String b)          { this.bankId = b; }

    public boolean isIncome()   { return type == TxType.INCOME; }
    public boolean isFromBank() { return mode == EntryMode.BANK; }

    public String getFmtDate() {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "";
    }

    public String getAmountFormatted() {
        return String.format("%,.2f", amount);
    }
}
