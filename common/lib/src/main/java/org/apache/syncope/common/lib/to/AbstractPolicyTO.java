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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.PolicyType;

@XmlRootElement(name = "abstractPolicy")
@XmlType
@XmlSeeAlso({ AccountPolicyTO.class, PasswordPolicyTO.class, SyncPolicyTO.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class AbstractPolicyTO extends AbstractBaseBean {

    private static final long serialVersionUID = -2903888572649721035L;

    private long key;

    private String description;

    private final PolicyType type;

    private final List<String> usedByResources = new ArrayList<>();

    private final List<String> usedByRealms = new ArrayList<>();

    private AbstractPolicyTO() {
        super();
        throw new UnsupportedOperationException("No-arg constructor is just to keep JAXB from complaining");
    }

    protected AbstractPolicyTO(final PolicyType type) {
        super();
        this.type = type;
    }

    public long getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final long key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public PolicyType getType() {
        return type;
    }

    @XmlElementWrapper(name = "usedByResources")
    @XmlElement(name = "resource")
    @JsonProperty("usedByResources")
    public List<String> getUsedByResources() {
        return usedByResources;
    }

    @XmlElementWrapper(name = "usedByRealms")
    @XmlElement(name = "group")
    @JsonProperty("usedByRealms")
    public List<String> getUsedByRealms() {
        return usedByRealms;
    }

}
