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

package idealworld.dew.serviceless.iam.process.common;

import com.ecfront.dew.common.$;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.auth.AuthCacheProcessor;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.util.KeyHelper;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.auth.Resource;
import idealworld.dew.serviceless.iam.domain.auth.ResourceSubject;
import idealworld.dew.serviceless.iam.domain.ident.*;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.ACAppProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.ACRoleProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.ResourceResp;
import idealworld.dew.serviceless.iam.process.common.dto.account.*;
import idealworld.dew.serviceless.iam.process.common.dto.app.AppRegisterReq;
import idealworld.dew.serviceless.iam.process.common.dto.tenant.TenantRegisterReq;
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
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础服务.
 *
 * @author gudaoxuri
 */
@Slf4j
public class CommonProcessor extends EventBusProcessor {

    {
        // 注册租户
        addProcessor(OptActionKind.CREATE, "/common/tenant", eventBusContext ->
                registerTenant(eventBusContext.req.body(TenantRegisterReq.class), eventBusContext.context));
        // 注册应用
        addProcessor(OptActionKind.CREATE, "/common/app", eventBusContext ->
                registerApp(eventBusContext.req.body(AppRegisterReq.class),
                        eventBusContext.req.identOptInfo.getAccountId(),
                        eventBusContext.req.identOptInfo.getAccountCode(),
                        eventBusContext.req.identOptInfo.getAccountName(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 注册账号
        addProcessor(OptActionKind.CREATE, "/common/account", eventBusContext ->
                registerAccount(eventBusContext.req.body(AccountRegisterReq.class), eventBusContext.context));
        // 登录
        addProcessor(OptActionKind.CREATE, "/common/login", eventBusContext ->
                login(eventBusContext.req.body(AccountLoginReq.class), eventBusContext.context));
        // 获取登录信息
        addProcessor(OptActionKind.FETCH, "/common/login", eventBusContext ->
                fetchLoginInfo(eventBusContext.req.identOptInfo.getToken(), eventBusContext.context));
        // OAuth登录
        addProcessor(OptActionKind.CREATE, "/common/oauth/login", eventBusContext ->
                OAuthProcessor.login(eventBusContext.req.body(AccountOAuthLoginReq.class), eventBusContext.context));
        // 退出登录
        addProcessor(OptActionKind.DELETE, "/common/logout", eventBusContext ->
                logout(eventBusContext.req.identOptInfo, eventBusContext.context));
        // 修改账号
        addProcessor(OptActionKind.PATCH, "/common/account", eventBusContext ->
                changeInfo(eventBusContext.req.body(AccountChangeReq.class), eventBusContext.req.identOptInfo.getAccountCode(),
                        eventBusContext.context));
        // 修改账号认证
        addProcessor(OptActionKind.PATCH, "/common/account/ident", eventBusContext ->
                changeIdent(eventBusContext.req.body(AccountIdentChangeReq.class), eventBusContext.req.identOptInfo.getAccountCode(),
                        eventBusContext.req.identOptInfo.getAppId(), eventBusContext.context));
        // 注销账号
        addProcessor(OptActionKind.DELETE, "/common/account", eventBusContext ->
                unRegister(eventBusContext.req.identOptInfo.getAccountCode(), eventBusContext.context));
        // 获取资源
        addProcessor(OptActionKind.FETCH, "/common/resource", eventBusContext ->
                findResources(eventBusContext.req.params.get("kind"), eventBusContext.req.identOptInfo.getUnauthorizedAppId(),
                        eventBusContext.req.identOptInfo.getUnauthorizedTenantId(), eventBusContext.context));
    }

    public CommonProcessor(String moduleName) {
        super(moduleName);
    }

    public static Future<IdentOptInfo> registerTenant(TenantRegisterReq tenantRegisterReq, ProcessContext context) {
        var iamConfig = (IAMConfig) context.conf;
        if (!iamConfig.getAllowTenantRegister()) {
            context.helper.error(new ConflictException("当前设置不允许自助注册租户"));
        }
        var dto = new RegisterTenantDataDTO();
        // 初始化租户
        return context.sql.tx(context, () ->
                SCTenantProcessor.addTenant(TenantAddReq.builder()
                        .name(tenantRegisterReq.getTenantName())
                        .icon(tenantRegisterReq.getIcon())
                        .parameters(tenantRegisterReq.getParameters())
                        .allowAccountRegister(tenantRegisterReq.getAllowAccountRegister())
                        .build(), context)
                        // 初始化租户认证
                        .compose(tenantId -> {
                            dto.tenantId = tenantId;
                            return TCTenantProcessor.addTenantIdent(TenantIdentAddReq.builder()
                                    .kind(AccountIdentKind.USERNAME)
                                    .validAkRuleNote(iamConfig.getSecurity().getDefaultValidAkRuleNote())
                                    .validAkRule(iamConfig.getSecurity().getDefaultValidAkRule())
                                    .validSkRuleNote(iamConfig.getSecurity().getDefaultValidSkRuleNote())
                                    .validSkRule(iamConfig.getSecurity().getDefaultValidSkRule())
                                    .validTimeSec(DewConstant.OBJECT_UNDEFINED)
                                    .build(), dto.tenantId, context);
                        })
                        // 初始化租户凭证
                        .compose(resp ->
                                TCTenantProcessor.addTenantCert(TenantCertAddReq.builder()
                                        .category("")
                                        .version(1)
                                        .build(), dto.tenantId, context))
                        // 初始化应用
                        .compose(resp ->
                                TCAppProcessor.addApp(AppAddReq.builder()
                                        .name(tenantRegisterReq.getAppName())
                                        .build(), dto.tenantId, context, false))
                        .compose(appId ->
                                TCAppProcessor.getApp(appId, dto.tenantId, context))
                        // 初始化应用认证
                        .compose(app -> {
                            dto.appId = app.getId();
                            dto.appCode = app.getOpenId();
                            return ACAppProcessor.addAppIdent(AppIdentAddReq.builder()
                                    .note("")
                                    .build(), dto.appId, dto.tenantId, context);
                        })
                        // 初始化账号
                        .compose(resp ->
                                TCAccountProcessor.innerAddAccount(
                                        AccountAddReq.builder()
                                                .name(tenantRegisterReq.getTenantName() + " 管理员")
                                                .build(),
                                        dto.tenantId,
                                        context
                                ))
                        // 初始化账号认证
                        .compose(account -> {
                            dto.accountId = account.getId();
                            dto.accountCode = account.getOpenId();
                            return TCAccountProcessor.addAccountIdent(dto.accountId,
                                    AccountIdentAddReq.builder()
                                            .kind(AccountIdentKind.USERNAME)
                                            .ak(tenantRegisterReq.getAccountUserName())
                                            .sk(tenantRegisterReq.getAccountPassword())
                                            .build(), dto.tenantId, context);
                        })
                        // 初始化账号应用
                        .compose(resp ->
                                TCAccountProcessor.addAccountApp(dto.accountId, dto.appId, dto.tenantId, context))
                        // 初始化账号角色
                        .compose(resp ->
                                ACRoleProcessor.getTenantAdminRoleId(context)
                                        .compose(tenantAdminRoleId ->
                                                TCAccountProcessor.addAccountRole(dto.accountId, tenantAdminRoleId, dto.tenantId, context)))
                        .compose(resp ->
                                ACRoleProcessor.getAppAdminRoleId(context)
                                        .compose(appAdminRoleId ->
                                                TCAccountProcessor.addAccountRole(dto.accountId, appAdminRoleId, dto.tenantId, context)))
                        // 登录
                        .compose(resp ->
                                loginWithoutAuth(dto.accountId, tenantRegisterReq.getTenantName() + " 管理员", dto.accountCode,
                                        dto.appId, dto.appCode, dto.tenantId, context)));
    }

    public static Future<IdentOptInfo> registerApp(AppRegisterReq appRegisterReq, Long relAccountId, String relAccountCode, String relAccountName,
                                                   Long relTenantId, ProcessContext context) {
        var dto = new RegisterAppDataDTO();
        return context.sql.tx(context, () ->
                // 初始化应用
                TCAppProcessor.addApp(AppAddReq.builder()
                        .name(appRegisterReq.getAppName())
                        .build(), relTenantId, context, false))
                .compose(appId ->
                        TCAppProcessor.getApp(appId, relTenantId, context))
                // 初始化应用认证
                .compose(app -> {
                    dto.appId = app.getId();
                    dto.appCode = app.getOpenId();
                    return ACAppProcessor.addAppIdent(AppIdentAddReq.builder()
                            .note("")
                            .build(), dto.appId, relTenantId, context);
                })
                // 初始化账号应用
                .compose(resp ->
                        TCAccountProcessor.addAccountApp(relAccountId, dto.appId, relTenantId, context))
                // 初始化账号角色
                .compose(resp ->
                        ACRoleProcessor.getAppAdminRoleId(context)
                                .compose(appAdminRoleId ->
                                        TCAccountProcessor.addAccountRole(relAccountId, appAdminRoleId, relTenantId, context)))
                // 登录
                .compose(resp ->
                        loginWithoutAuth(relAccountId, relAccountName, relAccountCode, dto.appId, dto.appCode, relTenantId, context));
    }

    public static Future<IdentOptInfo> registerAccount(AccountRegisterReq accountRegisterReq, ProcessContext context) {
        var accountCode = "ac" + $.field.createUUID();
        return context.sql.tx(context, () ->
                context.sql.getOne(
                        "SELECT id  FROM %s WHERE open_id = ?",
                        App.class, accountRegisterReq.getRelAppCode())
                        .compose(appInfo -> {
                            var appId = appInfo.getLong("id");
                            return IAMBasicProcessor.getEnabledTenantIdByAppId(appId, true, context)
                                    .compose(tenantId ->
                                            context.helper.existToError(
                                                    context.sql.exists(
                                                            AccountIdent.class,
                                                            new HashMap<>() {
                                                                {
                                                                    put("ak", accountRegisterReq.getAk());
                                                                    put("kind", accountRegisterReq.getKind());
                                                                    put("rel_tenant_id", tenantId);
                                                                }
                                                            }),
                                                    () -> new ConflictException("账号凭证[" + accountRegisterReq.getAk() + "]已存在"))
                                                    .compose(resp ->
                                                            context.sql.save(Account.builder()
                                                                    .name(accountRegisterReq.getName())
                                                                    .avatar(accountRegisterReq.getAvatar())
                                                                    .parameters(accountRegisterReq.getParameters())
                                                                    .openId(accountCode)
                                                                    .parentId(DewConstant.OBJECT_UNDEFINED)
                                                                    .status(CommonStatus.ENABLED)
                                                                    .relTenantId(tenantId)
                                                                    .build()))
                                                    .compose(accountId ->
                                                            IAMBasicProcessor.validRuleAndGetValidEndTime(
                                                                    accountRegisterReq.getKind(),
                                                                    accountRegisterReq.getAk(),
                                                                    accountRegisterReq.getSk(),
                                                                    tenantId,
                                                                    context
                                                            )
                                                                    .compose(validRuleAndGetValidEndTime ->
                                                                            IAMBasicProcessor.processIdentSk(accountRegisterReq.getKind(),
                                                                                    accountRegisterReq.getAk(),
                                                                                    accountRegisterReq.getSk(),
                                                                                    appId,
                                                                                    tenantId,
                                                                                    context)
                                                                                    .compose(processIdentSk ->
                                                                                            context.sql.save(AccountIdent.builder()
                                                                                                    .kind(accountRegisterReq.getKind())
                                                                                                    .ak(accountRegisterReq.getAk())
                                                                                                    .sk(processIdentSk)
                                                                                                    .validStartTime(System.currentTimeMillis())
                                                                                                    .validEndTime(validRuleAndGetValidEndTime)
                                                                                                    .relAccountId(accountId)
                                                                                                    .relTenantId(tenantId)
                                                                                                    .build()))
                                                                                    .compose(resp ->
                                                                                            context.sql.save(AccountApp.builder()
                                                                                                    .relAccountId(accountId)
                                                                                                    .relAppId(appId)
                                                                                                    .build()))
                                                                                    .compose(resp ->
                                                                                            loginWithoutAuth(accountId,
                                                                                                    accountRegisterReq.getName(), accountCode,
                                                                                                    appId, accountRegisterReq.getRelAppCode(),
                                                                                                    tenantId,
                                                                                                    context)
                                                                                    )))
                                    );
                        }));
    }

    public static Future<IdentOptInfo> login(AccountLoginReq accountLoginReq, ProcessContext context) {
        return context.sql.getOne(
                "SELECT id  FROM %s WHERE open_id = ?",
                App.class, accountLoginReq.getRelAppCode())
                .compose(appInfo -> {
                    var appId = appInfo.getLong("id");
                    return IAMBasicProcessor.getEnabledTenantIdByAppId(appId, false, context)
                            .compose(tenantId -> {
                                log.info("login : [{}-{}] kind = {}, ak = {}", tenantId, appId, accountLoginReq.getKind(), accountLoginReq.getAk());
                                var now = System.currentTimeMillis();
                                return context.sql.getOne(
                                        "SELECT accident.sk, acc.id, acc.name, acc.open_id FROM %s AS accident" +
                                                " INNER JOIN %s AS accapp ON accapp.rel_account_id = accident.rel_account_id" +
                                                " INNER JOIN %s AS tenantident ON tenantident.rel_tenant_id = accident.rel_tenant_id" +
                                                " INNER JOIN %s AS acc ON acc.id = accident.rel_account_id" +
                                                " WHERE tenantident.kind = ? AND accident.kind = ? AND accident.ak = ?" +
                                                " AND accident.valid_start_time < ? AND accident.valid_end_time > ?" +
                                                " AND accapp.rel_app_id = ?  AND acc.rel_tenant_id = ? AND acc.status = ?",
                                        AccountIdent.class, AccountApp.class, TenantIdent.class, Account.class,
                                        accountLoginReq.getKind(), accountLoginReq.getKind(), accountLoginReq.getAk(), now, now, appId, tenantId, CommonStatus.ENABLED)
                                        .compose(accountInfo -> {
                                            if (accountInfo == null) {
                                                log.warn("Login Fail: [{}-{}] AK {} doesn't exist or has expired or account doesn't exist",
                                                        tenantId, appId, accountLoginReq.getAk());
                                                context.helper.error(new UnAuthorizedException("登录认证[" + accountLoginReq.getAk() +
                                                        "]不存在或已过期或是（应用关联）账号不存在"));
                                            }
                                            var identSk = accountInfo.getString("sk");
                                            var accountId = accountInfo.getLong("id");
                                            var accountName = accountInfo.getString("name");
                                            var accountCode = accountInfo.getString("open_id");
                                            return login(accountId, accountName, accountCode, accountLoginReq.getKind(), accountLoginReq.getAk(),
                                                    accountLoginReq.getSk(), identSk, appId, accountLoginReq.getRelAppCode(), tenantId, context);
                                        });
                            });
                });
    }

    public static Future<IdentOptInfo> login(Long accountId, String accountName, String accountCode, AccountIdentKind kind, String ak,
                                             String inputSk, String persistedSk, Long appId, String appCode, Long tenantId, ProcessContext context) {
        return validateSK(kind, ak, inputSk, persistedSk, appId, tenantId, context)
                .compose(resp -> {
                    log.info("Login Success:  [{}-{}] ak {}", tenantId, appId, ak);
                    return loginWithoutAuth(accountId, accountName, accountCode, appId, appCode, tenantId, context);
                });
    }

    public static Future<IdentOptInfo> loginWithoutAuth(Long accountId, String accountName, String accountCode, Long appId, String appCode,
                                                        Long tenantId, ProcessContext context) {
        String token = KeyHelper.generateToken();
        var optInfo = new IdentOptCacheInfo();
        optInfo.setAccountId(accountId);
        optInfo.setAccountName(accountName);
        optInfo.setAccountCode(accountCode);
        optInfo.setToken(token);
        optInfo.setAppCode(appCode);
        optInfo.setAppId(appId);
        optInfo.setTenantId(tenantId);
        return IAMBasicProcessor.findRoleInfo(accountId, context)
                .compose(roles -> {
                    optInfo.setRoleInfo(roles);
                    return context.helper.success();
                })
                .compose(resp -> IAMBasicProcessor.findGroupInfo(accountId, context))
                .compose(groups -> {
                    optInfo.setGroupInfo(groups);
                    return context.helper.success();
                })
                .compose(resp -> {
                    // TODO 有效时间
                    AuthCacheProcessor.setOptInfo(optInfo, IAMConstant.OBJECT_UNDEFINED, context);
                    return context.helper.success(IdentOptInfo.builder()
                            .accountName(optInfo.getAccountName())
                            .accountCode(optInfo.getAccountCode())
                            .appCode(optInfo.getAppCode())
                            .token(optInfo.getToken())
                            .roleInfo(optInfo.getRoleInfo())
                            .groupInfo(optInfo.getGroupInfo())
                            .build());
                });
    }

    public static Future<IdentOptInfo> fetchLoginInfo(String token, ProcessContext context) {
        if (token.isBlank()) {
            throw context.helper.error(new UnAuthorizedException("用户未登录"));
        }
        return AuthCacheProcessor.getOptInfo(token, context)
                .compose(info -> {
                    if (info.isPresent()) {
                        return context.helper.success(info.get());
                    }
                    throw context.helper.error(new UnAuthorizedException("Token无效"));
                });
    }

    public static Future<Void> logout(IdentOptInfo identOptInfo, ProcessContext context) {
        if ((identOptInfo.getAccountCode()).isBlank()) {
            throw context.helper.error(new UnAuthorizedException("用户未登录"));
        }
        log.info("Logout Account {} by token {}", identOptInfo.getAccountCode(), identOptInfo.getToken());
        return AuthCacheProcessor.removeOptInfo(identOptInfo.getToken(), context);
    }

    public static Future<Void> changeInfo(AccountChangeReq accountChangeReq, String relOpenId, ProcessContext context) {
        if (relOpenId.isBlank()) {
            throw context.helper.error(new UnAuthorizedException("用户未登录"));
        }
        return context.sql.update(
                context.helper.convert(accountChangeReq, Account.class),
                new HashMap<>() {
                    {
                        put("open_id", relOpenId);
                    }
                });
    }

    public static Future<Void> changeIdent(AccountIdentChangeReq accountIdentChangeReq, String relOpenId, Long relAppId, ProcessContext context) {
        if (relOpenId.isBlank()) {
            throw context.helper.error(new UnAuthorizedException("用户未登录"));
        }
        return IAMBasicProcessor.getEnabledTenantIdByOpenId(relOpenId, context)
                .compose(tenantId ->
                        context.helper.notExistToError(
                                context.sql.exists(
                                        "SELECT 1 FROM %s AS ident" +
                                                " INNER JOIN %s AS acc ON acc.id = ident.rel_account_id" +
                                                " WHERE ident.kind = ? AND ident.ak = ? AND acc.open_id = ?",
                                        AccountIdent.class, Account.class, accountIdentChangeReq.getKind(), accountIdentChangeReq.getAk(), relOpenId),
                                () -> new UnAuthorizedException("用户[" + relOpenId + "]对应的认证方式不存在"))
                                .compose(resp ->
                                        IAMBasicProcessor.validRuleAndGetValidEndTime(
                                                accountIdentChangeReq.getKind(),
                                                accountIdentChangeReq.getAk(),
                                                accountIdentChangeReq.getSk(),
                                                tenantId,
                                                context)
                                                .compose(validRuleAndGetValidEndTime ->
                                                        IAMBasicProcessor.processIdentSk(accountIdentChangeReq.getKind(),
                                                                accountIdentChangeReq.getAk(),
                                                                accountIdentChangeReq.getSk(),
                                                                relAppId,
                                                                tenantId,
                                                                context)
                                                                .compose(processIdentSk ->
                                                                        context.sql.update(
                                                                                AccountIdent.builder()
                                                                                        .sk(processIdentSk)
                                                                                        .build(),
                                                                                new HashMap<>() {
                                                                                    {
                                                                                        put("kind", accountIdentChangeReq.getKind());
                                                                                        put("ak", accountIdentChangeReq.getAk());
                                                                                        put("rel_tenant_id", tenantId);
                                                                                    }
                                                                                })))));
    }

    public static Future<Void> unRegister(String relOpenId, ProcessContext context) {
        if (relOpenId.isBlank()) {
            throw context.helper.error(new UnAuthorizedException("用户未登录"));
        }
        return context.sql.update(
                Account.builder()
                        .status(CommonStatus.DISABLED)
                        .build(),
                new HashMap<>() {
                    {
                        put("open_id", relOpenId);
                    }
                });
    }

    public static Future<List<ResourceResp>> findResources(String kind, Long relAppId, Long relTenantId, ProcessContext context) {
        //  TODO 根据 identOptCacheInfo 过滤
        return context.sql.list(
                "SELECT resource.* FROM %s AS resource" +
                        " INNER JOIN %s AS subject ON subject.id = resource.rel_resource_subject_id" +
                        " WHERE ((resource.rel_tenant_id = ? AND resource.rel_app_id = ?)" +
                        " OR (resource.expose_kind = ? AND resource.rel_tenant_id = ?)" +
                        " OR resource.expose_kind = ?)" +
                        " AND subject.kind = ?" +
                        " ORDER BY resource.sort ASC",
                Resource.class, ResourceSubject.class, relTenantId, relAppId, ExposeKind.TENANT, relTenantId, ExposeKind.GLOBAL, kind)
                .compose(resourceInfos ->
                        context.helper.success(resourceInfos.stream()
                                .map(resourceInfo -> ResourceResp.builder()
                                        .id(resourceInfo.getLong("id"))
                                        .name(resourceInfo.getString("name"))
                                        .pathAndQuery(URIHelper.getPathAndQuery(resourceInfo.getString("uri")))
                                        .name(resourceInfo.getString("name"))
                                        .icon(resourceInfo.getString("icon"))
                                        .action(resourceInfo.getString("action"))
                                        .sort(resourceInfo.getInteger("sort"))
                                        .resGroup(resourceInfo.getInteger("res_group") != 0)
                                        .parentId(resourceInfo.getLong("parent_id"))
                                        .relResourceSubjectId(resourceInfo.getLong("rel_resource_subject_id"))
                                        .exposeKind(ExposeKind.parse(resourceInfo.getString("expose_kind")))
                                        .relAppId(resourceInfo.getLong("rel_app_id"))
                                        .relTenantId(resourceInfo.getLong("rel_tenant_id"))
                                        .build())
                                .collect(Collectors.toList()))
                );
    }

    private static Future<Void> validateSK(AccountIdentKind identKind,
                                           String ak, String inputSk, String storageSk, Long relAppId, Long tenantId, ProcessContext context) {
        switch (identKind) {
            case EMAIL:
            case PHONE:
                return context.helper.notExistToError(
                        context.cache.get(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + tenantId + ":" + ak),
                        () -> new UnAuthorizedException("验证码不存在或已过期，请重新获取"))
                        .compose(tmpSk -> {
                            if (tmpSk.equalsIgnoreCase(inputSk)) {
                                context.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + tenantId + ":" + ak);
                                context.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_ERROR_TIMES + tenantId + ":" + ak);
                                return context.helper.success();
                            }
                            return context.cache.incrby(IAMConstant.CACHE_ACCOUNT_VCODE_ERROR_TIMES + tenantId + ":" + ak, 1)
                                    .compose(errorTimes -> {
                                        if (errorTimes >= ((IAMConfig) context.conf).getSecurity().getAccountVCodeMaxErrorTimes()) {
                                            context.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + tenantId + ":" + ak);
                                            context.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_ERROR_TIMES + tenantId + ":" + ak);
                                            // TODO 需要特殊标记表示验证码过期
                                            context.helper.error(new UnAuthorizedException("验证码不存在或已过期，请重新获取"));
                                        }
                                        throw context.helper.error(new UnAuthorizedException("验证码错误"));
                                    });
                        });
            case USERNAME:
                if (!$.security.digest.validate(ak + inputSk, storageSk, "SHA-512")) {
                    context.helper.error(new UnAuthorizedException("用户名或密码错误"));
                }
                return context.helper.success();
            case WECHAT_XCX:
                return context.cache.get(IAMConstant.CACHE_ACCESS_TOKEN + relAppId + ":" + identKind.toString())
                        .compose(accessToken -> {
                            if (accessToken == null) {
                                throw context.helper.error(new UnAuthorizedException("Access Token不存在"));
                            }
                            if (!accessToken.equalsIgnoreCase(inputSk)) {
                                context.helper.error(new UnAuthorizedException("Access Token错误"));
                            }
                            return context.helper.success();
                        });
            default:
                return context.helper.success();
        }
    }

    private static class RegisterTenantDataDTO extends RegisterAppDataDTO {

        public Long tenantId;
        public Long accountId;
        public String accountCode;

    }

    private static class RegisterAppDataDTO {

        public Long appId;
        public String appCode;

    }

}
