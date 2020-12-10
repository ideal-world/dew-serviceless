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

package idealworld.dew.serviceless.iam.scene.appconsole.dto.role;

import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 添加角色请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "添加角色请求")
public class RoleAddReq implements Serializable {

    @NotNull
    @Schema(title = "关联角色定义Id", required = true)
    private Long relRoleDefId;

    @Schema(title = "关联群组节点Id")
    @Builder.Default
    private Long relGroupNodeId = Constant.OBJECT_UNDEFINED;

    @Size(max = 255)
    @Schema(title = "角色名称")
    private String name;

    @Schema(title = "显示排序，asc")
    @Builder.Default
    private Integer sort = 0;

    @Schema(title = "开放等级类型")
    @Builder.Default
    private ExposeKind exposeKind = ExposeKind.APP;

}
