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
package org.apache.syncope.common.lib.to;

import java.util.Date;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlType
public class AbstractStartEndBean extends AbstractBaseBean {

    private static final long serialVersionUID = 2399577415544539917L;

    protected Date startDate;

    protected Date endDate;

    public Date getStartDate() {
        return startDate == null
                ? null
                : new Date(startDate.getTime());
    }

    public void setStartDate(final Date startDate) {
        if (startDate != null) {
            this.startDate = new Date(startDate.getTime());
        }
    }

    public Date getEndDate() {
        return endDate == null
                ? null
                : new Date(endDate.getTime());
    }

    public void setEndDate(final Date endDate) {
        if (endDate != null) {
            this.endDate = new Date(endDate.getTime());
        }
    }
}
