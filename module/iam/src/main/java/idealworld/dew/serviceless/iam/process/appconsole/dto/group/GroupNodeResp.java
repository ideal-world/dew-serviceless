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

package idealworld.dew.serviceless.iam.process.appconsole.dto.group;

import idealworld.dew.framework.dto.IdResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 群组节点响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupNodeResp extends IdResp {

    // 业务编码
    @NotNull
    @Size(max = 1000)
    private String busCode;
    // 节点名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    // 节点扩展信息，Json格式
    @NotNull
    @Size(max = 2000)
    private String parameters;
    // 上级节点Id
    @NotNull
    private Long parentId;
    // 关联群组Id
    @NotNull
    private Long relGroupId;

}
