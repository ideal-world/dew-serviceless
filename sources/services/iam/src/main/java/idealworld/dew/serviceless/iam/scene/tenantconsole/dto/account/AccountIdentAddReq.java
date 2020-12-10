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

package idealworld.dew.serviceless.iam.scene.tenantconsole.dto.account;

import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * 添加账号认证请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "添加账号认证请求")
public class AccountIdentAddReq implements Serializable {

    @NotNull
    @Schema(title = "账号认证类型", required = true)
    private AccountIdentKind kind;

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "账号认证名称", required = true)
    private String ak;

    @Size(max = 255)
    @Schema(title = "账号认证密钥")
    @Builder.Default
    private String sk = "";

    @Schema(title = "账号认证有效开始时间")
    private Date validStartTime;

    @Schema(title = "账号认证有效结束时间")
    private Date validEndTime;

}
