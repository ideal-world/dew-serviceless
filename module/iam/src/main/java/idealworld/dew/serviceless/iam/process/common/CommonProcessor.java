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

package idealworld.dew.serviceless.iam.process.common;

import com.ecfront.dew.common.$;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.auth.AuthCacheProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.framework.util.KeyHelper;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.ident.Account;
import idealworld.dew.serviceless.iam.domain.ident.AccountApp;
import idealworld.dew.serviceless.iam.domain.ident.AccountIdent;
import idealworld.dew.serviceless.iam.domain.ident.TenantIdent;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.ACAppProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.ACRoleProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.serviceless.iam.process.common.dto.account.AccountChangeReq;
import idealworld.dew.serviceless.iam.process.common.dto.account.AccountIdentChangeReq;
import idealworld.dew.serviceless.iam.process.common.dto.account.AccountLoginReq;
import idealworld.dew.serviceless.iam.process.common.dto.account.AccountRegisterReq;
import idealworld.dew.serviceless.iam.process.common.dto.tenant.TenantRegisterReq;
import idealworld.dew.serviceless.iam.process.common.service.OAuthProcessor;
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

import java.util.Date;
import java.util.HashMap;

/**
 * 公共函数服务.
 *
 * @author gudaoxuri
 */
@Slf4j
public class CommonProcessor {

    static {
        // 注册租户
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/common/tenant", registerTenant());
        // 注册账号
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/common/account", registerAccount());
        // 登录
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/common/login", login());
        // OAuth登录
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/common/oauth/login", OAuthProcessor.login());
        // 退出登录
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/common/logout", logout());
        // 修改账号
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/common/account", changeInfo());
        // 修改账号认证
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/common/account/ident", changeIdent());
        // 注销账号
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/common/account", unRegister());
    }

    public static ProcessFun<IdentOptInfo> registerTenant() {
        return context -> {
            var iamConfig = (IAMConfig) context.conf;
            if (!iamConfig.getAllowTenantRegister()) {
                context.helper.error(new ConflictException("当前设置不允许自助注册租户"));
            }
            var tenantRegisterReq = context.req.body(TenantRegisterReq.class);
            var tempIdentOptInfo = IdentOptCacheInfo.builder().build();
            // 初始化租户
            // TODO 事务处理
            return context.fun.sql.tx(client ->
                    context.helper.invoke(
                            SCTenantProcessor.addTenant(),
                            TenantAddReq.builder()
                                    .name(tenantRegisterReq.getTenantName())
                                    .icon(tenantRegisterReq.getIcon())
                                    .parameters(tenantRegisterReq.getParameters())
                                    .allowAccountRegister(tenantRegisterReq.getAllowAccountRegister())
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
                                                    .name(tenantRegisterReq.getAppName())
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
                            // 初始化账号
                            .compose(resp ->
                                    TCAccountProcessor.innerAddAccount(
                                            AccountAddReq.builder()
                                                    .name(tenantRegisterReq.getTenantName() + " 管理员")
                                                    .build(),
                                            tempIdentOptInfo.getTenantId(),
                                            context
                                    ))
                            // 初始化账号认证
                            .compose(account ->
                                    context.helper.invoke(
                                            TCAccountProcessor.addAccountIdent(),
                                            AccountIdentAddReq.builder()
                                                    .kind(AccountIdentKind.USERNAME)
                                                    .ak(tenantRegisterReq.getAccountUserName())
                                                    .sk(tenantRegisterReq.getAccountPassword())
                                                    .build(),
                                            new HashMap<>() {
                                                {
                                                    put("accountId", account.getId() + "");
                                                }
                                            },
                                            tempIdentOptInfo)
                                            //  初始化账号应用
                                            .compose(resp ->
                                                    context.helper.invoke(
                                                            TCAccountProcessor.addAccountApp(),
                                                            new HashMap<>() {
                                                                {
                                                                    put("accountId", account.getId() + "");
                                                                    put("appId", tempIdentOptInfo.getAppId() + "");
                                                                }
                                                            },
                                                            tempIdentOptInfo))
                                            //  初始化账号角色
                                            .compose(resp ->
                                                    ACRoleProcessor.getTenantAdminRoleId(context)
                                                            .compose(tenantAdminRoleId -> context.helper.invoke(
                                                                    TCAccountProcessor.addAccountRole(),
                                                                    new HashMap<>() {
                                                                        {
                                                                            put("accountId", account.getId() + "");
                                                                            put("roleId", tenantAdminRoleId + "");
                                                                        }
                                                                    },
                                                                    tempIdentOptInfo)))
                                            .compose(resp ->
                                                    ACRoleProcessor.getAppAdminRoleId(context)
                                                            .compose(appAdminRoleId -> context.helper.invoke(
                                                                    TCAccountProcessor.addAccountRole(),
                                                                    new HashMap<>() {
                                                                        {
                                                                            put("accountId", account.getId() + "");
                                                                            put("roleId", appAdminRoleId + "");
                                                                        }
                                                                    },
                                                                    tempIdentOptInfo)))
                                            // 登录
                                            .compose(resp ->
                                                    loginWithoutAuth(
                                                            account.getId(), account.getOpenId(), tenantRegisterReq.getAccountUserName(),
                                                            tempIdentOptInfo.getAppId(), tempIdentOptInfo.getTenantId(),
                                                            context)
                                            )));

        };
    }

    public static ProcessFun<IdentOptInfo> registerAccount() {
        return context -> {
            var accountRegisterReq = context.req.body(AccountRegisterReq.class);
            var openId = $.field.createUUID();
            return context.fun.sql.tx(client ->
                    IAMBasicProcessor.getEnabledTenantIdByAppId(accountRegisterReq.getRelAppId(), true, context)
                            .compose(tenantId ->
                                    context.helper.existToError(
                                            client.exists(
                                                    new HashMap<>() {
                                                        {
                                                            put("ak", accountRegisterReq.getAk());
                                                            put("kind", accountRegisterReq.getKind());
                                                            put("rel_tenant_id", tenantId);
                                                        }
                                                    },
                                                    AccountIdent.class
                                            ), () -> new ConflictException("账号凭证[" + accountRegisterReq.getAk() + "]已存在"))
                                            .compose(resp ->
                                                    client.save(Account.builder()
                                                            .name(accountRegisterReq.getName())
                                                            .avatar(accountRegisterReq.getAvatar())
                                                            .parameters(accountRegisterReq.getParameters())
                                                            .openId(openId)
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
                                                                            accountRegisterReq.getRelAppId(),
                                                                            tenantId,
                                                                            context)
                                                                            .compose(processIdentSk ->
                                                                                    client.save(AccountIdent.builder()
                                                                                            .kind(accountRegisterReq.getKind())
                                                                                            .ak(accountRegisterReq.getAk())
                                                                                            .sk(processIdentSk)
                                                                                            .validStartTime(new Date())
                                                                                            .validEndTime(validRuleAndGetValidEndTime)
                                                                                            .relAccountId(accountId)
                                                                                            .relTenantId(tenantId)
                                                                                            .build()))
                                                                            .compose(resp ->
                                                                                    client.save(AccountApp.builder()
                                                                                            .relAccountId(tenantId)
                                                                                            .relAppId(accountRegisterReq.getRelAppId())
                                                                                            .build()))
                                                                            .compose(resp ->
                                                                                    loginWithoutAuth(accountId, openId, accountRegisterReq.getAk(),
                                                                                            accountRegisterReq.getRelAppId(), tenantId,
                                                                                            context)
                                                                            )))
                            ));
        };
    }

    public static ProcessFun<IdentOptInfo> login() {
        return context -> {
            var accountLoginReq = context.req.body(AccountLoginReq.class);
            return IAMBasicProcessor.getEnabledTenantIdByAppId(accountLoginReq.getRelAppId(), false, context)
                    .compose(tenantId -> {
                        log.info("login : [{}-{}] kind = {}, ak = {}", tenantId, accountLoginReq.getRelAppId(), accountLoginReq.getKind(), accountLoginReq.getAk());
                        var now = new Date();
                        return context.fun.sql.getOne(
                                String.format("SELECT accident.sk, acc.id, acc.openId FROM %s AS accident" +
                                                "  INNER JOIN %s AS accapp ON accapp.rel_account_id = accident.rel_account_id" +
                                                "  INNER JOIN %s AS tenantident ON tenantident.rel_tenant_id = accident.rel_tenant_id" +
                                                "  INNER JOIN %s AS acc ON acc.id = accident.rel_account_id" +
                                                "  WHERE tenantident.kind = #{kind} AND accident.kind = #{kind} AND accident.ak = #{ak}" +
                                                "    AND accident.valid_start_time < #{now} AND accident.valid_end_time > #{now}" +
                                                "    AND acc.rel_tenant_id = #{rel_tenant_id} AND acc.status = #{status}",
                                        new AccountIdent().tableName(), new AccountApp().tableName(), new TenantIdent().tableName(), new Account().tableName()),
                                new HashMap<>() {
                                    {
                                        put("kind", accountLoginReq.getKind());
                                        put("ak", accountLoginReq.getAk());
                                        put("now", now);
                                        put("rel_tenant_id", tenantId);
                                        put("status", CommonStatus.ENABLED);
                                    }
                                })
                                .compose(accountInfo -> {
                                    if (accountInfo == null) {
                                        log.warn("Login Fail: [{}-{}] AK {} doesn't exist or has expired or account doesn't exist", tenantId, accountLoginReq.getRelAppId(), accountLoginReq.getAk());
                                        context.helper.error(new BadRequestException("登录认证[" + accountLoginReq.getAk() + "]不存在或已过期或是（应用关联）账号不存在"));
                                    }
                                    var identSk = accountInfo.getString("accident.sk");
                                    var accountId = accountInfo.getLong("acc.id");
                                    var openId = accountInfo.getString("acc.openId");
                                    return login(accountId, openId, accountLoginReq.getKind(), accountLoginReq.getAk(), accountLoginReq.getSk(), identSk, accountLoginReq.getRelAppId(), tenantId, context);
                                });
                    });
        };
    }

    public static Future<IdentOptInfo> login(Long accountId, String openId, AccountIdentKind kind, String ak, String inputSk, String persistedSk, Long appId, Long tenantId, ProcessContext context) {
        return validateSK(kind, ak, inputSk, persistedSk, appId, tenantId, context)
                .compose(resp -> loginWithoutAuth(accountId, openId, ak, appId, tenantId, context));
    }

    public static Future<IdentOptInfo> loginWithoutAuth(Long accountId, String openId, String ak, Long appId, Long tenantId, ProcessContext context) {
        log.info("Login Success:  [{}-{}] ak {}", tenantId, appId, ak);
        String token = KeyHelper.generateToken();
        var optInfo = new IdentOptCacheInfo();
        optInfo.setAccountCode(openId);
        optInfo.setToken(token);
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
                    context.req.identOptInfo = optInfo;
                    return context.helper.success(IdentOptInfo.builder()
                            .accountCode(optInfo.getAccountCode())
                            .token(optInfo.getToken())
                            .roleInfo(optInfo.getRoleInfo())
                            .groupInfo(optInfo.getGroupInfo())
                            .build());
                });
    }

    public static ProcessFun<Void> logout() {
        return context -> {
            log.info("Logout Account {} by token {}", context.req.identOptInfo.getAccountCode(), context.req.identOptInfo.getToken());
            return AuthCacheProcessor.removeOptInfo(context.req.identOptInfo.getToken(), context);
        };
    }

    public static ProcessFun<Void> changeInfo() {
        return context -> {
            var accountChangeReq = context.req.body(AccountChangeReq.class);
            return context.fun.sql.update(
                    new HashMap<>() {
                        {
                            put("open_id", context.req.identOptInfo.getAccountCode());
                        }
                    },
                    context.helper.convert(accountChangeReq, Account.class)
            );
        };
    }

    public static ProcessFun<Void> changeIdent() {
        return context ->
                IAMBasicProcessor.getEnabledTenantIdByOpenId((String) context.req.identOptInfo.getAccountCode(), context)
                        .compose(tenantId -> {
                            var accountIdentChangeReq = context.req.body(AccountIdentChangeReq.class);
                            return context.helper.notExistToError(
                                    context.fun.sql.exists(
                                            String.format("SELECT 1 FROM %s AS ident" +
                                                            "  INNER JOIN %s AS acc ON acc.id  = ident.rel_account_id" +
                                                            "  WHERE ident.kind = #{kind} AND ident.ak = #{ak} AND acc.open_id = #{open_id}",
                                                    new AccountIdent().tableName(), new Account().tableName()),
                                            new HashMap<>() {
                                                {
                                                    put("kind", accountIdentChangeReq.getKind());
                                                    put("ak", accountIdentChangeReq.getAk());
                                                    put("open_id", context.req.identOptInfo.getAccountCode());
                                                }
                                            }), () -> new UnAuthorizedException("用户[" + context.req.identOptInfo.getAccountCode() + "]对应的认证方式不存在"))
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
                                                                    context.req.identOptInfo.getAppId(),
                                                                    tenantId,
                                                                    context)
                                                                    .compose(processIdentSk ->
                                                                            context.fun.sql.update(
                                                                                    new HashMap<>() {
                                                                                        {
                                                                                            put("kind", accountIdentChangeReq.getKind());
                                                                                            put("ak", accountIdentChangeReq.getAk());
                                                                                            put("rel_tenant_id", tenantId);
                                                                                        }
                                                                                    },
                                                                                    AccountIdent.builder()
                                                                                            .sk(processIdentSk)
                                                                                            .build()))));
                        });
    }

    public static ProcessFun<Void> unRegister() {
        return context ->
                context.fun.sql.update(
                        new HashMap<>() {
                            {
                                put("open_id", context.req.identOptInfo.getAccountCode());
                            }
                        },
                        Account.builder()
                                .status(CommonStatus.DISABLED)
                                .build());
    }


    private static Future<Void> validateSK(AccountIdentKind identKind,
                                           String ak, String inputSk, String storageSk, Long relAppId, Long tenantId, ProcessContext context) {
        switch (identKind) {
            case EMAIL:
            case PHONE:
                return context.helper.notExistToError(
                        context.fun.cache.get(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + tenantId + ":" + ak),
                        () -> new BadRequestException("验证码不存在或已过期，请重新获取"))
                        .compose(tmpSk -> {
                            if (tmpSk.equalsIgnoreCase(inputSk)) {
                                context.fun.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + tenantId + ":" + ak);
                                context.fun.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_ERROR_TIMES + tenantId + ":" + ak);
                                return context.helper.success();
                            }
                            return context.fun.cache.incrby(IAMConstant.CACHE_ACCOUNT_VCODE_ERROR_TIMES + tenantId + ":" + ak, 1)
                                    .compose(errorTimes -> {
                                        if (errorTimes >= ((IAMConfig) context.conf).getSecurity().getAccountVCodeMaxErrorTimes()) {
                                            context.fun.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + tenantId + ":" + ak);
                                            context.fun.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_ERROR_TIMES + tenantId + ":" + ak);
                                            // TODO 需要特殊标记表示验证码过期
                                            return context.helper.error(new BadRequestException("验证码不存在或已过期，请重新获取"));
                                        }
                                        return context.helper.error(new BadRequestException("验证码错误"));
                                    });
                        });
            case USERNAME:
                if (!$.security.digest.validate(ak + inputSk, storageSk, "SHA-512")) {
                    return context.helper.error(new BadRequestException("密码错误"));
                }
                return context.helper.success();
            case WECHAT_XCX:
                return context.fun.cache.get(IAMConstant.CACHE_ACCESS_TOKEN + relAppId + ":" + identKind.toString())
                        .compose(accessToken -> {
                            if (accessToken == null) {
                                return context.helper.error(new BadRequestException("Access Token不存在"));
                            }
                            if (!accessToken.equalsIgnoreCase(inputSk)) {
                                return context.helper.error(new BadRequestException("Access Token错误"));
                            }
                            return context.helper.success();
                        });
            default:
                return context.helper.success();
        }
    }

}
