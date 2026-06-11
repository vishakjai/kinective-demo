package com.meridianbranch.branchdesk.trace;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One behavioral trace record: {@code { "seq", "face", "kind", "name", "data", "ts" }}.
 *
 * This is the SHARED schema the migration's Vue+REST build must reproduce
 * exactly — the BEL diffs source traces against target traces field-by-field.
 * Key order in the serialized object is fixed (seq, face, kind, name, data, ts).
 */
public class TraceRecord {
    private final long seq;
    private final Face face;
    private final String kind;   // "call" | "event" | "decision"
    private final String name;
    private final Map<String, Object> data;
    private final long ts;

    public TraceRecord(long seq, Face face, String kind, String name, Map<String, Object> data, long ts) {
        this.seq = seq;
        this.face = face;
        this.kind = kind;
        this.name = name;
        this.data = data;
        this.ts = ts;
    }

    public long getSeq() { return seq; }
    public Face getFace() { return face; }
    public String getKind() { return kind; }
    public String getName() { return name; }
    public Map<String, Object> getData() { return data; }
    public long getTs() { return ts; }

    /** Serialize to a single JSON line with a FIXED key order. */
    public String toJsonLine() {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("seq", seq);
        obj.put("face", face.name());
        obj.put("kind", kind);
        obj.put("name", name);
        obj.put("data", data);
        obj.put("ts", ts);
        return Json.write(obj);
    }
}
