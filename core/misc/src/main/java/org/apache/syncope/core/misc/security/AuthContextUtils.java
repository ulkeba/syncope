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
package org.apache.syncope.core.misc.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.MapUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public final class AuthContextUtils {

    public static String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? SyncopeConstants.UNAUTHENTICATED : authentication.getName();
    }

    public static void updateAuthenticatedUsername(final String newUsername) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                new User(newUsername, "FAKE_PASSWORD", auth.getAuthorities()),
                auth.getCredentials(), auth.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    public static Map<String, Set<String>> getAuthorizations() {
        Map<String, Set<String>> result = null;

        final SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx != null && ctx.getAuthentication() != null && ctx.getAuthentication().getAuthorities() != null) {
            result = new HashMap<>();
            for (GrantedAuthority authority : ctx.getAuthentication().getAuthorities()) {
                if (authority instanceof SyncopeGrantedAuthority) {
                    result.put(
                            SyncopeGrantedAuthority.class.cast(authority).getAuthority(),
                            SyncopeGrantedAuthority.class.cast(authority).getRealms());
                }
            }
        }

        return MapUtils.emptyIfNull(result);
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private AuthContextUtils() {
    }
}
