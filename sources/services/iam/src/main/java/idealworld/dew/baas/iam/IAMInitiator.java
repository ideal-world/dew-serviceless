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


import com.ecfront.dew.common.$;
import group.idealworld.dew.core.DewContext;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.dto.IdentOptCacheInfo;
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.baas.common.enumeration.ResourceKind;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.ident.QTenant;
import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
import idealworld.dew.baas.iam.interceptor.InterceptService;
import idealworld.dew.baas.iam.scene.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.resource.ResourceAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.resource.ResourceSubjectAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.role.RoleAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.role.RoleDefAddReq;
import idealworld.dew.baas.iam.scene.appconsole.service.ACAppService;
import idealworld.dew.baas.iam.scene.appconsole.service.ACAuthPolicyService;
import idealworld.dew.baas.iam.scene.appconsole.service.ACResourceService;
import idealworld.dew.baas.iam.scene.appconsole.service.ACRoleService;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import idealworld.dew.baas.iam.scene.systemconsole.dto.TenantAddReq;
import idealworld.dew.baas.iam.scene.systemconsole.service.SCTenantService;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.account.AccountAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.account.AccountIdentAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.tenant.TenantCertAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.tenant.TenantIdentAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.service.TCAccountService;
import idealworld.dew.baas.iam.scene.tenantconsole.service.TCAppService;
import idealworld.dew.baas.iam.scene.tenantconsole.service.TCTenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ident initiator.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class IAMInitiator extends IAMBasicService implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private IAMConfig iamConfig;
    @Autowired
    private SCTenantService scTenantService;
    @Autowired
    private TCTenantService tcTenantService;
    @Autowired
    private TCAppService tcAppService;
    @Autowired
    private ACAppService acAppService;
    @Autowired
    private ACRoleService acRoleService;
    @Autowired
    private TCAccountService tcAccountService;
    @Autowired
    private ACResourceService acResourceService;
    @Autowired
    private ACAuthPolicyService acAuthPolicyService;
    @Autowired
    private InterceptService interceptService;

    /**
     * Init.
     */
    @Transactional
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        StandardResp.setServiceFlag("IAM");
        DewContext.setOptInfoClazz(IdentOptCacheInfo.class);
        initIAMAppInfo();
        interceptService.cacheTenantAndAppStatus();
        interceptService.cacheAppIdents();
    }

    private void initIAMAppInfo() {
        var qTenant = QTenant.tenant;
        if (sqlBuilder.select(qTenant.id)
                .from(qTenant)
                .fetchCount() != 0) {
            return;
        }
        log.info("Initializing IAM app");
        // 初始化租户
        var tenantId = scTenantService.addTenant(TenantAddReq.builder()
                .name(iamConfig.getApp().getIamTenantName())
                .build()).getBody();
        // 初始化租户认证
        tcTenantService.addTenantIdent(TenantIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .validTimeSec(Constant.OBJECT_UNDEFINED)
                .build(), tenantId);
        // 初始化租户凭证
        tcTenantService.addTenantCert(TenantCertAddReq.builder()
                .category("")
                .version(1)
                .build(), tenantId);
        // 初始化应用
        var iamAppId = tcAppService.addApp(AppAddReq.builder()
                .name(iamConfig.getApp().getIamAppName())
                .build(), tenantId).getBody();
        // 初始化应用认证
        acAppService.addAppIdent(AppIdentAddReq.builder()
                .note("")
                .build(), iamAppId);
        // 初始化角色定义
        var systemRoleDefAdminId = acRoleService.addRoleDef(RoleDefAddReq.builder()
                .code(iamConfig.getSecurity().getSystemAdminRoleDefCode())
                .name(iamConfig.getSecurity().getSystemAdminRoleDefName())
                .build(), iamAppId, tenantId).getBody();
        var tenantRoleDefAdminId = acRoleService.addRoleDef(RoleDefAddReq.builder()
                .code(iamConfig.getSecurity().getTenantAdminRoleDefCode())
                .name(iamConfig.getSecurity().getTenantAdminRoleDefName())
                .build(), iamAppId, tenantId).getBody();
        var appRoleDefAdminId = acRoleService.addRoleDef(RoleDefAddReq.builder()
                .code(iamConfig.getSecurity().getAppAdminRoleDefCode())
                .name(iamConfig.getSecurity().getAppAdminRoleDefName())
                .build(), iamAppId, tenantId).getBody();
        // 初始化角色
        var systemRoleAdminId = acRoleService.addRole(RoleAddReq.builder()
                .relRoleDefId(systemRoleDefAdminId)
                .build(), iamAppId, tenantId).getBody();
        var tenantRoleAdminId = acRoleService.addRole(RoleAddReq.builder()
                .relRoleDefId(tenantRoleDefAdminId)
                .build(), iamAppId, tenantId).getBody();
        var appRoleAdminId = acRoleService.addRole(RoleAddReq.builder()
                .relRoleDefId(appRoleDefAdminId)
                .build(), iamAppId, tenantId).getBody();
        // 初始化账号
        var iamAccountId = tcAccountService.addAccount(AccountAddReq.builder()
                .name(iamConfig.getApp().getIamAppName())
                .build(), tenantId).getBody();
        // 初始化账号认证
        var tmpPwd = $.field.createShortUUID();
        tcAccountService.addAccountIdent(AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak(iamConfig.getApp().getIamAppName())
                .sk(tmpPwd)
                .build(), iamAccountId, tenantId);
        // 初始化账号应用
        tcAccountService.addAccountApp(iamAccountId, iamAppId, tenantId);
        // 初始化账号角色
        tcAccountService.addAccountRole(iamAccountId, systemRoleAdminId, tenantId);
        tcAccountService.addAccountRole(iamAccountId, tenantRoleAdminId, tenantId);
        tcAccountService.addAccountRole(iamAccountId, appRoleAdminId, tenantId);
        // 初始化资源主体
        var iamAPIResourceSubjectId = acResourceService.addResourceSubject(ResourceSubjectAddReq.builder()
                .code("")
                .name("")
                .uri(iamConfig.getServiceUrl())
                .kind(ResourceKind.HTTP)
                .build(), iamAppId, tenantId).getBody();
        // 初始化资源
        var systemResourceId = acResourceService.addResource(ResourceAddReq.builder()
                .name("系统控制台资源")
                .uri("/system/**")
                .relResourceSubjectId(iamAPIResourceSubjectId)
                .build(), iamAppId, tenantId).getBody();
        var tenantResourceId = acResourceService.addResource(ResourceAddReq.builder()
                .name("租户控制台资源")
                .uri("/tenant/**")
                .relResourceSubjectId(iamAPIResourceSubjectId)
                .build(), iamAppId, tenantId).getBody();
        var appResourceId = acResourceService.addResource(ResourceAddReq.builder()
                .name("应用控制台资源")
                .uri("/app/**")
                .relResourceSubjectId(iamAPIResourceSubjectId)
                .build(), iamAppId, tenantId).getBody();
        // 初始化权限策略
        acAuthPolicyService.addAuthPolicy(AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(iamAccountId + ",")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(systemResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .relSubjectAppId(iamAppId)
                .relSubjectTenantId(tenantId)
                .build(), iamAppId, tenantId);
        acAuthPolicyService.addAuthPolicy(AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(iamAccountId + ",")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(tenantResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .relSubjectAppId(iamAppId)
                .relSubjectTenantId(tenantId)
                .build(), iamAppId, tenantId);
        acAuthPolicyService.addAuthPolicy(AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(iamAccountId + ",")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(appResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .relSubjectAppId(iamAppId)
                .relSubjectTenantId(tenantId)
                .build(), iamAppId, tenantId);
        log.info("Initialized IAM app. \r\n >> appId = {} \r\n >> username = {} \r\n >> temporary password = {}", iamAppId, iamConfig.getApp().getIamAdminName(), tmpPwd);
    }

}
