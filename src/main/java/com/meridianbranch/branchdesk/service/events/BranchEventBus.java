package com.meridianbranch.branchdesk.service.events;

import java.util.ArrayList;
import java.util.List;

/**
 * A per-session event bus. The mock service publishes {@link BranchEvent}s; the
 * UI workflow subscribes and branches.
 *
 * Dispatch is SYNCHRONOUS and in listener-registration order — the session is
 * single-threaded and the relative order of events is behaviorally significant
 * to the UI workflows. Synchronous, ordered delivery keeps traces deterministic.
 */
public class BranchEventBus {
    private final List<BranchEventListener> listeners = new ArrayList<>();

    public void subscribe(BranchEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(BranchEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(BranchEvent event) {
        // Copy to tolerate listeners that (un)subscribe during dispatch.
        for (BranchEventListener l : new ArrayList<>(listeners)) {
            l.onEvent(event);
        }
    }
}
