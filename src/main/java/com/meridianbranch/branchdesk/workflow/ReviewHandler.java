package com.meridianbranch.branchdesk.workflow;

import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventListener;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.trace.Face;
import com.meridianbranch.branchdesk.trace.TraceEmitter;

/**
 * Renders supervisor-review state. The teller console surfaces the full review
 * detail so the supervisor can act on it; the customer terminal shows a neutral
 * wait message.
 */
public class ReviewHandler implements BranchEventListener {

    private final BranchSession session;

    public ReviewHandler(BranchSession session) {
        this.session = session;
        session.getService().eventBus().subscribe(this);
    }

    @Override
    public void onEvent(BranchEvent event) {
        if (event.getType() != BranchEventType.SUPERVISOR_REVIEW_REQUIRED) {
            return;
        }
        String reason = String.valueOf(event.get("reason"));
        if (session.getFace() == Face.SERVE) {
            session.traceDecision("supervisor_review_render", TraceEmitter.data(
                    "reasonShown", true,
                    "reason", reason,
                    "message", "Supervisor review required"));
        } else {
            session.traceDecision("supervisor_review_render", TraceEmitter.data(
                    "reasonShown", false,
                    "reason", null,
                    "message", "Please wait for assistance."));
        }
    }
}
