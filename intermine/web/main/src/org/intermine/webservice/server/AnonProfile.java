package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.template.ApiTemplate;

/**
 * A null-object for profiles for use with the web services.
 * This profile will act as through the user
 * has no history of any kind. Any attempt to save history will be discarded.
 *
 * @author Alex Kalderimis
 *
 */
final class AnonProfile extends Profile
{
    /**
     * Constructor
     */
    @SuppressWarnings("rawtypes")
    public AnonProfile() {
        super(null, null, null, null,
                new HashMap<String, SavedQuery>(), new HashMap<String, InterMineBag>(),
                new HashMap<String, ApiTemplate>(), null, true);
        savedQueries = new DevNullMap<String, SavedQuery>();
        savedBags = new DevNullMap<String, InterMineBag>();
        savedTemplates = new DevNullMap<String, ApiTemplate>();
        savedInvalidBags = new DevNullMap<String, InterMineBag>();
        queryHistory = new DevNullMap();
        savingDisabled = true;
    }

    @Override
    public void enableSaving() {
        throw new RuntimeException("Saving not supported by the Anonymous Profile");
    }

    @Override
    public boolean isSuperuser() {
        return false;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public InterMineBag createBag(String name, String type, String desc,
            Map classKeys) {
        throw new RuntimeException("Saving not supported by the Anonymous Profile");
    }

    @Override
    public String getUsername() {
        return "__ANONYMOUS_USER__";
    }
}
