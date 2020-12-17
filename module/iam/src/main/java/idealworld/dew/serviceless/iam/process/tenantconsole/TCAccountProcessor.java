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

package idealworld.dew.serviceless.iam.process.tenantconsole;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.domain.auth.AccountGroup;
import idealworld.dew.serviceless.iam.domain.auth.AccountRole;
import idealworld.dew.serviceless.iam.domain.ident.Account;
import idealworld.dew.serviceless.iam.domain.ident.AccountApp;
import idealworld.dew.serviceless.iam.domain.ident.AccountBind;
import idealworld.dew.serviceless.iam.domain.ident.AccountIdent;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.account.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * 租户控制台下的账号控制器.
 *
 * @author gudaoxuri
 */
public class TCAccountProcessor {

    static {
        // 添加当前租户的账号
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/account", addAccount());
        // 修改当前租户的某个账号
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/account/{accountId}", modifyAccount());
        // 获取当前租户的某个账号信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/account/{accountId}", getAccount());
        // 获取当前租户的账号列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/account", pageAccounts());
        // 删除当前租户的某个账号、关联的账号认证、账号群组、账号角色、账号应用、账号绑定
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}", deleteAccount());

        // 添加当前租户某个账号的认证
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/account/{accountId}/ident", addAccountIdent());
        // 修改当前租户某个账号的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/account/{accountId}/ident/{accountIdentId}", modifyAccountIdent());
        // 获取当前租户某个账号的认证列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/account/{accountId}/ident", findAccountIdents());
        // 删除当前租户某个账号的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}/ident/{accountIdentId}", deleteAccountIdent());

        // 添加当前租户某个账号的关联应用
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/account/{accountId}/app/{appId}", addAccountApp());
        // 删除当前租户某个账号的某个关联应用
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}/app/{accountAppId}", deleteAccountApp());

        // 添加当前租户某个账号的关联群组
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/account/{accountId}/group/{groupNodeId}", addAccountGroup());
        // 删除当前租户某个账号的某个关联群组
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}/group/{accountGroupId}", deleteAccountGroup());

        // 添加当前租户某个账号的关联角色
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/account/{accountId}/role/{roleId}", addAccountRole());
        // 删除当前租户某个账号的某个关联角色
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}/role/{accountRoleId}", deleteAccountRole());
    }

    public static Future<Account> innerAddAccount(AccountAddReq accountAddReq, Long relTenantId, ProcessContext context) {
        var account = context.helper.convert(accountAddReq, Account.class);
        account.setOpenId($.field.createUUID());
        account.setParentId(DewConstant.OBJECT_UNDEFINED);
        account.setRelTenantId(relTenantId);
        account.setStatus(CommonStatus.ENABLED);
        return context.fun.sql.save(account)
                .compose(accountId -> {
                    account.setId(accountId);
                    return context.helper.success(account);
                });
    }

    public static ProcessFun<Long> addAccount() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountAddReq = context.req.body(AccountAddReq.class);
            return innerAddAccount(accountAddReq, relTenantId, context)
                    .compose(account -> context.helper.success(account.getId()));
        };
    }

    public static ProcessFun<Void> modifyAccount() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountId = Long.parseLong(context.req.params.get("accountId"));
            var accountModifyReq = context.req.body(AccountModifyReq.class);
            var future = context.helper.success();
            if (accountModifyReq.getParentId() != null) {
                future.compose(resp ->
                        IAMBasicProcessor.checkAccountMembership(accountModifyReq.getParentId(), relTenantId, context));
            }
            return future
                    .compose(resp ->
                            context.fun.sql.update(
                                    new HashMap<>() {
                                        {
                                            put("id", accountId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    context.helper.convert(accountModifyReq, Account.class)));
        };
    }

    public static ProcessFun<AccountResp> getAccount() {
        return context ->
                context.fun.sql.getOne(
                        new HashMap<>() {
                            {
                                put("id", context.req.params.get("accountId"));
                                put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                            }
                        },
                        Account.class)
                        .compose(app -> context.helper.success(app, AccountResp.class));
    }

    public static ProcessFun<Page<AccountResp>> pageAccounts() {
        return context -> {
            var name = context.req.params.getOrDefault("name", null);
            var openId = context.req.params.getOrDefault("openId", null);
            var whereParameters = new HashMap<String, Object>() {
                {
                    put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                }
            };
            if (name != null && !name.isBlank()) {
                whereParameters.put("%name", "%" + name + "%");
            }
            if (openId != null && !openId.isBlank()) {
                whereParameters.put("%open_id", "%" + openId + "%");
            }
            return context.fun.sql.page(
                    whereParameters,
                    context.req.pageNumber(),
                    context.req.pageSize(),
                    Account.class)
                    .compose(accounts -> context.helper.success(accounts, AccountResp.class));
        };
    }

    public static ProcessFun<Void> deleteAccount() {
        return context -> {
            var accountId = Long.parseLong(context.req.params.get("accountId"));
            return context.fun.sql.tx(client ->
                    client.softDelete(
                            new HashMap<>() {
                                {
                                    put("id", accountId);
                                    put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                                }
                            },
                            Account.class)
                            .compose(deleteNumber ->
                                    CompositeFuture.all(
                                            client.softDelete(
                                                    new HashMap<>() {
                                                        {
                                                            put("rel_account_id", accountId);
                                                        }
                                                    },
                                                    AccountIdent.class),
                                            client.softDelete(
                                                    new HashMap<>() {
                                                        {
                                                            put("rel_account_id", accountId);
                                                        }
                                                    },
                                                    AccountRole.class),
                                            client.softDelete(
                                                    new HashMap<>() {
                                                        {
                                                            put("rel_account_id", accountId);
                                                        }
                                                    },
                                                    AccountApp.class),
                                            client.softDelete(
                                                    new HashMap<>() {
                                                        {
                                                            put("rel_account_id", accountId);
                                                        }
                                                    },
                                                    AccountGroup.class),
                                            client.softDelete(
                                                    new HashMap<>() {
                                                        {
                                                            put("rel_account_id", accountId);
                                                        }
                                                    },
                                                    AccountBind.class))
                                            .compose(resp -> context.helper.success())));
        };
    }

    // --------------------------------------------------------------------

    public static ProcessFun<Long> addAccountIdent() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountId = Long.parseLong(context.req.params.get("accountId"));
            var accountIdentAddReq = context.req.body(AccountIdentAddReq.class);
            return context.helper.notExistToError(
                    context.fun.sql.exists(
                            new HashMap<>() {
                                {
                                    put("id", accountId);
                                    put("rel_tenant_id", relTenantId);
                                }
                            },
                            Account.class), () -> new UnAuthorizedException("关联账号不合法"))
                    .compose(resp ->
                            context.helper.existToError(context.fun.sql.exists(
                                    new HashMap<>() {
                                        {
                                            put("kind", accountIdentAddReq.getKind());
                                            put("ak", accountIdentAddReq.getAk());
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    AccountIdent.class), () -> new ConflictException("账号认证类型与AK已存在")))
                    .compose(resp ->
                            IAMBasicProcessor.validRuleAndGetValidEndTime(
                                    accountIdentAddReq.getKind(),
                                    accountIdentAddReq.getAk(),
                                    accountIdentAddReq.getSk(),
                                    relTenantId,
                                    context))
                    .compose(validEndTime ->
                            IAMBasicProcessor.processIdentSk(
                                    accountIdentAddReq.getKind(),
                                    accountIdentAddReq.getAk(),
                                    accountIdentAddReq.getSk(),
                                    null,
                                    relTenantId,
                                    context)
                                    .compose(sk -> context.helper.success(new Tuple2<>(validEndTime, sk))))
                    .compose(processInfo -> {
                        var accountIdent = context.helper.convert(accountIdentAddReq, AccountIdent.class);
                        accountIdent.setRelAccountId(accountId);
                        accountIdent.setRelTenantId(relTenantId);
                        if (accountIdent.getValidStartTime() == null) {
                            accountIdent.setValidStartTime(new Date());
                        }
                        if (accountIdent.getValidEndTime() == null) {
                            accountIdent.setValidEndTime(processInfo._0);
                        }
                        accountIdent.setSk(processInfo._1);
                        return context.fun.sql.save(accountIdent);
                    });
        };
    }

    public static ProcessFun<Void> modifyAccountIdent() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountIdentId = Long.parseLong(context.req.params.get("accountIdentId"));
            var accountIdentModifyReq = context.req.body(AccountIdentModifyReq.class);
            return context.helper.notExistToError(
                    context.fun.sql.getOne(
                            new HashMap<>() {
                                {
                                    put("id", accountIdentId);
                                    put("rel_tenant_id", relTenantId);
                                }
                            },
                            AccountIdent.class), () -> new UnAuthorizedException("账号认证不合法"))
                    .compose(accountIdent -> {
                        var accountIdentAk = accountIdentModifyReq.getAk() != null ? accountIdentModifyReq.getAk() : accountIdent.getAk();
                        return context.helper.existToError(
                                context.fun.sql.exists(
                                        new HashMap<>() {
                                            {
                                                put("!id", accountIdentId);
                                                put("kind", accountIdent.getKind());
                                                put("ak", accountIdentAk);
                                                put("rel_tenant_id", relTenantId);
                                            }
                                        },
                                        AccountIdent.class), () -> new ConflictException("账号认证类型与AK已存在"))
                                .compose(resp ->
                                        IAMBasicProcessor.validRuleAndGetValidEndTime(
                                                accountIdent.getKind(),
                                                accountIdentModifyReq.getAk(),
                                                accountIdentModifyReq.getSk(),
                                                relTenantId,
                                                context))
                                .compose(resp -> {
                                    if (accountIdentModifyReq.getSk() == null) {
                                        return IAMBasicProcessor.processIdentSk(
                                                accountIdent.getKind(),
                                                accountIdentAk,
                                                accountIdentModifyReq.getSk(),
                                                null,
                                                relTenantId,
                                                context);
                                    }
                                    return context.helper.success(null);
                                });
                    })
                    .compose(inputSk -> {
                        if (inputSk != null) {
                            accountIdentModifyReq.setSk(inputSk);
                        }
                        return context.fun.sql.update(
                                new HashMap<>() {
                                    {
                                        put("id", accountIdentId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                },
                                context.helper.convert(accountIdentModifyReq, AccountIdent.class));
                    });
        };
    }

    public static ProcessFun<List<AccountIdentResp>> findAccountIdents() {
        return context -> context.fun.sql.list(
                new HashMap<>() {
                    {
                        put("rel_account_id", context.req.params.get("accountId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                AccountIdent.class)
                .compose(accountIdents -> context.helper.success(accountIdents, AccountIdentResp.class));
    }

    public static ProcessFun<Void> deleteAccountIdent() {
        return context ->
                context.fun.sql.softDelete(
                        new HashMap<>() {
                            {
                                put("id", Long.parseLong(context.req.params.get("accountId")));
                                put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                            }
                        },
                        AccountIdent.class);
    }

    // --------------------------------------------------------------------

    public static ProcessFun<Long> addAccountApp() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountId = Long.parseLong(context.req.params.get("accountId"));
            var appId = Long.parseLong(context.req.params.get("appId"));
            return IAMBasicProcessor.checkAccountMembership(accountId, relTenantId, context)
                    .compose(resp ->
                            IAMBasicProcessor.checkAppMembership(appId, relTenantId, context))
                    .compose(resp ->
                            context.fun.sql.save(AccountApp.builder()
                                    .relAccountId(accountId)
                                    .relAppId(appId)
                                    .build()));
        };
    }

    public static ProcessFun<Void> deleteAccountApp() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountAppId = Long.parseLong(context.req.params.get("accountAppId"));
            return context.helper.notExistToError(
                    context.fun.sql.getOne(
                            new HashMap<>() {
                                {
                                    put("id", accountAppId);
                                }
                            },
                            AccountApp.class), () -> new UnAuthorizedException("账号应用关联不合法"))
                    .compose(fetchAccountAppResult ->
                            IAMBasicProcessor.checkAccountMembership(
                                    fetchAccountAppResult.getRelAccountId(),
                                    relTenantId,
                                    context)
                                    .compose(resp -> IAMBasicProcessor.checkAppMembership(
                                            fetchAccountAppResult.getRelAppId(),
                                            relTenantId,
                                            context
                                    ))
                                    .compose(resp -> context.fun.sql.softDelete(
                                            fetchAccountAppResult.getId(),
                                            AccountApp.class)));
        };
    }

    // --------------------------------------------------------------------

    public static ProcessFun<Long> addAccountGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountId = Long.parseLong(context.req.params.get("accountId"));
            var groupNodeId = Long.parseLong(context.req.params.get("groupNodeId"));
            return IAMBasicProcessor.checkAccountMembership(accountId, relTenantId, context)
                    .compose(resp ->
                            IAMBasicProcessor.checkGroupNodeMembership(groupNodeId, relTenantId, context))
                    .compose(resp ->
                            context.fun.sql.save(AccountGroup.builder()
                                    .relAccountId(accountId)
                                    .relGroupNodeId(groupNodeId)
                                    .build())
                    );
        };
    }

    public static ProcessFun<Void> deleteAccountGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountGroupId = Long.parseLong(context.req.params.get("accountGroupId"));
            return context.helper.notExistToError(
                    context.fun.sql.getOne(
                            new HashMap<>() {
                                {
                                    put("id", accountGroupId);
                                }
                            },
                            AccountGroup.class), () -> new UnAuthorizedException("账号群组关联不合法"))
                    .compose(fetchAccountGroupResult ->
                            IAMBasicProcessor.checkAccountMembership(
                                    fetchAccountGroupResult.getRelAccountId(),
                                    relTenantId,
                                    context)
                                    .compose(resp -> context.fun.sql.softDelete(
                                            fetchAccountGroupResult.getId(),
                                            AccountGroup.class)));
        };
    }

    // --------------------------------------------------------------------

    public static ProcessFun<Long> addAccountRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountId = Long.parseLong(context.req.params.get("accountId"));
            var roleId = Long.parseLong(context.req.params.get("roleId"));
            return IAMBasicProcessor.checkAccountMembership(accountId, relTenantId, context)
                    .compose(resp ->
                            IAMBasicProcessor.checkRoleMembership(roleId, relTenantId, context))
                    .compose(resp ->
                            context.fun.sql.save(AccountRole.builder()
                                    .relAccountId(accountId)
                                    .relRoleId(roleId)
                                    .build()));
        };
    }

    public static ProcessFun<Void> deleteAccountRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var accountRoleId = Long.parseLong(context.req.params.get("accountRoleId"));
            return context.helper.notExistToError(
                    context.fun.sql.getOne(
                            new HashMap<>() {
                                {
                                    put("id", accountRoleId);
                                }
                            },
                            AccountRole.class), () -> new UnAuthorizedException("账号角色关联不合法"))
                    .compose(fetchAccountRoleResult ->
                            IAMBasicProcessor.checkAccountMembership(
                                    fetchAccountRoleResult.getRelAccountId(),
                                    relTenantId,
                                    context)
                                    .compose(resp -> context.fun.sql.softDelete(
                                            fetchAccountRoleResult.getId(),
                                            AccountRole.class)));
        };
    }

}
