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

package idealworld.dew.serviceless.iam.process.common.dto.tenant;

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
public class TenantRegisterReq implements Serializable {

    // 租户名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String tenantName;
    // 租户图标（路径）
    @Size(max = 1000)
    @Builder.Default
    private String icon = "";
    // 是否开放账号注册
    @Builder.Default
    private Boolean allowAccountRegister = false;
    // 租户扩展信息，Json格式
    @Size(max = 5000)
    @Builder.Default
    private String parameters = "{}";
    // 应用名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String appName;
    // 租户管理员登录用户名
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String accountUserName;
    // 租户管理员登录密钥
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String accountPassword;

}
