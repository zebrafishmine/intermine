package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 *
 * @author radek
 *
 */
public class MetabolicGeneSummaryDisplayer extends ReportDisplayer
{

    protected static final Logger LOG = Logger.getLogger(MetabolicGeneSummaryDisplayer.class);

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public MetabolicGeneSummaryDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        GeneSummary summary = new GeneSummary(reportObject.getObject(), request);

        // 1. Pathways count
        summary.addCollectionCount("Pathways", "description", "pathways", "pathways");
        // 2. Diseases count
        summary.addCollectionCount("Diseases", "description", "diseases", "diseases");
        // 3. Mouse Alleles count
        if (summary.isThisAMouser()) {
            summary.addCollectionCount("Phenotypes", "straight off mouse", "alleles",
                    "MouseAllelesDisplayer");
        } else {
            summary.addCollectionCount("Phenotypes", "through mouse",
                    allelesPathQuery(summary.getNewPathQuery(),
                    summary.getObjectId()), "MouseAllelesDisplayer");
        }
        // 4. GOTerm count
        summary.addCollectionCount("Gene Ontology", "description", "goAnnotation",
                "GeneOntologyDisplayer");

        // 5. ArrayExpress Gene Expression Tissues
        summary.addCustom("Tissues Expression", "ArrayExpress", this.arrayAtlasExpressionTissues(
                summary.getNewPathQuery(), summary), "gene-expression-atlas-tissues",
                "metabolicGeneSummaryArrayExpressExpressionTissuesDisplayer.jsp");

        // 6. ArrayExpress Gene Expression Diseases
        summary.addCustom("Diseases Expression", "ArrayExpress", this.arrayAtlasExpressionDiseases(
                summary.getNewPathQuery(), summary), "gene-expression-atlas-diseases",
                "metabolicGeneSummaryArrayExpressExpressionDiseasesDisplayer.jsp");

        request.setAttribute("summary", summary);
    }


    private Object arrayAtlasExpressionTissues(PathQuery query, GeneSummary summary) {
        query.addViews("Gene.atlasExpression.condition", "Gene.atlasExpression.expression");

        query.addOrderBy("Gene.atlasExpression.pValue", OrderDirection.ASC);

        query.addConstraint(Constraints.eq("Gene.id", summary.getObjectId().toString()), "A");
        query.addConstraint(Constraints.lessThan("Gene.atlasExpression.pValue", "1E-20"), "B");
        query.addConstraint(Constraints.neq("Gene.atlasExpression.pValue", "0"), "C");
        query.addConstraint(Constraints.eq("Gene.atlasExpression.type", "organism_part"), "D");
        query.addConstraint(Constraints.greaterThan("Gene.atlasExpression.tStatistic", "8"), "E");
        query.addConstraint(Constraints.lessThan("Gene.atlasExpression.tStatistic", "-8"), "F");
        query.setConstraintLogic("A and B and C and D and (E or F)");

        ExportResultsIterator results = summary.getExecutor().execute((PathQuery) query);

        HashMap<String, String> tissues = new HashMap<String, String>();
        while (results.hasNext()) {
            List<ResultElement> item = results.next();
            String tissue = item.get(0).getField().toString();
            String regulation = item.get(1).getField().toString();
            // obviously, we can have the same disease appear 2x (we will), but we don't care...
            tissues.put(tissue, regulation);
        }

        return tissues;
    }

    private Object arrayAtlasExpressionDiseases(PathQuery query, GeneSummary summary) {
        query.addViews("Gene.atlasExpression.condition", "Gene.atlasExpression.expression");

        query.addOrderBy("Gene.atlasExpression.pValue", OrderDirection.ASC);

        query.addConstraint(Constraints.eq("Gene.id", summary.getObjectId().toString()), "A");
        query.addConstraint(Constraints.lessThan("Gene.atlasExpression.pValue", "1E-20"), "B");
        query.addConstraint(Constraints.neq("Gene.atlasExpression.pValue", "0"), "C");
        query.addConstraint(Constraints.eq("Gene.atlasExpression.type", "disease_state"), "D");
        query.addConstraint(Constraints.greaterThan("Gene.atlasExpression.tStatistic", "8"), "E");
        query.addConstraint(Constraints.lessThan("Gene.atlasExpression.tStatistic", "-8"), "F");
        query.setConstraintLogic("A and B and C and D and (E or F)");

        ExportResultsIterator results = summary.getExecutor().execute((PathQuery) query);

        HashMap<String, String> diseases = new HashMap<String, String>();
        while (results.hasNext()) {
            List<ResultElement> item = results.next();
            String disease = item.get(0).getField().toString();
            String regulation = item.get(1).getField().toString();
            // obviously, we can have the same disease appear 2x (we will), but we don't care...
            diseases.put(disease, regulation);
        }

        return diseases;
    }

    /**
     * EMTAB-62 link generator from ebi.ac.uk
     * @param primaryId
     * @return
     * @deprecated because the image is too big
     */
    @SuppressWarnings("unused")
    @java.lang.Deprecated
    private String emtabExpression(String primaryId) {
        if (primaryId != null) {
            return "http://www.ebi.ac.uk/gxa/webanatomogram/" + primaryId + ".png";
        }
        return null;
    }

    /**
     * Generate PathQuery to Mousey Alleles
     * @param query
     * @param objectId
     * @return
     */
    private PathQuery allelesPathQuery(PathQuery query, Integer objectId) {
        query.addViews("Gene.homologues.homologue.alleles.primaryIdentifier");
        query.addConstraint(Constraints.eq("Gene.homologues.homologue.organism.shortName",
                "M. musculus"), "A");
        query.addConstraint(Constraints.eq("Gene.id", objectId.toString()), "B");
        query.setConstraintLogic("A and B");

        return query;
    }

    /**
     * Generate PathQuery to GOTerms
     * @param query
     * @param objectId
     * @return
     */
    @SuppressWarnings("unused")
    private PathQuery goTermPathQuery(PathQuery query, Integer objectId) {
        query.addViews("Gene.goAnnotation.ontologyTerm.namespace");
        query.addOrderBy("Gene.goAnnotation.ontologyTerm.namespace", OrderDirection.ASC);
        query.addConstraint(Constraints.eq("Gene.id", objectId.toString()));

        return query;
    }

    /**
     *
     * Internal wrapper.
     * @author radek
     *
     */
    public class GeneSummary
    {
        private InterMineObject imObj;
        private HttpServletRequest request;
        private PathQueryExecutor executor;
        private Model model = null;
        private LinkedHashMap<String, HashMap<String, Object>> storage;

        /**
         *
         * @param imObj InterMineObject
         * @param request Request
         */
        public GeneSummary(InterMineObject imObj, HttpServletRequest request) {
            this.imObj = imObj;
            this.request = request;
            storage = new LinkedHashMap<String, HashMap<String, Object>>();
        }

        /**
         * Add a custom object to the displayer.
         * @param key to show under in the summary
         * @param description to show under the title
         * @param data to save on the wrapper object
         * @param anchor says where we will scroll onlick, an ID attr of the target element
         * @param jsp to include that knows how to display us
         */
        public void addCustom(String key, String description,
                Object data, String anchor, String jsp) {
            storage.put(key, createWrapper("custom", data, anchor, description, jsp));
        }

        /**
         * Add collection count to the summary.
         * @param key to show under in the summary
         * @param description to show under the title
         * @param param can be a fieldName or a PathQuery
         * @param anchor says where we will scroll onlick, an ID attr of the target element
         */
        public void addCollectionCount(String key, String description, Object param,
                String anchor) {
            if (param instanceof PathQuery) {
                try {
                    storage.put(key, createWrapper("integer", executor.count((PathQuery)
                            param), anchor, description, null));
                } catch (ObjectStoreException e) {
                    LOG.error("Problem running PathQuery " + e.toString());
                }
            } else if (param instanceof String) {
                Collection<?> coll = null;
                try {
                    if ((coll = (Collection<?>) imObj.getFieldValue((String) param)) != null) {
                        storage.put(key, createWrapper("integer", coll.size(), anchor,
                                description, null));
                    }
                } catch (IllegalAccessException e) {
                    LOG.error("The field " + param + " does not exist");
                }
            } else {
                storage.put(key, createWrapper("unknown", param, anchor, description, null));
            }
        }

        private HashMap<String, Object> createWrapper(String type, Object data, String anchor,
                String description, String jsp) {
            HashMap<String, Object> inner = new HashMap<String, Object>();
            inner.put("type", type);
            inner.put("data", data);
            inner.put("anchor", anchor);
            inner.put("description", description);
            if (jsp != null) {
                inner.put("jsp", jsp);
            }
            return inner;
        }

        /**
         * Add collection distinct count to the summary. Will get the distinct value referenced
         * and get their count.
         * @param key to show under in the summary
         * @param description to show under the title
         * @param param can be a fieldName or a PathQuery
         * @param anchor says where we will scroll onlick, an ID attr of the target element
         */
        public void addCollectionDistinctCount(String key, String description, Object param,
                String anchor) {
            if (param instanceof PathQuery) {
                ExportResultsIterator results = executor.execute((PathQuery) param);

                HashMap<String, Integer> temp = new HashMap<String, Integer>();
                while (results.hasNext()) {
                    List<ResultElement> item = results.next();
                    String value = item.get(0).getField().toString();
                    if (!temp.keySet().contains(value)) {
                        temp.put(value, 0);
                    }
                    temp.put(value, temp.get(value) + 1);
                }
                storage.put(key, createWrapper("map", temp, anchor, description, null));
            } else {
                storage.put(key, createWrapper("unknown", param, anchor, description, null));
            }
        }

        /**
         * Add a link to an image for the summary.
         * @param key to show under in the summary
         * @param description to show under the title
         * @param link refers to the src attr of the img element
         * @param anchor says where we will scroll onlick, an ID attr of the target element
         */
        public void addImageLink(String key, String link, String anchor, String description) {
            storage.put(key, createWrapper("image", link, anchor, description, null));
        }

        /**
         * Give us a new PathQuery to work on.
         * @return PathQuery
         */
        public PathQuery getNewPathQuery() {
            if (model == null) {
                HttpSession session = request.getSession();
                final InterMineAPI im = SessionMethods.getInterMineAPI(session);
                model = im.getModel();
                executor = im.getPathQueryExecutor(SessionMethods.getProfile(session));
            }
            return new PathQuery(model);
        }

        /**
         *
         * @return InterMineObject ID
         */
        public Integer getObjectId() {
            return imObj.getId();
        }

        /**
         *
         * @return true if we are on a mouseified gene
         */
        public Boolean isThisAMouser() {
            try {
                return "Mus".equals(((InterMineObject) imObj.getFieldValue("organism"))
                        .getFieldValue("genus"));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
         *
         * @return ReportObject primaryIdentifier
         */
        public String getPrimaryId() {
            try {
                return (String) imObj.getFieldValue("primaryIdentifier");
            } catch (IllegalAccessException e) {
                LOG.error("The field primaryIdentifier does not exist");
            }
            return null;
        }

        /**
         *
         * @return PathQuery Executor
         */
        public PathQueryExecutor getExecutor() {
            return executor;
        }

        /**
         *
         * @return Map of the fields configged here for the JSP to traverse
         */
        public LinkedHashMap<String, HashMap<String, Object>> getFields() {
            return storage;
        }

    }

}
