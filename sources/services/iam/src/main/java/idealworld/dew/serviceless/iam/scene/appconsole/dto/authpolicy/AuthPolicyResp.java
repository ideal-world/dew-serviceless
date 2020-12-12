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

import idealworld.dew.framework.dto.IdResp;
import idealworld.dew.serviceless.common.enumeration.AuthResultKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
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
// 权限策略响应")
public class AuthPolicyResp extends IdResp {

    @NotNull
    // 关联权限主体类型名称
    private AuthSubjectKind relSubjectKind;

    @NotNull
    @NotBlank
    @Size(max = 10000)
    // 关联权限主体Ids,有多个时逗号分隔
    private String relSubjectIds;

    @NotNull
    // 关联权限主体运算类型名称
    private AuthSubjectOperatorKind subjectOperator;

    @NotNull
    // 生效时间
    protected Date effectiveTime;

    @NotNull
    // 失效时间
    protected Date expiredTime;

    @NotNull
    // 关联资源Id
    private Long relResourceId;

    @NotNull
    // 操作类型名称
    private OptActionKind actionKind;

    @NotNull
    // 操作结果名称
    private AuthResultKind resultKind;

    @NotNull
    // 是否排他
    private Boolean exclusive;

    @NotNull
    // 所属应用Id
    private Long relAppId;

    @NotNull
    // 所属租户Id
    private Long relTenantId;

}
