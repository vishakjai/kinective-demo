package com.meridianbranch.branchdesk.teller;

import com.meridianbranch.branchdesk.config.BranchConfig;
import com.meridianbranch.branchdesk.service.MockBranchOperationService;
import com.meridianbranch.branchdesk.trace.Face;
import com.meridianbranch.branchdesk.trace.TraceEmitter;
import com.meridianbranch.branchdesk.trace.TraceRecord;
import com.meridianbranch.branchdesk.workflow.BranchSession;
import com.meridianbranch.branchdesk.workflow.CashiersCheckWorkflow;
import com.meridianbranch.branchdesk.workflow.ReviewHandler;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Serve — the teller console. Screens a cashier's-check payee and surfaces the
 * supervisor-review state, including the review reason (the teller-facing view).
 */
public class ServeConsolePage extends WebPage {
    private static final long serialVersionUID = 1L;

    private final IModel<String> payee = Model.of("");
    private final IModel<String> reviewStatus = Model.of("");

    public ServeConsolePage() {
        add(new Label("appName", "BranchDesk — Serve"));

        Form<Void> form = new Form<Void>("screenForm") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onSubmit() {
                screen(payee.getObject());
            }
        };
        form.add(new TextField<>("payee", payee));
        add(form);

        add(new Label("reviewStatus", reviewStatus));
    }

    private void screen(String payeeName) {
        BranchConfig config = new BranchConfig();
        MockBranchOperationService service = new MockBranchOperationService(config);
        TraceEmitter trace = new TraceEmitter();
        BranchSession serve = new BranchSession(Face.SERVE, service, config, trace);
        new ReviewHandler(serve);
        CashiersCheckWorkflow cashiers = new CashiersCheckWorkflow(serve);

        cashiers.selectPayee(payeeName);

        if (cashiers.isReviewRequired()) {
            String reason = lastReviewReason(trace);
            reviewStatus.setObject("Supervisor review required — " + reason);
        } else {
            reviewStatus.setObject("Screening clear — proceed.");
        }
    }

    private static String lastReviewReason(TraceEmitter trace) {
        String reason = "";
        for (TraceRecord r : trace.getRecords()) {
            if ("decision".equals(r.getKind())
                    && "supervisor_review_render".equals(r.getName())
                    && r.getFace() == Face.SERVE) {
                Object reasonValue = r.getData().get("reason");
                reason = reasonValue == null ? "" : String.valueOf(reasonValue);
            }
        }
        return reason;
    }
}
