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

package idealworld.dew.serviceless.iam.domain.auth;

import idealworld.dew.framework.domain.SafeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 角色定义.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RoleDef extends SafeEntity {

    // 角色定义编码
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String code;
    // 角色定义名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    // 显示排序，asc
    @NotNull
    private Integer sort;
    // 关联应用Id
    @NotNull
    private Long relAppId;
    // 关联租户Id
    @NotNull
    private Long relTenantId;

    @Override
    public String tableName() {
        return "iam_" + super.tableName();
    }

}
