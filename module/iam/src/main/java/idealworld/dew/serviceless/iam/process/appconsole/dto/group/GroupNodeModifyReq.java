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

package idealworld.dew.serviceless.iam.process.appconsole.dto.group;

import idealworld.dew.framework.DewConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改群组节点请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupNodeModifyReq implements Serializable {

    // 业务编码
    @Size(max = 1000)
    private String busCode;
    // 节点名称
    @Size(max = 255)
    private String name;
    // 节点扩展信息，Json格式
    @Size(max = 2000)
    private String parameters;
    // 上级节点Id
    @Builder.Default
    private Long parentId = DewConstant.OBJECT_UNDEFINED;
    // 同级上一个节点Id
    @Builder.Default
    private Long siblingId = DewConstant.OBJECT_UNDEFINED;

}
