package com.meridianbranch.branchdesk.service.events;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** An event on the per-session bus. {@code data} carries salient payload in
 *  deterministic insertion order. */
public class BranchEvent {
    private final BranchEventType type;
    private final Map<String, Object> data;

    public BranchEvent(BranchEventType type, Map<String, Object> data) {
        this.type = type;
        this.data = data == null ? Collections.emptyMap() : data;
    }

    public BranchEventType getType() { return type; }
    public Map<String, Object> getData() { return data; }

    public Object get(String key) { return data.get(key); }

    public static Map<String, Object> data(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
