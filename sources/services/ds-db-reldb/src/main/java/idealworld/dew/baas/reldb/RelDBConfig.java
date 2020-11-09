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

package idealworld.dew.baas.reldb;

import idealworld.dew.baas.common.CommonConfig;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.security.Security;


/**
 * Gateway config.
 *
 * @author gudaoxuri
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RelDBConfig extends CommonConfig {

    @Builder.Default
    private Request request = new Request();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @Builder.Default
        private String path = "/exec";

        @Builder.Default
        private String identOptHeaderName = "Dew-Ident-Opt";

    }

}
