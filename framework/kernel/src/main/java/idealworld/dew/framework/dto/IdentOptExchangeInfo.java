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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Token对象，用于模块间传输.
 *
 * @author gudaoxuri
 */
// TODO
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IdentOptExchangeInfo extends IdentOptCacheInfo {

    // 不被信任的应用OpenId，直接来自于外部传入未做校验
    private String unauthorizedAppCode;
    // 不被信任的应用Id，直接来自于外部传入未做校验
    private Long unauthorizedAppId;
    // 不被信任的租户Id，直接来自于外部传入未做校验
    private Long unauthorizedTenantId;

}
