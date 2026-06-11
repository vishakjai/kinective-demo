package com.meridianbranch.branchdesk.scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deterministic replay script (parsed from {@code scenarios/*.json}). The same
 * scenario format is replayed against the migrated Vue+REST build, so it is a
 * framework-agnostic contract: a config block + an ordered list of steps, each
 * step a {@code {face, action, ...params}} map.
 */
public class Scenario {
    public String scenario;
    public Map<String, String> config = new LinkedHashMap<>();
    public List<Map<String, Object>> steps = new ArrayList<>();
}
