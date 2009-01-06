package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.webservice.server.TestUtil;
import org.intermine.webservice.server.output.Output;

/*
 * !!! When constructing url parameters must be url encoded.
 */


/**
 * Tests query result web service.  
 * Tests urls like: http://localhost:8080/service/query/results?query=...
 * 

 * @author Jakub Kulaviak
 **/
public class QueryResultTest extends TestCase
{
    
    private String serviceUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.serviceUrl = TestUtil.getServiceBaseURL() + "/query/results?";
    }

    /**
     * Tests tab output.
     * @throws Exception if some error occurs
     */
    public void testEmployeeTabOutput() throws Exception {
        String tabResult = getResultForQueryString("query=" + getQuery());
        List<List<String>> results = TestUtil.parseTabResult(tabResult);
        checkEmployees(results);
    }
     

    /**
     * Tests xml output that is formed information about employees.
     * @throws Exception if some error occurs
     */
    public void testEmployeeXMLOutput() throws Exception {
        String xmlResult = getResultForQueryString("format=xml&query=" + getQuery());
        List<List<String>> results = TestUtil.parseXMLResult(xmlResult);
        checkEmployees(results);
    }
    
    /**
     * Tests that error message appear when query xml is not well formatted.
     * @throws Exception when an error occurs
     */
    public void testErrorResponseMessage() throws Exception {
        /* There isn't probably way how to check error message generated by InterMinew 
         * (like <error><message>Invalid request</message></error>) with httpunit because
         * httpunit in case of http error throws exception without response body*/
        String req = getRequestString("query=a");
        String msg = TestUtil.getResponseMessage(req);
        assertEquals("Bad Request", msg);
    }
    
    public void testErrorXMLResponseCode() throws Exception {
        String req = getRequestString("query=a" + getQuery());
        assertEquals(Output.SC_BAD_REQUEST, TestUtil.getResponseCode(req));        
    }
    
    public String getServiceUrl() {
        return serviceUrl;
    }
    
    private void checkEmployees(List<List<String>> results) {
        TestUtil.checkEmployee(results.get(0), "EmployeeA1", "10", "1", "true");
        TestUtil.checkEmployee(results.get(1), "EmployeeA2", "20", "2", "true");
        TestUtil.checkEmployee(results.get(2), "EmployeeA3", "30", "3", "false");
        TestUtil.checkEmployee(results.get(3), "EmployeeB1", "40", "4", "true");
        TestUtil.checkEmployee(results.get(4), "EmployeeB2", "50", "5", "true");
        TestUtil.checkEmployee(results.get(5), "EmployeeB3", "60", "6", "true");
    }

    private String getResultForQueryString(String parameterString) throws Exception {
        return TestUtil.getResult(getRequestString(parameterString));
    }

    private String getRequestString(String parameterString) {
        return getServiceUrl() + parameterString;
    }

    private String getQuery() throws IOException {
        //BufferedReader br = new BufferedReader(new FileReader("/home/jakub/svn/dev/testmodel/webapp/test/resources/ServiceServletTest1.xml"));
        InputStream is = getClass().getClassLoader().getResourceAsStream("ServiceServletTest.xml");
        if (is == null) {
            throw new FileNotFoundException("ServiceServletTest.xml wasn't found.");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String ret = "";
        String l;
        while ((l = br.readLine()) != null) {
            ret = ret + l;
        }
        // !!! must be encoded
        ret = URLEncoder.encode(ret, "UTF-8");
        return ret;
    }
}
