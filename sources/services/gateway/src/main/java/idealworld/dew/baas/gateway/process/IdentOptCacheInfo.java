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

package idealworld.dew.baas.gateway.process;

import lombok.Data;

import java.util.Set;

/**
 * token opt info.
 *
 * @author gudaoxuri
 */
@Data
public class IdentOptCacheInfo {

    private String token;
    private Object accountCode;
    private Set<RoleInfo> roleInfo;
    private Set<GroupInfo> groupInfo;
    private Long appId;
    private Long tenantId;

    @Data
    public static class RoleInfo {
        private String code;
        private String name;
    }

    @Data
    public static class GroupInfo {
        private String groupCode;
        private String groupNodeCode;
        private String groupNodeBusCode;
        private String groupName;
        private String groupNodeName;
    }

}
