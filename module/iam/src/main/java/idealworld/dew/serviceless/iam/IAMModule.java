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

import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.DewModule;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectOperatorKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.domain.ident.Tenant;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.dto.GroupKind;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.*;
import idealworld.dew.serviceless.iam.process.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.group.GroupAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.group.GroupNodeAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.ResourceAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.ResourceSubjectAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.role.RoleAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.role.RoleDefAddReq;
import idealworld.dew.serviceless.iam.process.common.CommonProcessor;
import idealworld.dew.serviceless.iam.process.systemconsole.SCTenantProcessor;
import idealworld.dew.serviceless.iam.process.systemconsole.dto.TenantAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.TCAccountProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.TCAppProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.TCTenantProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.account.AccountAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.account.AccountIdentAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.TenantCertAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.TenantIdentAddReq;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * @author gudaoxuri
 */
@Slf4j
public class IAMModule extends DewModule<IAMConfig> {

    @Override
    protected void start(IAMConfig config, Promise<Void> startPromise) {
        new CommonProcessor();
        new SCTenantProcessor();
        new TCTenantProcessor();
        new TCAppProcessor();
        new TCAccountProcessor();
        new ACAppProcessor();
        new ACRoleProcessor();
        new ACGroupProcessor();
        new ACResourceProcessor();
        new ACAuthPolicyProcessor();
        var context = ProcessContext.builder()
                .conf(config)
                .moduleName(getModuleName())
                .build()
                .init();
        ReceiveProcessor.watch(getModuleName(), config)
                .compose(resp -> ExchangeProcessor.cacheAppIdents(context))
                .compose(resp -> context.fun.sql.count(
                        new HashMap<>(),
                        Tenant.class))
                .compose(tenantCount -> {
                    if (tenantCount != 0) {
                        return Future.succeededFuture();
                    }
                    return initData(config, context);
                })
                .onSuccess(startResult -> startPromise.complete())
                .onFailure(startPromise::fail);
    }

    private static class InitDataDTO {

        public Long systemRoleDefAdminId;
        public Long tenantRoleDefAdminId;
        public Long appRoleDefAdminId;
        public Long systemRoleAdminId;
        public Long tenantRoleAdminId;
        public Long appRoleAdminId;
        public Long groupId;
        public Long iamAccountId;
        public Long iamAPIResourceSubjectId;
        public Long iamMenuResourceSubjectId;
        public Long systemAPIResourceId;
        public Long tenantAPIResourceId;
        public Long appAPIResourceId;
        public Long appMenuResourceId;

    }

    private Future<Void> initData(IAMConfig iamConfig, ProcessContext context) {
        log.info("[Startup]Initializing IAM application");
        var tempIdentOptInfo = IdentOptCacheInfo.builder().build();
        var dto = new InitDataDTO();
        // 初始化租户
        return context.fun.sql.tx(context, () ->
                context.helper.invoke(
                        SCTenantProcessor.addTenant(),
                        TenantAddReq.builder()
                                .name(iamConfig.getApp().getIamTenantName())
                                .build())
                        // 初始化租户认证
                        .compose(tenantId -> {
                            tempIdentOptInfo.setTenantId(tenantId);
                            return context.helper.invoke(
                                    TCTenantProcessor.addTenantIdent(),
                                    TenantIdentAddReq.builder()
                                            .kind(AccountIdentKind.USERNAME)
                                            .validAkRuleNote(iamConfig.getSecurity().getDefaultValidAkRuleNote())
                                            .validAkRule(iamConfig.getSecurity().getDefaultValidAkRule())
                                            .validSkRuleNote(iamConfig.getSecurity().getDefaultValidSkRuleNote())
                                            .validSkRule(iamConfig.getSecurity().getDefaultValidSkRule())
                                            .validTimeSec(DewConstant.OBJECT_UNDEFINED)
                                            .build(),
                                    tempIdentOptInfo);
                        })
                        // 初始化租户凭证
                        .compose(resp ->
                                context.helper.invoke(
                                        TCTenantProcessor.addTenantCert(),
                                        TenantCertAddReq.builder()
                                                .category("")
                                                .version(1)
                                                .build(),
                                        tempIdentOptInfo))
                        // 初始化应用
                        .compose(resp ->
                                context.helper.invoke(
                                        TCAppProcessor.addApp(),
                                        AppAddReq.builder()
                                                .name(iamConfig.getApp().getIamAppName())
                                                .build(),
                                        tempIdentOptInfo))
                        // 初始化应用认证
                        .compose(appId -> {
                            tempIdentOptInfo.setAppId(appId);
                            return context.helper.invoke(
                                    ACAppProcessor.addAppIdent(),
                                    AppIdentAddReq.builder()
                                            .note("")
                                            .build(),
                                    tempIdentOptInfo);
                        })
                        // 初始化角色定义
                        .compose(resp ->
                                context.helper.invoke(
                                        ACRoleProcessor.addRoleDef(),
                                        RoleDefAddReq.builder()
                                                .code(iamConfig.getSecurity().getSystemAdminRoleDefCode())
                                                .name(iamConfig.getSecurity().getSystemAdminRoleDefName())
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(systemRoleAdminId -> {
                            dto.systemRoleAdminId = systemRoleAdminId;
                            return Future.succeededFuture();
                        })
                        .compose(resp ->
                                context.helper.invoke(
                                        ACRoleProcessor.addRoleDef(),
                                        RoleDefAddReq.builder()
                                                .code(iamConfig.getSecurity().getTenantAdminRoleDefCode())
                                                .name(iamConfig.getSecurity().getTenantAdminRoleDefName())
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(tenantRoleDefAdminId -> {
                            dto.tenantRoleDefAdminId = tenantRoleDefAdminId;
                            return Future.succeededFuture();
                        })
                        .compose(resp ->
                                context.helper.invoke(
                                        ACRoleProcessor.addRoleDef(),
                                        RoleDefAddReq.builder()
                                                .code(iamConfig.getSecurity().getAppAdminRoleDefCode())
                                                .name(iamConfig.getSecurity().getAppAdminRoleDefName())
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(appRoleDefAdminId -> {
                            dto.appRoleDefAdminId = appRoleDefAdminId;
                            return Future.succeededFuture();
                        })
                        // 初始化角色
                        .compose(resp ->
                                context.helper.invoke(
                                        ACRoleProcessor.addRole(),
                                        RoleAddReq.builder()
                                                .relRoleDefId(dto.systemRoleDefAdminId)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(systemRoleAdminId -> {
                            dto.systemRoleAdminId = systemRoleAdminId;
                            return Future.succeededFuture();
                        })
                        .compose(resp ->
                                context.helper.invoke(
                                        ACRoleProcessor.addRole(),
                                        RoleAddReq.builder()
                                                .relRoleDefId(dto.tenantRoleDefAdminId)
                                                .exposeKind(ExposeKind.GLOBAL)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(tenantRoleAdminId -> {
                            dto.tenantRoleAdminId = tenantRoleAdminId;
                            return Future.succeededFuture();
                        })
                        .compose(resp ->
                                context.helper.invoke(
                                        ACRoleProcessor.addRole(),
                                        RoleAddReq.builder()
                                                .relRoleDefId(dto.appRoleDefAdminId)
                                                .exposeKind(ExposeKind.GLOBAL)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(appRoleAdminId -> {
                            dto.appRoleAdminId = appRoleAdminId;
                            return Future.succeededFuture();
                        })
                        // 初始化群组
                        .compose(resp ->
                                context.helper.invoke(
                                        ACGroupProcessor.addGroup(),
                                        GroupAddReq.builder()
                                                .code("default")
                                                .kind(GroupKind.ADMINISTRATION)
                                                .name("默认")
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(groupId -> {
                            dto.groupId = groupId;
                            return Future.succeededFuture();
                        })
                        // 初始化群组节点
                        .compose(resp ->
                                context.helper.invoke(
                                        ACGroupProcessor.addGroupNode(),
                                        GroupNodeAddReq.builder()
                                                .name("默认节点")
                                                .build(),
                                        tempIdentOptInfo))
                        // 初始化账号
                        .compose(resp ->
                                context.helper.invoke(
                                        TCAccountProcessor.addAccount(),
                                        AccountAddReq.builder()
                                                .name(iamConfig.getApp().getIamAdminName())
                                                .build(),
                                        tempIdentOptInfo)
                                        .compose(iamAccountId -> {
                                            dto.iamAccountId = iamAccountId;
                                            return Future.succeededFuture();
                                        }))
                        // 初始化账号认证
                        .compose(resp ->
                                context.helper.invoke(
                                        TCAccountProcessor.addAccountIdent(),
                                        AccountIdentAddReq.builder()
                                                .kind(AccountIdentKind.USERNAME)
                                                .ak(iamConfig.getApp().getIamAdminName())
                                                .sk(iamConfig.getApp().getIamAdminPwd())
                                                .build(),
                                        new HashMap<>() {
                                            {
                                                put("accountId", dto.iamAccountId + "");
                                            }
                                        },
                                        tempIdentOptInfo))
                        //  初始化账号应用
                        .compose(resp ->
                                context.helper.invoke(
                                        TCAccountProcessor.addAccountApp(),
                                        new HashMap<>() {
                                            {
                                                put("accountId", dto.iamAccountId + "");
                                                put("appId", tempIdentOptInfo.getAppId() + "");
                                            }
                                        },
                                        tempIdentOptInfo))
                        //  初始化账号角色
                        .compose(resp ->
                                context.helper.invoke(
                                        TCAccountProcessor.addAccountRole(),
                                        new HashMap<>() {
                                            {
                                                put("accountId", dto.iamAccountId + "");
                                                put("roleId", dto.systemRoleAdminId + "");
                                            }
                                        },
                                        tempIdentOptInfo))
                        .compose(resp ->
                                context.helper.invoke(
                                        TCAccountProcessor.addAccountRole(),
                                        new HashMap<>() {
                                            {
                                                put("accountId", dto.iamAccountId + "");
                                                put("roleId", dto.tenantRoleAdminId + "");
                                            }
                                        },
                                        tempIdentOptInfo))
                        .compose(resp ->
                                context.helper.invoke(
                                        TCAccountProcessor.addAccountRole(),
                                        new HashMap<>() {
                                            {
                                                put("accountId", dto.iamAccountId + "");
                                                put("roleId", dto.appRoleAdminId + "");
                                            }
                                        },
                                        tempIdentOptInfo))
                        // 初始化资源主体
                        .compose(resp ->
                                context.helper.invoke(
                                        ACResourceProcessor.addResourceSubject(),
                                        ResourceSubjectAddReq.builder()
                                                .codePostfix(IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_POSTFIX)
                                                .name(iamConfig.getApp().getIamAppName() + " APIs")
                                                .uri("http://"+getModuleName())
                                                .kind(ResourceKind.HTTP)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(iamAPIResourceSubjectId -> {
                            dto.iamAPIResourceSubjectId = iamAPIResourceSubjectId;
                            return Future.succeededFuture();
                        })
                        .compose(resp ->
                                context.helper.invoke(
                                        ACResourceProcessor.addResourceSubject(),
                                        ResourceSubjectAddReq.builder()
                                                .codePostfix(IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_POSTFIX)
                                                .name(iamConfig.getApp().getIamAppName() + "Menus")
                                                .uri("menu://"+getModuleName())
                                                .kind(ResourceKind.MENU)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(iamMenuResourceSubjectId -> {
                            dto.iamMenuResourceSubjectId = iamMenuResourceSubjectId;
                            return Future.succeededFuture();
                        })
                        // 初始化资源
                        .compose(resp ->
                                context.helper.invoke(
                                        ACResourceProcessor.addResource(),
                                        ResourceAddReq.builder()
                                                .name("系统控制台资源")
                                                .pathAndQuery("/console/system/**")
                                                .relResourceSubjectId(dto.iamAPIResourceSubjectId)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(systemAPIResourceId -> {
                            dto.systemAPIResourceId = systemAPIResourceId;
                            return Future.succeededFuture();
                        })
                        .compose(resp ->
                                context.helper.invoke(
                                        ACResourceProcessor.addResource(),
                                        ResourceAddReq.builder()
                                                .name("租户控制台资源")
                                                .pathAndQuery("/console/tenant/**")
                                                .relResourceSubjectId(dto.iamAPIResourceSubjectId)
                                                .exposeKind(ExposeKind.GLOBAL)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(tenantAPIResourceId -> {
                            dto.tenantAPIResourceId = tenantAPIResourceId;
                            return Future.succeededFuture();
                        })
                        .compose(resp ->
                                context.helper.invoke(
                                        ACResourceProcessor.addResource(),
                                        ResourceAddReq.builder()
                                                .name("应用控制台资源")
                                                .pathAndQuery("/console/app/**")
                                                .relResourceSubjectId(dto.iamAPIResourceSubjectId)
                                                .exposeKind(ExposeKind.GLOBAL)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(appAPIResourceId -> {
                            dto.appAPIResourceId = appAPIResourceId;
                            return Future.succeededFuture();
                        })
                        .compose(resp ->
                                context.helper.invoke(
                                        ACResourceProcessor.addResource(),
                                        ResourceAddReq.builder()
                                                .name("应用控制台菜单")
                                                .pathAndQuery("/app")
                                                .relResourceSubjectId(dto.iamMenuResourceSubjectId)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(iamMenuResourceSubjectId -> {
                            dto.iamMenuResourceSubjectId = iamMenuResourceSubjectId;
                            return Future.succeededFuture();
                        })
                        // 初始化权限策略
                        .compose(resp ->
                                context.helper.invoke(
                                        ACAuthPolicyProcessor.addAuthPolicy(),
                                        AuthPolicyAddReq.builder()
                                                .relSubjectKind(AuthSubjectKind.ROLE)
                                                .relSubjectIds(dto.systemRoleAdminId + ",")
                                                .subjectOperator(AuthSubjectOperatorKind.EQ)
                                                .relResourceId(dto.systemAPIResourceId)
                                                .resultKind(AuthResultKind.ACCEPT)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(resp ->
                                context.helper.invoke(
                                        ACAuthPolicyProcessor.addAuthPolicy(),
                                        AuthPolicyAddReq.builder()
                                                .relSubjectKind(AuthSubjectKind.ROLE)
                                                .relSubjectIds(dto.tenantRoleAdminId + ",")
                                                .subjectOperator(AuthSubjectOperatorKind.EQ)
                                                .relResourceId(dto.tenantAPIResourceId)
                                                .resultKind(AuthResultKind.ACCEPT)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(resp ->
                                context.helper.invoke(
                                        ACAuthPolicyProcessor.addAuthPolicy(),
                                        AuthPolicyAddReq.builder()
                                                .relSubjectKind(AuthSubjectKind.ROLE)
                                                .relSubjectIds(dto.appRoleAdminId + ",")
                                                .subjectOperator(AuthSubjectOperatorKind.EQ)
                                                .relResourceId(dto.appAPIResourceId)
                                                .resultKind(AuthResultKind.ACCEPT)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(resp ->
                                context.helper.invoke(
                                        ACAuthPolicyProcessor.addAuthPolicy(),
                                        AuthPolicyAddReq.builder()
                                                .relSubjectKind(AuthSubjectKind.ROLE)
                                                .relSubjectIds(dto.appRoleAdminId + ",")
                                                .subjectOperator(AuthSubjectOperatorKind.EQ)
                                                .relResourceId(dto.appMenuResourceId)
                                                .resultKind(AuthResultKind.ACCEPT)
                                                .build(),
                                        tempIdentOptInfo))
                        .compose(resp -> {
                            log.info("[Startup]Initialization complete.\n" +
                                            "##################################\n" +
                                            " System administrator information:\n" +
                                            " appId = {}\n" +
                                            " username = {}\n" +
                                            " temporary password= {}\n" +
                                            " please change it in time.\n" +
                                            "##################################",
                                    tempIdentOptInfo.getAppId(), iamConfig.getApp().getIamAdminName(), iamConfig.getApp().getIamAdminPwd());
                            return Future.succeededFuture();
                        })
        );
    }

    @Override
    protected void stop(IAMConfig config, Promise<Void> stopPromise) {
        stopPromise.complete();
    }

    @Override
    protected boolean enabledRedisFun() {
        return true;
    }

    @Override
    protected boolean enabledHttpServerFun() {
        return false;
    }

    @Override
    protected boolean enabledHttpClientFun() {
        return true;
    }

    @Override
    protected boolean enabledJDBCFun() {
        return true;
    }

}
