package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;


/**
 *
 * @author Julie Sullivan
 */
public class EnsemblSnpConverter extends BioFileConverter
{

    private static final String DATASET_TITLE = "Ensembl SNP";
    private static final String DATA_SOURCE_NAME = "Ensembl";

    private Map<String, String> transcripts = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public EnsemblSnpConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * TODO set taxon ID and chromosome
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        System.out.println("READER");
        BufferedReader bReader = (BufferedReader) reader;
        GFF3Record record;
        for (Iterator<?> i = GFF3Parser.parse(bReader); i.hasNext();) {
            record = (GFF3Record) i.next();

            String type = record.getType();
            String className = TypeUtil.javaiseClassName(type);
            Item snp = createItem(className);

            List<String> dbxrefs = record.getAttributes().get("Dbxref");
            String identifier = getIdentifier(dbxrefs);
            if (identifier != null) {
                snp.setAttribute("primaryIdentifier", identifier);
            }

            List<String> variantSeqs = record.getAttributes().get("Variant_seq");
            if (variantSeqs != null && variantSeqs.size() > 0) {
                snp.setAttribute("variantSequence", variantSeqs.get(0));
            }
            List<String> referenceSeqs = record.getAttributes().get("Reference_seq");
            if (referenceSeqs != null && referenceSeqs.size() > 0) {
                snp.setAttribute("referenceSequence", referenceSeqs.get(0));
            }

            List<String> variantEffects = record.getAttributes().get("Variant_effect");
            if (variantEffects == null || variantEffects.isEmpty()) {
                return;
            }
            for (String effect : variantEffects) {
                // Variant_effect=upstream_gene_variant 0 transcript ENST00000519787
                String transcriptIdentifier = getTranscriptIdentifier(effect);

                Item consequence = createItem("Consequence");
                consequence.setAttribute("description", effect);
                if (StringUtils.isNotEmpty(transcriptIdentifier)) {
                    String transcriptRefId;
                    try {
                        transcriptRefId = getTranscript(transcriptIdentifier);
                        consequence.setReference("transcript", transcriptRefId);
                    } catch (ObjectStoreException e) {
                        throw new RuntimeException("Can't store transcript", e);
                    }
                }
                snp.addToCollection("consequences", consequence);
                try {
                    store(consequence);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("Can't store consequence", e);
                }
            }
            store(snp);
        }
    }

    private String getTranscript(String identifier) throws ObjectStoreException {
        String refId = transcripts.get(identifier);
        if (refId == null) {
            Item transcript = createItem("Transcript");
            transcript.setAttribute("primaryIdentifier", identifier);
            refId = transcript.getIdentifier();
            transcripts.put(identifier, refId);
            store(transcript);
        }
        return refId;
    }

    private String getIdentifier(List<String> xrefs) {
        if (xrefs == null) {
            return null;
        }
        for (String xref : xrefs) {
            String bits[] = xref.split(":");
            if (xref.length() == 2) {
                String identifier = bits[1];
                if (identifier != null && identifier.startsWith("rs")) {
                    return identifier;
                }
            }
        }
        return null;
    }

    private String getTranscriptIdentifier(String description) {
        if (description == null) {
            return null;
        }
        String bits[] = description.split(" ");
        for (String word : bits) {
            if (word != null && word.startsWith("ENST")) {
                return word;
            }
        }
        return null;
    }
}
