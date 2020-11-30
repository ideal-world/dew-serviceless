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

package idealworld.dew.serviceless.iam.scene.common.dto.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 注册租户请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "注册租户请求")
public class TenantRegisterReq implements Serializable {

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "租户名称", required = true)
    private String tenantName;

    @Size(max = 1000)
    @Schema(title = "租户图标（路径）")
    @Builder.Default
    private String icon = "";

    @Schema(title = "是否开放账号注册")
    @Builder.Default
    private Boolean allowAccountRegister = false;

    @Size(max = 5000)
    @Schema(title = "租户扩展信息，Json格式")
    @Builder.Default
    private String parameters = "{}";

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "应用名称", required = true)
    private String appName;

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "租户管理员登录用户名", required = true)
    private String accountUserName;

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "租户管理员登录密钥", required = true)
    private String accountPassword;

}
