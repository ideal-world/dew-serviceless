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

import idealworld.dew.baas.iam.enumeration.ExposeKind;
import idealworld.dew.baas.common.enumeration.ResourceKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改资源主体请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "修改资源主体请求")
public class ResourceSubjectModifyReq implements Serializable {

    @Size(max = 255)
    @Schema(title = "资源主体编码")
    private String code;

    @Size(max = 255)
    @Schema(title = "资源主体名称")
    private String name;

    @Schema(title = "资源主体显示排序，asc")
    private Integer sort;

    @Schema(title = "是否默认")
    private Boolean defaultByApp;

    @Schema(title = "资源类型")
    private ResourceKind kind;

    @Size(max = 5000)
    @Schema(title = "资源主体连接URI")
    private String uri;

    @Size(max = 1000)
    @Schema(title = "AK，部分类型支持写到URI中")
    private String ak;

    @Size(max = 1000)
    @Schema(title = "SK，部分类型支持写到URI中")
    private String sk;

    @Size(max = 1000)
    @Schema(title = "第三方平台账号名")
    private String platformAccount;

    @Size(max = 1000)
    @Schema(title = "第三方平台项目名，如华为云的ProjectId")
    private String platformProjectId;

    @Schema(title = "开放等级类型")
    private ExposeKind exposeKind;

}
