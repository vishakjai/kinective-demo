package com.meridianbranch.branchdesk;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

/**
 * Landing page — the face selector. A real teller/customer is routed to the
 * Serve or Kiosk shell from here. Kept deliberately plain (scaffold phase).
 */
public class HomePage extends WebPage {
    private static final long serialVersionUID = 1L;

    public HomePage() {
        add(new Label("appName", "BranchDesk"));
        add(new Label("vendor", "Meridian Branch Systems"));
    }
}
