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
package org.apache.syncope.core.persistence.jpa.validation.entity;

import java.util.regex.Pattern;
import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.Realm;

public class RealmValidator extends AbstractValidator<RealmCheck, Realm> {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9]+");

    @Override
    public boolean isValid(final Realm object, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid = true;

        if (SyncopeConstants.ROOT_REALM.equals(object.getName())) {
            if (object.getParent() != null) {
                isValid = false;

                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidRealm,
                                "Root realm cannot have a parent realm")).
                        addPropertyNode("parent").addConstraintViolation();
            }
        } else {
            if (object.getParent() == null) {
                isValid = false;

                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidRealm,
                                "A realm needs to reference a parent realm")).
                        addPropertyNode("parent").addConstraintViolation();
            }

            if (!NAME_PATTERN.matcher(object.getName()).matches()) {
                isValid = false;

                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidRealm,
                                "Only letters and numbers are allowed in realm name")).
                        addPropertyNode("name").addConstraintViolation();
            }
        }

        return isValid;
    }
}
