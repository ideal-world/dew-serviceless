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

package idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy;

import idealworld.dew.serviceless.common.dto.IdResp;
import idealworld.dew.serviceless.common.enumeration.AuthResultKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 权限策略响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(title = "权限策略响应")
public class AuthPolicyResp extends IdResp {

    @NotNull
    @Schema(title = "关联权限主体类型名称", required = true)
    private AuthSubjectKind relSubjectKind;

    @NotNull
    @NotBlank
    @Size(max = 10000)
    @Schema(title = "关联权限主体Ids,有多个时逗号分隔", required = true)
    private String relSubjectIds;

    @NotNull
    @Schema(title = "关联权限主体运算类型名称", required = true)
    private AuthSubjectOperatorKind subjectOperator;

    @NotNull
    @Schema(title = "生效时间", required = true)
    protected Date effectiveTime;

    @NotNull
    @Schema(title = "失效时间", required = true)
    protected Date expiredTime;

    @NotNull
    @Schema(title = "关联资源Id", required = true)
    private Long relResourceId;

    @NotNull
    @Schema(title = "操作类型名称", required = true)
    private OptActionKind actionKind;

    @NotNull
    @Schema(title = "操作结果名称", required = true)
    private AuthResultKind resultKind;

    @NotNull
    @Schema(title = "是否排他", required = true)
    private Boolean exclusive;

    @NotNull
    @Schema(title = "所属应用Id", required = true)
    private Long relAppId;

    @NotNull
    @Schema(title = "所属租户Id", required = true)
    private Long relTenantId;

}
