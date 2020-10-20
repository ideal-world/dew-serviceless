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

package idealworld.dew.baas.iam.scene.tenantconsole.dto.tenant;

import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 添加租户认证配置请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "添加租户认证配置请求")
public class TenantIdentAddReq implements Serializable {

    @NotNull
    @Schema(title = "租户认证类型名称", required = true)
    private AccountIdentKind kind;

    @Size(max = 2000)
    @Schema(title = "认证AK校验正则规则说明")
    @Builder.Default
    private String validAKRuleNote = "";

    @Size(max = 2000)
    @Schema(title = "认证AK校验正则规则")
    @Builder.Default
    private String validAKRule = "";

    @Size(max = 2000)
    @Schema(title = "认证SK校验正则规则说明")
    @Builder.Default
    private String validSKRuleNote = "";

    @Size(max = 2000)
    @Schema(title = "认证SK校验正则规则")
    @Builder.Default
    private String validSKRule = "";

    // TODO 登录过期设置
    @NotNull
    @Schema(title = "认证有效时间（秒）", required = true)
    private Long validTimeSec;

}
