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
public class ResourceSubjectModifyReq implements Serializable {

    // 资源主体编码后缀
    @Size(max = 255)
    private String codePostfix;
    // 资源主体名称
    @Size(max = 255)
    private String name;
    // 资源主体显示排序，asc
    private Integer sort;
    // 资源类型
    private ResourceKind kind;
    // 资源主体连接URI
    @Size(max = 5000)
    private String uri;
    // AK，部分类型支持写到URI中
    @Size(max = 1000)
    private String ak;
    // SK，部分类型支持写到URI中
    @Size(max = 1000)
    private String sk;
    // 第三方平台账号名
    @Size(max = 1000)
    private String platformAccount;
    // 第三方平台项目名，如华为云的ProjectId
    @Size(max = 1000)
    private String platformProjectId;
    // 执行超时
    private Long timeoutMS;

}
