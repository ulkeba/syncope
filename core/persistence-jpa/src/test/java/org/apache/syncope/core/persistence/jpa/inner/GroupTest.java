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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class GroupTest extends AbstractTest {

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Test
    public void findAll() {
        List<Group> list = groupDAO.findAll(SyncopeConstants.FULL_ADMIN_REALMS, 1, 100);
        assertEquals("did not get expected number of groups ", 14, list.size());
    }

    @Test
    public void find() {
        Group group = groupDAO.find("root");
        assertNotNull("did not find expected group", group);
    }

    @Test
    public void save() {
        Group group = entityFactory.newEntity(Group.class);
        group.setName("secondChild");
        group.setRealm(realmDAO.find(SyncopeConstants.ROOT_REALM));

        group = groupDAO.save(group);

        Group actual = groupDAO.find(group.getKey());
        assertNotNull("expected save to work", actual);
    }

    @Test
    public void delete() {
        Group group = groupDAO.find(4L);
        groupDAO.delete(group.getKey());

        Group actual = groupDAO.find(4L);
        assertNull("delete did not work", actual);
    }
}
