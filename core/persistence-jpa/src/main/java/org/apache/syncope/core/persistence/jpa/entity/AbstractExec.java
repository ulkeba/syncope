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
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.syncope.core.persistence.api.entity.Exec;

@MappedSuperclass
public abstract class AbstractExec extends AbstractEntity<Long> implements Exec {

    private static final long serialVersionUID = -812344822970166317L;

    @Column(nullable = false)
    protected String status;

    /**
     * Any information to be accompanied to this execution's result.
     */
    @Lob
    protected String message;

    /**
     * Start instant of this execution.
     */
    @Temporal(TemporalType.TIMESTAMP)
    protected Date startDate;

    /**
     * End instant of this execution.
     */
    @Temporal(TemporalType.TIMESTAMP)
    protected Date endDate;

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {
        this.message = message == null ? null : message.replace('\0', '\n');
    }

    @Override
    public Date getEndDate() {
        return endDate == null
                ? null
                : new Date(endDate.getTime());
    }

    @Override

    public void setEndDate(final Date endDate) {
        this.endDate = endDate == null
                ? null
                : new Date(endDate.getTime());
    }

    @Override

    public Date getStartDate() {
        return startDate == null
                ? null
                : new Date(startDate.getTime());
    }

    @Override

    public void setStartDate(final Date startDate) {
        this.startDate = startDate == null
                ? null
                : new Date(startDate.getTime());
    }
}
