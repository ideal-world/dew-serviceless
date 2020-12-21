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

package idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy;

import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectOperatorKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 添加权限策略请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthPolicyAddReq implements Serializable {

    // 关联权限主体类型
    @NotNull
    private AuthSubjectKind relSubjectKind;
    // 关联权限主体Ids,有多个时逗号分隔
    @NotNull
    @NotBlank
    @Size(max = 10000)
    private String relSubjectIds;
    // 关联权限主体运算类型
    @NotNull
    private AuthSubjectOperatorKind subjectOperator;
    // 生效时间
    @Builder.Default
    protected Long effectiveTime = DewConstant.MIN_TIME;
    // 失效时间
    @Builder.Default
    protected Long expiredTime = DewConstant.MAX_TIME;
    // 关联资源Id
    @NotNull
    private Long relResourceId;
    // 操作类型
    private OptActionKind actionKind;
    // 操作结果
    @NotNull
    private AuthResultKind resultKind;
    // 是否排他
    @Builder.Default
    private Boolean exclusive = true;

}
