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

package idealworld.dew.baas.iam.dto;

import idealworld.dew.baas.common.dto.IdResp;
import idealworld.dew.baas.iam.enumeration.ExposeKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

/**
 * 带关联租户应用的响应.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppBasedResp extends IdResp {

    @NotNull
    @Schema(title = "所属应用Id", required = true)
    private Long relAppId;

    @NotNull
    @Schema(title = "所属租户Id", required = true)
    private Long relTenantId;

    @NotNull
    @Schema(title = "开放等级类型", required = true)
    private ExposeKind exposeKind;

}
