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

package idealworld.dew.serviceless.iam.domain.auth;

import idealworld.dew.framework.domain.SafeEntity;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectOperatorKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 权限策略.
 * <p>
 * 支持跨应用、租户的权限分配（发布--订阅模式）.
 * <p>
 * 仅资源所有者可以分配自己的资源权限.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AuthPolicy extends SafeEntity {

    @Override
    public String tableName() {
        return "iam_"+super.tableName();
    }

    // 关联权限主体类型名称
    @NotNull
    private AuthSubjectKind relSubjectKind;
    // 关联权限主体Ids,有多个时逗号分隔,注意必须存在最后一个逗号
    @NotNull
    @NotBlank
    @Size(max = 10000)
    private String relSubjectIds;
    // 关联权限主体运算类型名称
    @NotNull
    private AuthSubjectOperatorKind subjectOperator;
    // 生效时间
    @NotNull
    protected Date effectiveTime;
    // 失效时间
    @NotNull
    protected Date expiredTime;
    // 关联资源Id
    @NotNull
    private Long relResourceId;
    // 操作类型名称
    @NotNull
    private OptActionKind actionKind;
    // 操作结果名称
    @NotNull
    private AuthResultKind resultKind;
    // 是否排他
    @NotNull
    private Boolean exclusive;
    // 关联应用Id
    @NotNull
    private Long relAppId;
    // 关联租户Id
    @NotNull
    private Long relTenantId;

}
