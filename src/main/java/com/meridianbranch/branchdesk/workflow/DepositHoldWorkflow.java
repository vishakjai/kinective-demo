package com.meridianbranch.branchdesk.workflow;

import com.meridianbranch.branchdesk.model.Account;
import com.meridianbranch.branchdesk.model.Transaction;
import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventListener;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.trace.Face;
import com.meridianbranch.branchdesk.trace.TraceEmitter;

/**
 * Deposit hold (Kiosk + Serve). A check deposit over the hold threshold places a
 * hold. The teller console shows the hold detail and reason; the customer
 * terminal shows a neutral funds-availability notice.
 */
public class DepositHoldWorkflow implements BranchEventListener {

    private final BranchSession session;

    public DepositHoldWorkflow(BranchSession session) {
        this.session = session;
        session.getService().eventBus().subscribe(this);
    }

    public void deposit(Transaction txn, Account account) {
        session.traceCall("postTransaction", TraceEmitter.data(
                "txnId", txn.getId(), "type", txn.getType(), "amount", txn.getAmount()));
        session.getService().postTransaction(txn, account);
    }

    @Override
    public void onEvent(BranchEvent event) {
        if (event.getType() != BranchEventType.HOLD_PLACED) {
            return;
        }
        session.traceEvent("HOLD_PLACED", TraceEmitter.data(
                "amount", event.get("amount"), "reason", event.get("reason")));
        if (session.getFace() == Face.SERVE) {
            session.traceDecision("hold_render", TraceEmitter.data(
                    "reasonShown", true,
                    "reason", event.get("reason"),
                    "message", "Hold placed"));
        } else {
            session.traceDecision("hold_render", TraceEmitter.data(
                    "reasonShown", false,
                    "reason", null,
                    "message", "Funds availability notice"));
        }
    }
}
