package com.meridianbranch.branchdesk.config;

import com.meridianbranch.branchdesk.model.DeviceMode;

import java.io.Serializable;
import java.math.BigDecimal;

/** Per-branch configuration: thresholds, device mode, dual-control flags. */
public class BranchConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal ofacReviewThreshold = new BigDecimal("0.85");
    private BigDecimal holdThreshold = new BigDecimal("5000.00");
    private DeviceMode deviceMode = DeviceMode.TELLER;
    private boolean dualControlEnabled = true;

    public BranchConfig() { }

    public BigDecimal getOfacReviewThreshold() { return ofacReviewThreshold; }
    public void setOfacReviewThreshold(BigDecimal v) { this.ofacReviewThreshold = v; }

    public BigDecimal getHoldThreshold() { return holdThreshold; }
    public void setHoldThreshold(BigDecimal v) { this.holdThreshold = v; }

    public DeviceMode getDeviceMode() { return deviceMode; }
    public void setDeviceMode(DeviceMode v) { this.deviceMode = v; }

    public boolean isDualControlEnabled() { return dualControlEnabled; }
    public void setDualControlEnabled(boolean v) { this.dualControlEnabled = v; }
}
