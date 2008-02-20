package org.intermine.webservice;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Jakub Kulaviak
 **/
public class PagedServiceInput extends WebServiceInput
{
    
    private Integer start;

    private Integer maxCount;

    /**
     * Sets from which index should be results returned 1-based.
     * @param start start
     */
    public void setStart(Integer start) {
        this.start = start;
    }

    /**
     * Sets maximum of returned results.
     * @param maxCount maximal count
     */
    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    /**
     * Returns index of first returned result
     * @return index of first returned result
     */
    public Integer getStart() {
        return start;
    }

    /**
     * Returns maximum count of results do be returned.
     * @return maximum count
     */
    public Integer getMaxCount() {
        return maxCount;
    }
}
