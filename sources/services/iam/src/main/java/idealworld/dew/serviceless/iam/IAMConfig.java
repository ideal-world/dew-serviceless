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

package idealworld.dew.serviceless.iam;

import com.ecfront.dew.common.$;
import idealworld.dew.serviceless.common.CommonConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * IAM config.
 *
 * @author gudaoxuri
 */
@Component
@Data
@ConfigurationProperties(prefix = "dew.serviceless.iam")
public class IAMConfig extends CommonConfig {

    @Value("${spring.application.name:please-setting-this}")
    private String applicationName;

    @Override
    protected void doSync(Map<String, String> config) {
        if (config.containsKey(IAMConstant.CONFIG_TENANT_REGISTER_ALLOW)) {
            allowTenantRegister = Boolean.parseBoolean(config.get(IAMConstant.CONFIG_TENANT_REGISTER_ALLOW));
        }
        if (config.containsKey(IAMConstant.CONFIG_ACCOUNT_VCODE_ERROR_TIMES)) {
            security.accountVCodeMaxErrorTimes = Integer.parseInt(config.get(IAMConstant.CONFIG_ACCOUNT_VCODE_ERROR_TIMES));
        }
        if (config.containsKey(IAMConstant.CONFIG_ACCOUNT_VCODE_EXPIRE_SEC)) {
            security.accountVCodeExpireSec = Integer.parseInt(config.get(IAMConstant.CONFIG_ACCOUNT_VCODE_EXPIRE_SEC));
        }
        if (config.containsKey(IAMConstant.CONFIG_AUTH_POLICY_EXPIRE_CLEAN_INTERVAL_SEC)) {
            security.authPolicyExpireCleanIntervalSec = Long.parseLong(config.get(IAMConstant.CONFIG_AUTH_POLICY_EXPIRE_CLEAN_INTERVAL_SEC));
        }
        if (config.containsKey(IAMConstant.CONFIG_AUTH_POLICY_MAX_FETCH_COUNT)) {
            security.authPolicyMaxFetchCount = Integer.parseInt(config.get(IAMConstant.CONFIG_AUTH_POLICY_MAX_FETCH_COUNT));
        }
    }


    private Boolean allowTenantRegister = true;
    private String serviceUrl;

    private Security security = new Security();
    private App app = new App();

    @PostConstruct
    private void init() {
        serviceUrl = "http://" + applicationName;
    }


    /**
     * Security.
     */
    @Data
    public static class Security {

        private String systemAdminRoleDefCode = "SYSTEM_ADMIN";
        private String systemAdminRoleDefName = "系统管理员";
        private String tenantAdminRoleDefCode = "TENANT_ADMIN";
        private String tenantAdminRoleDefName = "租户管理员";
        private String appAdminRoleDefCode = "APP_ADMIN";
        private String appAdminRoleDefName = "应用管理员";

        private String defaultValidAKRuleNote = "用户名校验规则";
        private String defaultValidAKRule = "^[a-zA-Z\\d\\.]{3,20}$";
        private String defaultValidSKRuleNote = "密码校验规则，8-20位字母+数字";
        private String defaultValidSKRule = "^(?![0-9]+$)(?![a-zA-Z]+$)\\S{8,20}$";

        private Integer accountVCodeExpireSec = 60 * 5;
        private Integer accountVCodeMaxErrorTimes = 5;

        private Long authPolicyExpireCleanIntervalSec = 60 * 60 * 24L;
        private Integer authPolicyMaxFetchCount = 1000;

    }

    /**
     * App.
     */
    @Data
    public static class App {

        private String iamTenantName = "平台租户";
        private String iamAppName = "用户权限中心";
        private String iamAdminName = "dew";
        private String iamAdminPwd = $.field.createShortUUID() + "1A";
        private String initNodeCode = "10000";

    }

}
