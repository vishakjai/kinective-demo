package com.meridianbranch.branchdesk.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** A customer and their accounts. */
public class Consumer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final List<Account> accounts = new ArrayList<>();

    public Consumer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<Account> getAccounts() { return accounts; }

    public Account primaryAccount() {
        return accounts.isEmpty() ? null : accounts.get(0);
    }
}
