package com.meridianbranch.branchdesk.sim;

import com.meridianbranch.branchdesk.service.ScreeningResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deterministic OFAC screening simulator backed by a fixture table. The table
 * spans the range around the default review threshold — a payee at 0.85, one
 * just above, and one just below — so the screening branch can be exercised at
 * the boundary. Unknown payees screen clean (0.00).
 */
public class ScreeningSim {

    private final Map<String, ScreeningResult> fixtures = new LinkedHashMap<>();

    public ScreeningSim() {
        // payee -> {matches, confidence}
        put("Boris Ivanov", Arrays.asList("IVANOV, Boris"), "0.97");        // clear hit
        put("Maria Sanctn", Arrays.asList("SANCTN, Maria"), "0.86");        // just above 0.85
        put("Exactly Eighty Five", Arrays.asList("FIVE, Exactly Eighty"), "0.85"); // on the threshold
        put("Almost Match", Arrays.asList("MATCH, Almost"), "0.84");        // just below 0.85
        put("Jane Customer", Collections.<String>emptyList(), "0.00");      // clean
        put("Acme Payroll LLC", Collections.<String>emptyList(), "0.12");   // clean
    }

    private void put(String payee, java.util.List<String> matches, String confidence) {
        fixtures.put(payee, new ScreeningResult(matches, new BigDecimal(confidence)));
    }

    public ScreeningResult screen(String payee) {
        ScreeningResult r = fixtures.get(payee);
        if (r != null) {
            return r;
        }
        // Unknown payees screen clean.
        return new ScreeningResult(Collections.<String>emptyList(), new BigDecimal("0.00"));
    }
}
