package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.intermine.api.util.PathUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.pathquery.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;

/**
 * Class to represent an object for display in the webapp. Various maps and collections
 * are calculated here the first time an object is viewed and cached for later use. Only
 * information that remains static throughout the session should be precalcualted here.
 * Anything dynamic is computed in ObjectDetailsController.
 *
 * @author Mark Woodbridge
 */
public class DisplayObject
{
    private InterMineObject object;
    private WebConfig webConfig;
    private Map webProperties;
    private Model model;

    private Set<ClassDescriptor> clds;
    private Map<String, Object> fieldValues = new HashMap<String, Object>();
    private Map<String, Object> attributes = null;
    private Map<String, FieldDescriptor> attributeDescriptors = null;
    private Map<String, DisplayReference> references = null;
    private Map<String, DisplayCollection> collections = null;
    private Map<String, DisplayField> refsAndCollections = null;
    private Map<String, FieldConfig> fieldConfigMap = null;
    private List<String> fieldExprs = null;
    private Map<String, String> verbosity = new HashMap<String, String>();
    private final Map classKeys;

    /**
     * Create a new DisplayObject.
     * @param object the object to display
     * @param model the metadata for the object
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties the web properties from the session
     * @param classKeys map of classname to set of keys
     * @throws Exception if an error occurs
     */
    public DisplayObject(InterMineObject object, Model model, WebConfig webConfig,
                         Map webProperties, Map classKeys)
    throws Exception {
        this.object = object;
        this.model = model;
        this.webConfig = webConfig;
        this.webProperties = webProperties;
        this.classKeys = classKeys;
        clds = getLeafClds(object.getClass(), model);
    }

    /**
     * Get the set of leaf ClassDescriptors for a given InterMineObject class.
     * @param clazz object type
     * @param model model
     * @return Set of ClassDescriptor objects
     */
    public static Set<ClassDescriptor> getLeafClds(Class clazz, Model model) {
        Set<ClassDescriptor> leafClds = new LinkedHashSet<ClassDescriptor>();
        for (Iterator j = DynamicUtil.decomposeClass(clazz).iterator();
            j.hasNext();) {
            Class c = (Class) j.next();
            ClassDescriptor cld = model.getClassDescriptorByName(c.getName());
            if (cld != null) {
                leafClds.add(cld);
            }
        }
        return leafClds;
    }

    /**
     * Get the real business object
     * @return the object
     */
    public InterMineObject getObject() {
        return object;
    }

    /**
     * Get the id of this object
     * @return the id
     */
    public int getId() {
        return object.getId().intValue();
    }

    /**
     * Get the class descriptors for this object
     * @return the class descriptors
     */
    public Set<ClassDescriptor> getClds() {
        return clds;
    }

    /**
     * Get attribute descriptors.
     * @return map of attribute descriptors
     */
    public Map<String, FieldDescriptor> getAttributeDescriptors() {
        if (attributeDescriptors == null) {
            initialise();
        }
        return attributeDescriptors;
    }

    /**
     * Get the attribute fields and values for this object
     * @return the attributes
     */
    public Map<String, Object> getAttributes() {
        if (attributes == null) {
            initialise();
        }
        return attributes;
    }

    /**
     * Get the reference fields and values for this object
     * @return the references
     */
    public Map<String, DisplayReference> getReferences() {
        if (references == null) {
            initialise();
        }
        return references;
    }

    /**
     * Get the collection fields and values for this object
     * @return the collections
     */
    public Map<String, DisplayCollection> getCollections() {
        if (collections == null) {
            initialise();
        }
        return collections;
    }

    /**
     * Get all the reference and collection fields and values for this object
     * @return the collections
     */
    public Map<String, DisplayField> getRefsAndCollections() {
        if (refsAndCollections == null) {
            initialise();
        }
        return refsAndCollections;
    }


    /**
     * Return the path expressions for the fields that should be used when summarising this
     * DisplayObject.
     * @return the expressions
     */
    public List<String> getFieldExprs() {
        if (fieldExprs == null) {
            fieldExprs = new ArrayList<String>();
            for (Iterator<String> i = getFieldConfigMap().keySet().iterator(); i.hasNext();) {
                String fieldExpr = i.next();
                fieldExprs.add(fieldExpr);
            }
        }
        return fieldExprs;
    }

    /**
     * Get map from field expr to FieldConfig.
     * @return map from field expr to FieldConfig
     */
    public Map<String, FieldConfig> getFieldConfigMap() {
        if (fieldConfigMap == null) {
            fieldConfigMap = new LinkedHashMap<String, FieldConfig>();

            for (Iterator<ClassDescriptor> i = clds.iterator(); i.hasNext();) {
                ClassDescriptor cld = i.next();

                for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig, cld)) {
                    fieldConfigMap.put(fc.getFieldExpr(), fc);
                }
            }
        }
        return fieldConfigMap;
    }

    /**
     * Get the map indication whether individuals fields are to be display verbosely
     * @return the map
     */
    public Map<String, String> getVerbosity() {
        return Collections.unmodifiableMap(verbosity);
    }

    /**
     * Set the verbosity for a field
     * @param fieldName the field name
     * @param verbose true or false
     */
    public void setVerbosity(String fieldName, boolean verbose) {
        verbosity.put(fieldName, verbose ? fieldName : null);
    }

    /**
     * Get verbosity of a field
     * @param placementAndField a String that combines the name of the current placement/aspect and
     * a fieldName
     * @return true or false
     */
    public boolean isVerbose(String placementAndField) {
        return verbosity.get(placementAndField) != null;
    }

    /**
     * Create the Maps and Lists returned by the getters in this class.
     */
    private void initialise() {
        attributes = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        references = new TreeMap<String, DisplayReference>(String.CASE_INSENSITIVE_ORDER);
        collections = new TreeMap<String, DisplayCollection>(String.CASE_INSENSITIVE_ORDER);
        refsAndCollections = new TreeMap<String, DisplayField>(String.CASE_INSENSITIVE_ORDER);
        attributeDescriptors = new HashMap<String, FieldDescriptor>();

        try {
            for (Iterator<ClassDescriptor> i = clds.iterator(); i.hasNext();) {
                ClassDescriptor cld = i.next();

                for (Iterator j = cld.getAllFieldDescriptors().iterator(); j.hasNext();) {
                    FieldDescriptor fd = (FieldDescriptor) j.next();

                    if (fd.isAttribute() && !fd.getName().equals("id")) {
                        Object fieldValue = TypeUtil.getFieldValue(object, fd.getName());
                        if (fieldValue != null) {
                            attributes.put(fd.getName(), fieldValue);
                            attributeDescriptors.put(fd.getName(), fd);
                        }
                    } else if (fd.isReference()) {
                        ReferenceDescriptor ref = (ReferenceDescriptor) fd;
                        //check whether reference is null without dereferencing
                        ProxyReference proxy =
                            (ProxyReference) TypeUtil.getFieldProxy(object, ref.getName());
                        //if (proxy != null) {
                            DisplayReference newReference =
                                new DisplayReference(proxy, ref,
                                                     webConfig, webProperties, classKeys);
                            references.put(fd.getName(), newReference);
                        //}

                    } else if (fd.isCollection()) {
                        Object fieldValue = TypeUtil.getFieldValue(object, fd.getName());
                        DisplayCollection newCollection =
                            new DisplayCollection((Collection) fieldValue,
                                    (CollectionDescriptor) fd, webConfig, webProperties,
                                    classKeys);
                        //if (newCollection.getSize() > 0) {
                            collections.put(fd.getName(), newCollection);
                        //}
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while creating a DisplayObject", e);
        }

        // make a combined Map
        refsAndCollections.putAll(references);
        refsAndCollections.putAll(collections);
    }

/**
 * gets the fields to display on the object details page for this display object
 * @return map of fieldnames to display for this object
 */
    public Map<String, Object> getFieldValues() {
        if (fieldValues == null || fieldValues.isEmpty()) {
            fieldValues = new HashMap<String, Object>();
            for (Iterator<String> i = fieldExprs.iterator(); i.hasNext();) {
                String expr = i.next();
                Set<Class> classes = DynamicUtil.decomposeClass(object.getClass());
                String className = "";
                for (Class c : classes) {
                    if (!className.equals("")) {
                        className += ".";
                    }
                    className += c.getName();
                }
                if (className.indexOf('.') != -1) {
                    className = TypeUtil.unqualifiedName(className);
                }
                String pathString = className + "." + expr;
                Path path = new Path(model, pathString);
                fieldValues.put(expr, PathUtil.resolvePath(path, object));
            }
        }
        return fieldValues;
    }
}
