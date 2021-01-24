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

package idealworld.dew.serviceless.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 网关配置.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayConfig {

    @Builder.Default
    private Distribute distribute = new Distribute();
    @Builder.Default
    private Security security = new Security();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Distribute {

        @Builder.Default
        private String gatewayRequestPath = "/exec";
        @Builder.Default
        private Long gatewayTimeoutMS = 10000L;

        @Builder.Default
        private String iamModuleName = "iam";
        @Builder.Default
        private String cacheModuleName = "cache";
        @Builder.Default
        private String httpModuleName = "http";
        @Builder.Default
        private String reldbModuleName = "reldb";
        @Builder.Default
        private String mqModuleName = "mq";
        @Builder.Default
        private String objModuleName = "obj";
        @Builder.Default
        private String taskModuleName = "task";

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Security {

        @Builder.Default
        private String tokenFieldName = "Dew-Token";
        @Builder.Default
        private String akSkFieldName = "Authorization";
        @Builder.Default
        private String appId = "Dew-App-Id";
        @Builder.Default
        private String akSkDateFieldName = "Dew-Date";
        @Builder.Default
        private Integer tokenCacheExpireSec = 60;
        @Builder.Default
        private Integer akSkCacheExpireSec = 60;
        @Builder.Default
        private Integer appInfoCacheExpireSec = 60;
        @Builder.Default
        private Integer resourceCacheExpireSec = 60 * 60 * 24;
        @Builder.Default
        private Integer appRequestDateOffsetMs = 5000;
        @Builder.Default
        private Map<String, List<String>> blockIps = new LinkedHashMap<>();
        @Builder.Default
        private Integer groupNodeLength = 5;

    }

}
