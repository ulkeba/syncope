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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.search.AttributeCond;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.ConfigurationTO;
import org.apache.syncope.client.to.UserRequestTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.client.validation.SyncopeClientException;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@FixMethodOrder(MethodSorters.JVM)
public class UserRequestTestITCase extends AbstractTest {

    @Test
    public void create() {
        // 1. set create request not allowed
        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey("createRequest.allowed");
        configurationTO.setValue("false");

        configurationTO = configurationService.create(configurationTO);
        assertNotNull(configurationTO);

        UserTO userTO = UserTestITCase.getSampleTO("selfcreate@syncope.apache.org");

        // 2. get unauthorized when trying to request user create
        SyncopeClientException exception = null;
        try {
            userRequestService.create(userTO);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // 3. set create request allowed
        configurationTO.setValue("true");

        configurationTO = configurationService.create(configurationTO);
        assertNotNull(configurationTO);

        // 4. as anonymous, request user create works
        UserRequestTO request = anonymousRestTemplate().postForObject(BASE_URL + "user/request/create",
                userTO, UserRequestTO.class);
        assertNotNull(request);

        // 5. switch back to admin
        super.resetRestTemplate();

        // 6. try to find user
        AttributeCond attrCond = new AttributeCond(AttributeCond.Type.EQ);
        attrCond.setSchema("userId");
        attrCond.setExpression("selfcreate@syncope.apache.org");

        final List<UserTO> matchingUsers = userService.search(NodeCond.getLeafCond(attrCond));
        assertTrue(matchingUsers.isEmpty());

        // 7. actually create user
        userTO = userService.create(request.getUserTO());
        assertNotNull(userTO);
    }

    @Test
    public void update() {
        // 1. create an user (as admin)
        UserTO userTO = UserTestITCase.getSampleTO("selfupdate@syncope.apache.org");
        String initialPassword = userTO.getPassword();

        userTO = userService.create(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword(initialPassword);

        // 2. try to request user update as admin: failure
        SyncopeClientException exception = null;
        try {
            userRequestService.update(userMod);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // 3. auth as user just created
        super.setupRestTemplate(userTO.getUsername(), initialPassword);

        // 4. update with same password: not matching password policy
        exception = null;
        try {
            userRequestService.update(userMod);
        } catch (SyncopeClientCompositeErrorException scce) {
            exception = scce.getException(SyncopeClientExceptionType.InvalidSyncopeUser);
        }
        assertNotNull(exception);

        // 5. now request user update works
        userMod.setPassword("new" + initialPassword);
        UserRequestTO request = userRequestService.update(userMod);
        assertNotNull(request);

        // 6. switch back to admin
        super.resetRestTemplate();

        // 7. user password has not changed yet
        Boolean verify = userService.verifyPassword(userTO.getUsername(), userMod.getPassword());
        assertFalse(verify);

        // 8. actually update user
        userTO = userService.update(userMod.getId(), userMod);
        assertNotNull(userTO);

        // 9. user password has now changed
        verify = userService.verifyPassword(userTO.getUsername(), userMod.getPassword());
        assertTrue(verify);
    }

    @Test
    public void delete() {
        // 1. create an user (as admin)
        UserTO userTO = UserTestITCase.getSampleTO("selfdelete@syncope.apache.org");
        String initialPassword = userTO.getPassword();

        userTO = userService.create(userTO);
        assertNotNull(userTO);

        // 2. try to request user delete as admin: failure
        SyncopeClientException exception = null;
        try {

            userRequestService.delete(userTO.getId());
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // 3. auth as user just created
        super.setupRestTemplate(userTO.getUsername(), initialPassword);

        // 4. now request user delete works
        UserRequestTO request = userRequestService.delete(userTO.getId());
        assertNotNull(request);

        // 5. switch back to admin
        super.resetRestTemplate();

        // 6. user still exists
        UserTO actual = userService.read(userTO.getId());
        assertNotNull(actual);

        // 7. actually delete user
        userService.delete(userTO.getId());

        // 8. user does not exist any more
        try {
            userService.read(userTO.getId());
            fail();
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}