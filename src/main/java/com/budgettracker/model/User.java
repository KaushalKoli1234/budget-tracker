package com.budgettracker.model;

import java.time.LocalDate;

public class User {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private LocalDate joinDate;

    public User() {}

    public User(String id, String name, String email, String phone,
                String passwordHash, LocalDate joinDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.joinDate = joinDate;
    }

    public String getId()            { return id; }
    public String getName()          { return name; }
    public String getEmail()         { return email; }
    public String getPhone()         { return phone; }
    public String getPasswordHash()  { return passwordHash; }
    public LocalDate getJoinDate()   { return joinDate; }

    public void setId(String id)                   { this.id = id; }
    public void setName(String name)               { this.name = name; }
    public void setEmail(String email)             { this.email = email; }
    public void setPhone(String phone)             { this.phone = phone; }
    public void setPasswordHash(String h)          { this.passwordHash = h; }
    public void setJoinDate(LocalDate d)           { this.joinDate = d; }
}
