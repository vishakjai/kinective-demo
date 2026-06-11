package com.meridianbranch.branchdesk.workflow;

import com.meridianbranch.branchdesk.trace.TraceEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transaction queue / batch (Serve). The teller batches transactions into a
 * queue and processes them as a group. The queue is held in server-side
 * component state and snapshotted per page version, so navigating back restores
 * the queue as it was on that page.
 */
public class QueueWorkflow {

    private final BranchSession session;
    private final Map<Integer, List<String>> snapshotsByPageVersion = new LinkedHashMap<>();
    private int currentPageVersion = 0;
    private List<String> queue = new ArrayList<>();

    public QueueWorkflow(BranchSession session) {
        this.session = session;
        snapshotsByPageVersion.put(0, new ArrayList<>());
    }

    public void enqueue(String txnId) {
        queue.add(txnId);
        currentPageVersion++;
        snapshotsByPageVersion.put(currentPageVersion, new ArrayList<>(queue));
        session.traceCall("enqueueTransaction", TraceEmitter.data("txnId", txnId));
        session.traceDecision("queue_snapshot", TraceEmitter.data(
                "pageVersion", currentPageVersion,
                "queue", new ArrayList<Object>(queue)));
    }

    public void navigateBack() {
        if (currentPageVersion == 0) {
            return;
        }
        currentPageVersion--;
        queue = new ArrayList<>(snapshotsByPageVersion.get(currentPageVersion));
        session.traceDecision("queue_resumed_snapshot", TraceEmitter.data(
                "pageVersion", currentPageVersion,
                "queue", new ArrayList<Object>(queue)));
    }

    public List<String> getQueue() { return queue; }
    public int getCurrentPageVersion() { return currentPageVersion; }
}
