package com.meridianbranch.branchdesk;

import com.meridianbranch.branchdesk.kiosk.KioskHomePage;
import com.meridianbranch.branchdesk.teller.ServeConsolePage;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Happy-path WicketTester coverage for the Serve + Kiosk pages. */
class UiPagesTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new BranchDeskApplication());
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    @Test
    void serveScreensCleanPayeeWithoutReview() {
        tester.startPage(ServeConsolePage.class);
        tester.assertRenderedPage(ServeConsolePage.class);

        FormTester form = tester.newFormTester("screenForm");
        form.setValue("payee", "Jane Customer"); // confidence 0.00 — clean
        form.submit();

        tester.assertLabel("reviewStatus", "Screening clear — proceed.");
    }

    @Test
    void kioskWithdrawalShowsReconciledBalance() {
        tester.startPage(KioskHomePage.class);
        tester.assertRenderedPage(KioskHomePage.class);

        FormTester form = tester.newFormTester("withdrawForm");
        form.setValue("amount", "100.00");
        form.submit();

        // 1200.00 starting available − 100.00 withdrawn, reconciled to ledger.
        tester.assertLabel("result", "Dispensed 100.00. Available balance: 1100.00");
    }
}
