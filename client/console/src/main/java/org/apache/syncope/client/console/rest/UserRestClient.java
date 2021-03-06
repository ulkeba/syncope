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
package org.apache.syncope.client.console.rest;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.ResourceAssociationMod;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceAssociationActionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.wrap.ResourceKey;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking rest users services.
 */
@Component
public class UserRestClient extends AbstractAnyRestClient {

    private static final long serialVersionUID = -1575748964398293968L;

    @Override
    public int count(final String realm) {
        return getService(UserService.class).
                list(SyncopeClient.getAnyListQueryBuilder().realm(realm).page(1).size(1).build()).
                getTotalCount();
    }

    @Override
    public List<UserTO> list(final String realm, final int page, final int size, final SortParam<String> sort) {
        return getService(UserService.class).
                list(SyncopeClient.getAnyListQueryBuilder().realm(realm).page(page).size(size).
                        orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    public UserTO create(final UserTO userTO, final boolean storePassword) {
        Response response = getService(UserService.class).create(userTO, storePassword);
        return response.readEntity(UserTO.class);
    }

    public UserTO update(final String etag, final UserMod userMod) {
        UserTO result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            result = service.update(userMod).readEntity(UserTO.class);
            resetClient(UserService.class);
        }
        return result;
    }

    @Override
    public UserTO delete(final String etag, final Long id) {
        UserTO result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            result = service.delete(id).readEntity(UserTO.class);
            resetClient(UserService.class);
        }
        return result;
    }

    public UserTO read(final Long id) {
        UserTO userTO = null;
        try {
            userTO = getService(UserService.class).read(id);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a user", e);
        }
        return userTO;
    }

    @Override
    public int searchCount(final String realm, final String fiql) {
        return getService(UserService.class).
                search(SyncopeClient.getAnySearchQueryBuilder().realm(realm).fiql(fiql).page(1).size(1).build()).
                getTotalCount();
    }

    @Override
    public List<UserTO> search(
            final String realm, final String fiql, final int page, final int size, final SortParam<String> sort) {

        return getService(UserService.class).
                search(SyncopeClient.getAnySearchQueryBuilder().realm(realm).fiql(fiql).page(page).size(size).
                        orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    @Override
    public ConnObjectTO readConnObject(final String resourceName, final Long id) {
        return getService(ResourceService.class).readConnObject(resourceName, AnyTypeKind.USER.name(), id);
    }

    public void suspend(final String etag, final long userKey, final List<StatusBean> statuses) {
        StatusMod statusMod = StatusUtils.buildStatusMod(statuses, false);
        statusMod.setType(StatusMod.ModType.SUSPEND);
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            service.status(userKey, statusMod);
            resetClient(UserService.class);
        }
    }

    public void reactivate(final String etag, final long userKey, final List<StatusBean> statuses) {
        StatusMod statusMod = StatusUtils.buildStatusMod(statuses, true);
        statusMod.setType(StatusMod.ModType.REACTIVATE);
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            service.status(userKey, statusMod);
            resetClient(UserService.class);
        }
    }

    @Override
    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(UserService.class).bulk(action);
    }

    public void unlink(final String etag, final long userKey, final List<StatusBean> statuses) {
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            service.bulkDeassociation(userKey, ResourceDeassociationActionType.UNLINK,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceKey.class));
            resetClient(UserService.class);
        }
    }

    public void link(final String etag, final long userKey, final List<StatusBean> statuses) {
        synchronized (this) {
            UserService service = getService(etag, UserService.class);

            ResourceAssociationMod associationMod = new ResourceAssociationMod();
            associationMod.getTargetResources().addAll(
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceKey.class));
            service.bulkAssociation(userKey, ResourceAssociationActionType.LINK, associationMod);

            resetClient(UserService.class);
        }
    }

    public BulkActionResult deprovision(final String etag, final long userKey, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            result = service.bulkDeassociation(userKey, ResourceDeassociationActionType.DEPROVISION,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceKey.class)).
                    readEntity(BulkActionResult.class);
            resetClient(UserService.class);
        }
        return result;
    }

    public BulkActionResult provision(final String etag, final long userKey,
            final List<StatusBean> statuses, final boolean changepwd, final String password) {

        BulkActionResult result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);

            ResourceAssociationMod associationMod = new ResourceAssociationMod();
            associationMod.getTargetResources().addAll(
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceKey.class));
            associationMod.setChangePwd(changepwd);
            associationMod.setPassword(password);

            result = service.bulkAssociation(userKey, ResourceAssociationActionType.PROVISION, associationMod).
                    readEntity(BulkActionResult.class);
            resetClient(UserService.class);
        }
        return result;
    }

    public BulkActionResult unassign(final String etag, final long userKey, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            result = service.bulkDeassociation(userKey, ResourceDeassociationActionType.UNASSIGN,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceKey.class)).
                    readEntity(BulkActionResult.class);
            resetClient(UserService.class);
        }
        return result;
    }

    public BulkActionResult assign(final String etag, final long userKey,
            final List<StatusBean> statuses, final boolean changepwd, final String password) {

        BulkActionResult result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);

            ResourceAssociationMod associationMod = new ResourceAssociationMod();
            associationMod.getTargetResources().addAll(
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceKey.class));
            associationMod.setChangePwd(changepwd);
            associationMod.setPassword(password);

            result = service.bulkAssociation(userKey, ResourceAssociationActionType.ASSIGN, associationMod).
                    readEntity(BulkActionResult.class);
            resetClient(UserService.class);
        }
        return result;
    }
}
