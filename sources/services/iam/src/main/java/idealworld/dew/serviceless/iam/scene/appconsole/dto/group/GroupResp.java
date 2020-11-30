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

package idealworld.dew.serviceless.iam.scene.appconsole.dto.group;

import idealworld.dew.serviceless.iam.enumeration.GroupKind;
import idealworld.dew.serviceless.iam.scene.common.dto.AppBasedResp;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(title = "群组响应")
public class GroupResp extends AppBasedResp {

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "群组编码", required = true)
    private String code;

    @NotNull
    @Schema(title = "群组类型", required = true)
    private GroupKind kind;

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "群组名称", required = true)
    private String name;

    @NotNull
    @Size(max = 1000)
    @Schema(title = "群组图标（路径）", required = true)
    private String icon;

    @NotNull
    @Schema(title = "显示排序，asc", required = true)
    private Integer sort;

    @NotNull
    @Schema(title = "关联群组Id，用于多树合成", required = true)
    private Long relGroupId;

    @NotNull
    @Schema(title = "关联群起始组节点Id，用于多树合成", required = true)
    private Long relGroupNodeId;

}
