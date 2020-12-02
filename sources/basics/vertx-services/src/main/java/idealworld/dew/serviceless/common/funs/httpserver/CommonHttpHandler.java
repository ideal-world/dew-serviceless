/*
 * Copyright 2020. gudaoxuri
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

package idealworld.dew.serviceless.common.funs.httpserver;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.StandardCode;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.IdentOptCacheInfo;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP 处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public abstract class CommonHttpHandler implements Handler<RoutingContext> {

    protected static final String CONTEXT_INFO = "CONTEXT";

    protected void error(StandardCode statusCode, Class<?> clazz, String msg, RoutingContext ctx) {
        log.warn("[" + clazz.getSimpleName() + "]Request error [{}]: {}", statusCode.toString(), msg);
        ctx.response().setStatusCode(200).end($.json.toJsonString(new Resp<Void>(statusCode.toString(), msg, null)));
    }

    protected void error(StandardCode statusCode, Class<?> clazz, String msg, RoutingContext ctx, Throwable e) {
        log.warn("[" + clazz.getSimpleName() + "]Request error [{}]{}", statusCode.toString(), e.getMessage(), e);
        ctx.response().setStatusCode(200).end($.json.toJsonString(new Resp<Void>(statusCode.toString(), msg, null)));
    }

    protected Map<AuthSubjectKind, List<String>> packageSubjectInfo(IdentOptCacheInfo identOptInfo) {
        var subjectInfo = new LinkedHashMap<AuthSubjectKind, List<String>>();
        if (identOptInfo != null) {
            if (identOptInfo.getAccountCode() != null) {
                subjectInfo.put(AuthSubjectKind.ACCOUNT, new ArrayList<>() {
                    {
                        add(identOptInfo.getAccountCode().toString());
                    }
                });
            }
            if (identOptInfo.getGroupInfo() != null && !identOptInfo.getGroupInfo().isEmpty()) {
                subjectInfo.put(AuthSubjectKind.GROUP_NODE, identOptInfo.getGroupInfo().stream()
                        .map(group -> group.getGroupCode() + Constant.GROUP_CODE_NODE_CODE_SPLIT + group.getGroupNodeCode())
                        .collect(Collectors.toList()));
            }
            if (identOptInfo.getRoleInfo() != null && !identOptInfo.getRoleInfo().isEmpty()) {
                subjectInfo.put(AuthSubjectKind.ROLE, identOptInfo.getRoleInfo().stream()
                        .map(IdentOptCacheInfo.RoleInfo::getCode)
                        .collect(Collectors.toList()));
            }
            if (identOptInfo.getAppId() != null) {
                subjectInfo.put(AuthSubjectKind.APP, new ArrayList<>() {
                    {
                        add(identOptInfo.getAppId().toString());
                    }
                });
            }
            if (identOptInfo.getTenantId() != null) {
                subjectInfo.put(AuthSubjectKind.TENANT, new ArrayList<>() {
                    {
                        add(identOptInfo.getTenantId().toString());
                    }
                });
            }
        }
        return subjectInfo;
    }

}
