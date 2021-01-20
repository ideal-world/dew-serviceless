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

package idealworld.dew.framework;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Basic config.
 *
 * @author gudaoxuri
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DewAuthConfig extends DewConfig {

    @Builder.Default
    protected IAMConfig iam = new IAMConfig();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IAMConfig {

        @Builder.Default
        private String moduleName = "iam";
        private Long appId;
        private Long tenantId;

    }

}
