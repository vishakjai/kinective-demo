package com.meridianbranch.branchdesk.model;

import java.io.Serializable;
import java.math.BigDecimal;

/** A single branch transaction. {@code payee} is set for cashier's checks. */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final TransactionType type;
    private final BigDecimal amount;
    private TransactionStatus status;
    private String payee;

    public Transaction(String id, TransactionType type, BigDecimal amount) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.status = TransactionStatus.STARTED;
    }

    public String getId() { return id; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getPayee() { return payee; }
    public void setPayee(String payee) { this.payee = payee; }
}
