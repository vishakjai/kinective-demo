package com.meridianbranch.branchdesk.service.events;

/** A UI subscriber to a session's event bus. */
public interface BranchEventListener {
    void onEvent(BranchEvent event);
}
