package com.meridianbranch.branchdesk.service.events;

/** The asynchronous events the backend emits onto a session's bus. */
public enum BranchEventType {
    AUTH_OK,
    CONSUMER_IDENTIFIED,
    DISPENSE_STARTED,
    DISPENSE_COMPLETE,
    POST_CONFIRMED,
    HOLD_PLACED,
    SCREENING_RESULT,
    SUPERVISOR_REVIEW_REQUIRED,
    DEVICE_ERROR,
    DEVICE_RECOVERED,
    AUDIT_REQUIRED
}
