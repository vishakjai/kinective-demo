package com.meridianbranch.branchdesk;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * Common base page — contributes the shared BranchDesk stylesheet to every
 * page's head. The stylesheet is a packaged resource (served by Wicket) so it
 * works regardless of the servlet filter mapping.
 */
public abstract class BasePage extends WebPage {
    private static final long serialVersionUID = 1L;

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(BasePage.class, "branchdesk.css")));
    }
}
