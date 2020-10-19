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

package idealworld.dew.baas.iam;

import idealworld.dew.baas.common.CommonConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * IAM config.
 *
 * @author gudaoxuri
 */
@Component
@Data
@ConfigurationProperties(prefix = "dew.baas.iam")
public class IAMConfig extends CommonConfig {

    // TODO config
    private boolean allowTenantRegister = false;

    private Security security = new Security();
    private App app = new App();

    /**
     * Security.
     */
    @Data
    public static class Security {

        private String systemAdminPositionCode = "SYSTEM_ADMIN";
        private String systemAdminPositionName = "系统管理员";
        private String tenantAdminPositionCode = "TENANT_ADMIN";
        private String tenantAdminPositionName = "租户管理员";
        private String defaultPositionCode = "DEFAULT_ROLE";
        private String defaultPositionName = "默认角色";

        private Integer skKindByVCodeExpireSec = 60 * 5;
        private Integer skKindByVCodeMaxErrorTimes = 5;
        private Integer appRequestDateOffsetMs = 5000;

    }

    /**
     * App.
     */
    @Data
    public static class App {
        private String authFieldName = "Authorization";
        private String initNodeCode = "10000";
    }

}
