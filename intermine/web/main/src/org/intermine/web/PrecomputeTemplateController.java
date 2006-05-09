package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;

/**
 * Controller for the precomputeTemplate.tile
 * 
 * @author Xavier Watkins
 * 
 */
public class PrecomputeTemplateController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        String templateName = (String) context.getAttribute("templateName");
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();

        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        Map templates = profile.getSavedTemplates();
        TemplateQuery template = (TemplateQuery) templates.get(templateName);
        Query query = MainHelper.makeQuery(template.getQuery(), profile.getSavedBags());

        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) servletContext
                .getAttribute(Constants.OBJECTSTORE);

        String precomputedMessage;
        if (session.getAttribute("precomputing_" + templateName) != null
                && session.getAttribute("precomputing_" + templateName).equals("true")) {
            precomputedMessage = "precomputing";
        } else if (os.isPrecomputed(query, "template")) {
            precomputedMessage = "true";
        } else {
            precomputedMessage = "false";
        }
        request.setAttribute("isPrecomputed", precomputedMessage);
        return null;
    }
}
