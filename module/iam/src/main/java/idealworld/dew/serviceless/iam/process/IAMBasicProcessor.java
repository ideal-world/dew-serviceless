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
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.framework.exception.BadRequestException;
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
import java.util.stream.IntStream;

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
                        String.format("SELECT rel_tenant_id FROM %s" +
                                        " WHERE id = #{id} AND status = #{status}",
                                new App().tableName()),
                        new HashMap<>() {
                            {
                                put("id", appId);
                                put("status", CommonStatus.ENABLED);
                            }
                        }), () -> new BadRequestException("应用[" + appId + "]不存在或未启用"))
                .compose(fetchAppResult -> {
                    if (registerAction == null || !registerAction) {
                        return context.sql.getOne(
                                String.format("SELECT id FROM %s" +
                                                " WHERE id = #{id} AND status = #{status}",
                                        new Tenant().tableName()),
                                new HashMap<>() {
                                    {
                                        put("id", fetchAppResult.getLong("rel_tenant_id"));
                                        put("status", CommonStatus.ENABLED);
                                    }
                                });
                    } else {
                        return context.sql.getOne(
                                String.format("SELECT id FROM %s" +
                                                " WHERE id = #{id} AND status = #{status} AND allow_account_register = #{allow_account_register}",
                                        new Tenant().tableName()),
                                new HashMap<>() {
                                    {
                                        put("id", fetchAppResult.getLong("rel_tenant_id"));
                                        put("status", CommonStatus.ENABLED);
                                        put("allow_account_register", true);
                                    }
                                });
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
                        String.format("SELECT rel_tenant_id FROM %s" +
                                        " WHERE open_id = #{open_id} AND status = #{status}",
                                new Account().tableName()),
                        new HashMap<>() {
                            {
                                put("open_id", openId);
                                put("status", CommonStatus.ENABLED);
                            }
                        }), () -> new BadRequestException("账号[" + openId + "]不存在或未启用"))
                .compose(fetchAccountResult ->
                        context.helper.notExistToError(
                                context.sql.getOne(
                                        String.format("SELECT id FROM %s" +
                                                        " WHERE id = #{id} AND status = #{status}",
                                                new Tenant().tableName()),
                                        new HashMap<>() {
                                            {
                                                put("id", fetchAccountResult.getLong("rel_tenant_id"));
                                                put("status", CommonStatus.ENABLED);
                                            }
                                        }), () -> new BadRequestException("账号[" + openId + "]对应租户不存在或未启用")))
                .compose(fetchTenantResult ->
                        context.helper.success(fetchTenantResult.getLong("id")));
    }

    public static Future<String> processIdentSk(AccountIdentKind identKind, String ak, String sk, Long relAppId, Long relTenantId, ProcessContext context) {
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
                        String.format("SELECT valid_ak_rule, valid_sk_rule, valid_time_sec FROM %s" +
                                        " WHERE kind = #{kind} AND rel_tenant_id = #{rel_tenant_id}",
                                new TenantIdent().tableName()),
                        new HashMap<>() {
                            {
                                put("kind", kind);
                                put("rel_tenant_id", relTenantId);
                            }
                        }), () -> new BadRequestException("认证类型不存在或已禁用"))
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
                        String.format("SELECT id FROM %s" +
                                        " WHERE id = #{id} AND rel_tenant_id = #{rel_tenant_id}",
                                new Account().tableName()),
                        new HashMap<>() {
                            {
                                put("id", accountId);
                                put("rel_tenant_id", tenantId);
                            }
                        }), () -> new UnAuthorizedException("账号不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> checkAccountMembership(List<Long> accountIds, Long appId, Long tenantId, ProcessContext context) {
        var whereParametersMap = IntStream.range(0, accountIds.size())
                .mapToObj(i -> new Object[]{"id" + i, accountIds.get(i)})
                .collect(Collectors.toMap(i -> (String) i[0], i -> i[1]));
        var accountsWhere = whereParametersMap.keySet().stream().map(acc -> "#{" + acc + "}").collect(Collectors.joining(", "));
        whereParametersMap.putAll(new HashMap<>() {
            {
                put("rel_tenant_id", tenantId);
                put("rel_app_id", appId);
            }
        });
        return context.helper.notExistToError(
                context.sql.exists(
                        String.format("SELECT acc.id FROM %s AS acc" +
                                        " INNER JOIN %s AS accapp ON accapp.rel_account_id = acc.id" +
                                        " WHERE acc.id in (" + accountsWhere + ") AND acc.rel_tenant_id = #{rel_tenant_id} AND accapp.rel_app_id = #{rel_app_id}",
                                new Account().tableName(), new AccountApp().tableName()),
                        whereParametersMap), () -> new UnAuthorizedException("账号不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> checkAppMembership(Long appId, Long tenantId, ProcessContext context) {
        return context.helper.notExistToError(
                context.sql.exists(
                        String.format("SELECT id FROM %s" +
                                        " WHERE id = #{id} AND rel_tenant_id = #{rel_tenant_id}",
                                new App().tableName()),
                        new HashMap<>() {
                            {
                                put("id", appId);
                                put("rel_tenant_id", tenantId);
                            }
                        }), () -> new UnAuthorizedException("应用不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> checkAppMembership(List<Long> appIds, Long tenantId, ProcessContext context) {
        var whereParametersMap = IntStream.range(0, appIds.size())
                .mapToObj(i -> new Object[]{"id" + i, appIds.get(i)})
                .collect(Collectors.toMap(i -> (String) i[0], i -> i[1]));
        var appsWhere = whereParametersMap.keySet().stream().map(app -> "#{" + app + "}").collect(Collectors.joining(", "));
        whereParametersMap.putAll(new HashMap<>() {
            {
                put("rel_tenant_id", tenantId);
            }
        });
        return context.helper.notExistToError(
                context.sql.exists(
                        String.format("SELECT id FROM %s" +
                                        " WHERE id in (" + appsWhere + ") AND rel_tenant_id = #{rel_tenant_id}",
                                new App().tableName()),
                        whereParametersMap), () -> new UnAuthorizedException("应用不合法"))
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
        var whereParametersMap = IntStream.range(0, roleIds.size())
                .mapToObj(i -> new Object[]{"id" + i, roleIds.get(i)})
                .collect(Collectors.toMap(i -> (String) i[0], i -> i[1]));
        var rolesWhere = whereParametersMap.keySet().stream().map(role -> "#{" + role + "}").collect(Collectors.joining(", "));

        var sql = appId == DewConstant.OBJECT_UNDEFINED
                ? "SELECT id FROM %s" +
                " WHERE id in (" + rolesWhere + ")" +
                " AND (expose_kind in (#{expose_kind_app}, #{expose_kind_tenant}) AND rel_tenant_id = #{rel_tenant_id}" +
                " OR expose_kind = #{expose_kind_global})"
                : "SELECT id FROM %s" +
                " WHERE id in (" + rolesWhere + ")" +
                " AND (rel_app_id = #{rel_app_id} AND expose_kind = #{expose_kind_app}" +
                " OR rel_tenant_id = #{rel_tenant_id} AND expose_kind = #{expose_kind_tenant}" +
                " OR expose_kind = #{expose_kind_global})";
        whereParametersMap.putAll(new HashMap<>() {
            {
                put("expose_kind_app", ExposeKind.APP);
                put("expose_kind_tenant", ExposeKind.TENANT);
                put("expose_kind_global", ExposeKind.GLOBAL);
                put("rel_tenant_id", tenantId);
                put("rel_app_id", appId);
            }
        });
        return context.helper.notExistToError(
                context.sql.exists(
                        String.format(sql, new Role().tableName()),
                        whereParametersMap), () -> new UnAuthorizedException("角色不合法"))
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
        var whereParametersMap = IntStream.range(0, groupNodeIds.size())
                .mapToObj(i -> new Object[]{"id" + i, groupNodeIds.get(i)})
                .collect(Collectors.toMap(i -> (String) i[0], i -> i[1]));
        var groupNodesWhere = whereParametersMap.keySet().stream().map(node -> "#{" + node + "}").collect(Collectors.joining(", "));

        var sql = appId == DewConstant.OBJECT_UNDEFINED
                ? "SELECT node.id FROM %s AS node" +
                " INNER JOIN %s AS _group ON _group.id = node.rel_group_id" +
                " WHERE node.id in (" + groupNodesWhere + ") AND" +
                " (_group.expose_kind in (#{expose_kind_app}, #{expose_kind_tenant}) AND _group.rel_tenant_id = #{rel_tenant_id}" +
                " OR _group.expose_kind = #{expose_kind_global})"
                : "SELECT node.id FROM %s AS node" +
                " INNER JOIN %s AS _group ON _group.id = node.rel_group_id" +
                " WHERE node.id in (" + groupNodesWhere + ") AND" +
                " (_group.rel_app_id = #{rel_app_id} AND _group.expose_kind = #{expose_kind_app}" +
                " OR _group.rel_tenant_id = #{rel_tenant_id} AND _group.expose_kind = #{expose_kind_tenant}" +
                " OR _group.expose_kind = #{expose_kind_global})";
        whereParametersMap.putAll(new HashMap<>() {
            {
                put("expose_kind_app", ExposeKind.APP);
                put("expose_kind_tenant", ExposeKind.TENANT);
                put("expose_kind_global", ExposeKind.GLOBAL);
                put("rel_app_id", appId);
                put("rel_tenant_id", tenantId);
            }
        });
        return context.helper.notExistToError(
                context.sql.exists(
                        String.format(sql, new GroupNode().tableName(), new Group().tableName()),
                        whereParametersMap), () -> new UnAuthorizedException("群组节点不合法"))
                .compose(resp -> context.helper.success());
    }

    public static Future<Set<IdentOptInfo.RoleInfo>> findRoleInfo(Long accountId, ProcessContext context) {
        return context.sql.list(
                String.format("SELECT role.id, role.name, role_def.code FROM %s AS accrole" +
                                " INNER JOIN %s role ON role.id = accrole.rel_role_id" +
                                " INNER JOIN %s role_def ON role_def.id = role.rel_role_def_id" +
                                " WHERE accrole.rel_account_id = #{rel_account_id}",
                        new AccountRole().tableName(), new Role().tableName(), new RoleDef().tableName()),
                new HashMap<>() {
                    {
                        put("rel_account_id", accountId);
                    }
                })
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
                String.format("SELECT _group.code AS group_code, _group.name AS group_name, node.code AS node_code, node.name AS node_name, node.bus_code FROM %s AS accgroup" +
                                " INNER JOIN %s node ON node.id = accgroup.rel_group_node_id" +
                                " INNER JOIN %s _group ON _group.id = node.rel_group_id" +
                                " WHERE accgroup.rel_account_id = #{rel_account_id}",
                        new AccountGroup().tableName(), new GroupNode().tableName(), new Group().tableName()),
                new HashMap<>() {
                    {
                        put("rel_account_id", accountId);
                    }
                })
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

}
