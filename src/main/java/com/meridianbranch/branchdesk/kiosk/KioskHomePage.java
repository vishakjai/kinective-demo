package com.meridianbranch.branchdesk.kiosk;

import com.meridianbranch.branchdesk.BasePage;
import com.meridianbranch.branchdesk.config.BranchConfig;
import com.meridianbranch.branchdesk.model.Account;
import com.meridianbranch.branchdesk.model.Consumer;
import com.meridianbranch.branchdesk.model.DeviceMode;
import com.meridianbranch.branchdesk.model.Transaction;
import com.meridianbranch.branchdesk.model.TransactionType;
import com.meridianbranch.branchdesk.service.MockBranchOperationService;
import com.meridianbranch.branchdesk.trace.Face;
import com.meridianbranch.branchdesk.trace.TraceEmitter;
import com.meridianbranch.branchdesk.workflow.BranchSession;
import com.meridianbranch.branchdesk.workflow.WithdrawalWorkflow;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.math.BigDecimal;

/**
 * Kiosk — the customer self-service terminal. Performs a cash withdrawal and
 * shows the available balance after the dispense is reconciled to the
 * authoritative figure (the customer-facing view).
 */
public class KioskHomePage extends BasePage {
    private static final long serialVersionUID = 1L;

    private final IModel<String> amount = Model.of("100.00");
    private final IModel<String> result = Model.of("Enter an amount to withdraw.");
    private final IModel<String> resultClass = Model.of("bd-status");

    public KioskHomePage() {
        Form<Void> form = new Form<Void>("withdrawForm") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onSubmit() {
                withdraw(amount.getObject());
            }
        };
        form.add(new TextField<>("amount", amount));
        add(form);

        Label resultLabel = new Label("result", result);
        resultLabel.add(AttributeModifier.replace("class", resultClass));
        add(resultLabel);
    }

    private void withdraw(String amountText) {
        BranchConfig config = new BranchConfig();
        config.setDeviceMode(DeviceMode.KIOSK);
        MockBranchOperationService service = new MockBranchOperationService(config);
        TraceEmitter trace = new TraceEmitter();
        BranchSession kiosk = new BranchSession(Face.KIOSK, service, config, trace);
        WithdrawalWorkflow withdrawal = new WithdrawalWorkflow(kiosk);

        Consumer consumer = service.identifyConsumer("C-1001", "barcode");
        Account account = consumer.primaryAccount();
        Transaction txn = new Transaction("T-UI", TransactionType.CASH_WITHDRAWAL, new BigDecimal(amountText));

        withdrawal.startWithdrawal(txn, account);
        withdrawal.post(txn, account);

        result.setObject("Dispensed " + new BigDecimal(amountText).toPlainString()
                + ". Available balance: " + withdrawal.getDisplayedBalance().toPlainString());
        resultClass.setObject("bd-status bd-ok");
    }
}
