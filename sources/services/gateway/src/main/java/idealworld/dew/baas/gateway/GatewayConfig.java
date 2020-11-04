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

package idealworld.dew.baas.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway config.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayConfig {

    @Builder.Default
    private Request request = new Request();
    @Builder.Default
    private Exchange exchange = new Exchange();
    @Builder.Default
    private Distribute distribute = new Distribute();
    @Builder.Default
    private RedisConfig redis = new RedisConfig();
    @Builder.Default
    private Security security = new Security();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @Builder.Default
        private Integer port = 9000;
        @Builder.Default
        private String path = "/exec";
        @Builder.Default
        private String resourceUriKey = "res";
        @Builder.Default
        private String actionKey = "act";

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Exchange {

        @Builder.Default
        private String topic = "iam:exchange";

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Distribute {

        @Builder.Default
        private Integer timeoutMs = 5000;
        @Builder.Default
        private String iamServiceName = "iam.service";
        @Builder.Default
        private Integer iamServicePort = 9000;
        @Builder.Default
        private String iamServicePath = "/exec";
        @Builder.Default
        private String cacheServiceName = "cache.service";
        @Builder.Default
        private Integer cacheServicePort = 9000;
        @Builder.Default
        private String cacheServicePath = "/exec";
        @Builder.Default
        private String reldbServiceName = "reldb.service";
        @Builder.Default
        private Integer reldbServicePort = 9000;
        @Builder.Default
        private String reldbServicePath = "/exec";
        @Builder.Default
        private String mqServiceName = "mq.service";
        @Builder.Default
        private Integer mqServicePort = 9000;
        @Builder.Default
        private String mqServicePath = "/exec";
        @Builder.Default
        private String objServiceName = "obj.service";
        @Builder.Default
        private Integer objServicePort = 9000;
        @Builder.Default
        private String objServicePath = "/exec";

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisConfig {

        private String uri;
        @Builder.Default
        private String password = "123456";
        @Builder.Default
        private Integer maxPoolSize = 100;
        @Builder.Default
        private Integer maxPoolWaiting = 1000;

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
        private Integer tokenCacheExpireSec = 60;
        @Builder.Default
        private Integer akSkCacheExpireSec = 60;
        @Builder.Default
        private Integer resourceCacheExpireSec = 60 * 60 * 24;
        @Builder.Default
        private Integer appRequestDateOffsetMs = 5000;
        @Builder.Default
        private String cacheTokenInfoKey = "dew:auth:token:info:";
        @Builder.Default
        private String cacheAkSkInfoKey = "dew:auth:app:ak:";
        @Builder.Default
        private Map<String, List<String>> blockIps = new LinkedHashMap<>();
        @Builder.Default
        private Integer groupNodeLength = 5;

    }

}
