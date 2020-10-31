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

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway config.
 *
 * @author gudaoxuri
 */
@Data
public class GatewayConfig {

    private Request request = new Request();
    private RedisConfig redis = new RedisConfig();
    private Security security = new Security();

    @Data
    public static class Request {

        private Integer port = 9000;
        private String path="/exec";
        private String resourceUriKey ="res";
        private String actionKey="act";

    }

    @Data
    public static class RedisConfig {

        private String uri;
        private String password;
        private Integer maxPoolSize = 6;
        private Integer maxPoolWaiting = 24;

    }

    @Data
    public static class Security {

        private String tokenFieldName = "Dew-Token";
        private String akSkFieldName = "Authorization";
        private Integer tokenCacheExpireSec = 60;
        private Integer akSkCacheExpireSec = 60;
        private Integer resourceCacheExpireSec = 60*60*24;
        private Integer appRequestDateOffsetMs = 5000;
        private String cacheTokenInfoKey = "dew:auth:token:info:";
        private String cacheAkSkInfoKey = "dew:auth:app:ak:";
        private Map<String, List<String>> blockIps = new LinkedHashMap<>();
        private Integer groupNodeLength = 5;

    }

}
