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
package org.apache.syncope.core.persistence.jpa.entity.anyobject;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.Valid;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADerAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AVirAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.jpa.entity.AbstractAny;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;

@Entity
@Table(name = JPAAnyObject.TABLE)
@Cacheable
public class JPAAnyObject extends AbstractAny<APlainAttr, ADerAttr, AVirAttr> implements AnyObject {

    private static final long serialVersionUID = 9063766472970643492L;

    public static final String TABLE = "AnyObject";

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPAAnyType type;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAAPlainAttr> plainAttrs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAADerAttr> derAttrs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAAVirAttr> virAttrs = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "anyObject_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_name"))
    private List<JPAExternalResource> resources = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "anyObject_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_name"))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "leftEnd")
    @Valid
    private List<JPAARelationship> relationships = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "leftEnd")
    @Valid
    private List<JPAAMembership> memberships = new ArrayList<>();

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public AnyType getType() {
        return type;
    }

    @Override
    public void setType(final AnyType type) {
        checkType(type, JPAAnyType.class);
        this.type = (JPAAnyType) type;
    }

    @Override
    public boolean add(final APlainAttr attr) {
        checkType(attr, JPAAPlainAttr.class);
        return plainAttrs.add((JPAAPlainAttr) attr);
    }

    @Override
    public boolean remove(final APlainAttr attr) {
        checkType(attr, JPAAPlainAttr.class);
        return plainAttrs.remove((JPAAPlainAttr) attr);
    }

    @Override
    public List<? extends APlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public boolean add(final ADerAttr attr) {
        checkType(attr, JPAADerAttr.class);
        return derAttrs.add((JPAADerAttr) attr);
    }

    @Override
    public boolean remove(final ADerAttr attr) {
        checkType(attr, JPAADerAttr.class);
        return derAttrs.remove((JPAADerAttr) attr);
    }

    @Override
    public List<? extends ADerAttr> getDerAttrs() {
        return derAttrs;
    }

    @Override
    public boolean add(final AVirAttr attr) {
        checkType(attr, JPAAVirAttr.class);
        return virAttrs.add((JPAAVirAttr) attr);
    }

    @Override
    public boolean remove(final AVirAttr attr) {
        checkType(attr, JPAAVirAttr.class);
        return virAttrs.remove((JPAAVirAttr) attr);
    }

    @Override
    public List<? extends AVirAttr> getVirAttrs() {
        return virAttrs;
    }

    @Override
    protected List<JPAExternalResource> internalGetResources() {
        return resources;
    }

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        checkType(auxClass, JPAAnyTypeClass.class);
        return this.auxClasses.add((JPAAnyTypeClass) auxClass);
    }

    @Override
    public boolean remove(final AnyTypeClass auxClass) {
        checkType(auxClass, JPAAnyTypeClass.class);
        return this.auxClasses.remove((JPAAnyTypeClass) auxClass);
    }

    @Override
    public List<? extends AnyTypeClass> getAuxClasses() {
        return auxClasses;
    }

    @Override
    public boolean add(final ARelationship relationship) {
        checkType(relationship, JPAARelationship.class);
        return this.relationships.add((JPAARelationship) relationship);
    }

    @Override
    public boolean remove(final ARelationship relationship) {
        checkType(relationship, JPAARelationship.class);
        return this.relationships.remove((JPAARelationship) relationship);
    }

    @Override
    public ARelationship getRelationship(final RelationshipType relationshipType) {
        return CollectionUtils.find(getRelationships(), new Predicate<ARelationship>() {

            @Override
            public boolean evaluate(final ARelationship relationship) {
                return relationshipType != null && relationshipType.equals(relationship.getType());
            }
        });
    }

    @Override
    public ARelationship getRelationship(final Long anyObjectKey) {
        return CollectionUtils.find(getRelationships(), new Predicate<ARelationship>() {

            @Override
            public boolean evaluate(final ARelationship relationship) {
                return anyObjectKey != null && anyObjectKey.equals(relationship.getRightEnd().getKey());
            }
        });
    }

    @Override
    public List<? extends ARelationship> getRelationships() {
        return relationships;
    }

    @Override
    public boolean add(final AMembership membership) {
        checkType(membership, JPAAMembership.class);
        return this.memberships.add((JPAAMembership) membership);
    }

    @Override
    public boolean remove(final AMembership membership) {
        checkType(membership, JPAAMembership.class);
        return this.memberships.remove((JPAAMembership) membership);
    }

    @Override
    public AMembership getMembership(final Long groupKey) {
        return CollectionUtils.find(getMemberships(), new Predicate<AMembership>() {

            @Override
            public boolean evaluate(final AMembership membership) {
                return groupKey != null && groupKey.equals(membership.getRightEnd().getKey());
            }
        });
    }

    @Override
    public List<? extends AMembership> getMemberships() {
        return memberships;
    }
}
