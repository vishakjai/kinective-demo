package com.meridianbranch.branchdesk.sim;

import com.meridianbranch.branchdesk.model.Account;
import com.meridianbranch.branchdesk.model.Consumer;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deterministic core-banking simulator: a fixed set of consumers/accounts and
 * authoritative posting. No randomness — balances are reproducible from this
 * seed so traces are stable.
 */
public class CoreBankingSim {

    private final Map<String, Consumer> consumersById = new LinkedHashMap<>();

    public CoreBankingSim() {
        Consumer jane = new Consumer("C-1001", "Jane Customer");
        jane.getAccounts().add(new Account("0001-CHK", "CHECKING",
                new BigDecimal("1200.00"), new BigDecimal("1200.00")));
        jane.getAccounts().add(new Account("0001-SAV", "SAVINGS",
                new BigDecimal("8000.00"), new BigDecimal("8000.00")));
        consumersById.put(jane.getId(), jane);

        Consumer acme = new Consumer("C-2002", "Acme Payroll LLC");
        acme.getAccounts().add(new Account("0002-CHK", "CHECKING",
                new BigDecimal("54250.00"), new BigDecimal("54250.00")));
        consumersById.put(acme.getId(), acme);
    }

    public Consumer findConsumer(String consumerId) {
        return consumersById.get(consumerId);
    }

    /** Find an account by number across all consumers (or null). */
    public Account findAccount(String accountNumber) {
        for (Consumer c : consumersById.values()) {
            for (Account a : c.getAccounts()) {
                if (a.getNumber().equals(accountNumber)) {
                    return a;
                }
            }
        }
        return null;
    }

    /** Authoritatively post a debit to an account's ledger; returns the new ledger balance. */
    public BigDecimal postDebit(Account account, BigDecimal amount) {
        BigDecimal newLedger = account.getLedgerBalance().subtract(amount);
        account.setLedgerBalance(newLedger);
        account.setAvailableBalance(newLedger);
        return newLedger;
    }

    /** Authoritatively post a credit (deposit) to an account's ledger. */
    public BigDecimal postCredit(Account account, BigDecimal amount) {
        BigDecimal newLedger = account.getLedgerBalance().add(amount);
        account.setLedgerBalance(newLedger);
        return newLedger;
    }
}
