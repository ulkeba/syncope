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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.sync.SyncActions;
import org.apache.syncope.core.misc.security.UnauthorizedException;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.AnyTransformer;
import org.apache.syncope.core.provisioning.api.sync.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.provisioning.api.sync.SyncopeSyncResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractSyncResultHandler extends AbstractSyncopeResultHandler<SyncTask, SyncActions>
        implements SyncopeSyncResultHandler {

    @Autowired
    protected SyncUtils syncUtilities;

    @Autowired
    protected AnyTransformer anyTransformer;

    protected abstract String getName(AnyTO anyTO);

    protected abstract AnyTO doCreate(AnyTO anyTO, SyncDelta delta, ProvisioningResult result);

    protected abstract AnyTO doLink(AnyTO before, ProvisioningResult result, boolean unlink);

    protected abstract AnyTO doUpdate(AnyTO before, AnyMod anyMod, SyncDelta delta, ProvisioningResult result);

    protected abstract void doDeprovision(Long key, boolean unlink);

    protected abstract void doDelete(Long key);

    @Override
    public boolean handle(final SyncDelta delta) {
        Provision provision = null;
        try {
            provision = profile.getTask().getResource().getProvision(delta.getObject().getObjectClass());
            if (provision == null) {
                throw new JobExecutionException("No provision found on " + profile.getTask().getResource() + " for "
                        + delta.getObject().getObjectClass());
            }

            doHandle(delta, provision);
            return true;
        } catch (IgnoreProvisionException e) {
            ProvisioningResult result = new ProvisioningResult();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(provision == null
                    ? getAnyUtils().getAnyTypeKind().name() : provision.getAnyType().getKey());
            result.setStatus(ProvisioningResult.Status.IGNORE);
            result.setKey(0L);
            result.setName(delta.getObject().getName().getNameValue());
            profile.getResults().add(result);

            LOG.warn("Ignoring during synchronization", e);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Synchronization failed", e);
            return false;
        }
    }

    protected List<ProvisioningResult> assign(
            final SyncDelta delta, final Provision provision, final AnyUtils anyUtils)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            return Collections.<ProvisioningResult>emptyList();
        }

        AnyTO anyTO = connObjectUtils.getAnyTO(delta.getObject(), profile.getTask(), provision, anyUtils);

        anyTO.getResources().add(profile.getTask().getResource().getKey());

        ProvisioningResult result = new ProvisioningResult();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningResult.Status.SUCCESS);

        // Any transformation (if configured)
        AnyTO transformed = anyTransformer.transform(anyTO);
        LOG.debug("Transformed: {}", transformed);

        result.setName(getName(transformed));

        if (profile.isDryRun()) {
            result.setKey(0L);
        } else {
            SyncDelta actionedDelta = delta;
            for (SyncActions action : profile.getActions()) {
                actionedDelta = action.beforeAssign(this.getProfile(), actionedDelta, transformed);
            }

            create(transformed, actionedDelta, UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), result);
        }

        return Collections.singletonList(result);
    }

    protected List<ProvisioningResult> provision(
            final SyncDelta delta, final Provision provision, final AnyUtils anyUtils)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            return Collections.<ProvisioningResult>emptyList();
        }

        AnyTO anyTO = connObjectUtils.getAnyTO(delta.getObject(), profile.getTask(), provision, anyUtils);

        // Any transformation (if configured)
        AnyTO transformed = anyTransformer.transform(anyTO);
        LOG.debug("Transformed: {}", transformed);

        ProvisioningResult result = new ProvisioningResult();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningResult.Status.SUCCESS);

        result.setName(getName(transformed));

        if (profile.isDryRun()) {
            result.setKey(0L);
        } else {
            SyncDelta actionedDelta = delta;
            for (SyncActions action : profile.getActions()) {
                actionedDelta = action.beforeProvision(this.getProfile(), actionedDelta, transformed);
            }

            create(transformed, actionedDelta, UnmatchingRule.toEventName(UnmatchingRule.PROVISION), result);
        }

        return Collections.<ProvisioningResult>singletonList(result);
    }

    private void create(
            final AnyTO anyTO,
            final SyncDelta delta,
            final String operation,
            final ProvisioningResult result)
            throws JobExecutionException {

        Object output;
        Result resultStatus;

        try {
            AnyTO actual = doCreate(anyTO, delta, result);
            result.setName(getName(actual));
            output = actual;
            resultStatus = Result.SUCCESS;

            for (SyncActions action : profile.getActions()) {
                action.after(this.getProfile(), delta, actual, result);
            }
        } catch (IgnoreProvisionException e) {
            throw e;
        } catch (PropagationException e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate {} {}", anyTO.getType(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;

            for (SyncActions action : profile.getActions()) {
                action.onError(this.getProfile(), delta, result, e);
            }
        } catch (Exception e) {
            result.setStatus(ProvisioningResult.Status.FAILURE);
            result.setMessage(ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Could not create {} {} ", anyTO.getType(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;

            for (SyncActions action : profile.getActions()) {
                action.onError(this.getProfile(), delta, result, e);
            }
        }

        audit(operation, resultStatus, null, output, delta);
    }

    protected List<ProvisioningResult> update(final SyncDelta delta, final List<Long> anys,
            final Provision provision) throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.<ProvisioningResult>emptyList();
        }

        LOG.debug("About to update {}", anys);

        List<ProvisioningResult> results = new ArrayList<>();

        SyncDelta workingDelta = delta;
        for (Long key : anys) {
            LOG.debug("About to update {}", key);

            ProvisioningResult result = new ProvisioningResult();
            result.setOperation(ResourceOperation.UPDATE);
            result.setAnyType(provision.getAnyType().getKey());
            result.setStatus(ProvisioningResult.Status.SUCCESS);
            result.setKey(key);

            AnyTO before = getAnyTO(key);
            if (before == null) {
                result.setStatus(ProvisioningResult.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%d)' not found", provision.getAnyType().getKey(), key));
            } else {
                result.setName(getName(before));
            }

            Result resultStatus;
            Object output;
            if (!profile.isDryRun()) {
                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    try {
                        AnyMod anyMod = connObjectUtils.getAnyMod(
                                before.getKey(),
                                workingDelta.getObject(),
                                before,
                                profile.getTask(),
                                provision,
                                getAnyUtils());

                        // Attribute value transformation (if configured)
                        AnyMod actual = anyTransformer.transform(anyMod);
                        LOG.debug("Transformed: {}", actual);

                        for (SyncActions action : profile.getActions()) {
                            workingDelta = action.beforeUpdate(this.getProfile(), workingDelta, before, anyMod);
                        }

                        AnyTO updated = doUpdate(before, anyMod, workingDelta, result);

                        for (SyncActions action : profile.getActions()) {
                            action.after(this.getProfile(), workingDelta, updated, result);
                        }

                        output = updated;
                        resultStatus = Result.SUCCESS;
                        result.setName(getName(updated));
                        LOG.debug("{} {} successfully updated", provision.getAnyType().getKey(), key);
                    } catch (IgnoreProvisionException e) {
                        throw e;
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a synchronization failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}",
                                provision.getAnyType().getKey(), workingDelta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;

                        for (SyncActions action : profile.getActions()) {
                            action.onError(this.getProfile(), workingDelta, result, e);
                        }
                    } catch (Exception e) {
                        result.setStatus(ProvisioningResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}",
                                provision.getAnyType().getKey(), workingDelta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;

                        for (SyncActions action : profile.getActions()) {
                            action.onError(this.getProfile(), workingDelta, result, e);
                        }
                    }
                }
                audit(MatchingRule.toEventName(MatchingRule.UPDATE), resultStatus, before, output, workingDelta);
            }
            results.add(result);
        }
        return results;
    }

    protected List<ProvisioningResult> deprovision(
            final SyncDelta delta,
            final List<Long> anys,
            final Provision provision,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.<ProvisioningResult>emptyList();
        }

        LOG.debug("About to update {}", anys);

        final List<ProvisioningResult> updResults = new ArrayList<>();

        for (Long id : anys) {
            LOG.debug("About to unassign resource {}", id);

            Object output;
            Result resultStatus;

            ProvisioningResult result = new ProvisioningResult();
            result.setOperation(ResourceOperation.DELETE);
            result.setAnyType(provision.getAnyType().getKey());
            result.setStatus(ProvisioningResult.Status.SUCCESS);
            result.setKey(id);

            AnyTO before = getAnyTO(id);

            if (before == null) {
                result.setStatus(ProvisioningResult.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%d)' not found", provision.getAnyType().getKey(), id));
            }

            if (!profile.isDryRun()) {
                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    result.setName(getName(before));

                    try {
                        if (unlink) {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeUnassign(this.getProfile(), delta, before);
                            }
                        } else {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeDeprovision(this.getProfile(), delta, before);
                            }
                        }

                        doDeprovision(id, unlink);
                        output = getAnyTO(id);

                        for (SyncActions action : profile.getActions()) {
                            action.after(this.getProfile(), delta, AnyTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;
                        LOG.debug("{} {} successfully updated", provision.getAnyType().getKey(), id);
                    } catch (IgnoreProvisionException e) {
                        throw e;
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a synchronization failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}",
                                provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;

                        for (SyncActions action : profile.getActions()) {
                            action.onError(this.getProfile(), delta, result, e);
                        }
                    } catch (Exception e) {
                        result.setStatus(ProvisioningResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}",
                                provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;

                        for (SyncActions action : profile.getActions()) {
                            action.onError(this.getProfile(), delta, result, e);
                        }
                    }
                }
                audit(unlink
                        ? MatchingRule.toEventName(MatchingRule.UNASSIGN)
                        : MatchingRule.toEventName(MatchingRule.DEPROVISION), resultStatus, before, output, delta);
            }
            updResults.add(result);
        }

        return updResults;
    }

    protected List<ProvisioningResult> link(
            final SyncDelta delta,
            final List<Long> anys,
            final Provision provision,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.<ProvisioningResult>emptyList();
        }

        LOG.debug("About to update {}", anys);

        final List<ProvisioningResult> updResults = new ArrayList<>();

        for (Long key : anys) {
            LOG.debug("About to unassign resource {}", key);

            Object output;
            Result resultStatus;

            ProvisioningResult result = new ProvisioningResult();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(provision.getAnyType().getKey());
            result.setStatus(ProvisioningResult.Status.SUCCESS);
            result.setKey(key);

            AnyTO before = getAnyTO(key);

            if (before == null) {
                result.setStatus(ProvisioningResult.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%d)' not found", provision.getAnyType().getKey(), key));
            }

            if (!profile.isDryRun()) {
                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    result.setName(getName(before));

                    try {
                        if (unlink) {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeUnlink(this.getProfile(), delta, before);
                            }
                        } else {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeLink(this.getProfile(), delta, before);
                            }
                        }

                        output = doLink(before, result, unlink);

                        for (SyncActions action : profile.getActions()) {
                            action.after(this.getProfile(), delta, AnyTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;
                        LOG.debug("{} {} successfully updated", provision.getAnyType().getKey(), key);
                    } catch (IgnoreProvisionException e) {
                        throw e;
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a synchronization failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}",
                                provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;

                        for (SyncActions action : profile.getActions()) {
                            action.onError(this.getProfile(), delta, result, e);
                        }
                    } catch (Exception e) {
                        result.setStatus(ProvisioningResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}",
                                provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;

                        for (SyncActions action : profile.getActions()) {
                            action.onError(this.getProfile(), delta, result, e);
                        }
                    }
                }
                audit(unlink ? MatchingRule.toEventName(MatchingRule.UNLINK)
                        : MatchingRule.toEventName(MatchingRule.LINK), resultStatus, before, output, delta);
            }
            updResults.add(result);
        }

        return updResults;
    }

    protected List<ProvisioningResult> delete(
            final SyncDelta delta,
            final List<Long> anys,
            final Provision provision)
            throws JobExecutionException {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("SyncTask not configured for delete");
            return Collections.<ProvisioningResult>emptyList();
        }

        LOG.debug("About to delete {}", anys);

        List<ProvisioningResult> delResults = new ArrayList<>();

        SyncDelta workingDelta = delta;
        for (Long id : anys) {
            Object output;
            Result resultStatus = Result.FAILURE;

            ProvisioningResult result = new ProvisioningResult();

            try {
                AnyTO before = getAnyTO(id);

                result.setKey(id);
                result.setName(getName(before));
                result.setOperation(ResourceOperation.DELETE);
                result.setAnyType(provision.getAnyType().getKey());
                result.setStatus(ProvisioningResult.Status.SUCCESS);

                if (!profile.isDryRun()) {
                    for (SyncActions action : profile.getActions()) {
                        workingDelta = action.beforeDelete(this.getProfile(), workingDelta, before);
                    }

                    try {
                        doDelete(id);
                        output = null;
                        resultStatus = Result.SUCCESS;

                        for (SyncActions action : profile.getActions()) {
                            action.after(this.getProfile(), workingDelta, before, result);
                        }
                    } catch (IgnoreProvisionException e) {
                        throw e;
                    } catch (Exception e) {
                        result.setStatus(ProvisioningResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {} {}", provision.getAnyType().getKey(), id, e);
                        output = e;

                        for (SyncActions action : profile.getActions()) {
                            action.onError(this.getProfile(), workingDelta, result, e);
                        }
                    }

                    audit(ResourceOperation.DELETE.name().toLowerCase(), resultStatus, before, output, workingDelta);
                }

                delResults.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find {} {}", provision.getAnyType().getKey(), id, e);
            } catch (UnauthorizedException e) {
                LOG.error("Not allowed to read {} {}", provision.getAnyType().getKey(), id, e);
            } catch (Exception e) {
                LOG.error("Could not delete {} {}", provision.getAnyType().getKey(), id, e);
            }
        }

        return delResults;
    }

    private List<ProvisioningResult> ignore(
            final SyncDelta delta,
            final Provision provision,
            final boolean matching)
            throws JobExecutionException {

        LOG.debug("Any to ignore {}", delta.getObject().getUid().getUidValue());

        final List<ProvisioningResult> ignoreResults = new ArrayList<>();
        ProvisioningResult result = new ProvisioningResult();

        result.setKey(null);
        result.setName(delta.getObject().getUid().getUidValue());
        result.setOperation(ResourceOperation.NONE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningResult.Status.SUCCESS);
        ignoreResults.add(result);

        if (!profile.isDryRun()) {
            audit(matching
                    ? MatchingRule.toEventName(MatchingRule.IGNORE)
                    : UnmatchingRule.toEventName(UnmatchingRule.IGNORE), Result.SUCCESS, null, null, delta);
        }

        return ignoreResults;
    }

    /**
     * Look into SyncDelta and take necessary profile.getActions() (create / update / delete) on any object(s).
     *
     * @param delta returned by the underlying profile.getConnector()
     * @param provision provisioning info
     * @throws JobExecutionException in case of synchronization failure.
     */
    protected void doHandle(final SyncDelta delta, final Provision provision)
            throws JobExecutionException {

        AnyUtils anyUtils = getAnyUtils();

        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        String uid = delta.getPreviousUid() == null
                ? delta.getUid().getUidValue()
                : delta.getPreviousUid().getUidValue();

        try {
            List<Long> anyKeys = syncUtilities.findExisting(uid, delta.getObject(), provision, anyUtils);

            if (anyKeys.size() > 1) {
                switch (profile.getResAct()) {
                    case IGNORE:
                        throw new IllegalStateException("More than one match " + anyKeys);

                    case FIRSTMATCH:
                        anyKeys = anyKeys.subList(0, 1);
                        break;

                    case LASTMATCH:
                        anyKeys = anyKeys.subList(anyKeys.size() - 1, anyKeys.size());
                        break;

                    default:
                    // keep anyIds as is
                }
            }

            if (SyncDeltaType.CREATE_OR_UPDATE == delta.getDeltaType()) {
                if (anyKeys.isEmpty()) {
                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            profile.getResults().addAll(assign(delta, provision, anyUtils));
                            break;

                        case PROVISION:
                            profile.getResults().addAll(provision(delta, provision, anyUtils));
                            break;

                        case IGNORE:
                            profile.getResults().addAll(ignore(delta, provision, false));
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            profile.getResults().addAll(update(delta, anyKeys, provision));
                            break;
                        case DEPROVISION:
                            profile.getResults().addAll(deprovision(delta, anyKeys, provision, false));
                            break;
                        case UNASSIGN:
                            profile.getResults().addAll(deprovision(delta, anyKeys, provision, true));
                            break;
                        case LINK:
                            profile.getResults().addAll(link(delta, anyKeys, provision, false));
                            break;
                        case UNLINK:
                            profile.getResults().addAll(link(delta, anyKeys, provision, true));
                            break;
                        case IGNORE:
                            profile.getResults().addAll(ignore(delta, provision, true));
                            break;
                        default:
                        // do nothing
                    }
                }
            } else if (SyncDeltaType.DELETE == delta.getDeltaType()) {
                if (anyKeys.isEmpty()) {
                    LOG.debug("No match found for deletion");
                } else {
                    profile.getResults().addAll(delete(delta, anyKeys, provision));
                }
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.warn(e.getMessage());
        }
    }

    private void audit(
            final String event,
            final Result result,
            final Object before,
            final Object output,
            final Object... input) {

        notificationManager.createTasks(AuditElements.EventCategoryType.SYNCHRONIZATION,
                getAnyUtils().getAnyTypeKind().name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                input);

        auditManager.audit(AuditElements.EventCategoryType.SYNCHRONIZATION,
                getAnyUtils().getAnyTypeKind().name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                input);
    }
}
