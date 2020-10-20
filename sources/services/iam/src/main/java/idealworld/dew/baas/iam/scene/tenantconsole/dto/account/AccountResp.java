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

package idealworld.dew.baas.iam.scene.tenantconsole.dto.account;

import idealworld.dew.baas.common.dto.IdResp;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 账号响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(title = "账号响应")
public class AccountResp extends IdResp {

    @NotNull
    @NotBlank
    @Size(max = 100)
    @Schema(title = "Open Id", required = true)
    private String openId;

    @NotNull
    @NotBlank
    @Size(max = 255)
    @Schema(title = "账号名称", required = true)
    private String name;

    @NotNull
    @Size(max = 1000)
    @Schema(title = "账号头像（路径）", required = true)
    private String avatar;

    @NotNull
    @Size(max = 2000)
    @Schema(title = "账号扩展信息，Json格式", required = true)
    private String parameters ;

    @NotNull
    @Schema(title = "父账号Id")
    private Long parentId;

    @NotNull
    @Schema(title = "账号状态")
    private CommonStatus status;

}
