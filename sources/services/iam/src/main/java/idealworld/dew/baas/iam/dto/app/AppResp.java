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

package idealworld.dew.baas.iam.dto.app;

import idealworld.dew.baas.common.dto.IdResp;
import idealworld.dew.baas.common.enumeration.CommonStatus;
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
 * 应用响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(title = "应用响应")
public class AppResp extends IdResp {

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "应用名称", required = true)
    private String name;

    @NotNull
    @NotBlank
    @Size(max = 1000)
    @Schema(title = "应用图标", required = true)
    private String icon;

    @NotNull
    @NotBlank
    @Size(max = 2000)
    @Schema(title = "应用扩展信息（Json格式）", required = true)
    private String parameters;

    @NotNull
    @Schema(title = "应用状态", required = true)
    private CommonStatus status;

}
