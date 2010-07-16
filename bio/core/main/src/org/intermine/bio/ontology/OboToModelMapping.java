package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * This class handles the ontologies for OboToModel.
 *
 * @author Julie Sullivan
 */
public class OboToModelMapping
{
    private String namespace;
    private Map<String, Set<String>> childToParents, parentToChildren, partOfs;
    // SO terms to filter on, eg. sequence_feature
    private Set<String> termsToKeep = new HashSet<String>();
    // copy of termsToKeep without underscores, this allows the terms to be either SO terms or
    // java classes
    private Set<String> termsToKeepNoUnderscores = new HashSet<String>();

    // list of classes to load into the model.  OboOntology is an object that contains the SO term
    // value eg. sequence_feature and the Java name, eg. org.intermine.bio.SequenceFeature
    private Map<String, OboOntology> validOboTerms = new HashMap<String, OboOntology>();

    // contains ALL non-obsolete terms.  key = sequence_feature, value = SO:001
    private Map<String, String> soNameToIdentifier = new HashMap<String, String>();

    /**
     * Constructor.
     *
     * @param termsToKeep list of terms to filter the results on
     * @param namespace the namespace to use in generating URI-based identifiers
     */
    public OboToModelMapping(Set<String> termsToKeep, String namespace) {
        this.namespace = namespace;
        this.termsToKeep = termsToKeep;
        stripTerms();
    }

    // make a copy of the terms without underscores, just in case the terms are Java classes
    private void stripTerms() {
        for (String term : termsToKeep) {
            term = term.toLowerCase();
            termsToKeepNoUnderscores.add(StringUtils.replace(term, "_", ""));
        }
    }

    /**
     * Returns name of the package, eg. org.intermine.bio.
     *
     * @return name of the package, eg. org.intermine.bio
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param childIdentifier the oboterm to get relationships for
     * @return all collections for given class
     */
    public Set<String> getPartOfs(String childIdentifier) {
        return partOfs.get(childIdentifier);
    }

    /**
     * Test is class is in the model.  Only used after the obo terms have been processed and
     * trimmed.
     *
     * @param identifier for obo term
     * @return true if the class is in the model
     */
    public boolean classInModel(String identifier) {
        return validOboTerms.containsKey(identifier);
    }

    /**
     * Test whether a term is in the list the user provided.
     *
     * @param identifier for obo term
     * @return false if oboterm isn't in list user provided, true if term in list or list empty
     */
    private boolean validTerm(String identifier) {
        OboOntology o = validOboTerms.get(identifier);
        if (o == null) {
            return false;
        }
        String oboName = o.getOboTermName();
        String javaName = StringUtils.replace(oboName, "_", "");
        if (termsToKeep.isEmpty() || termsToKeep.contains(oboName)
                || termsToKeepNoUnderscores.contains(javaName)) {
            return true;
        }
        return false;
    }

    // this term has children
    private boolean validParent(String identifier) {
        return parentToChildren.containsKey(identifier);
    }

    /**
     * @param childIdentifier identifier for obo term of interest
     * @return list of identifiers for parent obo terms
     */
    public Set<String> getParents(String childIdentifier) {
        return childToParents.get(childIdentifier);
    }

    /**
     * @param identifier for obo term
     * @return name of term, eg. sequence_feature
     */
    public String getName(String identifier) {
        OboOntology o = validOboTerms.get(identifier);
        if (o == null) {
            return null;
        }
        return o.getOboTermName();
    }

    /**
     * Returns list of (valid for this model) obo term identifiers.
     *
     * @return set of obo term identifiers to process, eg. SO:001
     */
    public Set<String> getOboTermIdentifiers() {
        return validOboTerms.keySet();
    }

    /**
     * Processes obo relations from OBOEdit.  Parses each relationship and builds collections and
     * parents.  Also assigns grandchildren the collections of the grandparents and trims/flattens
     * terms if the user has provided a terms list.
     *
     * @param oboRelations List of obo relations from OBOEdit
     */
    public void processRelations(List<OboRelation> oboRelations) {
        childToParents = new HashMap<String, Set<String>>();
        partOfs = new HashMap<String, Set<String>>();
        for (OboRelation r : oboRelations) {
            String child = r.childTermId;
            String parent = r.parentTermId;
            if (StringUtils.isEmpty(child) || StringUtils.isEmpty(parent) || !classInModel(child)) {
                continue;
            }
            String relationshipType = r.getRelationship().getName();
            if (relationshipType.equals("part_of") && r.direct) {
                Set<String> colls = partOfs.get(child);
                if (colls == null) {
                    colls = new HashSet<String>();
                    partOfs.put(child, colls);
                }
                colls.add(parent);
            } else if (relationshipType.equals("is_a") && r.direct) {
                Set<String> parents = childToParents.get(child);
                if (parents == null) {
                    parents = new HashSet<String>();
                    childToParents.put(child, parents);
                }
                parents.add(parent);
            }
        }

        buildParentsMap();

        assignPartOfsToGrandchildren(oboRelations, partOfs);

        if (!termsToKeep.isEmpty()) {
            trimModel();
        }
    }

    private void assignPartOfsToGrandchildren(List<OboRelation> oboRelations,
            Map<String, Set<String>> namesToPartOfs) {
        for (OboRelation r : oboRelations) {
            String child = r.childTermId;
            String parent = r.parentTermId;

            String relationshipType = r.getRelationship().getName();
            if (relationshipType.equals("part_of") && r.direct) {
                Set<String> grandchildren = parentToChildren.get(child);
                if (grandchildren == null || grandchildren.isEmpty()) {
                    continue;
                }
                for (String grandchild : grandchildren) {
                    Set<String> grandchildPartOfs = partOfs.get(grandchild);
                    if (grandchildPartOfs == null) {
                        grandchildPartOfs = new HashSet<String>();
                        partOfs.put(grandchild, grandchildPartOfs);
                    }
                    grandchildPartOfs.add(parent);
                }
            }
        }
    }

    // build parent --> children map
    private void buildParentsMap() {
        parentToChildren = new HashMap<String, Set<String>>();
        for (String child : childToParents.keySet()) {
            Set<String> parents = childToParents.get(child);
            for (String parent : parents) {
                Set<String> kids = parentToChildren.get(parent);
                if (kids == null) {
                    kids = new HashSet<String>();
                    parentToChildren.put(parent, kids);
                }
                kids.add(child);
            }
        }
    }

    private void trimModel() {

        Map<String, OboOntology> oboTermsCopy = new HashMap<String, OboOntology>(validOboTerms);

        System.out .println("Total terms: " + validOboTerms.size());

        for (String oboTermIdentifier : oboTermsCopy.keySet()) {
            prune(oboTermIdentifier);
        }

        System.out .println("Total terms, post-pruning: " + validOboTerms.size());

        oboTermsCopy = new HashMap<String, OboOntology>(validOboTerms);

        for (String oboTermIdentifier : oboTermsCopy.keySet()) {
            if (!validTerm(oboTermIdentifier)) {
                flatten(oboTermIdentifier);
            }
        }

        System.out .println("Total terms, post-flattening: " + validOboTerms.size());
    }

    /*
     * remove term if:
     *  1. not in list of desired terms
     *  2. no children
     */
    private void prune(String oboTermIdentifier) {
        // process each child term
        if (parentToChildren.get(oboTermIdentifier) != null) {
            Set<String> children = new HashSet<String>(parentToChildren.get(oboTermIdentifier));
            for (String child : children) {
                prune(child);
            }
        }

        // if this term has no children AND it's not on our list = DELETE
        if (!validParent(oboTermIdentifier) && !validTerm(oboTermIdentifier)) {
            removeTerm(oboTermIdentifier);
            debugOutput(oboTermIdentifier, "Pruning [no children, not on list]");
        }
    }

    /*
     * remove term if not on list AND:
     *  (a) term has only ONE parent and ONE child
     *  (b) term has only ONE parent and NO children
     *  (c) term has NO parents and only ONE child
     */
    private void flatten(String oboTerm) {

        // process children of this term first
        // can't do this, `Exception in thread "main" java.lang.StackOverflowError`
        //        if (parentNamesToChildNames.get(oboTerm) != null) {
        //            Set<String> children = new HashSet(parentNamesToChildNames.get(oboTerm));
        //            for (String child : children) {
        //                flatten(child);
        //            }
        //        }

        Set<String> parents = childToParents.get(oboTerm);
        Set<String> kids = parentToChildren.get(oboTerm);

        // has both parents and children
        if (parents != null && kids != null) {

            // multiple parents and children.  can't flatten.
            if (parents.size() > 1 && kids.size() > 1) {
                return;
            }

            // term only has one parent.  remove term and assign this terms parents and children
            // to each other
            if (parents.size() == 1) {
                String parent = parents.toArray()[0].toString();

                // add children to new parent
                parentToChildren.get(parent).addAll(kids);

                // add parent to new children
                for (String kid : kids) {
                    Set<String> otherParents = childToParents.get(kid);
                    transferPartOfs(oboTerm, kid);
                    otherParents.remove(oboTerm);
                    otherParents.add(parent);
                }
                removeTerm(oboTerm);
                debugOutput(oboTerm, "Flattening [term had only one parent]");
                return;
            }

            // term has only one child.  remove term and assign child to new parents.
            if (kids.size() == 1) {
                String kid = kids.toArray()[0].toString();

                // add parents to new kid
                childToParents .get(kid).addAll(parents);

                // pass down any relationships before parent term is removed from tree
                transferPartOfs(oboTerm, kid);

                // reassign parents to new kid
                for (String parent : parents) {
                    Set<String> otherChildren = parentToChildren.get(parent);
                    otherChildren.remove(oboTerm);
                    otherChildren.add(kid);
                }
                removeTerm(oboTerm);
                debugOutput(oboTerm, "Flattening [term had only one child]");
                return;
            }

            // root term
        } else if (parents == null) {
            removeTerm(oboTerm);
            debugOutput(oboTerm, "Flattening [root term]");
        }

        // no children, delete!
        if (kids == null) {
            removeTerm(oboTerm);
            debugOutput(oboTerm, "Flattening [no children] ");
        }
    }

    private void debugOutput(String oboTerm, String err) {
        err += oboTerm + "-count: " + validOboTerms.size();
        System.out .println(err);
    }

    // pass down any relationships before parent term is removed from tree
    // parent will be removed from partOf map in removeTerm() - only after all children have
    // been processed
    private void transferPartOfs(String parentOboTerm, String childOboTerm) {
        Set<String> parentPartOfs = partOfs.get(parentOboTerm);
        if (parentPartOfs == null || parentPartOfs.isEmpty()) {
            return;
        }
        partOfs.put(childOboTerm, new HashSet<String>(parentPartOfs));
    }

    // remove term from every map
    private void removeTerm(String oboTerm) {
        validOboTerms.remove(oboTerm);

        childToParents.remove(oboTerm);
        parentToChildren.remove(oboTerm);
        partOfs.remove(oboTerm);
        removeCollections(oboTerm);

        // remove mention in maps
        Map<String, Set<String>> mapCopy
            = new HashMap<String, Set<String>>(parentToChildren);
        for (Map.Entry<String, Set<String>> entry : mapCopy.entrySet()) {
            String parent = entry.getKey();
            Set<String> children = entry.getValue();

            // remove current term
            children.remove(oboTerm);

            // if parent is childless, remove
            if (children.size() == 0) {
                parentToChildren.remove(parent);
            }
        }

        mapCopy = new HashMap<String, Set<String>>(childToParents);
        for (Map.Entry<String, Set<String>> entry : mapCopy.entrySet()) {
            String child = entry.getKey();
            Set<String> parents = entry.getValue();

            // remove current term
            parents.remove(oboTerm);

            // if child has no parents remove from p
            if (parents.size() == 0) {
                childToParents.remove(child);
            }
        }
    }

    private void removeCollections(String oboTerm) {
        Map<String, Set<String>> mapCopy = new HashMap<String, Set<String>>(partOfs);
        for (Map.Entry<String, Set<String>> entry : mapCopy.entrySet()) {
            Set<String> collections = entry.getValue();
            if (collections.contains(oboTerm)) {
                collections.remove(oboTerm);
            }
        }
    }

    /**
     * For each term in our list, add to our map if the term is not obsolete.  List has a multi
     * key, so term can later be retrieved by identifier (SO:001) or fully qualified name
     * (eg. org.intermine.org.bio.SequenceFeature).
     *
     * @param terms set of obo terms to process
     */
    public void processOboTerms(Set<OboTerm> terms) {
        for (OboTerm term : terms) {
            if (!term.isObsolete()) {
                String identifier = term.getId().trim();
                String name = term.getName().trim();
                if (!StringUtils.isEmpty(identifier) && !StringUtils.isEmpty(name)) {
                    OboOntology c = new OboOntology(identifier, name);
                    validOboTerms.put(identifier, c);
                    soNameToIdentifier.put(name, identifier);
                }
            }
        }
    }

    /**
     * Represents a class/oboterm in the Model.
     *
     * @author julie sullivan
     *
     */
    public class OboOntology
    {
        private String oboTermIdentifier, oboTermName;

        /**
         * The constructor.
         *
         * @param oboTermIdentifier the sequence ontology term identifier, eg. SO:001
         * @param oboTermName name of so term, eg. sequence_feature
         */
        protected OboOntology(String oboTermIdentifier, String oboTermName) {
            super();
            this.oboTermIdentifier = oboTermIdentifier;
            this.oboTermName = oboTermName;
        }

        /**
         * @return the identifier, eg. SO:001
         */
        protected String getOboTermIdentifier() {
            return oboTermIdentifier;
        }

        /**
         * @return obo term name, eg. sequence_feature
         */
        protected String getOboTermName() {
            return oboTermName;
        }
    }
}
