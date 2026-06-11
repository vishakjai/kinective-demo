package com.meridianbranch.branchdesk;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Happy-path WicketTester smoke test. Mirrors the team's posture: happy paths
 * automated, full regression performed manually before release.
 */
class HomePageTest {

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
    void homePageRenders() {
        tester.startPage(HomePage.class);
        tester.assertRenderedPage(HomePage.class);
        tester.assertLabel("appName", "BranchDesk");
        tester.assertLabel("vendor", "Meridian Branch Systems");
    }
}
