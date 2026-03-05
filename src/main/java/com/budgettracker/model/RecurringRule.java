package com.budgettracker.model;

import java.time.LocalDate;

public class RecurringRule {

    public enum Frequency { DAILY, WEEKLY, MONTHLY, YEARLY }

    private String    id;
    private Transaction.TxType type;
    private double    amount;
    private String    category;
    private String    description;
    private Frequency frequency;
    private LocalDate nextDue;
    private boolean   active;

    public RecurringRule() {}

    public RecurringRule(String id, Transaction.TxType type, double amount,
                         String category, String description,
                         Frequency frequency, LocalDate nextDue, boolean active) {
        this.id = id; this.type = type; this.amount = amount;
        this.category = category; this.description = description;
        this.frequency = frequency; this.nextDue = nextDue; this.active = active;
    }

    public String              getId()          { return id; }
    public Transaction.TxType  getType()        { return type; }
    public double              getAmount()      { return amount; }
    public String              getCategory()    { return category; }
    public String              getDescription() { return description; }
    public Frequency           getFrequency()   { return frequency; }
    public LocalDate           getNextDue()     { return nextDue; }
    public boolean             isActive()       { return active; }

    public void setId(String id)                    { this.id = id; }
    public void setType(Transaction.TxType t)       { this.type = t; }
    public void setAmount(double a)                 { this.amount = a; }
    public void setCategory(String c)               { this.category = c; }
    public void setDescription(String d)            { this.description = d; }
    public void setFrequency(Frequency f)           { this.frequency = f; }
    public void setNextDue(LocalDate d)             { this.nextDue = d; }
    public void setActive(boolean a)                { this.active = a; }

    public boolean isDue() {
        return active && !LocalDate.now().isBefore(nextDue);
    }

    public void advanceNextDue() {
        switch (frequency) {
            case DAILY:   nextDue = nextDue.plusDays(1);   break;
            case WEEKLY:  nextDue = nextDue.plusWeeks(1);  break;
            case MONTHLY: nextDue = nextDue.plusMonths(1); break;
            case YEARLY:  nextDue = nextDue.plusYears(1);  break;
        }
    }

    public String getAmountFormatted() {
        return String.format("%,.2f", amount);
    }
}
