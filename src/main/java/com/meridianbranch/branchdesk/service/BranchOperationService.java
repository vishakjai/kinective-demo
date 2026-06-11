package com.meridianbranch.branchdesk.service;

import com.meridianbranch.branchdesk.model.Account;
import com.meridianbranch.branchdesk.model.Consumer;
import com.meridianbranch.branchdesk.model.DeviceMode;
import com.meridianbranch.branchdesk.model.Transaction;
import com.meridianbranch.branchdesk.service.events.BranchEventBus;

/**
 * The coarse backend the UI calls. The interesting behavior lives in how the
 * frontend orchestrates these calls, subscribes to the {@link BranchEventBus},
 * and branches on the events — not in this interface. Deliberately NOT a
 * REST-friendly facade: a single in-process abstraction, the way the legacy app
 * actually talked to its backend.
 */
public interface BranchOperationService {

    /** The per-session event bus the UI subscribes to. */
    BranchEventBus eventBus();

    /** Authenticate an associate (password / badge / iris). Emits AUTH_OK. */
    void authenticate(String associateId, String method);

    /** Attach a supervisor to the active session to approve overrides. */
    void attachSupervisor(String supervisorId);

    /** Identify a consumer and pull their profile. Emits CONSUMER_IDENTIFIED. */
    Consumer identifyConsumer(String consumerId, String method);

    /** Begin a transaction. For cash withdrawals, emits DISPENSE_STARTED
     *  (or DEVICE_ERROR → DEVICE_RECOVERED when the recycler is armed to fault). */
    void startTransaction(Transaction txn, Account account);

    /** Screen a cashier's-check payee against OFAC. Emits SCREENING_RESULT. */
    ScreeningResult screenPayee(String payee);

    /** Post the transaction to core. Emits HOLD_PLACED (over hold threshold) then
     *  POST_CONFIRMED with the authoritative balance. */
    void postTransaction(Transaction txn, Account account);

    /** Begin a cash-inventory audit. Emits AUDIT_REQUIRED. */
    void auditCashInventory();

    /** Switch device mode (TELLER / KIOSK / AFTER_HOURS). */
    void setDeviceMode(DeviceMode mode);
}
