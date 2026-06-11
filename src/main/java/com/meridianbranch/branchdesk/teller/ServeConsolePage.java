package com.meridianbranch.branchdesk.teller;

import com.meridianbranch.branchdesk.BasePage;
import com.meridianbranch.branchdesk.config.BranchConfig;
import com.meridianbranch.branchdesk.service.MockBranchOperationService;
import com.meridianbranch.branchdesk.trace.Face;
import com.meridianbranch.branchdesk.trace.TraceEmitter;
import com.meridianbranch.branchdesk.trace.TraceRecord;
import com.meridianbranch.branchdesk.workflow.BranchSession;
import com.meridianbranch.branchdesk.workflow.CashiersCheckWorkflow;
import com.meridianbranch.branchdesk.workflow.ReviewHandler;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Serve — the teller console. Screens a cashier's-check payee and surfaces the
 * supervisor-review state, including the review reason (the teller-facing view).
 */
public class ServeConsolePage extends BasePage {
    private static final long serialVersionUID = 1L;

    private final IModel<String> payee = Model.of("");
    private final IModel<String> reviewStatus = Model.of("Enter a payee and screen.");
    private final IModel<String> statusClass = Model.of("bd-status");

    public ServeConsolePage() {
        Form<Void> form = new Form<Void>("screenForm") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onSubmit() {
                screen(payee.getObject());
            }
        };
        form.add(new TextField<>("payee", payee));
        add(form);

        Label status = new Label("reviewStatus", reviewStatus);
        status.add(AttributeModifier.replace("class", statusClass));
        status.setOutputMarkupId(true);
        add(status);
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
            reviewStatus.setObject("Supervisor review required — " + lastReviewReason(trace));
            statusClass.setObject("bd-status bd-warn");
        } else {
            reviewStatus.setObject("Screening clear — proceed.");
            statusClass.setObject("bd-status bd-ok");
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
