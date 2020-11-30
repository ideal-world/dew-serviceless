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


import group.idealworld.dew.core.DewContext;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.IdentOptCacheInfo;
import idealworld.dew.serviceless.common.enumeration.AuthResultKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.domain.ident.QTenant;
import idealworld.dew.serviceless.iam.enumeration.AccountIdentKind;
import idealworld.dew.serviceless.iam.enumeration.ExposeKind;
import idealworld.dew.serviceless.iam.enumeration.GroupKind;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.group.GroupAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.group.GroupNodeAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.ResourceAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.ResourceSubjectAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.role.RoleAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.role.RoleDefAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.service.*;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import idealworld.dew.serviceless.iam.scene.systemconsole.dto.TenantAddReq;
import idealworld.dew.serviceless.iam.scene.systemconsole.service.SCTenantService;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.account.AccountAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.account.AccountIdentAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.tenant.TenantCertAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.tenant.TenantIdentAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.service.TCAccountService;
import idealworld.dew.serviceless.iam.scene.tenantconsole.service.TCAppService;
import idealworld.dew.serviceless.iam.scene.tenantconsole.service.TCTenantService;
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
    private ACGroupService acGroupService;
    @Autowired
    private TCAccountService tcAccountService;
    @Autowired
    private ACResourceService acResourceService;
    @Autowired
    private ACAuthPolicyService acAuthPolicyService;
    @Autowired
    private ExchangeProcessor exchangeProcessor;

    /**
     * Init.
     */
    @Transactional
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        StandardResp.setServiceFlag("IAM");
        DewContext.setOptInfoClazz(IdentOptCacheInfo.class);
        initIAMAppInfo();
        exchangeProcessor.cacheAppIdents();
    }

    private void initIAMAppInfo() {
        var qTenant = QTenant.tenant;
        if (sqlBuilder.select(qTenant.id)
                .from(qTenant)
                .fetchCount() != 0) {
            return;
        }
        log.info("[Startup]Initializing IAM application");
        // 初始化租户
        var tenantId = scTenantService.addTenant(TenantAddReq.builder()
                .name(iamConfig.getApp().getIamTenantName())
                .build()).getBody();
        // 初始化租户认证
        tcTenantService.addTenantIdent(TenantIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .validAKRuleNote(iamConfig.getSecurity().getDefaultValidAKRuleNote())
                .validAKRule(iamConfig.getSecurity().getDefaultValidAKRule())
                .validSKRuleNote(iamConfig.getSecurity().getDefaultValidSKRuleNote())
                .validSKRule(iamConfig.getSecurity().getDefaultValidSKRule())
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
                .build(), iamAppId, tenantId);
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
                .exposeKind(ExposeKind.GLOBAL)
                .build(), iamAppId, tenantId).getBody();
        var appRoleAdminId = acRoleService.addRole(RoleAddReq.builder()
                .relRoleDefId(appRoleDefAdminId)
                .exposeKind(ExposeKind.GLOBAL)
                .build(), iamAppId, tenantId).getBody();
        // 初始化群组
        var groupId = acGroupService.addGroup(GroupAddReq.builder()
                .code("default")
                .kind(GroupKind.ADMINISTRATION)
                .name("默认")
                .build(), iamAppId, tenantId).getBody();
        acGroupService.addGroupNode(GroupNodeAddReq.builder()
                .name("默认节点")
                .build(), groupId, iamAppId, tenantId);
        // 初始化账号
        var iamAccountId = tcAccountService.addAccount(AccountAddReq.builder()
                .name(iamConfig.getApp().getIamAdminName())
                .build(), tenantId).getBody();
        // 初始化账号认证
        tcAccountService.addAccountIdent(AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak(iamConfig.getApp().getIamAdminName())
                .sk(iamConfig.getApp().getIamAdminPwd())
                .build(), iamAccountId, tenantId);
        // 初始化账号应用
        tcAccountService.addAccountApp(iamAccountId, iamAppId, tenantId);
        // 初始化账号角色
        tcAccountService.addAccountRole(iamAccountId, systemRoleAdminId, tenantId);
        tcAccountService.addAccountRole(iamAccountId, tenantRoleAdminId, tenantId);
        tcAccountService.addAccountRole(iamAccountId, appRoleAdminId, tenantId);
        // 初始化资源主体
        var iamAPIResourceSubjectId = acResourceService.addResourceSubject(ResourceSubjectAddReq.builder()
                .code("iam")
                .name(iamConfig.getApp().getIamAppName())
                .uri(iamConfig.getServiceUrl())
                .kind(ResourceKind.HTTP)
                .build(), iamAppId, tenantId).getBody();
        // 初始化资源
        var systemResourceId = acResourceService.addResource(ResourceAddReq.builder()
                .name("系统控制台资源")
                .uri(iamConfig.getServiceUrl() + "/console/system/**")
                .relResourceSubjectId(iamAPIResourceSubjectId)
                .build(), iamAppId, tenantId).getBody();
        var tenantResourceId = acResourceService.addResource(ResourceAddReq.builder()
                .name("租户控制台资源")
                .uri(iamConfig.getServiceUrl() + "/console/tenant/**")
                .relResourceSubjectId(iamAPIResourceSubjectId)
                .exposeKind(ExposeKind.GLOBAL)
                .build(), iamAppId, tenantId).getBody();
        var appResourceId = acResourceService.addResource(ResourceAddReq.builder()
                .name("应用控制台资源")
                .uri(iamConfig.getServiceUrl() + "/console/app/**")
                .relResourceSubjectId(iamAPIResourceSubjectId)
                .exposeKind(ExposeKind.GLOBAL)
                .build(), iamAppId, tenantId).getBody();
        // 初始化权限策略
        acAuthPolicyService.addAuthPolicy(AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(systemRoleAdminId + ",")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(systemResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), iamAppId, tenantId);
        acAuthPolicyService.addAuthPolicy(AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(tenantRoleAdminId + ",")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(tenantResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), iamAppId, tenantId);
        acAuthPolicyService.addAuthPolicy(AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(appRoleAdminId + ",")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(appResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), iamAppId, tenantId);
        log.info("[Startup]Initialization complete.\n" +
                        "##################################\n" +
                        " System administrator information:\n" +
                        " appId = {}\n" +
                        " username = {}\n" +
                        " temporary password= {}\n" +
                        " please change it in time.\n" +
                        "##################################",
                iamAppId, iamConfig.getApp().getIamAdminName(), iamConfig.getApp().getIamAdminPwd());
    }

}
