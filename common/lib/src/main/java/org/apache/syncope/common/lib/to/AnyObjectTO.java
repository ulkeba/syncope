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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "anyObject")
@XmlType
public class AnyObjectTO extends AnyTO {

    private static final long serialVersionUID = 8841697496476959639L;

    private final List<RelationshipTO> relationships = new ArrayList<>();

    private final List<MembershipTO> memberships = new ArrayList<>();

    private final List<Long> dynGroups = new ArrayList<>();

    @XmlElementWrapper(name = "relationships")
    @XmlElement(name = "relationship")
    @JsonProperty("relationships")
    public List<RelationshipTO> getRelationships() {
        return relationships;
    }

    @JsonIgnore
    public Map<Long, RelationshipTO> getRelationshipMap() {
        Map<Long, RelationshipTO> result = new HashMap<>(getRelationships().size());
        for (RelationshipTO membership : getRelationships()) {
            result.put(membership.getRightKey(), membership);
        }
        result = Collections.unmodifiableMap(result);

        return result;
    }

    @XmlElementWrapper(name = "memberships")
    @XmlElement(name = "membership")
    @JsonProperty("memberships")
    public List<MembershipTO> getMemberships() {
        return memberships;
    }

    @JsonIgnore
    public Map<Long, MembershipTO> getMembershipMap() {
        Map<Long, MembershipTO> result = new HashMap<>(getMemberships().size());
        for (MembershipTO membership : getMemberships()) {
            result.put(membership.getRightKey(), membership);
        }
        result = Collections.unmodifiableMap(result);

        return result;
    }

    @XmlElementWrapper(name = "dynGroups")
    @XmlElement(name = "role")
    @JsonProperty("dynGroups")
    public List<Long> getDynGroups() {
        return dynGroups;
    }
}
