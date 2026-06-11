package com.meridianbranch.branchdesk.trace;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The behavioral trace oracle. Records, for one session:
 *   - each FE→service call            (kind = "call")
 *   - each BranchEvent received        (kind = "event")
 *   - each UI state transition / branch decision (kind = "decision")
 * each tagged with the {@link Face} that produced it.
 *
 * {@code seq} is a monotonic counter; {@code ts} comes from a {@link VirtualClock}
 * (NOT wall time) so the JSON-lines output is byte-stable across runs given the
 * same scenario + seed. This is the oracle the migration BEL diffs against.
 */
public class TraceEmitter {
    private final List<TraceRecord> records = new ArrayList<>();
    private final VirtualClock clock;
    private long seq = 0;

    public TraceEmitter() {
        this(new VirtualClock());
    }

    public TraceEmitter(VirtualClock clock) {
        this.clock = clock;
    }

    private TraceRecord add(Face face, String kind, String name, Map<String, Object> data) {
        TraceRecord r = new TraceRecord(
                seq++, face, kind, name,
                data == null ? new LinkedHashMap<>() : data,
                clock.tick());
        records.add(r);
        return r;
    }

    /** Record an FE→service call (name + salient args). */
    public TraceRecord call(Face face, String name, Map<String, Object> data) {
        return add(face, "call", name, data);
    }

    /** Record a BranchEvent received by the UI. */
    public TraceRecord event(Face face, String name, Map<String, Object> data) {
        return add(face, "event", name, data);
    }

    /** Record a UI state transition / branch decision. */
    public TraceRecord decision(Face face, String name, Map<String, Object> data) {
        return add(face, "decision", name, data);
    }

    public List<TraceRecord> getRecords() {
        return records;
    }

    /** The full trace as newline-terminated JSON lines (trailing newline). */
    public String toJsonLines() {
        StringBuilder sb = new StringBuilder();
        for (TraceRecord r : records) {
            sb.append(r.toJsonLine()).append('\n');
        }
        return sb.toString();
    }

    /** Write the trace to a file as deterministic UTF-8 JSON-lines. */
    public void writeTo(Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write(toJsonLines());
        }
    }

    /** Convenience for building a single-record data map in insertion order. */
    public static Map<String, Object> data(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
