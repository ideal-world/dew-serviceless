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

package idealworld.dew.framework.dto;

import idealworld.dew.framework.DewConstant;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Token对象，写入到缓存中.
 *
 * @author gudaoxuri
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IdentOptCacheInfo extends IdentOptInfo {

    @Builder.Default
    private String tokenKind = DewConstant.PARAM_DEFAULT_TOKEN_KIND;
    private Long accountId;
    private Long appId;
    private Long tenantId;

}
