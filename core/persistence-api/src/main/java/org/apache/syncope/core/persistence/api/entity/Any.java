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
package org.apache.syncope.core.persistence.api.entity;

import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import java.util.List;
import java.util.Set;

public interface Any<P extends PlainAttr<?>, D extends DerAttr<?>, V extends VirAttr<?>> extends AnnotatedEntity<Long> {

    AnyType getType();

    void setType(AnyType type);

    Realm getRealm();

    void setRealm(Realm realm);

    String getStatus();

    void setStatus(String status);

    String getWorkflowId();

    void setWorkflowId(String workflowId);

    boolean add(P attr);

    boolean remove(P attr);

    P getPlainAttr(String plainSchemaName);

    List<? extends P> getPlainAttrs();

    boolean add(D derAttr);

    boolean remove(D derAttr);

    D getDerAttr(String derSchemaName);

    List<? extends D> getDerAttrs();

    boolean add(V virAttr);

    boolean remove(V virAttr);

    V getVirAttr(String virSchemaName);

    List<? extends V> getVirAttrs();

    boolean add(ExternalResource resource);

    boolean remove(ExternalResource resource);

    List<String> getResourceNames();

    List<? extends ExternalResource> getResources();

    boolean add(AnyTypeClass auxClass);

    boolean remove(AnyTypeClass auxClass);

    List<? extends AnyTypeClass> getAuxClasses();

    Set<PlainSchema> getAllowedPlainSchemas();

    Set<DerSchema> getAllowedDerSchemas();

    Set<VirSchema> getAllowedVirSchemas();
}
