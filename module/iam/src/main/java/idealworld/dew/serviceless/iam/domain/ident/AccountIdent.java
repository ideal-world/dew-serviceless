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
 * 账号认证.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AccountIdent extends SafeEntity {

    // 账号认证类型名称
    @NotNull
    private AccountIdentKind kind;
    // 账号认证名称
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String ak;
    // 账号认证密钥
    @NotNull
    @NotBlank
    @Size(max = 2000)
    private String sk;
    // 账号认证有效开始时间
    @NotNull
    private Long validStartTime;
    // 账号认证有效结束时间
    @NotNull
    private Long validEndTime;
    // 关联账号Id
    @NotNull
    private Long relAccountId;
    // 关联租户Id
    @NotNull
    private Long relTenantId;

    @Override
    public String tableName() {
        return "iam_" + super.tableName();
    }

}
