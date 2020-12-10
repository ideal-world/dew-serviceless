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

import idealworld.dew.serviceless.iam.domain.AppBasedEntity;
import idealworld.dew.serviceless.iam.dto.GroupKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 群组.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Group extends AppBasedEntity {

    @Override
    public String tableName() {
        return "iam_"+super.tableName();
    }

    // 群组编码
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String code;
    // 群组类型名称
    @NotNull
    private GroupKind kind;
    // 群组名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String icon;
    // 显示排序，asc
    @NotNull
    private Integer sort;
    // 关联群组Id，用于多树合成
    @NotNull
    private Long relGroupId;
    // 关联群起始组节点Id，用于多树合成
    @NotNull
    private Long relGroupNodeId;

}
