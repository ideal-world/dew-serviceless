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
 * 群组节点.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GroupNode extends SafeEntity {

    // 节点编码
    @NotNull
    @Size(max = 1000)
    private String code;
    // 业务编码
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String busCode;
    // 节点名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    // 节点扩展信息，Json格式
    @NotNull
    @Size(max = 2000)
    private String parameters;
    // 关联群组Id
    @NotNull
    private Long relGroupId;

    @Override
    protected String tableName() {
        return "iam_" + super.tableName();
    }

}
