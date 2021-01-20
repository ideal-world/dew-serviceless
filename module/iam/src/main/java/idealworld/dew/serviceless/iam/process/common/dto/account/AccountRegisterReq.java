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

package idealworld.dew.serviceless.iam.process.common.dto.account;

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
 * 注册账号请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRegisterReq implements Serializable {

    // 账号名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    // 账号头像（路径）
    @Size(max = 1000)
    @Builder.Default
    private String avatar = "";
    // 账号扩展信息，Json格式
    @Size(max = 2000)
    @Builder.Default
    private String parameters = "{}";
    // 账号认证类型
    @Builder.Default
    private AccountIdentKind kind = AccountIdentKind.USERNAME;
    // 账号认证名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String ak;
    // 账号认证密钥
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String sk;
    // 关联应用OpenId
    @NotNull
    @NotBlank
    @Size(max = 100)
    private String relAppCode;


}
