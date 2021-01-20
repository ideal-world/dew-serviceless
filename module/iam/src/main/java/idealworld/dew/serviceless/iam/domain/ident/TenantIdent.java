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

package idealworld.dew.serviceless.iam.domain.ident;

import idealworld.dew.framework.domain.SafeEntity;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 租户认证配置.
 * <p>
 * 用于指定当前租户可使用的认证类型及个性化配置。
 * <p>
 * {@link AccountIdentKind#USERNAME} 为基础认证，所有租户都必须包含此认证类型。
 * <p>
 * 所有认证类型都支持使用自己的AK及基础认证的SK(密码)作为认证方式。
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TenantIdent extends SafeEntity {

    // 租户认证类型名称
    @NotNull
    private AccountIdentKind kind;
    // 认证AK校验正则规则说明
    @NotNull
    @NotBlank
    @Size(max = 2000)
    private String validAkRuleNote;
    // 认证AK校验正则规则
    @NotNull
    @NotBlank
    @Size(max = 2000)
    private String validAkRule;
    // 认证SK校验正则规则说明
    @NotNull
    @NotBlank
    @Size(max = 2000)
    private String validSkRuleNote;
    // 认证SK校验正则规则
    @NotNull
    @NotBlank
    @Size(max = 2000)
    private String validSkRule;
    // 认证有效时间（秒）
    @NotNull
    private Long validTimeSec;
    // 关联租户Id
    @NotNull
    private Long relTenantId;

    @Override
    public String tableName() {
        return "iam_" + super.tableName();
    }

}
