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

package idealworld.dew.serviceless.iam.process.appconsole.dto.resource;

import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.serviceless.iam.process.common.dto.AppBasedResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * 资源信息响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResourceSubjectResp extends AppBasedResp {

    // 资源主体编码
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String code;
    // 资源主体编码后缀
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String codePostfix;
    // 资源主体名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    // 资源主体显示排序，asc
    @NotNull
    private Integer sort;
    // 资源类型
    @NotNull
    private ResourceKind kind;
    // 资源主体连接URI
    @NotNull
    @NotBlank
    @Size(max = 5000)
    private String uri;
    // AK，部分类型支持写到URI中
    @NotNull
    @Size(max = 1000)
    private String ak;
    // SK，部分类型支持写到URI中
    @NotNull
    @Size(max = 1000)
    private String sk;
    // 第三方平台账号名
    @NotNull
    @Size(max = 1000)
    private String platformAccount;
    // 第三方平台项目名，如华为云的ProjectId
    @NotNull
    @Size(max = 1000)
    private String platformProjectId;
    // 执行超时
    @NotNull
    private Long timeoutMs;

}
