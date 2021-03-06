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
package org.apache.syncope.core.provisioning.java.sync;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.sync.PushActions;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.misc.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.job.PushJob;
import org.apache.syncope.core.provisioning.api.sync.AnyObjectPushResultHandler;
import org.apache.syncope.core.provisioning.api.sync.GroupPushResultHandler;
import org.apache.syncope.core.provisioning.api.sync.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.sync.UserPushResultHandler;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Job for executing synchronization (towards external resource) tasks.
 *
 * @see AbstractProvisioningJob
 * @see PushTask
 * @see PushActions
 */
public class PushJobImpl extends AbstractProvisioningJob<PushTask, PushActions> implements PushJob {

    private static final int PAGE_SIZE = 1000;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * Search DAO.
     */
    @Autowired
    private AnySearchDAO searchDAO;

    /**
     * Group DAO.
     */
    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    private AnyDAO<?> getAnyDAO(final AnyTypeKind anyTypeKind) {
        AnyDAO<?> result;
        switch (anyTypeKind) {
            case USER:
                result = userDAO;
                break;

            case GROUP:
                result = groupDAO;
                break;

            case ANY_OBJECT:
            default:
                result = anyObjectDAO;
        }

        return result;
    }

    @Override
    protected String executeWithSecurityContext(
            final PushTask pushTask,
            final Connector connector,
            final boolean dryRun) throws JobExecutionException {

        LOG.debug("Executing push on {}", pushTask.getResource());

        ProvisioningProfile<PushTask, PushActions> profile = new ProvisioningProfile<>(connector, pushTask);
        if (actions != null) {
            profile.getActions().addAll(actions);
        }
        profile.setDryRun(dryRun);
        profile.setResAct(null);

        AnyObjectPushResultHandler ahandler =
                (AnyObjectPushResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().
                createBean(AnyObjectPushResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ahandler.setProfile(profile);

        UserPushResultHandler uhandler =
                (UserPushResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().
                createBean(UserPushResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        uhandler.setProfile(profile);

        GroupPushResultHandler ghandler =
                (GroupPushResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().
                createBean(GroupPushResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ghandler.setProfile(profile);

        if (actions != null && !profile.isDryRun()) {
            for (PushActions action : actions) {
                action.beforeAll(profile);
            }
        }

        for (Provision provision : pushTask.getResource().getProvisions()) {
            if (provision.getMapping() != null) {
                AnyDAO<?> anyDAO = getAnyDAO(provision.getAnyType().getKind());
                String filter = pushTask.getFilter(provision.getAnyType()) == null
                        ? null
                        : pushTask.getFilter(provision.getAnyType()).get();

                int count = anyDAO.count(SyncopeConstants.FULL_ADMIN_REALMS);
                for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
                    List<? extends Any<?, ?, ?>> localAnys = StringUtils.isBlank(filter)
                            ? anyDAO.findAll(SyncopeConstants.FULL_ADMIN_REALMS, page, PAGE_SIZE)
                            : searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                                    SearchCondConverter.convert(filter),
                                    Collections.<OrderByClause>emptyList(), provision.getAnyType().getKind());

                    for (Any<?, ?, ?> any : localAnys) {
                        SyncopePushResultHandler handler;
                        switch (provision.getAnyType().getKind()) {
                            case USER:
                                handler = uhandler;
                                break;

                            case GROUP:
                                handler = ghandler;
                                break;

                            case ANY_OBJECT:
                            default:
                                handler = ahandler;
                        }

                        try {
                            handler.handle(any.getKey());
                        } catch (Exception e) {
                            LOG.warn("Failure pushing '{}' on '{}'", any, pushTask.getResource(), e);
                            throw new JobExecutionException(
                                    "While pushing " + any + " on " + pushTask.getResource(), e);
                        }
                    }
                }
            }
        }

        if (actions != null && !profile.isDryRun()) {
            for (PushActions action : actions) {
                action.afterAll(profile);
            }
        }

        String result = createReport(profile.getResults(), pushTask.getResource().getSyncTraceLevel(), dryRun);
        LOG.debug("Sync result: {}", result);
        return result;
    }
}
