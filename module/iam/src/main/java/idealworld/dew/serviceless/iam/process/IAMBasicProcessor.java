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

package idealworld.dew.serviceless.iam.process;

import com.ecfront.dew.common.$;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.exception.NotFoundException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.auth.*;
import idealworld.dew.serviceless.iam.domain.ident.*;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 公共函数服务.
 *
 * @author gudaoxuri
 */
@Slf4j
public class IAMBasicProcessor {

    private static final Map<String, Pattern> VALID_RULES = new ConcurrentHashMap<>();

    public static Future<Long> getEnabledTenantIdByAppId(Long appId, Boolean registerAction, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.getOne(
                        "SELECT rel_tenant_id FROM %s" +
                                " WHERE id = ? AND status = ?",
                        App.class, appId, CommonStatus.ENABLED),
                () -> new BadRequestException("应用[" + appId + "]不存在或未启用"))
                .compose(fetchAppResult -> {
                    if (registerAction == null || !registerAction) {
                        return context.sql.getOne(
                                "SELECT id FROM %s" +
                                        " WHERE id = ? AND status = ?",
                                Tenant.class, fetchAppResult.getLong("rel_tenant_id"), CommonStatus.ENABLED);
                    } else {
                        return context.sql.getOne(
                                "SELECT id FROM %s" +
                                        " WHERE id = ? AND status = ? AND allow_account_register = ?",
                                Tenant.class, fetchAppResult.getLong("rel_tenant_id"), CommonStatus.ENABLED, true);
                    }
                })
                .compose(fetchTenantResult -> {
                    if (fetchTenantResult == null) {
                        if (registerAction == null || !registerAction) {
                            context.helper.error(new BadRequestException("应用[" + appId + "]对应租户不存在或未启用"));
                        } else {
                            context.helper.error(new BadRequestException("应用[" + appId + "]对应租户不存在、未启用或禁止注册"));
                        }
                    }
                    return context.helper.success(fetchTenantResult.getLong("id"));
                });
    }

    public static Future<Long> getEnabledTenantIdByOpenId(String openId, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.getOne(
                        "SELECT rel_tenant_id FROM %s" +
                                " WHERE open_id = ? AND status = ?",
                        Account.class, openId, CommonStatus.ENABLED),
                () -> new BadRequestException("账号[" + openId + "]不存在或未启用"))
                .compose(fetchAccountResult ->
                        context.helper.notExistToError(
                                context.sql.getOne(
                                        "SELECT id FROM %s" +
                                                " WHERE id = ? AND status = ?",
                                        Tenant.class, fetchAccountResult.getLong("rel_tenant_id"), CommonStatus.ENABLED),
                                () -> new BadRequestException("账号[" + openId + "]对应租户不存在或未启用")))
                .compose(fetchTenantResult ->
                        context.helper.success(fetchTenantResult.getLong("id")));
    }

    public static Future<String> processIdentSk(AccountIdentKind identKind, String ak, String sk, Long relAppId, Long relTenantId,
                                                ProcessContext context) {
        switch (identKind) {
            case EMAIL:
            case PHONE:
                return context.cache.get(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + relTenantId + ":" + ak)
                        .compose(tmpSk -> {
                            if (tmpSk == null) {
                                context.helper.error(new BadRequestException("验证码不存在或已过期"));
                            }
                            if (!tmpSk.equalsIgnoreCase(sk)) {
                                context.helper.error(new BadRequestException("验证码错误"));
                            }
                            return context.helper.success("");
                        });
            case USERNAME:
                if (sk == null || sk.trim().isBlank()) {
                    context.helper.error(new BadRequestException("密码必填"));
                }
                return context.helper.success($.security.digest.digest(ak + sk, "SHA-512"));
            case WECHAT_XCX:
                // 需要关联AppId
                return context.cache.get(IAMConstant.CACHE_ACCESS_TOKEN + relAppId + ":" + identKind.toString())
                        .compose(accessToken -> {
                            if (accessToken == null) {
                                context.helper.error(new UnAuthorizedException("Access Token不存在"));
                            }
                            if (!accessToken.equalsIgnoreCase(sk)) {
                                context.helper.error(new UnAuthorizedException("Access Token错误"));
                            }
                            return context.helper.success("");
                        });
            default:
                return context.helper.success("");
        }
    }

    public static Future<Long> validRuleAndGetValidEndTime(AccountIdentKind kind, String ak, String sk, Long relTenantId, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.getOne(
                        "SELECT valid_ak_rule, valid_sk_rule, valid_time_sec FROM %s" +
                                " WHERE kind = ? AND rel_tenant_id = ?",
                        TenantIdent.class, kind, relTenantId),
                () -> new BadRequestException("认证类型不存在或已禁用"))
                .compose(fetchTenantIdentResult -> {
                    var validAkRule = fetchTenantIdentResult.getString("valid_ak_rule");
                    var validSkRule = fetchTenantIdentResult.getString("valid_sk_rule");
                    var validTimeSec = fetchTenantIdentResult.getLong("valid_time_sec");
                    if (ak != null && validAkRule != null && !validAkRule.isBlank()) {
                        if (!VALID_RULES.containsKey(validAkRule)) {
                            VALID_RULES.put(validAkRule, Pattern.compile(validAkRule));
                        }
                        if (!VALID_RULES.get(validAkRule).matcher(ak).matches()) {
                            context.helper.error(new BadRequestException("认证名规则不合法"));
                        }
                    }
                    if (sk != null && validSkRule != null && !validSkRule.isBlank()) {
                        if (!VALID_RULES.containsKey(validSkRule)) {
                            VALID_RULES.put(validSkRule, Pattern.compile(validSkRule));
                        }
                        if (!VALID_RULES.get(validSkRule).matcher(sk).matches()) {
                            context.helper.error(new BadRequestException("认证密钥规则不合法"));
                        }
                    }
                    return context.helper.success(
                            validTimeSec == null || validTimeSec.equals(DewConstant.OBJECT_UNDEFINED)
                                    ? DewConstant.MAX_TIME
                                    : new Date(System.currentTimeMillis() + validTimeSec * 1000).getTime()
                    );
                });
    }

    public static Future<Void> checkAccountMembership(Long accountId, Long tenantId, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.exists(
                        "SELECT id FROM %s" +
                                " WHERE id = ? AND rel_tenant_id = ?",
                        Account.class, accountId, tenantId),
                () -> new UnAuthorizedException("账号不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> checkAccountMembership(List<Long> accountIds, Long appId, Long tenantId, ProcessContext context) {
        accountIds = accountIds.stream()
                // 排除AKSK认证的虚拟账号
                .filter(id -> id.longValue() != DewAuthConstant.AK_SK_IDENT_ACCOUNT_FLAG)
                .collect(Collectors.toList());
        if (accountIds.isEmpty()) {
            return context.helper.success(null);
        }
        var accountsWhere = accountIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        var parameters = new ArrayList<>();
        parameters.add(Account.class);
        parameters.add(AccountApp.class);
        parameters.addAll(accountIds);
        parameters.add(tenantId);
        parameters.add(appId);
        return context.helper.notExistToError(
                context.sql.exists(
                        "SELECT acc.id FROM %s AS acc" +
                                " INNER JOIN %s AS accapp ON accapp.rel_account_id = acc.id" +
                                " WHERE acc.id in (" + accountsWhere + ") AND acc.rel_tenant_id = ? AND accapp.rel_app_id = ?",
                        parameters),
                () -> new UnAuthorizedException("账号不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> checkAppMembership(Long appId, Long tenantId, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.exists(
                        "SELECT id FROM %s" +
                                " WHERE id = ? AND rel_tenant_id = ?",
                        App.class, appId, tenantId),
                () -> new UnAuthorizedException("应用不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> checkAppMembership(List<Long> appIds, Long tenantId, ProcessContext context) {
        var parameters = new ArrayList<>();
        var appsWhere = appIds.stream().map(app -> "?").collect(Collectors.joining(", "));
        parameters.add(App.class);
        parameters.addAll(appIds);
        parameters.add(tenantId);
        return context.helper.notExistToError(
                context.sql.exists(
                        "SELECT id FROM %s" +
                                " WHERE id in (" + appsWhere + ") AND rel_tenant_id = ?",
                        parameters),
                () -> new UnAuthorizedException("应用不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> checkRoleMembership(Long roleId, Long tenantId, ProcessContext context) {
        return checkRoleMembership(new ArrayList<>() {
            {
                add(roleId);
            }
        }, DewConstant.OBJECT_UNDEFINED, tenantId, context);
    }

    public static Future<Void> checkRoleMembership(Long roleId, Long appId, Long tenantId, ProcessContext context) {
        return checkRoleMembership(new ArrayList<>() {
            {
                add(roleId);
            }
        }, appId, tenantId, context);
    }

    public static Future<Void> checkRoleMembership(List<Long> roleIds, Long appId, Long tenantId, ProcessContext context) {
        var rolesWhere = roleIds.stream().map(role -> "?").collect(Collectors.joining(", "));
        var parameters = new ArrayList<>();
        parameters.add(Role.class);
        parameters.addAll(roleIds);
        var sql = "";
        if (appId == DewConstant.OBJECT_UNDEFINED) {
            sql = "SELECT id FROM %s" +
                    " WHERE id in (" + rolesWhere + ")" +
                    " AND (expose_kind in (?, ?) AND rel_tenant_id = ?" +
                    " OR expose_kind = ?)";
            parameters.add(ExposeKind.APP);
            parameters.add(ExposeKind.TENANT);
            parameters.add(tenantId);
            parameters.add(ExposeKind.GLOBAL);
        } else {
            sql = "SELECT id FROM %s" +
                    " WHERE id in (" + rolesWhere + ")" +
                    " AND (rel_app_id = ? AND expose_kind = ?" +
                    " OR rel_tenant_id = ? AND expose_kind = ?" +
                    " OR expose_kind = ?)";
            parameters.add(appId);
            parameters.add(ExposeKind.APP);
            parameters.add(tenantId);
            parameters.add(ExposeKind.TENANT);
            parameters.add(ExposeKind.GLOBAL);
        }
        return context.helper.notExistToError(
                context.sql.exists(sql, parameters),
                () -> new UnAuthorizedException("角色不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> checkGroupNodeMembership(Long groupNodeId, Long tenantId, ProcessContext context) {
        return checkGroupNodeMembership(new ArrayList<>() {
            {
                add(groupNodeId);
            }
        }, DewConstant.OBJECT_UNDEFINED, tenantId, context);
    }

    public static Future<Void> checkGroupNodeMembership(Long groupNodeId, Long appId, Long tenantId, ProcessContext context) {
        return checkGroupNodeMembership(new ArrayList<>() {
            {
                add(groupNodeId);
            }
        }, appId, tenantId, context);
    }

    public static Future<Void> checkGroupNodeMembership(List<Long> groupNodeIds, Long appId, Long tenantId, ProcessContext context) {
        var groupNodesWhere = groupNodeIds.stream().map(node -> "?").collect(Collectors.joining(", "));
        var parameters = new ArrayList<>();
        parameters.add(GroupNode.class);
        parameters.add(Group.class);
        parameters.addAll(groupNodeIds);
        var sql = "";
        if (appId == DewConstant.OBJECT_UNDEFINED) {
            sql = "SELECT node.id FROM %s AS node" +
                    " INNER JOIN %s AS _group ON _group.id = node.rel_group_id" +
                    " WHERE node.id in (" + groupNodesWhere + ") AND" +
                    " (_group.expose_kind in (?, ?) AND _group.rel_tenant_id = ?" +
                    " OR _group.expose_kind = ?)";
            parameters.add(ExposeKind.APP);
            parameters.add(ExposeKind.TENANT);
            parameters.add(tenantId);
            parameters.add(ExposeKind.GLOBAL);
        } else {
            sql = "SELECT node.id FROM %s AS node" +
                    " INNER JOIN %s AS _group ON _group.id = node.rel_group_id" +
                    " WHERE node.id in (" + groupNodesWhere + ") AND" +
                    " (_group.rel_app_id = ? AND _group.expose_kind = ?" +
                    " OR _group.rel_tenant_id = ? AND _group.expose_kind = ?" +
                    " OR _group.expose_kind = ?)";
            parameters.add(appId);
            parameters.add(ExposeKind.APP);
            parameters.add(tenantId);
            parameters.add(ExposeKind.TENANT);
            parameters.add(ExposeKind.GLOBAL);
        }
        return context.helper.notExistToError(
                context.sql.exists(sql, parameters),
                () -> new UnAuthorizedException("群组节点不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Set<IdentOptInfo.RoleInfo>> findRoleInfo(Long accountId, ProcessContext context) {
        return context.sql.list(
                "SELECT role.id, role.name, role_def.code FROM %s AS accrole" +
                        " INNER JOIN %s role ON role.id = accrole.rel_role_id" +
                        " INNER JOIN %s role_def ON role_def.id = role.rel_role_def_id" +
                        " WHERE accrole.rel_account_id = ?",
                AccountRole.class, Role.class, RoleDef.class, accountId)
                .compose(fetchRolesResult -> {
                    var roleInfos = fetchRolesResult.stream()
                            .map(info -> {
                                var role = new IdentOptInfo.RoleInfo();
                                role.setId(info.getLong("id"));
                                role.setDefCode(info.getString("code"));
                                role.setName(info.getString("name"));
                                return role;
                            })
                            .collect(Collectors.toSet());
                    return context.helper.success(roleInfos);
                });
    }

    public static Future<Set<IdentOptInfo.GroupInfo>> findGroupInfo(Long accountId, ProcessContext context) {
        return context.sql.list(
                "SELECT _group.code AS group_code, _group.name AS group_name, node.code AS node_code, node.name AS node_name, node" +
                        ".bus_code FROM %s AS accgroup" +
                        " INNER JOIN %s node ON node.id = accgroup.rel_group_node_id" +
                        " INNER JOIN %s _group ON _group.id = node.rel_group_id" +
                        " WHERE accgroup.rel_account_id = ?",
                AccountGroup.class, GroupNode.class, Group.class, accountId)
                .compose(fetchGroupsResult -> {
                    var groupInfos = fetchGroupsResult.stream()
                            .map(info ->
                                    IdentOptInfo.GroupInfo.builder()
                                            .groupCode(info.getString("group_code"))
                                            .groupName(info.getString("group_name"))
                                            .groupNodeCode(info.getString("node_code"))
                                            .groupNodeName(info.getString("node_name"))
                                            .groupNodeBusCode(info.getString("bus_code"))
                                            .build()
                            )
                            .collect(Collectors.toSet());
                    return context.helper.success(groupInfos);
                });
    }

    public static Future<String> getAppCodeById(Long appId, Long relTenantId, ProcessContext context) {
        if (appId == null || appId == DewConstant.OBJECT_UNDEFINED) {
            throw new NotFoundException("找不到对应的应用");
        }
        return context.helper.notExistToError(
                context.sql.getOne(
                        "SELECT open_id FROM %s WHERE id = ? AND rel_tenant_id = ?",
                        App.class, appId, relTenantId),
                () -> new NotFoundException("找不到对应的应用"))
                .compose(resp -> context.helper.success(resp.getString("open_id")));
    }

    public static Future<Long> getAppIdByCode(String appCode, Long relTenantId, ProcessContext context) {
        if (appCode == null || appCode.trim().isEmpty()) {
            throw new NotFoundException("找不到对应的应用");
        }
        return context.helper.notExistToError(
                context.sql.getOne(
                        "SELECT id FROM %s WHERE open_id = ? AND rel_tenant_id = ?",
                        App.class, appCode, relTenantId),
                () -> new NotFoundException("找不到对应的应用"))
                .compose(resp -> context.helper.success(resp.getLong("id")));
    }

}
