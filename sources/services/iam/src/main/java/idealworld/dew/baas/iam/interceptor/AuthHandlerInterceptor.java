/*
 * Copyright 2020. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package idealworld.dew.baas.iam.interceptor;

import com.ecfront.dew.common.StandardCode;
import group.idealworld.dew.Dew;
import group.idealworld.dew.core.auth.dto.OptInfo;
import group.idealworld.dew.core.web.error.ErrorController;
import idealworld.dew.baas.common.auth.BasicAuthPolicy;
import idealworld.dew.baas.common.auth.ReadonlyAuthPolicy;
import idealworld.dew.baas.common.dto.IdentOptCacheInfo;
import idealworld.dew.baas.common.enumeration.AuthActionKind;
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.common.enumeration.ResourceKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Auth Servlet拦截器.
 *
 * @author gudaoxuri
 * @author gjason
 */
@Slf4j
@Component
public class AuthHandlerInterceptor implements AsyncHandlerInterceptor {

    @Autowired
    private ReadonlyAuthPolicy readonlyAuthPolicy;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var reqUri = "HTTP://" + Dew.Info.name + request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        var subjectInfo = new LinkedHashMap<AuthSubjectKind, List<String>>();
        Dew.auth.getOptInfo().ifPresent(optInfo -> {
            var iamOptInfo = (IdentOptCacheInfo) optInfo;
            if (iamOptInfo.getRoleInfo() != null && !iamOptInfo.getRoleInfo().isEmpty()) {
                subjectInfo.put(AuthSubjectKind.ROLE, iamOptInfo.getRoleInfo().stream()
                        .map(OptInfo.RoleInfo::getCode)
                        .collect(Collectors.toList()));
            }
            if (iamOptInfo.getGroupInfo() != null && !iamOptInfo.getGroupInfo().isEmpty()) {
                subjectInfo.put(AuthSubjectKind.GROUP_NODE, iamOptInfo.getGroupInfo().stream()
                        .map(group -> group.getGroupCode() + BasicAuthPolicy.GROUP_CODE_NODE_CODE_SPLIT + group.getGroupNodeCode())
                        .collect(Collectors.toList()));
            }
            if (iamOptInfo.getAccountCode() != null) {
                subjectInfo.put(AuthSubjectKind.ACCOUNT, new ArrayList<>() {
                    {
                        add(iamOptInfo.getAccountCode().toString());
                    }
                });
            }
            if (iamOptInfo.getTenantId() != null) {
                subjectInfo.put(AuthSubjectKind.TENANT, new ArrayList<>() {
                    {
                        add(iamOptInfo.getTenantId().toString());
                    }
                });
            }
            if (iamOptInfo.getAppId() != null) {
                subjectInfo.put(AuthSubjectKind.APP, new ArrayList<>() {
                    {
                        add(iamOptInfo.getAppId().toString());
                    }
                });
            }
        });
        AuthActionKind actionKind;
        switch (request.getMethod().toLowerCase()) {
            case "get":
                actionKind = AuthActionKind.FETCH;
                break;
            case "post":
                actionKind = AuthActionKind.MODIFY;
                break;
            case "patch":
                actionKind = AuthActionKind.PATCH;
                break;
            case "put":
                actionKind = AuthActionKind.MODIFY;
                break;
            case "delete":
                actionKind = AuthActionKind.DELETE;
                break;
            default:
                actionKind = AuthActionKind.FETCH;
        }
        var authR = readonlyAuthPolicy.authentication(ResourceKind.API, reqUri, actionKind, subjectInfo);
        if (!authR.ok()) {
            ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "鉴权错误，[" + authR.getCode() + "] " + authR.getMessage(),
                    AuthException.class.getName());
            return false;
        }
        if (authR.getBody() == AuthResultKind.REJECT
                || authR.getBody() == AuthResultKind.MODIFY) {
            ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "鉴权错误，没有权限访问对应的资源",
                    AuthException.class.getName());
            return false;
        }
        return true;
    }

}
