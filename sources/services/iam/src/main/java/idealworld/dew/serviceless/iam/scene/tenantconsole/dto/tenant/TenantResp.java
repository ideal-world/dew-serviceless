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

package idealworld.dew.serviceless.iam.scene.tenantconsole.dto.tenant;

import idealworld.dew.serviceless.common.dto.IdResp;
import idealworld.dew.serviceless.common.enumeration.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 租户响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(title = "租户响应")
public class TenantResp extends IdResp {

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "租户名称", required = true)
    private String name;

    @NotNull
    @NotBlank
    @Size(max = 1000)
    @Schema(title = "租户图标（路径）", required = true)
    private String icon;

    @NotNull
    @Schema(title = "是否开放账号注册", required = true)
    private Boolean allowAccountRegister;

    @NotNull
    @NotBlank
    @Size(max = 5000)
    @Schema(title = "租户扩展信息，Json格式", required = true)
    private String parameters;

    @NotNull
    @Schema(title = "租户状态", required = true)
    private CommonStatus status;

}