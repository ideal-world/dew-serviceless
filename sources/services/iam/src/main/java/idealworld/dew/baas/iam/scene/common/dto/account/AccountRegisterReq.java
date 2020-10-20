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

package idealworld.dew.baas.iam.scene.common.dto.account;

import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
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
 * 注册账号请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "注册账号请求")
public class AccountRegisterReq implements Serializable {

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "账号名称", required = true)
    private String name;

    @Size(max = 1000)
    @Schema(title = "账号头像（路径）")
    @Builder.Default
    private String avatar = "";

    @Size(max = 2000)
    @Schema(title = "账号扩展信息，Json格式")
    @Builder.Default
    private String parameters = "{}";

    @Schema(title = "账号认证类型")
    @Builder.Default
    private AccountIdentKind kind = AccountIdentKind.USERNAME;

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "账号认证名称", required = true)
    private String ak;

    @Size(max = 255)
    @Schema(title = "账号认证密钥")
    @Builder.Default
    private String sk = "";

    @NotNull
    @Schema(title = "关联应用Id")
    private Long relAppId;


}
