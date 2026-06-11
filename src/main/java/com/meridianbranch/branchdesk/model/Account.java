package com.meridianbranch.branchdesk.model;

import java.io.Serializable;
import java.math.BigDecimal;

/** A consumer account. availableBalance is the spendable figure the UI shows and
 *  optimistically adjusts; ledgerBalance is the authoritative posted figure. */
public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String number;
    private final String type;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;

    public Account(String number, String type, BigDecimal availableBalance, BigDecimal ledgerBalance) {
        this.number = number;
        this.type = type;
        this.availableBalance = availableBalance;
        this.ledgerBalance = ledgerBalance;
    }

    public String getNumber() { return number; }
    public String getType() { return type; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public BigDecimal getLedgerBalance() { return ledgerBalance; }

    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    public void setLedgerBalance(BigDecimal ledgerBalance) { this.ledgerBalance = ledgerBalance; }
}
