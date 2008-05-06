  package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryHelper;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;

/**
 * @author Xavier Watkins
 *
 */
public class BagTableWidgetLoader
{
    private List<String> columns;
    private List flattenedResults;
    private String title, description;
    protected Query q;
    private int widgetTotal = 0;
    private InterMineBag bag;
    private String pathString;
    private Model model;
    private String displayFields, exportFields;
    private ObjectStore os;
    private Path origPath;
    private Map classKeys;
    private String type;

    /**
     * This class loads and formats the data for the count
     * table widgets in the bag details page
     *
     * @param pathStr the path to group the objects by, ie Employee.city will return the list of
     * employees by city
     * @param imBag the bag
     * @param os the objectstore
     * @param webConfig the webConfig
     * @param model the model
     * @param classKeys the classKeys
     * @param displayFields fields displayed in widget
     * @param exportFields fields, along with the key for the originating class, that will be
     * printed in the export file
     * @param urlGen the class that generates the pathquery used in the links from the widget
     * @param columnTitle title for count column
     * @param externalLink link to external source
     * @param externalLinkLabel name of external data source
     * @throws ClassNotFoundException if some class in the widget paths is not found
     * @throws UnsupportedEncodingException if something goes wrong encoding the URL
     */
    public BagTableWidgetLoader(String pathStr, InterMineBag imBag, ObjectStore os,
                                WebConfig webConfig, Model model,
                                Map<String, List<FieldDescriptor>> classKeys,
                                String displayFields, String exportFields, String urlGen,
                                String columnTitle,
                                String externalLink, String externalLinkLabel)
    throws ClassNotFoundException, UnsupportedEncodingException {

        this.pathString = pathStr;
        this.origPath = new Path(model, pathWithNoConstraints(pathString));
        ClassDescriptor cld = origPath.getEndClassDescriptor();
        this.type = cld.getUnqualifiedName();
        this.classKeys = classKeys;
        this.bag = imBag;
        this.model = model;
        this.displayFields = displayFields;
        this.exportFields = exportFields;
        this.os = os;

        // TODO validate start type vs. bag type

        q = constructQuery(false, null);

        List results;
        try {
            results = os.execute(q, 0, 50, true, true, ObjectStore.SEQUENCE_IGNORE);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        columns = new ArrayList<String>();
        if ((displayFields != null) && (displayFields.length() != 0)) {
            String[] fieldArray = displayFields.split(",");
            for (int i = 0; i < fieldArray.length; i++) {
                String field = fieldArray[i];
                String newColumnName = type + "." + field;
                columns.add(newColumnName);
            }
        } else {
            List cldFieldConfigs = FieldConfigHelper.getClassFieldConfigs(webConfig, cld);
            for (Iterator<FieldConfig> iter = cldFieldConfigs.iterator(); iter.hasNext();) {
                FieldConfig fc = iter.next();
                if (!fc.getShowInResults()) {
                    continue;
                }
                String fieldExpr = fc.getFieldExpr();
                String newColumnName = type + "." + fieldExpr;
                columns.add(newColumnName);
            }
        }

        flattenedResults = new ArrayList<ArrayList>();

        for (Iterator iter = results.iterator(); iter.hasNext();) {
            ArrayList<String[]> flattenedRow = new ArrayList<String[]>();
            ResultsRow resRow = (ResultsRow) iter.next();
            //Integer lastObjectId = null;
            String key = "";
            for (Iterator iterator = resRow.iterator(); iterator.hasNext();) {

                Object element = iterator.next();
                if (element instanceof InterMineObject) {

                    InterMineObject o = (InterMineObject) element;
                    boolean isFirst = true;
                    for (Iterator iterator3 = columns.iterator(); iterator3.hasNext();) {

                        String columnName = (String) iterator3.next();
                        Path path = new Path(model, columnName);
                        Object fieldValue = path.resolve(o);
                        Class thisType = path.getStartClassDescriptor().getType();
                        String fieldName = path.getEndFieldDescriptor().getName();
                        boolean isKeyField = ClassKeyHelper.isKeyField(classKeys,
                                TypeUtil.unqualifiedName(thisType.getName()), fieldName);
                        String link = null;
                        String val = fieldValue.toString();
                        if (isKeyField) {
                            key = fieldValue.toString();
                            link = "objectDetails.do?id=" + o.getId() + "&amp;trail=|bag."
                                   + bag.getName() + "|" + o.getId();
                        } else if (externalLink != null && !externalLink.equals("")) {
                            val = val + " <a href=\"" + externalLink + key
                            + "\" target=\"_new\" class=\"extlink\">["
                            + externalLinkLabel + "]</a>";
                        }

                        if (isFirst) {

                            String checkbox = "<input name=\"selected\" value=\"" + key
                            + "\" id=\"selected_" + key + "\" type=\"checkbox\">";

                            flattenedRow.add(new String[]
                                                        {
                                checkbox
                                                        });
                            isFirst = false;
                        }

                        flattenedRow.add(new String[]
                                                    {
                            val, link
                                                    });

                    }
                } else if (element instanceof Long) {
                    flattenedRow.add(new String[]
                                                {
                        String.valueOf((Long) element),
                        "widgetAction.do?bagName=" + bag.getName() + "&link=" + urlGen
                        + "&key=" + URLEncoder.encode(key, "UTF-8")
                                                });
                }

            }
            flattenedResults.add(flattenedRow);
        }
        // Add the count column
        if (columnTitle != null) {
            columns.add(columnTitle);
        } else {
            columns.add(bag.getType() + "s");
        }

        q = constructQuery(true, null);
        widgetTotal = calcTotal(os, q);
    }

    /**
     * get the flattened results
     * @return the flattened results
     */
    public List getFlattenedResults() {
        return flattenedResults;
    }

    /**
     * Get the columnNames
     * @return the columnNames
     */
    public List getColumns() {
        return columns;
    }

    /**
     * Get the title
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the description
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    private Query constructQuery(boolean calcTotal, List<String> keys)
        throws ClassNotFoundException, IllegalArgumentException {

        Query q = new Query();
        boolean first = true;
        String[] queryBits = pathString.split("\\.");
        QueryClass qcStart = null;
        QueryField qfStartId = null;
        ClassDescriptor cldStart = origPath.getStartClassDescriptor();
        QueryClass qcExport = null;
        for (int i = 1; i < queryBits.length; i++) {

            if (qcStart == null) {
                qcStart = new QueryClass(cldStart.getType());
                qfStartId = new QueryField(qcStart, "id");
                qcExport = qcStart;
                q.addFrom(qcStart);
                QueryHelper.addConstraint(q,
                new BagConstraint(qfStartId, ConstraintOp.IN, bag.getOsb()));
            }

            String refName;
            String constraintName = null, constraintValue = null;
            // extra constraints have syntax Company.departments[name=DepartmentA].employees
            if (queryBits[i].indexOf('[') > 0) {
                String s = queryBits[i];
                refName = s.substring(0, s.indexOf('['));
                constraintName = s.substring(s.indexOf('[') + 1, s.indexOf('='));
                constraintValue = s.substring(s.indexOf('=') + 1, s.indexOf(']'));
            } else {
                refName = queryBits[i];
            }

            FieldDescriptor fld = cldStart.getFieldDescriptorByName(refName);
            if (fld == null) {
                throw new IllegalArgumentException("Class '" + cldStart.getType() + "' has no '"
                                                   + refName + "' field");
            }
            if (fld.isAttribute()) {
                throw new IllegalArgumentException("path element not a reference/collection: "
                                                   + fld.getName());
            }
            ClassDescriptor cldEnd = ((ReferenceDescriptor) fld).getReferencedClassDescriptor();
            QueryClass qcEnd = new QueryClass(cldEnd.getType());

            addReferenceConstraint(model, q, qcStart, refName, qcEnd);

            if (constraintName != null && constraintValue != null) {
                AttributeDescriptor attFld = cldEnd.getAttributeDescriptorByName(constraintName);
                if (attFld == null) {
                    throw new IllegalArgumentException("Class '" + cldEnd.getType() + "' has no '"
                                                       + constraintName + "' field");
                }
                if (!attFld.getType().equals("java.lang.String")) {
                    throw new IllegalArgumentException("Constraints can only be on String fields '"
                                                       + constraintName
                                                       + "' is a " + attFld.getType());
                }
                SimpleConstraint sc = new SimpleConstraint(new QueryField(qcEnd, constraintName),
                                                           ConstraintOp.EQUALS,
                                                           new QueryValue(constraintValue));
                QueryHelper.addConstraint(q, sc);
                constraintName = null;
                constraintValue = null;
            }

            QueryFunction qf = new QueryFunction();

            // if we are at the end of the path, add to select and group by
            if (queryBits.length == (i + 1)) {

                if (keys != null) { // export
                    q.setDistinct(true);

                    QueryField keyField = new QueryField(qcEnd, getKeyField(displayFields));
                    BagConstraint bc = new BagConstraint(keyField, ConstraintOp.IN, keys);
                    QueryHelper.addConstraint(q, bc);

                    q.addToSelect(new QueryField(qcEnd, getKeyField(displayFields)));

                    String[] fields = exportFields.split(",");
                    for (String field : fields) {
                        q.addToSelect(new QueryField(qcExport, field));
                    }
                } else if (!calcTotal) {
                    q.setDistinct(false);
                    q.addToSelect(qcEnd);
                    q.addToGroupBy(qcEnd);

                    q.addToSelect(qf);
                    q.addToOrderBy(qf, "desc");

                } else {

                    Query subQ = new Query();
                    subQ = q;
                    subQ.setDistinct(true);
                    subQ.clearSelect();
                    subQ.addToSelect(qfStartId);

                    q = new Query();
                    q.setDistinct(false);
                    q.addToSelect(qf);
                    q.addFrom(subQ);
                }
            }
            cldStart = cldEnd;
            qcStart = qcEnd;
            first = false;
        }

        return q;
    }

    /**
     * Add a contains constraint to Query (q) from qcStart from qcEnd via reference refName.
     * Return qcEnd as it may need to be passed into mehod again as qcStart.
     * @param model the Model use to find meta data
     * @param q the query
     * @param qcStart the QueryClass that contains the reference
     * @param refName name of reference to qcEnd
     * @param qcEnd the target QueryClass of refName
     * to the query
     * @return QueryClass return qcEnd
     */
    private QueryClass addReferenceConstraint(Model model, Query q, QueryClass qcStart,
                                              String refName, QueryClass qcEnd) {
        q.addFrom(qcEnd);

        // already validated against model
        ClassDescriptor startCld = model.getClassDescriptorByName(qcStart.getType().getName());
        FieldDescriptor fd = startCld.getFieldDescriptorByName(refName);

        QueryReference qRef;
        if (fd.isReference()) {
            qRef = new QueryObjectReference(qcStart, refName);
        } else {
            qRef = new QueryCollectionReference(qcStart, refName);
        }
        ContainsConstraint cc = new ContainsConstraint(qRef, ConstraintOp.CONTAINS, qcEnd);
        QueryHelper.addConstraint(q, cc);

        return qcEnd;
    }

    // return a path with out any [] constraints
    private String pathWithNoConstraints(String path) {
        StringBuffer sb = new StringBuffer(path.length());
        String[] queryBits = path.split("\\.");
        for (int i = 0; i < queryBits.length; i++) {
            String refName = queryBits[i];
            if (refName.indexOf('[') > 0) {
                refName = refName.substring(0, refName.indexOf('['));
            }
            if (sb.length() > 0) {
                sb.append(".");
            }
            sb.append(refName);
        }
        return sb.toString();
    }

    /**
     * @return the total number of objects analysed in this widget
     */
    public int getWidgetTotal() {
        return widgetTotal;
    }

    private static int calcTotal(ObjectStore os, Query q) {
        Results res = os.execute(q);
        Iterator iter = res.iterator();
        int n = 0;
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            n = ((java.lang.Long) resRow.get(0)).intValue();
        }
        return n;
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String>> getExportResults(String[] selected) throws Exception {

        List<List<String>> exportResults = new ArrayList<List<String>>();
        List<String> selectedIds = Arrays.asList(selected);

        Query q = constructQuery(false, selectedIds);

        Results res = os.execute(q);
        Iterator iter = res.iterator();
        HashMap<String, List<String>> termsToIds = new HashMap();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            String term = (String) resRow.get(0);   // annotation (like go term)
            String id = (String) resRow.get(1);     // object identifier (like gene.identifier)
            if (!termsToIds.containsKey(term)) {
                termsToIds.put(term, new ArrayList<String>());
            }
            termsToIds.get(term).add(id);
        }

        for (String id : selectedIds) {
            if (termsToIds.get(id) != null) {
                List row = new LinkedList();
                row.add(id);
                List<String> ids = termsToIds.get(id);
                StringBuffer sb = new StringBuffer();
                for (String term : ids) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(term);
                }
                row.add(sb.toString());

                exportResults.add(row);
            }
        }
        return exportResults;
    }

    private String getKeyField(String s) {
       if (s.contains(",")) {
        String[] strings = s.split(",");
        return strings[0];
       }
       return s;
    }
}
