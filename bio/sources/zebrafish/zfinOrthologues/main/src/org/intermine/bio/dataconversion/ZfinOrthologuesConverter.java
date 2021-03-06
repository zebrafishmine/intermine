package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;


/**
 * @author
 */
public class ZfinOrthologuesConverter extends BioFileConverter {

    private static final Logger LOG = Logger.getLogger(ZfinOrthologuesConverter.class);
    //protected String organismRefId;
    private Map<String, Item> homologues = new HashMap();
    private Map<String, Item> genes = new HashMap();
    private Map<String, Item> orthos = new HashMap();
    private Map<String, Item> codes = new HashMap();
    private Map<String, Item> orthoEvs = new HashMap();
    private Map<String, Item> externalGenes = new HashMap();
    private Map<String, Item> links = new HashMap();
    private Map<String, Item> linkDbs = new HashMap();
    private Map<String, Item> pubs = new HashMap();
    private Map<String, Item> organisms = new HashMap();

    /**
     * Constructor
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @param model  the Model
     */
    public ZfinOrthologuesConverter(ItemWriter writer, Model model)
            throws ObjectStoreException {
        super(writer, model, "ZFIN", "ZFIN Curated Human, Mouse, Fly, Yeast Orthologue Data Set");

    }

    public void process(Reader reader) throws Exception {


        processOrthos(reader);

        try {
            for (Item ortho : orthos.values()) {
                store(ortho);
            }
        }
        catch (ObjectStoreException e) {
            throw new Exception(e);

        }
    }

    public void processOrthos(Reader reader) throws Exception {

        Iterator lineIter = FormattedTextParser.parseDelimitedReader(reader, '|');

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            if (line.length < 2) {
                throw new RuntimeException("Line does not have enough elements: " + line.length + line[0]);
            }

            String orthoPrimaryIdentifier = line[0];
            String genePrimaryIdentifier = line[1];
            String orthoSpecies = line[2];
            String orthoAbbrev = line[3];
            String orthoName = line[5];
            String orthoLG = line[6];
            String orthoAccession = line[8];
            String orthoForeignDBName = line[9];
            String orthoFdbType = line[10];
            String orthoEvidenceCode = line[12];
            String orthoPub = line[13];
            String dblinkPrimaryIdentifier = line[14];

            Item gene = getGene(genePrimaryIdentifier);
	    
            Item ortho = getOrtho(orthoPrimaryIdentifier, orthoEvidenceCode, orthoPub);
	    //System.out.println(orthoEvidenceCode);
	    if (orthoPrimaryIdentifier.equals("ZDB-ORTHO-050503-12")){
		System.out.println(orthoPrimaryIdentifier);
		System.out.println (orthoEvidenceCode);
		System.out.println(genePrimaryIdentifier);
		//System.out.println(gene.getReference("PrimaryIdentifier"));
	    }
            Item externalGene = getExternalGene(orthoAbbrev, orthoName, orthoSpecies, orthoPrimaryIdentifier);
            Item externalLink = getExternalLink(dblinkPrimaryIdentifier, orthoAccession, orthoFdbType, orthoForeignDBName);

            ortho.setReference("gene", gene);
	    //	    System.out.println (ortho.getAttribute("PrimaryIdentifier"));

	    ortho.setReference("homologue", externalGene);
            ortho.setAttribute("type", "orthologue");

            ortho.addToCollection("crossReferences", externalLink);
            if (!StringUtils.isEmpty(orthoLG)) {
                ortho.setAttribute("Chromosome", orthoLG);
            }
            gene.addToCollection("homologues", ortho);


        }
    }

    private Item getGene(String primaryIdentifier)
            throws SAXException {
        Item item = genes.get(primaryIdentifier);
        if (item == null) {
            item = createItem("Gene");
            item.setReference("organism", getOrganism("7955"));
            item.setAttribute("primaryIdentifier", primaryIdentifier);
            genes.put(primaryIdentifier, item);
            try {
                store(item);
            }
            catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return item;
    }

    private Item getExternalLink(String primaryIdentifier, String accessionNumber, String orthoFdbType, String orthoForeignDBName)
	throws SAXException {
        Item item = links.get(primaryIdentifier);
        if (item == null) {
            item = createItem("CrossReference");
            item.setAttribute("identifier", accessionNumber);
            item.setAttribute("linkType", orthoFdbType);
            //item.setAttribute("primaryIdentifier", primaryIdentifier);
            item.setReference("source", getLinkDb(orthoForeignDBName));
            links.put(primaryIdentifier, item);
            try {
                store(item);
            }
            catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return item;
    }

    private Item getLinkDb(String name)
            throws SAXException {
        Item item = linkDbs.get(name);
        if (item == null) {
            item = createItem("DataSource");
            item.setAttribute("name", name);
            linkDbs.put(name, item);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return item;
    }

    private Item getExternalGene(String orthoAbbrev, String orthoName, String orthoSpecies, String orthoPrimaryIdentifier)
            throws SAXException {
        Item item = externalGenes.get(orthoName.concat(orthoSpecies));
        if (item == null) {
            item = createItem("Gene");
            if (!StringUtils.isEmpty(orthoName)) {
                item.setAttribute("name", orthoName);
            } else {
		System.out.println("is OrthoName null");
                System.out.println(orthoAbbrev);
            }
            item.setAttribute("symbol", orthoAbbrev);
            item.setAttribute("primaryIdentifier", orthoPrimaryIdentifier);
            item.setReference("organism", getOrganism(getTaxon(orthoSpecies)));
            externalGenes.put(orthoName.concat(orthoSpecies), item);
            try {
                store(item);
            }
            catch (ObjectStoreException e) {
                throw new SAXException(e);
            }

        }
        return item;
    }

    private Item getOrtho(String orthoPrimaryIdentifier, String codeAbbrev, String orthoPub)
            throws SAXException {
        Item item = orthos.get(orthoPrimaryIdentifier);
        if (item == null) {
            item = createItem("Homologue");
            item.setAttribute("primaryIdentifier", orthoPrimaryIdentifier);
            orthos.put(orthoPrimaryIdentifier, item);
	    //           Item orthoEv = getOrthoEv(orthoPrimaryIdentifier, codeAbbrev, orthoPub);
            //item.addToCollection("evidence", orthoEv);
        }
	Item orthoEv = getOrthoEv(orthoPrimaryIdentifier, codeAbbrev, orthoPub);
	item.addToCollection("evidence", orthoEv);
        return item;
    }

    private Item getOrthoEv(String primaryIdentifier, String codeAbbrev, String orthoPub)
            throws SAXException {
        String orthoEvPK = primaryIdentifier.concat(codeAbbrev);
	String fullKey = orthoEvPK.concat(orthoPub);
	//System.out.println(fullKey);
	Item item = orthoEvs.get(fullKey);
        if (item == null) {
            item = createItem("OrthologueEvidence");
            item.addToCollection("publications", getPub(orthoPub));
	    item.setAttribute("primaryIdentifier", fullKey);
            orthoEvs.put(orthoEvPK, item);
            Item code = getCode(codeAbbrev);
            item.setReference("evidenceCode", code);
            try {
                store(item);
            }
            catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return item;
    }

    private Item getCode(String abbreviation)
            throws SAXException {
        Item item = codes.get(abbreviation);
        if (item == null) {
	    System.out.println(abbreviation);
            item = createItem("OrthologueEvidenceCode");
            item.setAttribute("abbreviation", abbreviation);
            codes.put(abbreviation, item);
            try {
                store(item);
            }
            catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return item;
    }

    private Item getPub(String primaryIdentifier)
            throws SAXException {
        Item item = pubs.get(primaryIdentifier);
        if (item == null) {
            item = createItem("Publication");
            item.setAttribute("primaryIdentifier", primaryIdentifier);
            //item.setReference("organism", getOrganism("7955"));
            pubs.put(primaryIdentifier, item);

            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return item;
    }

    private String getTaxon(String species)
            throws SAXException {
        String taxon = "";
            if (species.equals("Zebrafish")) {
		taxon = "7955";
            } else if (species.equals("Human")) {
                taxon = "9606";
            } else if (species.equals("Mouse")) {
                taxon = "10090";
            } else if (species.equals("Fruit fly")) {
                taxon = "7227";
            } else if (species.equals("Yeast")) {
                taxon = "4932";
            } else {
                System.out.println(species + "species is not in group");
            }
           
        return taxon;
    }


}
