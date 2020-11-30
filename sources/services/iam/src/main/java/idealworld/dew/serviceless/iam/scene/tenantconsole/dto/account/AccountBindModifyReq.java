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

package idealworld.dew.serviceless.iam.scene.tenantconsole.dto.account;

import idealworld.dew.serviceless.iam.enumeration.AccountIdentKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 修改账号绑定请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "修改账号绑定请求")
public class AccountBindModifyReq implements Serializable {

    @Schema(title = "源租户Id")
    private Long fromTenantId;

    @Schema(title = "源租户账号Id")
    private Long fromAccountId;

    @Schema(title = "目标租户Id")
    private Long toTenantId;

    @Schema(title = "目标户账号Id")
    private Long toAccountId;

    @Schema(title = "绑定使用的账号认证类型名称")
    private AccountIdentKind identKind;

}
