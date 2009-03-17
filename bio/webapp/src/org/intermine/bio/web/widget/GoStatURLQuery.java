package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.WidgetURLQuery;


/**
 * {@inheritDoc}
 * @author Julie Sullivan
 */
public class GoStatURLQuery implements WidgetURLQuery
{
    private ObjectStore os;
    private InterMineBag bag;
    private String key;

    /**
     * @param os object store
     * @param key go terms user selected
     * @param bag bag page they were on
     */
    public GoStatURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery() {

        PathQuery q = new PathQuery(os.getModel());
        String bagType = bag.getType();
        String pathStrings;

        if (bagType.equals("Protein")) {
            pathStrings = "Protein.genes.primaryAccession,Protein.genes.primaryIdentifier,"
                + "Protein.genes.name,Protein.genes.organism.name,"
                + "Protein.primaryIdentifier,Protein.primaryAccession,"
                + "Protein.genes.goAnnotation.ontologyTerm.relations.childTerm.identifier,"
                + "Protein.genes.goAnnotation.ontologyTerm.relations.childTerm.name,"
                + "Protein.genes.goAnnotation.ontologyTerm.relations.parentTerm.identifier,"
                + "Protein.genes.goAnnotation.ontologyTerm.relations.parentTerm.name";
        } else {
            pathStrings = "Gene.secondaryIdentifier,Gene.primaryIdentifier,"
                + "Gene.name,Gene.organism.name,"
                + "Gene.goAnnotation.ontologyTerm.relations.childTerm.identifier,"
                + "Gene.goAnnotation.ontologyTerm.relations.childTerm.name,"
                + "Gene.goAnnotation.ontologyTerm.relations.parentTerm.identifier,"
                + "Gene.goAnnotation.ontologyTerm.relations.parentTerm.name";
        }
        q.setView(pathStrings);
        q.setOrderBy(pathStrings);

        q.addConstraint(bagType, Constraints.in(bag.getName()));
        // can't be a NOT relationship!
        String pathString = (bagType.equals("Protein") ? "Protein.genes.goAnnotation.qualifier"
                                                         : "Gene.goAnnotation.qualifier");
        q.addConstraint(pathString, Constraints.isNull());

        // go term
        pathString = (bagType.equals("Protein") ? "Protein.genes" : "Gene");
        pathString += ".goAnnotation.ontologyTerm.relations.parentTerm.identifier";
        q.addConstraint(pathString, Constraints.lookup(key), "C", "GOTerm");

        q.setConstraintLogic("A and B and C");
        q.syncLogicExpression("and");
        return q;
    }
}
