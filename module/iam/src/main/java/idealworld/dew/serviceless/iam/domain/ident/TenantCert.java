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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 租户凭证配置.
 * <p>
 * 用于指定当前租户凭证类型及个性化配置。
 * <p>
 * 一个账号可以有一个或多个凭证，每个凭证类型有各自可保留的版本数量。
 * <p>
 * 此模型用于处理单点、单终端、多终端同时登录的问题。
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TenantCert extends SafeEntity {

    // 凭证类型名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String category;
    // 凭证保留的版本数量
    @NotNull
    private Integer version;
    // 关联租户Id
    @NotNull
    private Long relTenantId;

    @Override
    public String tableName() {
        return "iam_" + super.tableName();
    }


}
