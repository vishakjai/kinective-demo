package com.meridianbranch.branchdesk;

import org.apache.wicket.markup.html.basic.Label;

/**
 * Landing page — the face selector. A teller or customer is routed to the Serve
 * or Kiosk shell from here.
 */
public class HomePage extends BasePage {
    private static final long serialVersionUID = 1L;

    public HomePage() {
        add(new Label("appName", "BranchDesk"));
        add(new Label("vendor", "Meridian Branch Systems"));
    }
}
