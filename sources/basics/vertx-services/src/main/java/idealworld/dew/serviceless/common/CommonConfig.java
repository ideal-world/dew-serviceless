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

package idealworld.dew.serviceless.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Basic config.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class CommonConfig {

    @Builder.Default
    protected HttpServerConfig httpServer = new HttpServerConfig();
    @Builder.Default
    protected RedisConfig redis = new RedisConfig();
    @Builder.Default
    protected IAMConfig iam = new IAMConfig();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HttpServerConfig {

        @Builder.Default
        private Integer port = 9000;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisConfig {

        private String uri;
        private String password;
        @Builder.Default
        private Integer maxPoolSize = 100;
        @Builder.Default
        private Integer maxPoolWaiting = 1000;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IAMConfig {

        @Builder.Default
        private String uri = "http://iam";
        private Long appId;
        private Long tenantId;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JDBCConfig {

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

}
