package com.meridianbranch.branchdesk.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Result of an OFAC screening: the matched sanctioned names + a confidence. */
public class ScreeningResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<String> matches;
    private final BigDecimal confidence;

    public ScreeningResult(List<String> matches, BigDecimal confidence) {
        this.matches = matches == null ? new ArrayList<>() : matches;
        this.confidence = confidence;
    }

    public List<String> getMatches() { return matches; }
    public BigDecimal getConfidence() { return confidence; }
}
