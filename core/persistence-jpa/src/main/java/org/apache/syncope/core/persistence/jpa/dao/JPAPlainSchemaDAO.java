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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JPAPlainSchemaDAO extends AbstractDAO<PlainSchema, String> implements PlainSchemaDAO {

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Override
    public PlainSchema find(final String key) {
        return entityManager.find(JPAPlainSchema.class, key);
    }

    @Override
    public List<PlainSchema> findByAnyTypeClass(final AnyTypeClass anyTypeClass) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPAPlainSchema.class.getSimpleName()).
                append(" e WHERE e.anyTypeClass=:anyTypeClass");

        TypedQuery<PlainSchema> query = entityManager.createQuery(queryString.toString(), PlainSchema.class);
        query.setParameter("anyTypeClass", anyTypeClass);

        return query.getResultList();
    }

    @Override
    public List<PlainSchema> findAll() {
        TypedQuery<PlainSchema> query = entityManager.createQuery(
                "SELECT e FROM " + JPAPlainSchema.class.getSimpleName() + " e", PlainSchema.class);
        return query.getResultList();
    }

    @Override
    public <T extends PlainAttr<?>> List<T> findAttrs(final PlainSchema schema, final Class<T> reference) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(((JPAPlainAttrDAO) plainAttrDAO).getJPAEntityReference(reference).getSimpleName()).
                append(" e WHERE e.schema=:schema");

        TypedQuery<T> query = entityManager.createQuery(queryString.toString(), reference);
        query.setParameter("schema", schema);

        return query.getResultList();
    }

    @Override
    public PlainSchema save(final PlainSchema schema) {
        return entityManager.merge(schema);
    }

    @Override
    public void delete(final String key) {
        PlainSchema schema = find(key);
        if (schema == null) {
            return;
        }

        AnyUtilsFactory anyUtilsFactory = new JPAAnyUtilsFactory();
        for (AnyTypeKind anyTypeKind : AnyTypeKind.values()) {
            AnyUtils anyUtils = anyUtilsFactory.getInstance(anyTypeKind);

            for (PlainAttr<?> attr : findAttrs(schema, anyUtils.plainAttrClass())) {
                plainAttrDAO.delete(attr.getKey(), anyUtils.plainAttrClass());
            }

            resourceDAO.deleteMapping(key, anyUtils.plainIntMappingType());
        }

        if (schema.getAnyTypeClass() != null) {
            schema.getAnyTypeClass().remove(schema);
        }

        entityManager.remove(schema);
    }
}
