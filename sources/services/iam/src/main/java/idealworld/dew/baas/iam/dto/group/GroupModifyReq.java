/*
 * Copyright 2020. the original author or authors.
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

package idealworld.dew.baas.iam.dto.group;

import idealworld.dew.baas.iam.enumeration.GroupKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改角色定义请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "修改角色定义请求")
public class GroupModifyReq implements Serializable {

    @Schema(title = "群组类型")
    private GroupKind kind;

    @Size(max = 255)
    @Schema(title = "群组名称")
    private String name;

    @Size(max = 1000)
    @Schema(title = "群组图标（路径）")
    private String icon;

    @Schema(title = "显示排序，asc")
    private Integer sort;

    @Schema(title = "关联群组Id，用于多树合成")
    private Long relGroupId;

    @Schema(title = "关联群起始组节点Id，用于多树合成")
    private Long relGroupNodeId;

}
