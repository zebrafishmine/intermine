package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.template.ApiTemplate;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;

import junit.framework.TestCase;

public class TemplateQueryUpdateTest extends TestCase
{
    private Map<String, String> renamedClasses = new HashMap<String, String>();
    private Map<String, String> renamedFields = new HashMap<String, String>();

    public TemplateQueryUpdateTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        renamedClasses.put("CEOTest", "CEO");

        renamedFields.put("Company.CEOTest", "CEO");
        renamedFields.put("CEOTest.sal", "salary");
    }

    public void testUpdate() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("oldtestmodel"));
        query.addView("CEOTest.name");
        query.addView("CEOTest.title");
        query.addView("CEOTest.company.name");
        query.addView("CEOTest.sal");
        query.setDescription("CEOTest.name", "CEO name");
        PathConstraint con1 = new PathConstraintAttribute("CEOTest.name",
                                  ConstraintOp.CONTAINS, "ploy");
        query.addConstraint(con1);
        PathConstraint con2 = new PathConstraintAttribute("CEOTest.company.name",
                                  ConstraintOp.CONTAINS, "pany");
        query.addConstraint(con2);
        PathConstraint con3 = new PathConstraintLookup(
            "CEOTest.company.CEOTest.department.company.CEOTest", "ttt", "DepartmentA1");
        query.addConstraint(con3);
        query.setOuterJoinStatus("CEOTest.company", OuterJoinStatus.OUTER);
        query.addOrderBy("CEOTest.name", OrderDirection.ASC);
        query.addOrderBy("CEOTest.sal", OrderDirection.ASC);
        ApiTemplate templateQuery = new ApiTemplate("test", "title", "comment", query);
        templateQuery.setSwitchOffAbility(con1, org.intermine.template.SwitchOffAbility.ON);
        templateQuery.setConstraintDescription(con2, "Description on constraint");
        templateQuery.setEditable(con1, true);
        templateQuery.setEditable(con2, true);
        TemplateQueryUpdate templateQueryUpdate = new TemplateQueryUpdate(templateQuery,
            Model.getInstanceByName("oldtestmodel"));
        templateQueryUpdate.update(renamedClasses, renamedFields);
        ApiTemplate newTemplateQuery = templateQueryUpdate.getNewTemplateQuery();
        assertEquals("test", newTemplateQuery.getName());
        assertEquals("title", newTemplateQuery.getTitle());
        assertEquals("comment", newTemplateQuery.getComment());
        //verify view
        assertEquals(4, newTemplateQuery.getView().size());
        assertEquals("CEO.name", newTemplateQuery.getView().get(0));
        assertEquals("CEO.title", newTemplateQuery.getView().get(1));
        assertEquals("CEO.company.name", newTemplateQuery.getView().get(2));
        assertEquals("CEO.salary", newTemplateQuery.getView().get(3));
        //verify descriptions
        assertEquals(1, newTemplateQuery.getDescriptions().size());
        assertEquals("CEO name", newTemplateQuery.getDescription("CEO.name"));
        //verify constraint
        assertEquals(3, newTemplateQuery.getConstraints().size());
        assertEquals(1, newTemplateQuery.getConstraintsForPath("CEO.name").size());
        PathConstraintAttribute constraint1 = (PathConstraintAttribute) newTemplateQuery
                                              .getConstraintsForPath("CEO.name").get(0);
        assertEquals("ploy", constraint1.getValue());
        assertEquals(ConstraintOp.CONTAINS, constraint1.getOp().CONTAINS);
        assertEquals(1, newTemplateQuery.getConstraintsForPath("CEO.company.name").size());
        PathConstraintAttribute constraint2 = (PathConstraintAttribute) newTemplateQuery
                                              .getConstraintsForPath("CEO.company.name").get(0);
        assertEquals("pany", constraint2.getValue());
        assertEquals(ConstraintOp.CONTAINS, constraint2.getOp().CONTAINS);
        PathConstraintLookup constraint3 = (PathConstraintLookup) newTemplateQuery
            .getConstraintsForPath("CEO.company.CEO.department.company.CEO").get(0);
        assertEquals("ttt", constraint3.getValue());
        assertEquals("DepartmentA1", constraint3.getExtraValue());
        //verify outer join
        assertEquals(OuterJoinStatus.OUTER, newTemplateQuery.getOuterJoinStatus("CEO.company"));
        //verify order by
        assertEquals(2, newTemplateQuery.getOrderBy().size());
        assertEquals("CEO.name", newTemplateQuery.getOrderBy().get(0).getOrderPath());
        assertEquals("CEO.salary", newTemplateQuery.getOrderBy().get(1).getOrderPath());
        //verify switch off ability
        assertEquals(org.intermine.template.SwitchOffAbility.ON,
            newTemplateQuery.getSwitchOffAbility(constraint1));
        assertEquals(org.intermine.template.SwitchOffAbility.LOCKED,
            newTemplateQuery.getSwitchOffAbility(constraint2));
        assertEquals(org.intermine.template.SwitchOffAbility.LOCKED,
            newTemplateQuery.getSwitchOffAbility(constraint3));
        //verify constraint descriptions
        assertEquals("Description on constraint",
            newTemplateQuery.getConstraintDescription(constraint2));
        //verify editable constraints
        assertEquals(2, newTemplateQuery.getEditableConstraints().size());
        assertEquals(1, newTemplateQuery.getEditableConstraints("CEO.name").size());
        assertEquals(1, newTemplateQuery.getEditableConstraints("CEO.company.name").size());
    }
}
