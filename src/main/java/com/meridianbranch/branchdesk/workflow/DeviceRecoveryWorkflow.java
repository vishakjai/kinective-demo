package com.meridianbranch.branchdesk.workflow;

import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventListener;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.trace.Face;
import com.meridianbranch.branchdesk.trace.TraceEmitter;

/**
 * Device error recovery (Kiosk + Serve). On a device error the teller console
 * shows a recovery panel with operator actions; the customer terminal shows a
 * wait message. On recovery, both resume the interrupted step.
 */
public class DeviceRecoveryWorkflow implements BranchEventListener {

    private final BranchSession session;
    private boolean inRecovery = false;

    public DeviceRecoveryWorkflow(BranchSession session) {
        this.session = session;
        session.getService().eventBus().subscribe(this);
    }

    @Override
    public void onEvent(BranchEvent event) {
        if (event.getType() == BranchEventType.DEVICE_ERROR) {
            inRecovery = true;
            session.traceEvent("DEVICE_ERROR", TraceEmitter.data("fault", event.get("fault")));
            if (session.getFace() == Face.SERVE) {
                session.traceDecision("device_error_render", TraceEmitter.data(
                        "operatorActions", true, "message", "Recovery required"));
            } else {
                session.traceDecision("device_error_render", TraceEmitter.data(
                        "operatorActions", false, "message", "Please wait for assistance."));
            }
        } else if (event.getType() == BranchEventType.DEVICE_RECOVERED) {
            inRecovery = false;
            session.traceEvent("DEVICE_RECOVERED", TraceEmitter.data());
            session.traceDecision("resume_interrupted_step", TraceEmitter.data("resumed", true));
        }
    }

    public boolean isInRecovery() { return inRecovery; }
}
