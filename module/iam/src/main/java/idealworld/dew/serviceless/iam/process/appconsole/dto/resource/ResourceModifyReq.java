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

package idealworld.dew.serviceless.iam.process.appconsole.dto.resource;

import idealworld.dew.serviceless.iam.dto.ExposeKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改资源请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceModifyReq implements Serializable {

    // 资源名称
    @Size(max = 255)
    private String name;
    // 资源路径
    @Size(max = 5000)
    private String pathAndQuery;
    // 资源图标（路径）
    @Size(max = 1000)
    private String icon;
    // 触发后的操作，多用于菜单链接
    @Size(max = 1000)
    private String action;
    // 资源显示排序，asc
    private Integer sort;
    // 是否是资源组
    private Boolean resGroup;
    // 资源所属组Id
    private Long parentId;
    // 开放等级类型
    private ExposeKind exposeKind;

}
