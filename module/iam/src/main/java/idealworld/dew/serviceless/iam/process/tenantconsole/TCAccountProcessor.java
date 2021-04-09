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

package idealworld.dew.serviceless.iam.process.tenantconsole;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.exception.NotFoundException;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.iam.domain.auth.AccountGroup;
import idealworld.dew.serviceless.iam.domain.auth.AccountRole;
import idealworld.dew.serviceless.iam.domain.ident.Account;
import idealworld.dew.serviceless.iam.domain.ident.AccountApp;
import idealworld.dew.serviceless.iam.domain.ident.AccountIdent;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.account.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import java.util.HashMap;
import java.util.List;

/**
 * 租户控制台下的账号控制器.
 *
 * @author gudaoxuri
 */
public class TCAccountProcessor extends EventBusProcessor {

    {
        // 添加当前租户的账号
        addProcessor(OptActionKind.CREATE, "/console/tenant/account", eventBusContext ->
                addAccount(eventBusContext.req.body(AccountAddReq.class), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前租户的某个账号
        addProcessor(OptActionKind.PATCH, "/console/tenant/account/{accountId}", eventBusContext ->
                modifyAccount(Long.parseLong(eventBusContext.req.params.get("accountId")), eventBusContext.req.body(AccountModifyReq.class),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前租户的某个账号信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/account/{accountId}", eventBusContext ->
                getAccount(Long.parseLong(eventBusContext.req.params.get("accountId")), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));
        // 获取当前租户的账号列表信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/account", eventBusContext ->
                pageAccounts(eventBusContext.req.params.getOrDefault("name", null), eventBusContext.req.params.getOrDefault("name", null),
                        eventBusContext.req.pageNumber(), eventBusContext.req.pageSize(), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));
        // 删除当前租户的某个账号、关联的账号认证、账号群组、账号角色、账号应用、账号绑定
        addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}", eventBusContext ->
                deleteAccount(Long.parseLong(eventBusContext.req.params.get("accountId")), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));

        // 添加当前租户某个账号的认证
        addProcessor(OptActionKind.CREATE, "/console/tenant/account/{accountId}/ident", eventBusContext ->
                addAccountIdent(Long.parseLong(eventBusContext.req.params.get("accountId")), eventBusContext.req.body(AccountIdentAddReq.class),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前租户某个账号的某个认证
        addProcessor(OptActionKind.PATCH, "/console/tenant/account/{accountId}/ident/{accountIdentId}", eventBusContext ->
                modifyAccountIdent(Long.parseLong(eventBusContext.req.params.get("accountIdentId")),
                        eventBusContext.req.body(AccountIdentModifyReq.class), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));
        // 获取当前租户某个账号的认证列表信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/account/{accountId}/ident", eventBusContext ->
                findAccountIdents(Long.parseLong(eventBusContext.req.params.get("accountId")), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));
        // 删除当前租户某个账号的某个认证
        addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}/ident/{accountIdentId}", eventBusContext ->
                deleteAccountIdent(Long.parseLong(eventBusContext.req.params.get("accountIdentId")), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));

        // 添加当前租户某个账号的关联应用
        addProcessor(OptActionKind.CREATE, "/console/tenant/account/{accountId}/app/{appId}", eventBusContext ->
                addAccountApp(Long.parseLong(eventBusContext.req.params.get("accountId")), Long.parseLong(eventBusContext.req.params.get("appId")),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前租户某个账号的某个关联应用
        addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}/app/{accountAppId}", eventBusContext ->
                deleteAccountApp(Long.parseLong(eventBusContext.req.params.get("accountAppId")), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));

        // 添加当前租户某个账号的关联群组
        addProcessor(OptActionKind.CREATE, "/console/tenant/account/{accountId}/group/{groupNodeId}", eventBusContext ->
                addAccountGroup(Long.parseLong(eventBusContext.req.params.get("accountId")), Long.parseLong(eventBusContext.req.params.get(
                        "groupNodeId")), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前租户某个账号的某个关联群组
        addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}/group/{accountGroupId}", eventBusContext ->
                deleteAccountGroup(Long.parseLong(eventBusContext.req.params.get("accountGroupId")), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));

        // 添加当前租户某个账号的关联角色
        addProcessor(OptActionKind.CREATE, "/console/tenant/account/{accountId}/role/{roleId}", eventBusContext ->
                addAccountRole(Long.parseLong(eventBusContext.req.params.get("accountId")),
                        Long.parseLong(eventBusContext.req.params.get("roleId")), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));
        // 删除当前租户某个账号的某个关联角色
        addProcessor(OptActionKind.DELETE, "/console/tenant/account/{accountId}/role/{accountRoleId}", eventBusContext ->
                deleteAccountRole(Long.parseLong(eventBusContext.req.params.get("accountRoleId")), eventBusContext.req.identOptInfo.getTenantId(),
                        eventBusContext.context));
    }

    public TCAccountProcessor(String moduleName) {
        super(moduleName);
    }

    public static Future<Long> addAccount(AccountAddReq accountAddReq, Long relTenantId, ProcessContext context) {
        return innerAddAccount(accountAddReq, relTenantId, context)
                .compose(account -> context.helper.success(account.getId()));
    }

    public static Future<Account> innerAddAccount(AccountAddReq accountAddReq, Long relTenantId, ProcessContext context) {
        var account = context.helper.convert(accountAddReq, Account.class);
        account.setOpenId("ac" + $.field.createUUID());
        account.setParentId(DewConstant.OBJECT_UNDEFINED);
        account.setRelTenantId(relTenantId);
        account.setStatus(CommonStatus.ENABLED);
        return context.sql.save(account)
                .compose(accountId -> {
                    account.setId(accountId);
                    return context.helper.success(account);
                });
    }

    public static Future<Void> modifyAccount(Long accountId, AccountModifyReq accountModifyReq, Long relTenantId, ProcessContext context) {
        var future = context.helper.success();
        if (accountModifyReq.getParentId() != null) {
            future.compose(resp ->
                    IAMBasicProcessor.checkAccountMembership(accountModifyReq.getParentId(), relTenantId, context));
        }
        return future
                .compose(resp ->
                        context.sql.update(
                                context.helper.convert(accountModifyReq, Account.class),
                                new HashMap<>() {
                                    {
                                        put("id", accountId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                }));
    }

    public static Future<AccountResp> getAccount(Long accountId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                Account.class,
                new HashMap<>() {
                    {
                        put("id", accountId);
                        put("rel_tenant_id", relTenantId);
                    }
                })
                .compose(app -> context.helper.success(app, AccountResp.class));
    }

    public static Future<Page<AccountResp>> pageAccounts(String name, String openId, Long pageNumber, Long pageSize, Long relTenantId,
                                                         ProcessContext context) {
        var whereParameters = new HashMap<String, Object>() {
            {
                put("rel_tenant_id", relTenantId);
            }
        };
        if (name != null && !name.isBlank()) {
            whereParameters.put("%name", "%" + name + "%");
        }
        if (openId != null && !openId.isBlank()) {
            whereParameters.put("%open_id", "%" + openId + "%");
        }
        return context.sql.page(Account.class, pageNumber, pageSize, whereParameters)
                .compose(accounts -> context.helper.success(accounts, AccountResp.class));
    }

    public static Future<Void> deleteAccount(Long accountId, Long relTenantId, ProcessContext context) {
        return context.sql.tx(context, () ->
                context.sql.softDelete(
                        Account.class,
                        new HashMap<>() {
                            {
                                put("id", accountId);
                                put("rel_tenant_id", relTenantId);
                            }
                        })
                        .compose(deleteNumber ->
                                CompositeFuture.all(
                                        context.sql.softDelete(
                                                AccountIdent.class,
                                                new HashMap<>() {
                                                    {
                                                        put("rel_account_id", accountId);
                                                    }
                                                }),
                                        context.sql.softDelete(
                                                AccountRole.class,
                                                new HashMap<>() {
                                                    {
                                                        put("rel_account_id", accountId);
                                                    }
                                                }),
                                        context.sql.softDelete(
                                                AccountApp.class,
                                                new HashMap<>() {
                                                    {
                                                        put("rel_account_id", accountId);
                                                    }
                                                }),
                                        context.sql.softDelete(
                                                AccountGroup.class,
                                                new HashMap<>() {
                                                    {
                                                        put("rel_account_id", accountId);
                                                    }
                                                }))
                                        .compose(resp -> context.helper.success())));
    }

    // --------------------------------------------------------------------

    public static Future<Long> addAccountIdent(Long accountId, AccountIdentAddReq accountIdentAddReq, Long relTenantId, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.exists(
                        Account.class,
                        new HashMap<>() {
                            {
                                put("id", accountId);
                                put("rel_tenant_id", relTenantId);
                            }
                        }),
                () -> new NotFoundException("找不到对应的关联账号"))
                .compose(resp ->
                        context.helper.existToError(context.sql.exists(
                                AccountIdent.class,
                                new HashMap<>() {
                                    {
                                        put("kind", accountIdentAddReq.getKind());
                                        put("ak", accountIdentAddReq.getAk());
                                        put("rel_tenant_id", relTenantId);
                                    }
                                }),
                                () -> new ConflictException("账号认证类型[" + accountIdentAddReq.getKind() + "]与AK[" + accountIdentAddReq.getAk() + "]已存在")))
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
                        accountIdent.setValidStartTime(System.currentTimeMillis());
                    }
                    if (accountIdent.getValidEndTime() == null) {
                        accountIdent.setValidEndTime(processInfo._0);
                    }
                    accountIdent.setSk(processInfo._1);
                    return context.sql.save(accountIdent);
                });
    }

    public static Future<Void> modifyAccountIdent(Long accountIdentId, AccountIdentModifyReq accountIdentModifyReq, Long relTenantId,
                                                  ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.getOne(
                        AccountIdent.class,
                        new HashMap<>() {
                            {
                                put("id", accountIdentId);
                                put("rel_tenant_id", relTenantId);
                            }
                        }),
                () -> new NotFoundException("找不到对应的账号认证"))
                .compose(accountIdent -> {
                    var accountIdentAk = accountIdentModifyReq.getAk() != null ? accountIdentModifyReq.getAk() : accountIdent.getAk();
                    return context.helper.existToError(
                            context.sql.exists(
                                    AccountIdent.class,
                                    new HashMap<>() {
                                        {
                                            put("!id", accountIdentId);
                                            put("kind", accountIdent.getKind());
                                            put("ak", accountIdentAk);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    }),
                            () -> new ConflictException("账号认证类型[" + accountIdent.getKind() + "]与AK[" + accountIdentAk + "]已存在"))
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
                    return context.sql.update(
                            context.helper.convert(accountIdentModifyReq, AccountIdent.class),
                            new HashMap<>() {
                                {
                                    put("id", accountIdentId);
                                    put("rel_tenant_id", relTenantId);
                                }
                            });
                });
    }

    public static Future<List<AccountIdentResp>> findAccountIdents(Long accountId, Long relTenantId, ProcessContext context) {
        return context.sql.list(
                AccountIdent.class,
                new HashMap<>() {
                    {
                        put("rel_account_id", accountId);
                        put("rel_tenant_id", relTenantId);
                    }
                })
                .compose(accountIdents -> context.helper.success(accountIdents, AccountIdentResp.class));
    }

    public static Future<Void> deleteAccountIdent(Long accountIdentId, Long relTenantId, ProcessContext context) {
        return context.sql.softDelete(
                AccountIdent.class,
                new HashMap<>() {
                    {
                        put("id", accountIdentId);
                        put("rel_tenant_id", relTenantId);
                    }
                });
    }

    // --------------------------------------------------------------------

    public static Future<Long> addAccountApp(Long accountId, Long appId, Long relTenantId, ProcessContext context) {
        return IAMBasicProcessor.checkAccountMembership(accountId, relTenantId, context)
                .compose(resp ->
                        IAMBasicProcessor.checkAppMembership(appId, relTenantId, context))
                .compose(resp ->
                        context.sql.getOne(
                                AccountApp.class,
                                new HashMap<String,Object>() {
                                    {
                                        put("rel_account_id", accountId);
                                        put("rel_app_id", appId);
                                    }
                                }))
                .compose(accountApp -> {
                    if (accountApp == null) {
                        return context.sql.save(AccountApp.builder()
                                .relAccountId(accountId)
                                .relAppId(appId)
                                .build());
                    }
                    return context.helper.success(accountApp.getId());
                });
    }

    public static Future<Void> deleteAccountApp(Long accountAppId, Long relTenantId, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.getOne(
                        AccountApp.class,
                        new HashMap<>() {
                            {
                                put("id", accountAppId);
                            }
                        }),
                () -> new NotFoundException("找不到对应的账号应用"))
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
                                .compose(resp -> context.sql.softDelete(AccountApp.class, fetchAccountAppResult.getId())));
    }

    // --------------------------------------------------------------------

    public static Future<Long> addAccountGroup(Long accountId, Long groupNodeId, Long relTenantId, ProcessContext context) {
        return IAMBasicProcessor.checkAccountMembership(accountId, relTenantId, context)
                .compose(resp ->
                        IAMBasicProcessor.checkGroupNodeMembership(groupNodeId, relTenantId, context))
                .compose(resp ->
                        context.sql.getOne(
                                AccountGroup.class,
                                new HashMap<String,Object>() {
                                    {
                                        put("rel_account_id", accountId);
                                        put("rel_group_node_id", groupNodeId);
                                    }
                                }))
                .compose(accountGroup -> {
                    if (accountGroup == null) {
                        return context.sql.save(AccountGroup.builder()
                                .relAccountId(accountId)
                                .relGroupNodeId(groupNodeId)
                                .build());
                    }
                    return context.helper.success(accountGroup.getId());
                });
    }

    public static Future<Void> deleteAccountGroup(Long accountGroupId, Long relTenantId, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.getOne(
                        AccountGroup.class,
                        new HashMap<>() {
                            {
                                put("id", accountGroupId);
                            }
                        }),
                () -> new NotFoundException("找不到对应的账号群组"))
                .compose(fetchAccountGroupResult ->
                        IAMBasicProcessor.checkAccountMembership(
                                fetchAccountGroupResult.getRelAccountId(),
                                relTenantId,
                                context)
                                .compose(resp -> context.sql.softDelete(AccountGroup.class, fetchAccountGroupResult.getId())));
    }

    // --------------------------------------------------------------------

    public static Future<Long> addAccountRole(Long accountId, Long roleId, Long relTenantId, ProcessContext context) {
        return IAMBasicProcessor.checkAccountMembership(accountId, relTenantId, context)
                .compose(resp ->
                        IAMBasicProcessor.checkRoleMembership(roleId, relTenantId, context))
                .compose(resp ->
                        context.sql.getOne(
                                AccountRole.class,
                                new HashMap<String,Object>() {
                                    {
                                        put("rel_account_id", accountId);
                                        put("rel_role_id", roleId);
                                    }
                                }))
                .compose(accountRole -> {
                    if (accountRole == null) {
                        return context.sql.save(AccountRole.builder()
                                .relAccountId(accountId)
                                .relRoleId(roleId)
                                .build());
                    }
                    return context.helper.success(accountRole.getId());
                });
    }

    public static Future<Void> deleteAccountRole(Long accountRoleId, Long relTenantId, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.getOne(
                        AccountRole.class,
                        new HashMap<>() {
                            {
                                put("id", accountRoleId);
                            }
                        }),
                () -> new NotFoundException("找不到对应的账号角色"))
                .compose(fetchAccountRoleResult ->
                        IAMBasicProcessor.checkAccountMembership(
                                fetchAccountRoleResult.getRelAccountId(),
                                relTenantId,
                                context)
                                .compose(resp -> context.sql.softDelete(AccountRole.class, fetchAccountRoleResult.getId())));
    }

}
