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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 应用认证.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AppIdent extends SafeEntity {

    // 应用认证用途
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String note;
    // 应用认证名称（Access Key Id）
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String ak;
    // 应用认证密钥（Secret Access Key）
    @NotNull
    @NotBlank
    @Size(max = 2000)
    private String sk;
    // 应用认证有效时间
    @NotNull
    private Long validTime;
    // 关联应用Id
    @NotNull
    private Long relAppId;

    @Override
    public String tableName() {
        return "iam_" + super.tableName();
    }

}
