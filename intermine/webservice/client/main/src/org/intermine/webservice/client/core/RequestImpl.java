package org.intermine.webservice.client.core;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.URLParser;


/**
 * Implementation of Request interface.
 * @author Jakub Kulaviak
 **/
public class RequestImpl implements Request
{

    private RequestType type;

    private String serviceUrl;

    private ContentType contentType;

    private Map<String, List<String>> parameters = new HashMap<String, List<String>>();

    private Map<String, String> headers = new HashMap<String, String>();

    /**
     * Constructor.
     */
    public RequestImpl() {
    }

    /**
     * Constructor.
     * @param type type
     * @param url URL
     * @param contentType content type
     */
    public RequestImpl(RequestType type, String url, ContentType contentType) {
        this.type = type;
        setUrl(url);
        this.contentType = contentType;
    }

    /**
     * {@inheritDoc}
     */
    public void addParameter(String name, String value) {
        List<String> values = getParameterValues(name);
        if (values == null) {
            values = new ArrayList<String>();
            parameters.put(name, values);
        }
        values.add(value);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getParameterValues(String name) {
        return parameters.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public String getParameter(String name) {
        List<String>  pars = getParameterValues(name);
        if (pars != null && pars.size() > 0) {
            return pars.get(0);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getParameterNames() {
        return parameters.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public void setParameter(String name, String value) {
        List<String> values = parameters.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            parameters.put(name, values);
        }
        values.clear();
        values.add(value);
    }

    /**
     * {@inheritDoc}
     */
    public RequestType getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public void setType(RequestType type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * {@inheritDoc}
     */
    public void setServiceUrl(String url) {
        this.serviceUrl = url;
    }

    /**
     * {@inheritDoc}
     */
    public void setUrl(String url) {
        try {
            this.serviceUrl = URLParser.parseServiceUrl(url);
            this.parameters = URLParser.parseParameterMap(url);
        } catch (MalformedURLException e) {
            throw new ServiceException("Invalid url: " + url, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * {@inheritDoc}
     */
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, List<String>> getParameterMap() {
        return parameters;
    }

    /**
     * {@inheritDoc}
     */
    public String getUrl(boolean encode) {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceUrl);
        String separator = "?";
        for (String parName : parameters.keySet()) {
            for (String value : parameters.get(parName)) {
                sb.append(separator);
                if (separator.equals("?")) {
                    separator = "&";
                }
                sb.append(parName);
                if (value.length() > 0) {
                    sb.append("=");
                    sb.append(format(value, encode));
                }
            }
        }
        return sb.toString();
    }

    private String format(String str, boolean encode) {
        if (encode) {
            try {
                return URLEncoder.encode(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new ServiceException("URL encoding failed for string " + str, e);
            }
        } else {
            return str;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * {@inheritDoc}
     */
    public String getHeader(String name) {
        return headers.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return getUrl(true);
    }
}
