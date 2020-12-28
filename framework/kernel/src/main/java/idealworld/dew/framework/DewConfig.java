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

package idealworld.dew.framework;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic config.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class DewConfig {

    @Builder.Default
    protected FunConfig funs = new FunConfig();
    @Builder.Default
    protected List<ModuleConfig> modules = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunConfig {

        @Builder.Default
        private HttpServerConfig httpServer = new HttpServerConfig();
        @Builder.Default
        private CacheConfig cache = new CacheConfig();
        @Builder.Default
        private SQLConfig sql = new SQLConfig();
        @Builder.Default
        private HttpClientConfig httpClient = new HttpClientConfig();
        @Builder.Default
        private EventBusConfig eventBus = new EventBusConfig();

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class HttpServerConfig {

            @Builder.Default
            private Integer port = 9000;
            @Builder.Default
            private String allowedOriginPattern = "*";

        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CacheConfig {

            private String uri;
            private String password;
            @Builder.Default
            private Integer maxPoolSize = 100;
            @Builder.Default
            private Integer maxPoolWaiting = 1000;
            @Builder.Default
            private Integer electionPeriodSec = 30;

        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SQLConfig {

            private String uri;
            private String host;
            @Builder.Default
            private Integer port = 3306;
            private String db;
            private String userName;
            private String password;
            @Builder.Default
            private String charset = "utf8mb4";
            @Builder.Default
            private String collation = "utf8mb4_general_ci";
            @Builder.Default
            private Integer maxPoolSize = 4;
            @Builder.Default
            private Integer maxPoolWaitQueueSize = -1;

        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class HttpClientConfig {

            private String uri;
            private Long timeoutMs;

        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EventBusConfig {

            private Long timeoutMs;

        }

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleConfig {

        @Builder.Default
        private Boolean enabled = true;
        private String clazzPackage;
        @Builder.Default
        private Boolean ha = false;
        private Integer instance;
        @Builder.Default
        private Boolean work = false;
        private Integer workPoolSize;
        private Long workMaxTimeMs;
        @Builder.Default
        private Object config = new Object();
        @Builder.Default
        protected FunConfig funs = new FunConfig();
    }

}
