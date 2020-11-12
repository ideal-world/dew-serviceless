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

package idealworld.dew.baas.iam.scene.appconsole.dto.resource;

import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.iam.enumeration.ExposeKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 添加资源请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "添加资源请求")
public class ResourceAddReq implements Serializable {

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "资源名称", required = true)
    private String name;

    @NotNull
    @NotBlank
    @Size(max = 5000)
    @Schema(title = "资源URI", required = true)
    private String uri;

    @Size(max = 1000)
    @Schema(title = "资源图标（路径）")
    @Builder.Default
    private String icon = "";

    @Size(max = 1000)
    @Schema(title = "触发后的操作，多用于菜单链接")
    @Builder.Default
    private String action = "";

    @Schema(title = "资源显示排序，asc")
    @Builder.Default
    private Integer sort = 0;

    @Schema(title = "是否是资源组")
    @Builder.Default
    private Boolean group = false;

    @Schema(title = "资源所属组Id")
    @Builder.Default
    private Long parentId = Constant.OBJECT_UNDEFINED;

    @NotNull
    @Schema(title = "关联资源主体Id", required = true)
    private Long relResourceSubjectId;

    @Schema(title = "开放等级类型")
    @Builder.Default
    private ExposeKind exposeKind = ExposeKind.APP;

}
