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
package org.apache.syncope.core.provisioning.api;

import java.util.Set;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;

/**
 * Entry point for creating and destroying connectors for external resources.
 *
 * @see Connector
 */
public interface ConnectorFactory extends SyncopeLoader {

    /**
     * Create connector from given connector instance and configuration properties.
     *
     * @param connInstance connector instance
     * @param configuration configuration properties
     * @return connector
     */
    Connector createConnector(ConnInstance connInstance, Set<ConnConfProperty> configuration);

    /**
     * Get existing connector for the given resource.
     *
     * @param resource the resource.
     * @return live connector bran for given resource
     */
    Connector getConnector(ExternalResource resource);

    /**
     * Load connectors for all existing resources.
     *
     * @see ExternalResource
     */
    @Override
    void load();

    /**
     * Unload connectors for all existing resources.
     *
     * @see ExternalResource
     */
    void unload();
}
