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

package idealworld.dew.serviceless.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 安全响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StrSafeResp extends StrIdResp {

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "创建者", required = true)
    private String createUser;

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "最后一次修改者", required = true)
    private String updateUser;

    @NotNull
    @Schema(title = "创建时间", required = true)
    private Date createTime;

    @NotNull
    @Schema(title = "最后一次修改时间", required = true)
    private Date updateTime;

}
