package com.meridianbranch.branchdesk.model;

/** Operating mode of the branch device. AFTER_HOURS relaxes some dual-control steps. */
public enum DeviceMode {
    TELLER,
    KIOSK,
    AFTER_HOURS
}
