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
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.mod.AnyObjectMod;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.provisioning.api.sync.AnyObjectSyncResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;

public class AnyObjectSyncResultHandlerImpl extends AbstractSyncResultHandler implements AnyObjectSyncResultHandler {

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT);
    }

    @Override
    protected String getName(final AnyTO anyTO) {
        return StringUtils.EMPTY;
    }

    @Override
    protected AnyTO getAnyTO(final long key) {
        try {
            return anyObjectDataBinder.getAnyObjectTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving anyObject {}", key, e);
            return null;
        }
    }

    @Override
    protected AnyTO doCreate(final AnyTO anyTO, final SyncDelta delta, final ProvisioningResult result) {
        AnyObjectTO anyObjectTO = AnyObjectTO.class.cast(anyTO);

        Map.Entry<Long, List<PropagationStatus>> created = anyObjectProvisioningManager.create(
                anyObjectTO, Collections.singleton(profile.getTask().getResource().getKey()));

        anyObjectTO = anyObjectDataBinder.getAnyObjectTO(created.getKey());

        result.setKey(created.getKey());
        result.setName(getName(anyTO));

        return anyObjectTO;
    }

    @Override
    protected AnyTO doLink(
            final AnyTO before,
            final ProvisioningResult result,
            final boolean unlink) {

        AnyObjectMod anyObjectMod = new AnyObjectMod();
        anyObjectMod.setKey(before.getKey());

        if (unlink) {
            anyObjectMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        } else {
            anyObjectMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        }

        return anyObjectDataBinder.getAnyObjectTO(awfAdapter.update(anyObjectMod).getResult());
    }

    @Override
    protected AnyTO doUpdate(
            final AnyTO before,
            final AnyMod anyMod,
            final SyncDelta delta,
            final ProvisioningResult result) {

        AnyObjectMod anyObjectMod = AnyObjectMod.class.cast(anyMod);

        Map.Entry<Long, List<PropagationStatus>> updated = anyObjectProvisioningManager.update(anyObjectMod);

        AnyObjectTO after = anyObjectDataBinder.getAnyObjectTO(updated.getKey());
        result.setName(getName(after));
        return after;
    }

    @Override
    protected void doDeprovision(final Long key, final boolean unlink) {
        taskExecutor.execute(propagationManager.getAnyObjectDeleteTasks(
                key, profile.getTask().getResource().getKey()));

        if (unlink) {
            AnyObjectMod anyObjectMod = new AnyObjectMod();
            anyObjectMod.setKey(key);
            anyObjectMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        }
    }

    @Override
    protected void doDelete(final Long key) {
        try {
            taskExecutor.execute(propagationManager.getAnyObjectDeleteTasks(
                    key, profile.getTask().getResource().getKey()));
        } catch (Exception e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate anyObject " + key, e);
        }

        anyObjectProvisioningManager.delete(key);
    }
}
