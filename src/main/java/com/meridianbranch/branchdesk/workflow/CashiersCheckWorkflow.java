package com.meridianbranch.branchdesk.workflow;

import com.meridianbranch.branchdesk.service.ScreeningResult;
import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventListener;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.trace.TraceEmitter;

import java.math.BigDecimal;

/**
 * Cashier's check + screening (Serve). The teller selects a payee; the UI
 * screens it and, on the screening result, decides whether the transaction must
 * be held for supervisor review based on the branch's configured review
 * threshold. When review is needed the transaction is blocked behind a teller
 * review state and a supervisor-review event is raised.
 */
public class CashiersCheckWorkflow implements BranchEventListener {

    private final BranchSession session;
    private boolean reviewRequired = false;

    public CashiersCheckWorkflow(BranchSession session) {
        this.session = session;
        session.getService().eventBus().subscribe(this);
    }

    public ScreeningResult selectPayee(String payee) {
        session.traceCall("screenPayee", TraceEmitter.data("payee", payee));
        return session.getService().screenPayee(payee);
    }

    @Override
    public void onEvent(BranchEvent event) {
        if (event.getType() != BranchEventType.SCREENING_RESULT) {
            return;
        }
        BigDecimal confidence = (BigDecimal) event.get("confidence");
        BigDecimal threshold = session.getConfig().getOfacReviewThreshold();

        reviewRequired = confidence.compareTo(threshold) >= 0;

        session.traceEvent("SCREENING_RESULT", TraceEmitter.data(
                "payee", event.get("payee"),
                "confidence", confidence,
                "matchCount", event.get("matchCount")));
        session.traceDecision("review_required", TraceEmitter.data(
                "value", reviewRequired,
                "confidence", confidence,
                "threshold", threshold));

        if (reviewRequired) {
            String reason = "OFAC match confidence " + confidence.toPlainString()
                    + " >= threshold " + threshold.toPlainString();
            session.getService().eventBus().publish(new BranchEvent(
                    BranchEventType.SUPERVISOR_REVIEW_REQUIRED,
                    BranchEvent.data("reason", reason,
                            "payee", event.get("payee"),
                            "confidence", confidence)));
        }
    }

    public boolean isReviewRequired() { return reviewRequired; }
}
