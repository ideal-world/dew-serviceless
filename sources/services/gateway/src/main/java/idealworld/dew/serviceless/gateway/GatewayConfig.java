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

package idealworld.dew.serviceless.gateway;

import idealworld.dew.serviceless.common.CommonConfig;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


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
public class GatewayConfig extends CommonConfig {

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
        private Long timeoutMS = 10000L;
        @Builder.Default
        private String iamServiceName = "iam.service";
        @Builder.Default
        private Integer iamServicePort = 9000;
        @Builder.Default
        private String cacheServiceName = "cache.service";
        @Builder.Default
        private Integer cacheServicePort = 9000;
        @Builder.Default
        private String httpServiceName = "http.service";
        @Builder.Default
        private Integer httpServicePort = 9000;
        @Builder.Default
        private String reldbServiceName = "reldb.service";
        @Builder.Default
        private Integer reldbServicePort = 9000;
        @Builder.Default
        private String mqServiceName = "mq.service";
        @Builder.Default
        private Integer mqServicePort = 9000;
        @Builder.Default
        private String objServiceName = "obj.service";
        @Builder.Default
        private Integer objServicePort = 9000;

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
        private String cacheTokenInfoKey = "dew:auth:token:info:";
        @Builder.Default
        private Map<String, List<String>> blockIps = new LinkedHashMap<>();
        @Builder.Default
        private Integer groupNodeLength = 5;

    }

}
