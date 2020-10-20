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

package idealworld.dew.baas.iam.scene.tenantconsole.dto.account;

import idealworld.dew.baas.common.dto.IdResp;
import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

/**
 * 账号绑定响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(title = "账号绑定响应")
public class AccountBindResp extends IdResp {

    @NotNull
    @Schema(title = "源租户Id", required = true)
    private Long fromTenantId;

    @NotNull
    @Schema(title = "源租户账号Id", required = true)
    private Long fromAccountId;

    @NotNull
    @Schema(title = "目标租户Id", required = true)
    private Long toTenantId;

    @NotNull
    @Schema(title = "目标户账号Id", required = true)
    private Long toAccountId;

    @NotNull
    @Schema(title = "绑定使用的账号认证类型名称", required = true)
    private AccountIdentKind identKind;

}
