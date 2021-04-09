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

package idealworld.dew.serviceless.iam.domain.ident;

import idealworld.dew.framework.domain.SafeEntity;
import idealworld.dew.framework.dto.CommonStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 账号.
 * <p>
 * 用户身份单位。
 * <p>
 * 隶属于租户，即便是同一个自然人在不同租户间也会有各自的账号，同一租户的不同应用共享账号信息。
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Account extends SafeEntity {

    // OpenId是账号对外提供的主键，给业务方使用，用于标识账号唯一性的字段。
    @NotNull
    @NotBlank
    @Size(max = 100)
    private String openId;
    // 账号名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    // 账号头像（路径）
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String avatar;
    // 账号扩展信息，Json格式
    @NotNull
    @NotBlank
    @Size(max = 2000)
    private String parameters;
    // 父子账号可用于支持RAM（ Resource Access Management）用户功能。
    // 父子账号间OpenId相同。
    @NotNull
    private Long parentId;
    // 关联租户Id
    @NotNull
    private Long relTenantId;
    // 账号状态
    @NotNull
    private CommonStatus status;

    @Override
    protected String tableName() {
        return "iam_" + super.tableName();
    }

}
