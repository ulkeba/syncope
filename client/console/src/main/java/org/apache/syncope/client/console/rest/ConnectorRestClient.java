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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.wrap.ConnIdObjectClass;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Connectors services.
 */
@Component
public class ConnectorRestClient extends BaseRestClient {

    private static final long serialVersionUID = -6870366819966266617L;

    public List<ConnInstanceTO> getAllConnectors() {
        List<ConnInstanceTO> connectors = Collections.<ConnInstanceTO>emptyList();
        try {
            connectors = getService(ConnectorService.class).list(SyncopeConsoleSession.get().getLocale().toString());
        } catch (Exception e) {
            LOG.error("While reading connectors", e);
        }
        return connectors;
    }

    public void create(final ConnInstanceTO connectorTO) {
        Set<ConnConfProperty> filteredConf = filterProperties(connectorTO.getConfiguration());
        connectorTO.getConfiguration().clear();
        connectorTO.getConfiguration().addAll(filteredConf);
        getService(ConnectorService.class).create(connectorTO);
    }

    /**
     * Load an already existent connector by its name.
     *
     * @param connectorInstanceId the id
     * @return ConnInstanceTO
     */
    public ConnInstanceTO read(final Long connectorInstanceId) {
        ConnInstanceTO connectorTO = null;

        try {
            connectorTO = getService(ConnectorService.class).read(connectorInstanceId);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a connector", e);
        }

        return connectorTO;
    }

    public void update(final ConnInstanceTO connectorTO) {
        Set<ConnConfProperty> filteredConf = filterProperties(connectorTO.getConfiguration());
        connectorTO.getConfiguration().clear();
        connectorTO.getConfiguration().addAll(filteredConf);
        getService(ConnectorService.class).update(connectorTO);
    }

    public ConnInstanceTO delete(final Long id) {
        ConnInstanceTO instanceTO = getService(ConnectorService.class).read(id);
        getService(ConnectorService.class).delete(id);
        return instanceTO;
    }

    public List<ConnBundleTO> getAllBundles() {
        List<ConnBundleTO> bundles = Collections.<ConnBundleTO>emptyList();

        try {
            bundles = getService(ConnectorService.class).getBundles(SyncopeConsoleSession.get().getLocale().toString());
        } catch (SyncopeClientException e) {
            LOG.error("While getting connector bundles", e);
        }

        return bundles;
    }

    /**
     * Get all configuration properties for the given connector instance.
     *
     * @param connectorId the connector id
     * @return List of ConnConfProperty, or an empty list in case none found
     */
    public List<ConnConfProperty> getConnectorProperties(final Long connectorId) {
        List<ConnConfProperty> properties = null;

        try {
            properties = getService(ConnectorService.class).getConfigurationProperties(connectorId);
        } catch (SyncopeClientException e) {
            LOG.error("While getting connector configuration properties", e);
        }

        return properties;
    }

    private Set<ConnConfProperty> filterProperties(final Set<ConnConfProperty> properties) {
        Set<ConnConfProperty> newProperties = new HashSet<>();

        for (ConnConfProperty property : properties) {
            ConnConfProperty prop = new ConnConfProperty();
            prop.setSchema(property.getSchema());
            prop.setOverridable(property.isOverridable());

            final List<Object> parsed = new ArrayList<>();
            if (property.getValues() != null) {
                for (Object obj : property.getValues()) {
                    if (obj != null && !obj.toString().isEmpty()) {
                        parsed.add(obj);
                    }
                }
            }
            prop.getValues().addAll(parsed);
            newProperties.add(prop);
        }
        return newProperties;
    }

    /**
     * Test connector connection.
     *
     * @param connectorTO connector
     * @return Connection status
     */
    public boolean check(final ConnInstanceTO connectorTO) {
        ConnInstanceTO toBeChecked = new ConnInstanceTO();
        BeanUtils.copyProperties(connectorTO, toBeChecked, new String[] { "configuration", "configurationMap" });
        toBeChecked.getConfiguration().addAll(filterProperties(connectorTO.getConfiguration()));

        boolean check = false;
        try {
            check = getService(ConnectorService.class).check(toBeChecked).getElement();
        } catch (Exception e) {
            LOG.error("While checking {}", toBeChecked, e);
        }

        return check;
    }

    public boolean check(final ResourceTO resourceTO) {
        boolean check = false;
        try {
            check = getService(ResourceService.class).check(resourceTO).getElement();
        } catch (Exception e) {
            LOG.error("Connector not found {}", resourceTO.getConnector(), e);
        }

        return check;
    }

    public List<String> getSchemaNames(final ConnInstanceTO connectorTO) {
        List<String> schemaNames = new ArrayList<>();
        try {
            List<PlainSchemaTO> response = getService(ConnectorService.class).
                    getSchemaNames(connectorTO.getKey(), connectorTO, false);
            for (PlainSchemaTO schema : response) {
                schemaNames.add(schema.getKey());
            }
        } catch (Exception e) {
            LOG.error("While getting schema names", e);
        } finally {
            // re-order schema names list
            Collections.sort(schemaNames);
        }

        return schemaNames;
    }

    public List<ConnIdObjectClass> getSupportedObjectClasses(final ConnInstanceTO connectorTO) {
        List<ConnIdObjectClass> result = Collections.emptyList();
        try {
            result = getService(ConnectorService.class).getSupportedObjectClasses(connectorTO.getKey(), connectorTO);
        } catch (Exception e) {
            LOG.error("While getting supported object classes", e);
        }

        return result;
    }

    public void reload() {
        getService(ConnectorService.class).reload();
    }

    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(ConnectorService.class).bulk(action);
    }
}
