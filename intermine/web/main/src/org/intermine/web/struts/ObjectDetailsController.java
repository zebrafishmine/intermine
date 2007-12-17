package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.ObjectStoreBagsForObject;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.results.DisplayCollection;
import org.intermine.web.logic.results.DisplayField;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.DisplayReference;
import org.intermine.web.logic.results.DisplayType;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagTypes;

/**
 * Implementation of <strong>Action</strong> that assembles data for viewing an object.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class ObjectDetailsController extends InterMineAction
{

    protected static final Logger LOG = Logger.getLogger(ObjectDetailsController.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Map<Integer, DisplayObject> displayObjects = SessionMethods.getDisplayObjects(session);

        String idString = request.getParameter("id");

        Integer id = new Integer(Integer.parseInt(idString));
        InterMineObject object = os.getObjectById(id);
        if (object == null) {
            return null;
        }

        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        String superuser = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);

        DisplayObject dobj = displayObjects.get(id);
        if (dobj == null) {
            dobj = makeDisplayObject(session, object);
            displayObjects.put(id, dobj);
        }
        request.setAttribute("object", dobj);

        if (session.getAttribute(Constants.PORTAL_QUERY_FLAG) != null) {
            session.removeAttribute(Constants.PORTAL_QUERY_FLAG);
            setVerboseCollections(session, dobj);
        }

        Map<String, Map> placementRefsAndCollections = new TreeMap<String, Map>();
        Set aspects = new HashSet((Set) servletContext.getAttribute(Constants.CATEGORIES));

        Map placementMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        placementRefsAndCollections.put("placement:summary", placementMap);

        Set cds = os.getModel().getClassDescriptorsForClass(dobj.getObject().getClass());

        Iterator cdIter = cds.iterator();

        while (cdIter.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) cdIter.next();

            // get all placement:summary tags for all refs and collections of this class
            List placementTags =
                new ArrayList(pm.getTags("placement:summary", cd.getUnqualifiedName()
                                         + ".%", "reference", superuser));
            placementTags.addAll(pm.getTags("placement:summary", cd.getUnqualifiedName() + ".%",
                                            "collection", superuser));

            Iterator placementTagIter = placementTags.iterator();

            while (placementTagIter.hasNext()) {
                Tag tag = (Tag) placementTagIter.next();

                String objectIdentifier = tag.getObjectIdentifier();
                int dotIndex = objectIdentifier.indexOf(".");
                String fieldName = objectIdentifier.substring(dotIndex + 1);

                placementMap.put(fieldName, dobj.getRefsAndCollections().get(fieldName));
            }

        }

        for (Iterator i = aspects.iterator(); i.hasNext(); ) {
            String aspect = (String) i.next();
            placementRefsAndCollections.put(AspectController.ASPECT_PREFIX + aspect,
                                            new TreeMap(String.CASE_INSENSITIVE_ORDER));
        }

        Map miscRefs = new TreeMap(dobj.getRefsAndCollections());
        placementRefsAndCollections.put(SearchRepository.MISC, miscRefs);

        for (Iterator iter = dobj.getRefsAndCollections().entrySet().iterator();
            iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            DisplayField df = (DisplayField) entry.getValue();
            if (df instanceof DisplayReference) {
                categoriseBasedOnTags(((DisplayReference) df).getDescriptor(), "reference",
                                      df, miscRefs, pm, superuser, placementRefsAndCollections);
            } else if (df instanceof DisplayCollection) {
                categoriseBasedOnTags(((DisplayCollection) df).getDescriptor(), "collection",
                                      df, miscRefs, pm, superuser, placementRefsAndCollections);
            }
        }

        String publicBagsWithThisObject = getBags(os, session, servletContext, id, true);
        String myBagsWithThisObject = getBags(os, session, servletContext, id, false);

        request.setAttribute("bagsWithThisObject", publicBagsWithThisObject
                             + ((publicBagsWithThisObject.length() != 0
                               && myBagsWithThisObject.length() != 0) ? "," :"")
                             + myBagsWithThisObject);
        request.setAttribute("placementRefsAndCollections", placementRefsAndCollections);

        setAttributesForDisplayType(request, session, object);

        return null;
    }

    /**
     * Set request attributes, that are required for correct displaying which aspects will be
     * opened at page.
     * @param request request
     * @param session session
     * @param object object for which is page displayed
     */
    private void setAttributesForDisplayType(HttpServletRequest request, HttpSession session,
                                            InterMineObject object) {
        DisplayType displayType = (DisplayType) session.getAttribute(Constants.DISPLAY_TYPE);
        if (displayType == null) {
            displayType = new DisplayType();
            session.setAttribute(Constants.DISPLAY_TYPE, displayType);
        }
        Set<Class> cls = DynamicUtil.decomposeClass(object.getClass());
        String type = null;
        for (Class class1 : cls) {
            type =  class1.getCanonicalName();
        }
        Set<String> asps = displayType.getOpenedAspects(type);
        request.setAttribute("objectType", type);
        request.setAttribute("openedAspectIds", asps);
    }

    /**
     * For a given FieldDescriptor, look up its 'aspect:' tags and place it in the correct
     * map within placementRefsAndCollections. If categorised, remove it from the supplied
     * miscRefs map.
     *
     * @param fd the FieldDecriptor (a references or collection)
     * @param taggedType 'reference' or 'collection'
     * @param dispRef the corresponding DisplayReference or DisplayCollection
     * @param miscRefs map that contains dispRef (may be removed by this method)
     * @param pm the ProfileManager
     * @param sup the superuser account name
     * @param placementRefsAndCollections take from the DisplayObject
     */
    public static void categoriseBasedOnTags(FieldDescriptor fd,
                                             String taggedType,
                                             DisplayField dispRef,
                                             Map miscRefs,
                                             ProfileManager pm,
                                             String sup,
                                             Map<String, Map> placementRefsAndCollections) {
        List tags = pm.getTags(null, fd.getClassDescriptor().getUnqualifiedName() + "."
                               + fd.getName(), taggedType, sup);
        for (Iterator ti = tags.iterator(); ti.hasNext(); ) {
            Tag tag = (Tag) ti.next();
            String tagName = tag.getTagName();
            if (tagName.startsWith(AspectController.ASPECT_PREFIX)) {
                Map<String, DisplayField> refs = placementRefsAndCollections.get(tagName);
                if (refs != null) {
                    refs.put(fd.getName(), dispRef);
                    miscRefs.remove(fd.getName());
                }
            }
        }
    }

    /**
     * The prefix to use before properties the specify which collection fields are open when coming
     * from the portal page.  eg. portal.verbose.fields.Gene = proteins,chromosome
     */
    public static final String PORTAL_VERBOSE_FIELDS_PREFIX = "portal.verbose.fields.";

    /**
     * Read the port.verbose.fields.* properties from WEB_PROPERTIES and call
     * DisplayObject.setVerbosity(true) on the field in the property value.
     */
    private static void setVerboseCollections(HttpSession session, DisplayObject dobj) {
        ServletContext servletContext = session.getServletContext();
        Map webProperties = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);

        Set clds = dobj.getClds();

        Iterator iter = clds.iterator();

        while (iter.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) iter.next();

            String propName = PORTAL_VERBOSE_FIELDS_PREFIX + TypeUtil.unqualifiedName(cd.getName());
            String fieldNamesString = (String) webProperties.get(propName);

            if (fieldNamesString != null) {
                String[] fieldNames = fieldNamesString.split("\\s*,\\s*");
                for (int i = 0; i < fieldNames.length; i++) {
                    String fieldName = fieldNames[i];
                    dobj.setVerbosity(fieldName, true);
                }
            }
        }
    }

    /**
     * Make a new DisplayObject from the given object.
     * @param session used to get WEB_PROPERTIES and DISPLAYERS Maps
     * @param object the InterMineObject
     * @return the new DisplayObject
     * @throws Exception if an error occurs
     */
    public static DisplayObject makeDisplayObject(HttpSession session, InterMineObject object)
        throws Exception {
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Map webPropertiesMap = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        return new DisplayObject(object, os.getModel(), webConfig,
                                 webPropertiesMap, classKeys);
    }

    private String getBags(ObjectStore os,
                           HttpSession session,
                           ServletContext servletContext,
                           Integer id,
                           boolean isGlobal) {

        Map webSearchables = null;
        // get all of the bags with this object
        if (isGlobal) {
            SearchRepository searchRepository =
            (SearchRepository) servletContext.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
            webSearchables = searchRepository.getWebSearchableMap(TagTypes.BAG);
        } else {
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            webSearchables = profile.getSavedBags();
        }
        Collection<WebSearchable> webSearchableColl = webSearchables.values();
        Collection<ObjectStoreBag> objectStoreBags = new ArrayList<ObjectStoreBag>();


        // loop though and convert InterMineBag to ObjectStoreBag
        for (WebSearchable o : webSearchableColl) {
            InterMineBag bag = (InterMineBag) o;
            ObjectStoreBag osb = bag.getOsb();
            objectStoreBags.add(osb);
        }

        // this searches bags for an object
        ObjectStoreBagsForObject osbo = new ObjectStoreBagsForObject(id, objectStoreBags);

        // run query
        Query q = new Query();
        q.addToSelect(osbo);

        // this should return all bags with that object
        Results results = os.execute(q);
        StringBuffer sb = new StringBuffer();
        for (Object object : results) {
            List list = (List) object;
            if (sb.length() != 0) { sb.append(","); }
            sb.append(list.get(0));
        }
        return sb.toString();
   }
}
