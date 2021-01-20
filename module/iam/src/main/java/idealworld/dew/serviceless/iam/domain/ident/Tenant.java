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
 * 租户.
 * <p>
 * 数据隔离单位，不同租户间的数据不能直接共享。
 * <p>
 * 一个租户可以有多个应用。
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Tenant extends SafeEntity {

    // 租户名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    // 租户图标（路径）
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String icon;
    // 是否开放账号注册
    @NotNull
    private Boolean allowAccountRegister;
    // 租户扩展信息，Json格式
    @NotNull
    @NotBlank
    @Size(max = 5000)
    private String parameters;
    // 租户状态
    @NotNull
    private CommonStatus status;

    @Override
    public String tableName() {
        return "iam_" + super.tableName();
    }

}
