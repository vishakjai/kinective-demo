package com.meridianbranch.branchdesk.service;

import com.meridianbranch.branchdesk.config.BranchConfig;
import com.meridianbranch.branchdesk.model.Account;
import com.meridianbranch.branchdesk.model.Consumer;
import com.meridianbranch.branchdesk.model.DeviceMode;
import com.meridianbranch.branchdesk.model.Transaction;
import com.meridianbranch.branchdesk.model.TransactionType;
import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventBus;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.sim.CashRecyclerSim;
import com.meridianbranch.branchdesk.sim.CoreBankingSim;
import com.meridianbranch.branchdesk.sim.ScreeningSim;

import java.math.BigDecimal;

/**
 * Deterministic, in-process implementation. Emits asynchronous {@link BranchEvent}s
 * onto the per-session bus the UI subscribes to. No real hardware, no real core
 * banking, no randomness — everything is reproducible from the seeded simulators
 * so traces are byte-stable.
 *
 * NOTE: the OFAC review decision (confidence vs threshold) and the optimistic
 * balance behavior live in the FE-side workflow layer, NOT here — this service
 * only surfaces SCREENING_RESULT / DISPENSE_STARTED / HOLD_PLACED / POST_CONFIRMED.
 * That is faithful to the real product (the FE branches on backend events).
 */
public class MockBranchOperationService implements BranchOperationService {

    private final BranchEventBus bus = new BranchEventBus();
    private final BranchConfig config;
    private final ScreeningSim screeningSim;
    private final CoreBankingSim coreSim;
    private final CashRecyclerSim recyclerSim;

    public MockBranchOperationService(BranchConfig config) {
        this(config, new ScreeningSim(), new CoreBankingSim(), new CashRecyclerSim());
    }

    public MockBranchOperationService(BranchConfig config, ScreeningSim screeningSim,
                                      CoreBankingSim coreSim, CashRecyclerSim recyclerSim) {
        this.config = config;
        this.screeningSim = screeningSim;
        this.coreSim = coreSim;
        this.recyclerSim = recyclerSim;
    }

    @Override
    public BranchEventBus eventBus() { return bus; }

    public BranchConfig getConfig() { return config; }
    public ScreeningSim getScreeningSim() { return screeningSim; }
    public CoreBankingSim getCoreSim() { return coreSim; }
    public CashRecyclerSim getRecyclerSim() { return recyclerSim; }

    @Override
    public void authenticate(String associateId, String method) {
        bus.publish(new BranchEvent(BranchEventType.AUTH_OK,
                BranchEvent.data("associateId", associateId, "method", method)));
    }

    @Override
    public void attachSupervisor(String supervisorId) {
        // Supervisor attach is a session change with no distinct backend event;
        // the override-approval modal is driven by SUPERVISOR_REVIEW_REQUIRED.
    }

    @Override
    public Consumer identifyConsumer(String consumerId, String method) {
        Consumer c = coreSim.findConsumer(consumerId);
        if (c != null) {
            bus.publish(new BranchEvent(BranchEventType.CONSUMER_IDENTIFIED,
                    BranchEvent.data("consumerId", c.getId(), "name", c.getName(), "method", method)));
        }
        return c;
    }

    @Override
    public void startTransaction(Transaction txn, Account account) {
        if (txn.getType() == TransactionType.CASH_WITHDRAWAL) {
            if (recyclerSim.shouldFaultThisDispense()) {
                bus.publish(new BranchEvent(BranchEventType.DEVICE_ERROR,
                        BranchEvent.data("txnId", txn.getId(), "fault", recyclerSim.getLastFault())));
                bus.publish(new BranchEvent(BranchEventType.DEVICE_RECOVERED,
                        BranchEvent.data("txnId", txn.getId())));
            }
            bus.publish(new BranchEvent(BranchEventType.DISPENSE_STARTED,
                    BranchEvent.data("txnId", txn.getId(), "amount", txn.getAmount())));
        }
    }

    @Override
    public ScreeningResult screenPayee(String payee) {
        ScreeningResult result = screeningSim.screen(payee);
        bus.publish(new BranchEvent(BranchEventType.SCREENING_RESULT,
                BranchEvent.data("payee", payee,
                        "confidence", result.getConfidence(),
                        "matchCount", result.getMatches().size())));
        return result;
    }

    @Override
    public void postTransaction(Transaction txn, Account account) {
        boolean isDeposit = txn.getType() == TransactionType.CASH_DEPOSIT
                || txn.getType() == TransactionType.CHECK_DEPOSIT;

        if (isDeposit && txn.getAmount().compareTo(config.getHoldThreshold()) > 0) {
            bus.publish(new BranchEvent(BranchEventType.HOLD_PLACED,
                    BranchEvent.data("txnId", txn.getId(),
                            "amount", txn.getAmount(),
                            "reason", "DEPOSIT_OVER_HOLD_THRESHOLD")));
        }

        BigDecimal authoritative;
        if (isDeposit) {
            authoritative = coreSim.postCredit(account, txn.getAmount());
        } else {
            authoritative = coreSim.postDebit(account, txn.getAmount());
        }
        bus.publish(new BranchEvent(BranchEventType.POST_CONFIRMED,
                BranchEvent.data("txnId", txn.getId(),
                        "authoritativeBalance", authoritative)));
    }

    @Override
    public void auditCashInventory() {
        bus.publish(new BranchEvent(BranchEventType.AUDIT_REQUIRED,
                BranchEvent.data("deviceMode", config.getDeviceMode())));
    }

    @Override
    public void setDeviceMode(DeviceMode mode) {
        config.setDeviceMode(mode);
    }
}
