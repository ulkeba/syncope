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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.to.MembershipTO;
import org.apache.syncope.client.to.SchemaTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.util.AttributableOperations;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.client.validation.SyncopeClientException;
import org.apache.syncope.types.EntityViolationType;
import org.apache.syncope.types.SchemaType;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@FixMethodOrder(MethodSorters.JVM)
public class SchemaTestITCase extends AbstractTest {

    private static final String ROLE = "role";
    private static final String USER = "user";
    private static final String MEMBERSHIP = "membership";

    @Test
    public void create() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("testAttribute");
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(SchemaType.String);

        SchemaTO newSchemaTO = schemaService.create(USER, schemaTO);
        assertEquals(schemaTO, newSchemaTO);

        newSchemaTO = schemaService.create(MEMBERSHIP, schemaTO);
        assertEquals(schemaTO, newSchemaTO);
    }

    @Test
    public void createWithNotPermittedName() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("failedLogins");
        schemaTO.setType(SchemaType.String);

        try {
            schemaService.create(USER, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientCompositeErrorException scce) {
            SyncopeClientException sce = scce.getException(SyncopeClientExceptionType.InvalidUSchema);

            assertNotNull(sce.getElements());
            assertEquals(1, sce.getElements().size());
            assertTrue(sce.getElements().iterator().next()
                    .contains(EntityViolationType.InvalidUSchema.name()));
        }
    }

    @Test
    public void createREnumWithoutEnumeration() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("enumcheck");
        schemaTO.setType(SchemaType.Enum);

        try {
            schemaService.create(ROLE, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientCompositeErrorException scce) {
            SyncopeClientException sce = scce.getException(SyncopeClientExceptionType.InvalidRSchema);

            assertNotNull(sce.getElements());
            assertEquals(1, sce.getElements().size());
            assertTrue(sce.getElements().iterator().next()
                    .contains(EntityViolationType.InvalidSchemaTypeSpecification.name()));
        }
    }

    @Test
    public void createUEnumWithoutEnumeration() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("enumcheck");
        schemaTO.setType(SchemaType.Enum);

        try {
            schemaService.create(USER, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientCompositeErrorException scce) {
            SyncopeClientException sce = scce.getException(SyncopeClientExceptionType.InvalidUSchema);

            assertNotNull(sce.getElements());
            assertEquals(1, sce.getElements().size());
            assertTrue(sce.getElements().iterator().next()
                    .contains(EntityViolationType.InvalidSchemaTypeSpecification.name()));
        }
    }

    @Test
    public void delete() {
        SchemaTO deletedSchema = schemaService.delete(USER, "cool", SchemaTO.class);
        assertNotNull(deletedSchema);
        SchemaTO firstname = null;
        try {
            firstname = schemaService.read(USER, "cool", SchemaTO.class);
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
        assertNull(firstname);
    }

    @Test
    public void list() {
        List<SchemaTO> userSchemas = schemaService.list(USER, SchemaTO[].class);
        assertFalse(userSchemas.isEmpty());
        for (SchemaTO schemaTO : userSchemas) {
            assertNotNull(schemaTO);
        }

        List<SchemaTO> roleSchemas = schemaService.list(ROLE, SchemaTO[].class);
        assertFalse(roleSchemas.isEmpty());
        for (SchemaTO schemaTO : roleSchemas) {
            assertNotNull(schemaTO);
        }

        List<SchemaTO> membershipSchemas = schemaService.list(MEMBERSHIP, SchemaTO[].class);
        assertFalse(membershipSchemas.isEmpty());
        for (SchemaTO schemaTO : membershipSchemas) {
            assertNotNull(schemaTO);
        }
    }

    @Test
    public void update() {
        SchemaTO schemaTO = schemaService.read(ROLE, "icon", SchemaTO.class);
        assertNotNull(schemaTO);

        SchemaTO updatedTO = schemaService.update(ROLE, schemaTO.getName(), schemaTO);
        assertEquals(schemaTO, updatedTO);

        updatedTO.setType(SchemaType.Date);
        try {
            schemaService.update(ROLE, schemaTO.getName(), updatedTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientCompositeErrorException scce) {
            SyncopeClientException sce = scce.getException(SyncopeClientExceptionType.InvalidRSchema);
            assertNotNull(sce);
        }
    }

    @Test
    public void issue258() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("schema_issue258");
        schemaTO.setType(SchemaType.Double);

        schemaTO = schemaService.create(USER, schemaTO);
        assertNotNull(schemaTO);

        UserTO userTO = UserTestITCase.getSampleTO("issue258@syncope.apache.org");
        userTO.addAttribute(attributeTO(schemaTO.getName(), "1.2"));

        userTO = userService.create(userTO);
        assertNotNull(userTO);

        schemaTO.setType(SchemaType.Long);
        try {
            schemaService.update(USER, schemaTO.getName(), schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientCompositeErrorException scce) {
            SyncopeClientException sce = scce.getException(SyncopeClientExceptionType.InvalidUSchema);
            assertNotNull(sce);
        }
    }

    @Test
    public void issue259() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("schema_issue259");
        schemaTO.setUniqueConstraint(true);
        schemaTO.setType(SchemaType.Long);

        schemaTO = schemaService.create(USER, schemaTO);
        assertNotNull(schemaTO);

        UserTO userTO = UserTestITCase.getSampleTO("issue259@syncope.apache.org");
        userTO.addAttribute(attributeTO(schemaTO.getName(), "1"));
        userTO = userService.create(userTO);
        assertNotNull(userTO);

        UserTO newUserTO = AttributableOperations.clone(userTO);
        MembershipTO membership = new MembershipTO();
        membership.setRoleId(2L);
        newUserTO.addMembership(membership);

        UserMod userMod = AttributableOperations.diff(newUserTO, userTO);

        userTO = userService.update(userMod.getId(), userMod);
        assertNotNull(userTO);
    }

    @Test
    public void issue260() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("schema_issue260");
        schemaTO.setType(SchemaType.Double);
        schemaTO.setUniqueConstraint(true);

        schemaTO = schemaService.create(USER, schemaTO);
        assertNotNull(schemaTO);

        UserTO userTO = UserTestITCase.getSampleTO("issue260@syncope.apache.org");
        userTO.addAttribute(attributeTO(schemaTO.getName(), "1.2"));
        userTO = userService.create(userTO);
        assertNotNull(userTO);

        schemaTO.setUniqueConstraint(false);
        try {
            schemaService.update(USER, schemaTO.getName(), schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientCompositeErrorException scce) {
            SyncopeClientException sce = scce.getException(SyncopeClientExceptionType.InvalidUSchema);
            assertNotNull(sce);
        }
    }
}