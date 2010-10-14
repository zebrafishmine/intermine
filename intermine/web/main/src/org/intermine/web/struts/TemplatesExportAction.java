package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.search.Scope;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.XmlUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;

/**
 * Exports templates to XML.
 *
 * @author Thomas Riley
 */
public class TemplatesExportAction extends TemplateAction
{
    /**
     * {@inheritDoc}
     * @param mapping not used
     * @param form not used
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Profile profile = SessionMethods.getProfile(session);
        String name = request.getParameter("name");
        String scope = request.getParameter("scope");
        String originalTemplate = request.getParameter("originalTemplate");

        String xml = null;

        TemplateManager templateManager = im.getTemplateManager();
        if (name == null) {
            if (scope == null || scope.equals(Scope.USER)) {
                xml = TemplateHelper.templateMapToXml(profile.getSavedTemplates(),
                        PathQuery.USERPROFILE_VERSION);
            } else if (scope.equals(Scope.GLOBAL)) {
                xml = TemplateHelper.templateMapToXml(templateManager.getGlobalTemplates(),
                        PathQuery.USERPROFILE_VERSION);
            } else {
                throw new IllegalArgumentException("Cannot export all templates for scope "
                                                   + scope);
            }
        } else {
            TemplateQuery template = (originalTemplate != null)
                                     ? templateManager.getTemplate(profile, name, scope)
                                     : (TemplateQuery) SessionMethods.getQuery(session);
            if (template != null) {
                xml = template.toXml(PathQuery.USERPROFILE_VERSION);
            } else {
                throw new IllegalArgumentException("Cannot find template " + name + " in context "
                        + scope);
            }
        }
        xml = XmlUtil.indentXmlSimple(xml);

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=template-queries.xml");

        PrintStream out = new PrintStream(response.getOutputStream());
        out.print(xml);
        out.flush();
        return null;
    }
}
