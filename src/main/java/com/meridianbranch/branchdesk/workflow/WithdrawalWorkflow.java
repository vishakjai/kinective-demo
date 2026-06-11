package com.meridianbranch.branchdesk.workflow;

import com.meridianbranch.branchdesk.model.Account;
import com.meridianbranch.branchdesk.model.Transaction;
import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventListener;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.trace.TraceEmitter;

import java.math.BigDecimal;

/**
 * Cash withdrawal with optimistic balance (Kiosk + Serve). On DISPENSE_STARTED
 * the displayed available balance is optimistically decremented; on
 * POST_CONFIRMED it is reconciled to the authoritative figure. If a hold is
 * placed while a dispense is in flight the optimistic adjustment is unwound and
 * the UI waits for the authoritative balance instead.
 */
public class WithdrawalWorkflow implements BranchEventListener {

    private final BranchSession session;
    private BigDecimal displayedBalance = BigDecimal.ZERO;
    private boolean decrementApplied = false;
    private boolean optimisticSuppressed = false;
    private boolean posted = false;

    public WithdrawalWorkflow(BranchSession session) {
        this.session = session;
        session.getService().eventBus().subscribe(this);
    }

    public void startWithdrawal(Transaction txn, Account account) {
        displayedBalance = account.getAvailableBalance();
        decrementApplied = false;
        optimisticSuppressed = false;
        posted = false;
        session.traceCall("startTransaction", TraceEmitter.data(
                "txnId", txn.getId(), "type", txn.getType(), "amount", txn.getAmount()));
        session.getService().startTransaction(txn, account);
    }

    public void post(Transaction txn, Account account) {
        session.traceCall("postTransaction", TraceEmitter.data(
                "txnId", txn.getId(), "amount", txn.getAmount()));
        session.getService().postTransaction(txn, account);
    }

    @Override
    public void onEvent(BranchEvent event) {
        switch (event.getType()) {
            case DISPENSE_STARTED: {
                BigDecimal amount = (BigDecimal) event.get("amount");
                session.traceEvent("DISPENSE_STARTED", TraceEmitter.data("amount", amount));
                displayedBalance = displayedBalance.subtract(amount);
                decrementApplied = true;
                session.traceDecision("optimistic_decrement", TraceEmitter.data(
                        "applied", true, "displayedBalance", displayedBalance));
                break;
            }
            case HOLD_PLACED: {
                BigDecimal amount = (BigDecimal) event.get("amount");
                session.traceEvent("HOLD_PLACED", TraceEmitter.data(
                        "amount", amount, "reason", event.get("reason")));
                if (decrementApplied && !posted && !optimisticSuppressed) {
                    displayedBalance = displayedBalance.add(amount);
                    optimisticSuppressed = true;
                    session.traceDecision("optimistic_decrement_suppressed", TraceEmitter.data(
                            "value", true, "displayedBalance", displayedBalance));
                }
                break;
            }
            case POST_CONFIRMED: {
                BigDecimal authoritative = (BigDecimal) event.get("authoritativeBalance");
                posted = true;
                displayedBalance = authoritative;
                session.traceEvent("POST_CONFIRMED", TraceEmitter.data("authoritativeBalance", authoritative));
                session.traceDecision("reconciled", TraceEmitter.data("displayedBalance", displayedBalance));
                break;
            }
            default:
                // ignore
        }
    }

    public BigDecimal getDisplayedBalance() { return displayedBalance; }
    public boolean isOptimisticSuppressed() { return optimisticSuppressed; }
}
