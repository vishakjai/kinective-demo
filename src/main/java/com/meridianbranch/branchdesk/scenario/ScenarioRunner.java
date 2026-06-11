package com.meridianbranch.branchdesk.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridianbranch.branchdesk.config.BranchConfig;
import com.meridianbranch.branchdesk.model.Account;
import com.meridianbranch.branchdesk.model.Associate;
import com.meridianbranch.branchdesk.model.DeviceMode;
import com.meridianbranch.branchdesk.model.Role;
import com.meridianbranch.branchdesk.model.Transaction;
import com.meridianbranch.branchdesk.model.TransactionType;
import com.meridianbranch.branchdesk.service.MockBranchOperationService;
import com.meridianbranch.branchdesk.service.events.BranchEvent;
import com.meridianbranch.branchdesk.service.events.BranchEventType;
import com.meridianbranch.branchdesk.trace.Face;
import com.meridianbranch.branchdesk.trace.TraceEmitter;
import com.meridianbranch.branchdesk.workflow.AuditWorkflow;
import com.meridianbranch.branchdesk.workflow.AuthWorkflow;
import com.meridianbranch.branchdesk.workflow.BranchSession;
import com.meridianbranch.branchdesk.workflow.CashiersCheckWorkflow;
import com.meridianbranch.branchdesk.workflow.DepositHoldWorkflow;
import com.meridianbranch.branchdesk.workflow.DeviceRecoveryWorkflow;
import com.meridianbranch.branchdesk.workflow.QueueWorkflow;
import com.meridianbranch.branchdesk.workflow.ReviewHandler;
import com.meridianbranch.branchdesk.workflow.WithdrawalWorkflow;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Drives a {@link Scenario} headlessly through the workflow layer and produces
 * the behavioral trace. Workflow coordinators are created lazily per face on
 * first use, so a scenario only activates the workflows it touches (avoiding
 * overlapping event handlers). The emitted trace is byte-stable given the same
 * scenario + seed — it is the oracle the migration BEL diffs against.
 */
public class ScenarioRunner {

    private final ObjectMapper mapper = new ObjectMapper();

    public Scenario load(Path scenarioFile) throws IOException {
        return mapper.readValue(Files.newBufferedReader(scenarioFile, StandardCharsets.UTF_8), Scenario.class);
    }

    /** Run a scenario and return the populated trace emitter. */
    public TraceEmitter run(Scenario scenario) {
        BranchConfig config = buildConfig(scenario.config);
        MockBranchOperationService service = new MockBranchOperationService(config);
        TraceEmitter trace = new TraceEmitter();
        Ctx ctx = new Ctx(service, config, trace);

        for (Map<String, Object> step : scenario.steps) {
            dispatch(ctx, step);
        }
        return trace;
    }

    /** Load + run a scenario file, writing traces/&lt;name&gt;.jsonl under tracesDir. */
    public Path runToFile(Path scenarioFile, Path tracesDir) throws IOException {
        Scenario scenario = load(scenarioFile);
        TraceEmitter trace = run(scenario);
        Path out = tracesDir.resolve(scenario.scenario + ".jsonl");
        trace.writeTo(out);
        return out;
    }

    // ── dispatch ────────────────────────────────────────────────────────
    private void dispatch(Ctx ctx, Map<String, Object> step) {
        String action = str(step, "action");
        Face face = parseFace(str(step, "face"));
        switch (action) {
            case "login":
                ctx.auth(face).login(new Associate(str(step, "associateId"),
                        str(step, "name"), Role.valueOf(str(step, "role"))), str(step, "method"));
                break;
            case "attachSupervisor":
                ctx.auth(face).attachSupervisor(new Associate(str(step, "supervisorId"),
                        str(step, "name"), Role.SUPERVISOR));
                break;
            case "identifyConsumer":
                ctx.session(face).traceCall("identifyConsumer", TraceEmitter.data(
                        "consumerId", str(step, "consumerId"), "method", str(step, "method")));
                ctx.session(face).setCurrentConsumer(
                        ctx.service.identifyConsumer(str(step, "consumerId"), str(step, "method")));
                break;
            case "setDeviceMode":
                ctx.session(face).traceCall("setDeviceMode", TraceEmitter.data("mode", str(step, "mode")));
                ctx.service.setDeviceMode(DeviceMode.valueOf(str(step, "mode")));
                break;
            case "activateReview":
                ctx.review(face);
                break;
            case "activateDeviceRecovery":
                ctx.deviceRecovery(face);
                break;
            case "screenPayee":
                ctx.cashiersCheck(face).selectPayee(str(step, "payee"));
                break;
            case "startWithdrawal":
                ctx.withdrawal(face).startWithdrawal(
                        new Transaction(str(step, "txnId"), TransactionType.CASH_WITHDRAWAL, dec(step, "amount")),
                        account(ctx, str(step, "account")));
                break;
            case "postWithdrawal":
                ctx.withdrawal(face).post(
                        new Transaction(str(step, "txnId"), TransactionType.CASH_WITHDRAWAL, dec(step, "amount")),
                        account(ctx, str(step, "account")));
                break;
            case "deposit":
                ctx.depositHold(face).deposit(
                        new Transaction(str(step, "txnId"),
                                TransactionType.valueOf(strOr(step, "type", "CHECK_DEPOSIT")), dec(step, "amount")),
                        account(ctx, str(step, "account")));
                break;
            case "enqueue":
                ctx.queue(face).enqueue(str(step, "txnId"));
                break;
            case "navigateBack":
                ctx.queue(face).navigateBack();
                break;
            case "auditCashInventory":
                ctx.audit(face).startAudit();
                break;
            case "armRecyclerFault":
                ctx.service.getRecyclerSim().armFailure(strOr(step, "fault", "JAM"));
                break;
            case "injectEvent":
                injectEvent(ctx, step);
                break;
            default:
                throw new IllegalArgumentException("unknown scenario action: " + action);
        }
    }

    private void injectEvent(Ctx ctx, Map<String, Object> step) {
        BranchEventType type = BranchEventType.valueOf(str(step, "type"));
        Map<String, Object> data = TraceEmitter.data();
        if (step.containsKey("amount")) data.put("amount", dec(step, "amount"));
        if (step.containsKey("reason")) data.put("reason", str(step, "reason"));
        if (step.containsKey("authoritativeBalance")) data.put("authoritativeBalance", dec(step, "authoritativeBalance"));
        ctx.service.eventBus().publish(new BranchEvent(type, data));
    }

    private static Account account(Ctx ctx, String number) {
        Account a = ctx.service.getCoreSim().findAccount(number);
        if (a == null) {
            throw new IllegalArgumentException("unknown account in scenario: " + number);
        }
        return a;
    }

    private BranchConfig buildConfig(Map<String, String> cfg) {
        BranchConfig c = new BranchConfig();
        if (cfg.containsKey("ofacReviewThreshold")) c.setOfacReviewThreshold(new BigDecimal(cfg.get("ofacReviewThreshold")));
        if (cfg.containsKey("holdThreshold")) c.setHoldThreshold(new BigDecimal(cfg.get("holdThreshold")));
        if (cfg.containsKey("deviceMode")) c.setDeviceMode(DeviceMode.valueOf(cfg.get("deviceMode")));
        if (cfg.containsKey("dualControlEnabled")) c.setDualControlEnabled(Boolean.parseBoolean(cfg.get("dualControlEnabled")));
        return c;
    }

    private static Face parseFace(String f) {
        return f == null ? Face.SERVE : Face.valueOf(f.toUpperCase());
    }

    private static String str(Map<String, Object> step, String key) {
        Object v = step.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String strOr(Map<String, Object> step, String key, String dflt) {
        String v = str(step, key);
        return v == null ? dflt : v;
    }

    private static BigDecimal dec(Map<String, Object> step, String key) {
        return new BigDecimal(str(step, key));
    }

    /** Per-run context: shared service/config/trace + lazily-created workflows. */
    private static final class Ctx {
        final MockBranchOperationService service;
        final BranchConfig config;
        final TraceEmitter trace;
        final Map<Face, BranchSession> sessions = new EnumMap<>(Face.class);
        final Map<Face, AuthWorkflow> auths = new EnumMap<>(Face.class);
        final Map<Face, CashiersCheckWorkflow> cashiers = new EnumMap<>(Face.class);
        final Map<Face, ReviewHandler> reviews = new EnumMap<>(Face.class);
        final Map<Face, WithdrawalWorkflow> withdrawals = new EnumMap<>(Face.class);
        final Map<Face, DepositHoldWorkflow> deposits = new EnumMap<>(Face.class);
        final Map<Face, QueueWorkflow> queues = new EnumMap<>(Face.class);
        final Map<Face, AuditWorkflow> audits = new EnumMap<>(Face.class);
        final Map<Face, DeviceRecoveryWorkflow> recoveries = new EnumMap<>(Face.class);

        Ctx(MockBranchOperationService service, BranchConfig config, TraceEmitter trace) {
            this.service = service;
            this.config = config;
            this.trace = trace;
        }

        BranchSession session(Face f) {
            return sessions.computeIfAbsent(f, x -> new BranchSession(x, service, config, trace));
        }
        AuthWorkflow auth(Face f) { return auths.computeIfAbsent(f, x -> new AuthWorkflow(session(x))); }
        CashiersCheckWorkflow cashiersCheck(Face f) { return cashiers.computeIfAbsent(f, x -> new CashiersCheckWorkflow(session(x))); }
        ReviewHandler review(Face f) { return reviews.computeIfAbsent(f, x -> new ReviewHandler(session(x))); }
        WithdrawalWorkflow withdrawal(Face f) { return withdrawals.computeIfAbsent(f, x -> new WithdrawalWorkflow(session(x))); }
        DepositHoldWorkflow depositHold(Face f) { return deposits.computeIfAbsent(f, x -> new DepositHoldWorkflow(session(x))); }
        QueueWorkflow queue(Face f) { return queues.computeIfAbsent(f, x -> new QueueWorkflow(session(x))); }
        AuditWorkflow audit(Face f) { return audits.computeIfAbsent(f, x -> new AuditWorkflow(session(x))); }
        DeviceRecoveryWorkflow deviceRecovery(Face f) { return recoveries.computeIfAbsent(f, x -> new DeviceRecoveryWorkflow(session(x))); }
    }
}
