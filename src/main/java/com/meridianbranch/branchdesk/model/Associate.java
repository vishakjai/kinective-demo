package com.meridianbranch.branchdesk.model;

import java.io.Serializable;

/** A teller or supervisor. */
public class Associate implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final Role role;

    public Associate(String id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }
}
