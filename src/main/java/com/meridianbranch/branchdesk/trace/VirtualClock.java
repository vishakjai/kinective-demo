package com.meridianbranch.branchdesk.trace;

/**
 * A deterministic logical clock. Traces must be byte-stable across runs given
 * the same seed, so timestamps NEVER come from wall time — they come from here.
 *
 * Starts at a fixed epoch and advances a fixed step on each {@link #tick()}, so
 * the {@code ts} field of every trace record is reproducible.
 */
public class VirtualClock {
    /** Fixed base epoch (2024-01-01T00:00:00Z in millis). */
    public static final long BASE_EPOCH_MILLIS = 1704067200000L;
    /** Fixed advance per tick (ms). */
    public static final long STEP_MILLIS = 5L;

    private long current;

    public VirtualClock() {
        this.current = BASE_EPOCH_MILLIS;
    }

    /** Advance and return the new logical time. */
    public long tick() {
        long t = current;
        current += STEP_MILLIS;
        return t;
    }

    public long now() { return current; }
}
