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

package idealworld.dew.serviceless.iam;

import com.ecfront.dew.common.$;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Gateway config.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IAMConfig {

    @Builder.Default
    private Boolean allowTenantRegister = true;
    @Builder.Default
    private Security security = new Security();
    @Builder.Default
    private App app = new App();

    /**
     * Security.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Security {

        @Builder.Default
        private String systemAdminRoleDefCode = "SYSTEM_ADMIN";
        @Builder.Default
        private String systemAdminRoleDefName = "系统管理员";
        @Builder.Default
        private String tenantAdminRoleDefCode = "TENANT_ADMIN";
        @Builder.Default
        private String tenantAdminRoleDefName = "租户管理员";
        @Builder.Default
        private String appAdminRoleDefCode = "APP_ADMIN";
        @Builder.Default
        private String appAdminRoleDefName = "应用管理员";
        @Builder.Default
        private String defaultValidAkRuleNote = "用户名校验规则";
        @Builder.Default
        private String defaultValidAkRule = "^[a-zA-Z\\d\\.]{3,20}$";
        @Builder.Default
        private String defaultValidSkRuleNote = "密码校验规则，8-20位字母+数字";
        @Builder.Default
        private String defaultValidSkRule = "^(?![0-9]+$)(?![a-zA-Z]+$)\\S{8,20}$";
        @Builder.Default
        private Integer accountVCodeExpireSec = 60 * 5;
        @Builder.Default
        private Integer accountVCodeMaxErrorTimes = 5;
        @Builder.Default
        private Long authPolicyExpireCleanIntervalSec = 60 * 60 * 24L;
        @Builder.Default
        private Integer authPolicyMaxFetchCount = 1000;

    }

    /**
     * App.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class App {

        @Builder.Default
        private String iamTenantName = "平台租户";
        @Builder.Default
        private String iamAppName = "用户权限中心";
        @Builder.Default
        private String iamAdminName = "dew";
        @Builder.Default
        private String iamAdminPwd = "D_0_" + $.field.createShortUUID();
        @Builder.Default
        private String initNodeCode = "10000";

    }

}
