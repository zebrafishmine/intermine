package org.intermine.web.history;

import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.web.Constants;
import org.intermine.web.Constraint;
import org.intermine.web.PathQuery;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;
import org.intermine.web.SavedQuery;
import org.intermine.web.SessionMethods;
import org.intermine.web.TemplateQuery;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMineIdBag;
import org.intermine.web.bag.InterMinePrimitiveBag;
import org.intermine.model.InterMineObject;

import javax.servlet.http.HttpSession;
import servletunit.struts.MockStrutsTestCase;
import servletunit.HttpServletResponseSimulator;

import org.intermine.model.userprofile.UserProfile;


public class ModifyBagActionTest extends MockStrutsTestCase
{
    PathQuery query, queryBag;
    SavedQuery sq, sqBag, hist, hist2;
    Date date = new Date();
    InterMineBag bag, bag2;
    TemplateQuery template;
    ObjectStoreWriter userProfileOSW;
    Integer userId;
    ProfileManager profileManager;

    public ModifyBagActionTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        if (userProfileOSW == null) {
            SessionMethods.initSession(this.getSession());
            userProfileOSW =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        }
        //ObjectStoreDummyImpl userprofileDummy = new ObjectStoreDummyImpl();
        //userprofileDummy.setModel(Model.getInstanceByName("userprofile"));
        //userprofileOS = new ObjectStoreWriterInterMineImpl(userprofileDummy);
        //userprofileOS.setModel(Model.getInstanceByName("userprofile"));

        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        profileManager = new ProfileManager(os, userProfileOSW);
        userId = new Integer(101);

        Profile profile = new Profile(profileManager, "modifyBagActionTest", userId, "pass",
                                      new HashMap(), new HashMap(), new HashMap());
        HttpSession session = getSession();
        session.setAttribute(Constants.PROFILE, profile);

        query = new PathQuery(Model.getInstanceByName("testmodel"));
        query.setView(Arrays.asList(new String[]{"Employee", "Employee.name"}));
        queryBag = new PathQuery(Model.getInstanceByName("testmodel"));
        queryBag.setView(Arrays.asList(new String[]{"Employee", "Employee.name"}));
        queryBag.addNode("Employee.name").getConstraints().add(new Constraint(ConstraintOp.IN, "bag2"));
        bag = new InterMinePrimitiveBag(userId, "bag1", userProfileOSW, Collections.singleton("entry1"));
        bag2 = new InterMineIdBag(userId, "bag2", userProfileOSW, Collections.singleton(new Integer(1)));
        sq = new SavedQuery("query1", date, query);
        sqBag = new SavedQuery("query3", date, queryBag);
        hist = new SavedQuery("query2", date, (PathQuery) query.clone());
        hist2 = new SavedQuery("query1", date, (PathQuery) query.clone());
        template = new TemplateQuery("template", "tdesc",
                                     new PathQuery(Model.getInstanceByName("testmodel")), false,
                                     "");

        //Profile profile = (Profile) getSession().getAttribute(Constants.PROFILE);
        profile.saveQuery(sq.getName(), sq);
        profile.saveQuery(sqBag.getName(), sqBag);
        profile.saveBag("bag1", bag);
        profile.saveBag("bag2", bag2);
        profile.saveHistory(hist);
        profile.saveHistory(hist2);
    }

    public void tearDown() throws Exception {
        cleanUserProfile();
    }

    private void cleanUserProfile() throws ObjectStoreException {
        if (userProfileOSW.isInTransaction()) {
            userProfileOSW.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue("modifyBagActionTest"));
        q.setConstraint(sc);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = userProfileOSW.getObjectStore();
//         SingletonResults res = new SingletonResults(q, userProfileOSW.getObjectStore(),
//                                                     userProfileOSW.getObjectStore()
//                                                     .getSequence());

        SingletonResults res = new SingletonResults(q, userProfileOSW,
                                                    userProfileOSW.getSequence());

        System.out.println("results size: " + res.size());
        Iterator resIter = res.iterator();
        userProfileOSW.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            userProfileOSW.delete(o);
        }
        userProfileOSW.commitTransaction();
        userProfileOSW.close();
    }

    private Profile getProfile() {
        return (Profile) getSession().getAttribute(Constants.PROFILE);
    }

    public void testDeleteBag() {
        addRequestParameter("selectedBags", "bag1");
        addRequestParameter("delete", "Delete");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("bag");
        assertEquals(1, getProfile().getSavedBags().size());
        assertTrue(getProfile().getSavedBags().containsKey("bag2"));
    }

    public void testDeleteBagInUse() {
        addRequestParameter("selectedBags", "bag2");
        addRequestParameter("delete", "Delete");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"history.baginuse"});
        verifyForward("bag");
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testDeleteNothingSelected() {
        addRequestParameter("delete", "Delete");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyBag.none"});
        verifyForward("bag");
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testRenameBag() {
        addRequestParameter("newBagName", ""); // always there
        addRequestParameter("newName", "bagA"); // new name edit field
        addRequestParameter("name", "bag1"); // hidden target bag name
        addRequestParameter("type", "bag"); //
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("bag");
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testRenameBagNameInUse() {
        addRequestParameter("newBagName", ""); // always there
        addRequestParameter("newName", "bag2"); // new name edit field
        addRequestParameter("name", "bag1"); // hidden target bag name
        addRequestParameter("type", "bag"); //
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyQuery.queryExists"});
        assertEquals("/history.do?action=rename&type=bag&name=bag1", getActualForward());
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testRenameBagEmptyName() {
        addRequestParameter("newBagName", ""); // always there
        addRequestParameter("newName", ""); // new name edit field
        addRequestParameter("name", "bag1"); // hidden target bag name
        addRequestParameter("type", "bag"); //
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.required"});
        assertEquals("/history.do?action=rename&type=bag&name=bag1", getActualForward());
    }

    public void testUnionConflictingTypes() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"bag.typesDontMatch"});
        verifyForward("bag");
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testIntersectConflictingTypes() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"bag.typesDontMatch"});
        verifyForward("bag");
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testUnionConflictingTypesNothingSelected() {
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyBag.none"});
        verifyForward("bag");
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testIntersectConflictingTypesNothingSelected() {
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.modifyBag.none"});
        verifyForward("bag");
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testUnionNoName() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.required"});
        verifyForward("bag");
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testIntersectNoName() {
        addRequestParameter("selectedBags", new String[]{"bag2", "bag1"});
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyActionErrors(new String[]{"errors.required"});
        verifyForward("bag");
        assertEquals(2, getProfile().getSavedBags().size());
    }

    public void testIntersectPrimitiveBags() {
        InterMinePrimitiveBag bag = new InterMinePrimitiveBag(userId, "bag3", userProfileOSW, Collections.singleton("entry1"));
        getProfile().saveBag("bag3", bag);
        addRequestParameter("selectedBags", new String[]{"bag1", "bag3"});
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("bag");
        assertEquals(4, getProfile().getSavedBags().size());
    }

    public void testIntersectObjectBags() {
        InterMineIdBag bag = new InterMineIdBag(userId, "bag3", userProfileOSW, Collections.singleton(new Integer(1)));
        getProfile().saveBag("bag3", bag);
        addRequestParameter("selectedBags", new String[]{"bag2", "bag3"});
        addRequestParameter("intersect", "Intersect");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("bag");
        assertEquals(4, getProfile().getSavedBags().size());
    }

    public void testUnionPrimitiveBags() {
        InterMinePrimitiveBag bag = new InterMinePrimitiveBag(userId, "bag3", userProfileOSW, Collections.singleton("entry1"));
        getProfile().saveBag("bag3", bag);
        addRequestParameter("selectedBags", new String[]{"bag1", "bag3"});
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        verifyNoActionErrors();
        verifyForward("bag");
        assertEquals(4, getProfile().getSavedBags().size());
    }

    public void testUnionObjectBags() {

        System.out.println("error: " + getProfile().getSavedBags());
        InterMineIdBag bag = new InterMineIdBag(userId, "bag3", userProfileOSW, Collections.singleton(new Integer(1)));
        System.out.println("error: " + getProfile().getSavedBags());
        getProfile().saveBag("bag3", bag);
        addRequestParameter("selectedBags", new String[]{"bag2", "bag3"});
        addRequestParameter("union", "Union");
        addRequestParameter("newBagName", "bagA");
        setRequestPathInfo("/modifyBag");
        actionPerform();
        System.out.println("actionMessages: " + getRequest().getAttribute("stacktrace"));
        System.out.println("error: " + getProfile().getSavedBags());
        verifyNoActionErrors();
        verifyForward("bag");
        assertEquals(4, getProfile().getSavedBags().size());
    }
}
