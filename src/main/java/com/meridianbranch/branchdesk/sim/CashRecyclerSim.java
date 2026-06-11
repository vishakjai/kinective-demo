package com.meridianbranch.branchdesk.sim;

/**
 * Deterministic cash-recycler simulator. Can be scripted (per scenario) to fail
 * the NEXT dispense once with a jam/timeout, then recover — driving the
 * DEVICE_ERROR → DEVICE_RECOVERED flow (workflow 6.6). No randomness: the
 * failure is armed explicitly so traces stay reproducible.
 */
public class CashRecyclerSim {

    private boolean armFailureOnNextDispense = false;
    private String lastFault = null;

    /** Arm a single jam/timeout on the next dispense (then auto-recovers). */
    public void armFailure(String fault) {
        this.armFailureOnNextDispense = true;
        this.lastFault = fault;
    }

    /** @return true if this dispense should fault (consumes the armed failure). */
    public boolean shouldFaultThisDispense() {
        if (armFailureOnNextDispense) {
            armFailureOnNextDispense = false;
            return true;
        }
        return false;
    }

    public String getLastFault() {
        return lastFault;
    }
}
