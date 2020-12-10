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

package idealworld.dew.framework.fun.httpserver;

import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.IdentOptCacheInfo;
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
public abstract class AuthHttpHandler extends KernelHttpHandler {

    protected static final String CONTEXT_INFO = "CONTEXT";

    public AuthHttpHandler(String moduleName) {
        super(moduleName);
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
                        .map(group -> group.getGroupCode() + DewAuthConstant.GROUP_CODE_NODE_CODE_SPLIT + group.getGroupNodeCode())
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
