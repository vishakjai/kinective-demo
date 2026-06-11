package com.meridianbranch.branchdesk.workflow;

import com.meridianbranch.branchdesk.model.Associate;
import com.meridianbranch.branchdesk.model.Role;
import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventListener;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.trace.TraceEmitter;

/**
 * Workflow 6.1 — auth + supervisor attach (Serve). A teller logs in
 * (password / badge / iris); a SUPERVISOR can attach to the active session to
 * approve overrides WITHOUT logging the teller out.
 */
public class AuthWorkflow implements BranchEventListener {

    private final BranchSession session;
    private boolean authenticated = false;

    public AuthWorkflow(BranchSession session) {
        this.session = session;
        session.getService().eventBus().subscribe(this);
    }

    public void login(Associate associate, String method) {
        session.traceCall("authenticate", TraceEmitter.data(
                "associateId", associate.getId(), "method", method));
        session.setAssociate(associate);
        session.getService().authenticate(associate.getId(), method);
    }

    /** A supervisor attaches to approve overrides; the teller stays logged in. */
    public void attachSupervisor(Associate supervisor) {
        if (supervisor.getRole() != Role.SUPERVISOR) {
            throw new IllegalArgumentException("only a SUPERVISOR may attach");
        }
        session.traceCall("attachSupervisor", TraceEmitter.data("supervisorId", supervisor.getId()));
        session.setAttachedSupervisor(supervisor);
        session.getService().attachSupervisor(supervisor.getId());
        session.traceDecision("supervisor_attached", TraceEmitter.data("supervisorId", supervisor.getId()));
    }

    @Override
    public void onEvent(BranchEvent event) {
        if (event.getType() == BranchEventType.AUTH_OK) {
            authenticated = true;
            session.traceEvent("AUTH_OK", TraceEmitter.data("associateId", event.get("associateId")));
        }
    }

    public boolean isAuthenticated() { return authenticated; }
}
