/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.rest.api.service;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.lib.to.RealmTO;

/**
 * REST operations for realms.
 */
@Path("realms")
public interface RealmService extends JAXRSService {

    /**
     * Returns a list of all realms.
     *
     * @return list of all realms.
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<RealmTO> list();

    /**
     * Returns realms rooted at the given path.
     *
     * @param fullPath full path of the root realm where to read from
     * @return realms rooted at the given path
     */
    @GET
    @Path("{fullPath:.*}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<RealmTO> list(@NotNull @PathParam("fullPath") String fullPath);

    /**
     * Creates a new realm under the given path.
     *
     * @param parentPath full path of the parent realm
     * @param realmTO new realm.
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created realm
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created realm")
    })
    @POST
    @Path("{parentPath:.*}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull @PathParam("parentPath") String parentPath, @NotNull RealmTO realmTO);

    /**
     * Updates the realm under the given path.
     *
     * @param realmTO realm to be stored
     */
    @PUT
    @Path("{fullPath:.*}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@NotNull RealmTO realmTO);

    /**
     * Deletes the notification matching the given key.
     *
     * @param fullPath key for notification to be deleted
     */
    @DELETE
    @Path("{fullPath:.*}")
    void delete(@NotNull @PathParam("fullPath") String fullPath);
}
