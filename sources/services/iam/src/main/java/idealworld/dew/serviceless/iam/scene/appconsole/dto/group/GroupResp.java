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

package idealworld.dew.serviceless.iam.scene.appconsole.dto.group;

import idealworld.dew.serviceless.iam.dto.GroupKind;
import idealworld.dew.serviceless.iam.scene.common.dto.AppBasedResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * 群组响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
// 群组响应")
public class GroupResp extends AppBasedResp {

    @NotNull
    @NotBlank
    @Size(max = 255)
    // 群组编码
    private String code;

    @NotNull
    // 群组类型
    private GroupKind kind;

    @NotNull
    @NotBlank
    @Size(max = 255)
    // 群组名称
    private String name;

    @NotNull
    @Size(max = 1000)
    // 群组图标（路径）
    private String icon;

    @NotNull
    // 显示排序，asc
    private Integer sort;

    @NotNull
    // 关联群组Id，用于多树合成
    private Long relGroupId;

    @NotNull
    // 关联群起始组节点Id，用于多树合成
    private Long relGroupNodeId;

}
