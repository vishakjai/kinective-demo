package com.meridianbranch.branchdesk.workflow;

import com.meridianbranch.branchdesk.config.BranchConfig;
import com.meridianbranch.branchdesk.model.Associate;
import com.meridianbranch.branchdesk.model.Consumer;
import com.meridianbranch.branchdesk.service.BranchOperationService;
import com.meridianbranch.branchdesk.trace.Face;
import com.meridianbranch.branchdesk.trace.TraceEmitter;

import java.util.Map;

/**
 * Per-session FE-side state shared by the workflow coordinators on one face.
 * Holds the service handle, the trace emitter, the branch config, and the
 * current associate/consumer/supervisor. Trace helpers auto-tag {@link #face}.
 *
 * The workflow coordinators (one per workflow 6.1–6.7) hang off this session;
 * they hold the FE-side orchestration where most of the interesting branch
 * behavior lives.
 */
public class BranchSession {

    private final Face face;
    private final BranchOperationService service;
    private final TraceEmitter trace;
    private final BranchConfig config;

    private Associate associate;
    private Associate attachedSupervisor;
    private Consumer currentConsumer;

    public BranchSession(Face face, BranchOperationService service, BranchConfig config, TraceEmitter trace) {
        this.face = face;
        this.service = service;
        this.config = config;
        this.trace = trace;
    }

    public Face getFace() { return face; }
    public BranchOperationService getService() { return service; }
    public TraceEmitter getTrace() { return trace; }
    public BranchConfig getConfig() { return config; }

    public Associate getAssociate() { return associate; }
    public void setAssociate(Associate a) { this.associate = a; }
    public Associate getAttachedSupervisor() { return attachedSupervisor; }
    public void setAttachedSupervisor(Associate s) { this.attachedSupervisor = s; }
    public boolean hasSupervisorAttached() { return attachedSupervisor != null; }
    public Consumer getCurrentConsumer() { return currentConsumer; }
    public void setCurrentConsumer(Consumer c) { this.currentConsumer = c; }

    // ── face-tagged trace helpers ──────────────────────────────────────
    public void traceCall(String name, Map<String, Object> data) { trace.call(face, name, data); }
    public void traceEvent(String name, Map<String, Object> data) { trace.event(face, name, data); }
    public void traceDecision(String name, Map<String, Object> data) { trace.decision(face, name, data); }
}
