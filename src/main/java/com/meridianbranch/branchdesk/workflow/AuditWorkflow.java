package com.meridianbranch.branchdesk.workflow;

import com.meridianbranch.branchdesk.model.DeviceMode;
import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventListener;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.trace.TraceEmitter;

/**
 * Cash inventory audit (Serve). The audit confirmation is gated by branch
 * configuration and the current device mode.
 */
public class AuditWorkflow implements BranchEventListener {

    private final BranchSession session;
    private boolean dualControlRequired = true;

    public AuditWorkflow(BranchSession session) {
        this.session = session;
        session.getService().eventBus().subscribe(this);
    }

    public void startAudit() {
        session.traceCall("auditCashInventory", TraceEmitter.data());
        session.getService().auditCashInventory();
    }

    @Override
    public void onEvent(BranchEvent event) {
        if (event.getType() != BranchEventType.AUDIT_REQUIRED) {
            return;
        }
        DeviceMode mode = session.getConfig().getDeviceMode();
        session.traceEvent("AUDIT_REQUIRED", TraceEmitter.data("deviceMode", mode));

        if (mode == DeviceMode.AFTER_HOURS) {
            dualControlRequired = false;
            session.traceDecision("dual_control", TraceEmitter.data(
                    "required", false, "skipped", true, "reason", "AFTER_HOURS"));
        } else {
            dualControlRequired = true;
            session.traceDecision("dual_control", TraceEmitter.data(
                    "required", true, "skipped", false));
        }
    }

    public boolean isDualControlRequired() { return dualControlRequired; }
}
