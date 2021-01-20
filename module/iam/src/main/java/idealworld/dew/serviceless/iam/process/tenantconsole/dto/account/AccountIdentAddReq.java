/*
 * Copyright 2021. gudaoxuri
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

package idealworld.dew.serviceless.iam.process.tenantconsole.dto.account;

import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 添加账号认证请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountIdentAddReq implements Serializable {

    // 账号认证类型
    @NotNull
    private AccountIdentKind kind;
    // 账号认证名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String ak;
    // 账号认证密钥
    @Size(max = 255)
    @Builder.Default
    private String sk = "";
    // 账号认证有效开始时间
    private Long validStartTime;
    // 账号认证有效结束时间
    private Long validEndTime;

}
