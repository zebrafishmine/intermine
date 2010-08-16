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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.DisplayObjectFactory;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controls display of data in results tables.
 *
 * @author Julie Sullivan
 */
public class ObjectViewController extends TilesAction
{

    protected static final Logger LOG = Logger.getLogger(ObjectDetailsController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStore os = im.getObjectStore();
        DisplayObjectFactory displayObjects = SessionMethods.getDisplayObjects(session);

        String idString = (String) context.getAttribute("id");
        String fieldName = (String) context.getAttribute("fieldName");
        fieldName = fieldName.trim();

        Integer id = new Integer(Integer.parseInt(idString));
        InterMineObject object = os.getObjectById(id);
        if (object == null) {
            return null;
        }

        DisplayObject dobj = displayObjects.get(object);
        FieldConfig fc = dobj.getFieldConfigMap().get(fieldName);
        // truncate fields by default, unless it says otherwise in config
        boolean doNotTruncate = false;
        if (fc != null) {
            doNotTruncate = fc.getDoNotTruncate();
        }
        request.setAttribute("doNotTruncate", doNotTruncate);
        request.setAttribute("fieldName", fieldName);
        return null;
    }
}
