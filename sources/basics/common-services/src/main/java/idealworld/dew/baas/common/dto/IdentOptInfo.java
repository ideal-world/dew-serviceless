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

package idealworld.dew.baas.common.dto;

import group.idealworld.dew.core.auth.dto.OptInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * Ident opt info.
 *
 * @author gudaoxuri
 */
@EqualsAndHashCode(callSuper = true)
@Schema(title = "操作用户信息")
public class IdentOptInfo extends OptInfo<IdentOptInfo> {

    @Schema(title = "应用Id", required = true)
    private Long appId;

    @Schema(title = "租户Id", required = true)
    private Long tenantId;

    @Schema(title = "群组列表", required = true)
    private Set<GroupInfo> groupInfo;

    public Long getTenantId() {
        return tenantId;
    }

    public IdentOptInfo setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public Long getAppId() {
        return appId;
    }

    public IdentOptInfo setAppId(Long appId) {
        this.appId = appId;
        return this;
    }

    public Set<GroupInfo> getGroupInfo() {
        return groupInfo;
    }

    public IdentOptInfo setGroupInfo(Set<GroupInfo> groupInfo) {
        this.groupInfo = groupInfo;
        return this;
    }

    @Data
    @Builder
    @Schema(title = "群组信息", required = true)
    public static class GroupInfo {

        @Schema(title = "群组Id", required = true)
        private Long groupId;
        @Schema(title = "群组节点Id", required = true)
        private Long groupNodeId;
        @Schema(title = "群组业务编码", required = true)
        private String groupNodeBusCode;
        @Schema(title = "群组显示名称", required = true)
        private String groupName;
        @Schema(title = "群组节点显示名称", required = true)
        private String groupNodeName;

    }

}
