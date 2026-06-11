package com.meridianbranch.branchdesk;

import com.meridianbranch.branchdesk.config.BranchConfig;
import com.meridianbranch.branchdesk.model.Account;
import com.meridianbranch.branchdesk.model.Transaction;
import com.meridianbranch.branchdesk.model.TransactionType;
import com.meridianbranch.branchdesk.service.MockBranchOperationService;
import com.meridianbranch.branchdesk.service.ScreeningResult;
import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.trace.Face;
import com.meridianbranch.branchdesk.trace.TraceEmitter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.meridianbranch.branchdesk.trace.TraceEmitter.data;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 2 — deterministic service core + seeded simulators + byte-stable traces. */
class Phase2CoreTest {

    private List<BranchEvent> capture(MockBranchOperationService svc) {
        List<BranchEvent> events = new ArrayList<>();
        svc.eventBus().subscribe(events::add);
        return events;
    }

    @Test
    void screeningHasExactThresholdPayee() {
        MockBranchOperationService svc = new MockBranchOperationService(new BranchConfig());
        List<BranchEvent> events = capture(svc);

        ScreeningResult r = svc.screenPayee("Exactly Eighty Five");

        // The fixture payee sits on the default 0.85 review threshold.
        assertEquals(0, r.getConfidence().compareTo(new BigDecimal("0.85")));
        assertEquals(1, events.size());
        assertEquals(BranchEventType.SCREENING_RESULT, events.get(0).getType());
        assertEquals(new BigDecimal("0.85"), events.get(0).get("confidence"));
    }

    @Test
    void withdrawalEmitsDispenseStarted() {
        MockBranchOperationService svc = new MockBranchOperationService(new BranchConfig());
        List<BranchEvent> events = capture(svc);

        Account acct = svc.getCoreSim().findConsumer("C-1001").primaryAccount();
        Transaction txn = new Transaction("T-1", TransactionType.CASH_WITHDRAWAL, new BigDecimal("200.00"));
        svc.startTransaction(txn, acct);

        assertEquals(1, events.size());
        assertEquals(BranchEventType.DISPENSE_STARTED, events.get(0).getType());
    }

    @Test
    void postWithdrawalReconcilesAuthoritativeBalance() {
        MockBranchOperationService svc = new MockBranchOperationService(new BranchConfig());
        List<BranchEvent> events = capture(svc);

        Account acct = svc.getCoreSim().findConsumer("C-1001").primaryAccount(); // 1200.00
        Transaction txn = new Transaction("T-2", TransactionType.CASH_WITHDRAWAL, new BigDecimal("200.00"));
        svc.postTransaction(txn, acct);

        assertEquals(1, events.size());
        assertEquals(BranchEventType.POST_CONFIRMED, events.get(0).getType());
        assertEquals(new BigDecimal("1000.00"), events.get(0).get("authoritativeBalance"));
    }

    @Test
    void depositOverHoldThresholdEmitsHoldThenPost() {
        MockBranchOperationService svc = new MockBranchOperationService(new BranchConfig()); // holdThreshold 5000
        List<BranchEvent> events = capture(svc);

        Account acct = svc.getCoreSim().findConsumer("C-2002").primaryAccount();
        Transaction txn = new Transaction("T-3", TransactionType.CHECK_DEPOSIT, new BigDecimal("6000.00"));
        svc.postTransaction(txn, acct);

        assertEquals(2, events.size());
        assertEquals(BranchEventType.HOLD_PLACED, events.get(0).getType());
        assertEquals(BranchEventType.POST_CONFIRMED, events.get(1).getType());
    }

    @Test
    void armedRecyclerFaultEmitsErrorThenRecoveredThenDispense() {
        MockBranchOperationService svc = new MockBranchOperationService(new BranchConfig());
        List<BranchEvent> events = capture(svc);
        svc.getRecyclerSim().armFailure("JAM");

        Account acct = svc.getCoreSim().findConsumer("C-1001").primaryAccount();
        svc.startTransaction(new Transaction("T-4", TransactionType.CASH_WITHDRAWAL, new BigDecimal("50.00")), acct);

        assertEquals(3, events.size());
        assertEquals(BranchEventType.DEVICE_ERROR, events.get(0).getType());
        assertEquals(BranchEventType.DEVICE_RECOVERED, events.get(1).getType());
        assertEquals(BranchEventType.DISPENSE_STARTED, events.get(2).getType());
    }

    @Test
    void traceOutputIsByteStableAndSchemaExact() {
        String produced = buildSampleTrace();

        String expected =
            "{\"seq\":0,\"face\":\"SERVE\",\"kind\":\"call\",\"name\":\"screenPayee\",\"data\":{\"payee\":\"Exactly Eighty Five\"},\"ts\":1704067200000}\n"
          + "{\"seq\":1,\"face\":\"SERVE\",\"kind\":\"event\",\"name\":\"SCREENING_RESULT\",\"data\":{\"confidence\":0.85},\"ts\":1704067200005}\n"
          + "{\"seq\":2,\"face\":\"SERVE\",\"kind\":\"decision\",\"name\":\"review_required\",\"data\":{\"value\":true},\"ts\":1704067200010}\n";

        assertEquals(expected, produced);

        // Byte-stable: a second identical build produces identical bytes.
        assertEquals(produced, buildSampleTrace());
        assertTrue(produced.endsWith("\n"));
    }

    private String buildSampleTrace() {
        TraceEmitter t = new TraceEmitter();
        t.call(Face.SERVE, "screenPayee", data("payee", "Exactly Eighty Five"));
        t.event(Face.SERVE, "SCREENING_RESULT", data("confidence", new BigDecimal("0.85")));
        t.decision(Face.SERVE, "review_required", data("value", true));
        return t.toJsonLines();
    }
}
