package com.meridianbranch.branchdesk;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.Page;

/**
 * BranchDesk Wicket application — the single {@link WebApplication} behind both
 * faces (Serve teller console and Kiosk customer terminal). Pages are mounted in
 * {@link #init()}; the deterministic in-process {@code BranchOperationService}
 * and the seedable device/screening/core simulators are wired here as the build
 * progresses (Phase 2 of the build brief).
 */
public class BranchDeskApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    @Override
    protected void init() {
        super.init();
        // Stateful, server-rendered: rely on Wicket page versioning (page maps).
        getMarkupSettings().setStripWicketTags(true);

        // Page mounts are added per-face as the workflows land:
        //   mountPage("/serve", com.meridianbranch.branchdesk.teller.ServeConsolePage.class);
        //   mountPage("/kiosk", com.meridianbranch.branchdesk.kiosk.KioskHomePage.class);
    }
}
