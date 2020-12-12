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

package idealworld.dew.serviceless.iam.scene.appconsole.dto.resource;

import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
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
// 添加资源请求")
public class ResourceAddReq implements Serializable {

    @NotNull
    @NotBlank
    @Size(max = 255)
    // 资源名称
    private String name;

    @NotNull
    @NotBlank
    @Size(max = 5000)
    // 资源路径
    private String pathAndQuery;

    @Size(max = 1000)
    // 资源图标（路径）")
    @Builder.Default
    private String icon = "";

    @Size(max = 1000)
    // 触发后的操作，多用于菜单链接")
    @Builder.Default
    private String action = "";

    // 资源显示排序，asc")
    @Builder.Default
    private Integer sort = 0;

    // 是否是资源组")
    @Builder.Default
    private Boolean resGroup = false;

    // 资源所属组Id")
    @Builder.Default
    private Long parentId = Constant.OBJECT_UNDEFINED;

    @NotNull
    // 关联资源主体Id
    private Long relResourceSubjectId;

    // 开放等级类型")
    @Builder.Default
    private ExposeKind exposeKind = ExposeKind.APP;

}
