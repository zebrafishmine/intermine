package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;

/**
 * ID resolver for ZFIN genes.
 *
 * @author Fengyuan Hu
 */
public class ZfinGeneIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(ZfinGeneIdResolverFactory.class);
    private final String clsName = "gene";

    // data file path set in ~/.intermine/MINE.properties
    // e.g. resolver.zfin.file=/micklem/data/zfin/identifiers/zebrafishGeneToEnsdarg.txt
    private final String propName = "resolver.zfin.file";
    private final String taxonId = "7955";

    /**
     * Build an IdResolver from Entrez Gene gene_info file
     * @return an IdResolver for Entrez Gene
     */
    @Override
    protected IdResolver createIdResolver() {
        Properties props = PropertiesUtil.getProperties();
        String fileName = props.getProperty(propName);

        if (StringUtils.isBlank(fileName)) {
            String message = "ZFIN gene resolver has no file name specified, set " + propName
                + " to the location of the gene_info file.";
            LOG.warn(message);
            return null;
        }

        IdResolver resolver;
        BufferedReader reader;
        try {
            FileReader fr = new FileReader(new File(fileName));
            reader = new BufferedReader(fr);
            resolver = createFromFile(reader);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Failed to open zfin id mapping file: "
                    + fileName, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading from zfin id mapping file: "
                    + fileName, e);
        }

        return resolver;
    }

    private IdResolver createFromFile(BufferedReader reader) throws IOException {
        IdResolver resolver = new IdResolver(clsName);

        // ZDB-GENE-id | ENSDARG id
        Iterator<?> lineIter = FormattedTextParser.parseDelimitedReader(reader, '|');
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String zfinId = line[0];
            String ensemblId = line[1];

            resolver.addMainIds(taxonId, zfinId, Collections.singleton(zfinId));
            resolver.addSynonyms(taxonId, zfinId, Collections.singleton(ensemblId));
        }
        return resolver;
    }
}
