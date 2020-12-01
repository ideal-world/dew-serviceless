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

package idealworld.dew.serviceless.iam.scene.common.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import group.idealworld.dew.Dew;
import group.idealworld.dew.core.auth.dto.OptInfo;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.IdentOptInfo;
import idealworld.dew.serviceless.common.enumeration.CommonStatus;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.auth.*;
import idealworld.dew.serviceless.iam.domain.ident.*;
import idealworld.dew.serviceless.iam.enumeration.AccountIdentKind;
import idealworld.dew.serviceless.iam.enumeration.ExposeKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 公共函数服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class CommonFunctionService extends IAMBasicService {

    private static final Map<String, Pattern> VALID_RULES = new ConcurrentHashMap<>();

    public Resp<Long> getEnabledTenantIdByAppId(Long appId, Boolean registerAction) {
        var qApp = QApp.app;
        var tenantId = sqlBuilder.select(qApp.relTenantId)
                .from(qApp)
                .where(qApp.id.eq(appId))
                .where(qApp.status.eq(CommonStatus.ENABLED))
                .fetchOne();
        if (tenantId == null) {
            return StandardResp.badRequest(BUSINESS_APP, "应用[" + appId + "]不存在或未启用");
        }
        var qTenant = QTenant.tenant;
        if (registerAction == null || !registerAction) {
            if (sqlBuilder.select(qTenant.id)
                    .from(qTenant)
                    .where(qTenant.id.eq(tenantId))
                    .where(qTenant.status.eq(CommonStatus.ENABLED))
                    .fetchCount() == 0) {
                return StandardResp.badRequest(BUSINESS_APP, "应用[" + appId + "]对应租户不存在或未启用");
            }
        } else {
            if (sqlBuilder.select(qTenant.id)
                    .from(qTenant)
                    .where(qTenant.id.eq(tenantId))
                    .where(qTenant.status.eq(CommonStatus.ENABLED))
                    .where(qTenant.allowAccountRegister.eq(true))
                    .fetchCount() == 0) {
                return StandardResp.badRequest(BUSINESS_APP, "应用[" + appId + "]对应租户不存在、未启用或禁止注册");
            }
        }
        return Resp.success(tenantId);
    }

    public Resp<Long> getEnabledTenantIdByOpenId(String openId) {
        var qAccount = QAccount.account;
        var tenantId = sqlBuilder.select(qAccount.relTenantId)
                .from(qAccount)
                .where(qAccount.openId.eq(openId))
                .where(qAccount.status.eq(CommonStatus.ENABLED))
                .fetchOne();
        if (tenantId == null) {
            return StandardResp.badRequest(BUSINESS_APP, "账号[" + openId + "]不存在或未启用");
        }
        var qTenant = QTenant.tenant;
        if (sqlBuilder.select(qTenant.id)
                .from(qTenant)
                .where(qTenant.id.eq(tenantId))
                .where(qTenant.status.eq(CommonStatus.ENABLED))
                .fetchCount() == 0) {
            return StandardResp.badRequest(BUSINESS_APP, "账号[" + openId + "]对应租户不存在或未启用");
        }
        return Resp.success(tenantId);
    }

    public Resp<String> processIdentSk(AccountIdentKind identKind, String ak, String sk, @Nullable Long relAppId, Long relTenantId) {
        switch (identKind) {
            case EMAIL:
            case PHONE:
                String tmpSk = Dew.cluster.cache.get(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + relTenantId + ":" + ak);
                if (tmpSk == null) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "验证码不存在或已过期");
                }
                if (!tmpSk.equalsIgnoreCase(sk)) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "验证码错误");
                }
                return StandardResp.success("");
            case USERNAME:
                if (ObjectUtils.isEmpty(sk)) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "密码必填");
                }
                return StandardResp.success($.security.digest.digest(ak + sk, "SHA-512"));
            case WECHAT_XCX:
                // 需要关联AppId
                String accessToken = Dew.cluster.cache.get(IAMConstant.CACHE_ACCESS_TOKEN + relAppId + ":" + identKind.toString());
                if (accessToken == null) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "Access Token不存在");
                }
                if (!accessToken.equalsIgnoreCase(sk)) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "Access Token错误");
                }
                return StandardResp.success("");
            default:
                return StandardResp.success("");
        }
    }

    public Resp<Date> validRuleAndGetValidEndTime(AccountIdentKind kind, @Nullable String ak, @Nullable String sk, Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        var tenantIdent = sqlBuilder
                .select(qTenantIdent.validAKRule,
                        qTenantIdent.validSKRule,
                        qTenantIdent.validTimeSec)
                .from(qTenantIdent)
                .where(qTenantIdent.kind.eq(kind))
                .where(qTenantIdent.relTenantId.eq(relTenantId))
                .fetchOne();
        if (tenantIdent == null) {
            return StandardResp.badRequest(BUSINESS_TENANT_IDENT, "认证类型不存在或已禁用");
        }
        var validAkRule = tenantIdent.get(0, String.class);
        var validSkRule = tenantIdent.get(1, String.class);
        var validTimeSec = tenantIdent.get(2, Long.class);
        if (ak != null && !ObjectUtils.isEmpty(validAkRule)) {
            if (!VALID_RULES.containsKey(validAkRule)) {
                VALID_RULES.put(validAkRule, Pattern.compile(validAkRule));
            }
            if (!VALID_RULES.get(validAkRule).matcher(ak).matches()) {
                return StandardResp.badRequest(BUSINESS_TENANT_CERT, "认证名规则不合法");
            }
        }
        if (sk != null && !ObjectUtils.isEmpty(validSkRule)) {
            if (!VALID_RULES.containsKey(validSkRule)) {
                VALID_RULES.put(validSkRule, Pattern.compile(validSkRule));
            }
            if (!VALID_RULES.get(validSkRule).matcher(sk).matches()) {
                return StandardResp.badRequest(BUSINESS_TENANT_CERT, "认证密钥规则不合法");
            }
        }
        return StandardResp.success(validTimeSec == null || validTimeSec.equals(Constant.OBJECT_UNDEFINED)
                ? Constant.MAX_TIME
                : new Date(System.currentTimeMillis() + validTimeSec * 1000));
    }

    public Resp<Void> checkAccountMembership(Long accountId, Long tenantId) {
        var qAccount = QAccount.account;
        if (sqlBuilder.select(qAccount.id)
                .from(qAccount)
                .where(qAccount.id.eq(accountId))
                .where(qAccount.relTenantId.eq(tenantId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_ACCOUNT, "账号不合法");
        }
        return Resp.success(null);
    }

    public Resp<Void> checkAccountMembership(List<Long> accountIds, Long appId, Long tenantId) {
        var qAccount = QAccount.account;
        var qAccountApp = QAccountApp.accountApp;
        if (sqlBuilder.select(qAccount.id)
                .from(qAccount)
                .innerJoin(qAccountApp).on(qAccountApp.relAccountId.eq(qAccount.id))
                .where(qAccount.id.in(accountIds))
                .where(qAccountApp.relAppId.eq(appId))
                .where(qAccount.relTenantId.eq(tenantId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_ACCOUNT, "账号不合法");
        }
        return Resp.success(null);
    }

    public Resp<Void> checkAppMembership(Long appId, Long tenantId) {
        var qApp = QApp.app;
        if (sqlBuilder.select(qApp.id)
                .from(qApp)
                .where(qApp.id.eq(appId))
                .where(qApp.relTenantId.eq(tenantId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_APP, "应用不合法");
        }
        return Resp.success(null);
    }

    public Resp<Void> checkAppMembership(List<Long> appIds, Long tenantId) {
        var qApp = QApp.app;
        if (sqlBuilder.select(qApp.id)
                .from(qApp)
                .where(qApp.id.in(appIds))
                .where(qApp.relTenantId.eq(tenantId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_APP, "应用不合法");
        }
        return Resp.success(null);
    }

    public Resp<Void> checkRoleMembership(Long roleId, Long tenantId) {
        return checkRoleMembership(new ArrayList<>() {
            {
                add(roleId);
            }
        }, Constant.OBJECT_UNDEFINED, tenantId);
    }

    public Resp<Void> checkRoleMembership(Long roleId, Long appId, Long tenantId) {
        return checkRoleMembership(new ArrayList<>() {
            {
                add(roleId);
            }
        }, appId, tenantId);
    }

    public Resp<Void> checkRoleMembership(List<Long> roleIds, Long appId, Long tenantId) {
        var qRole = QRole.role;
        var checkQuery = appId == Constant.OBJECT_UNDEFINED
                ? sqlBuilder.select(qRole.id)
                .from(qRole)
                .where(qRole.id.in(roleIds))
                .where(qRole.relTenantId.eq(tenantId)
                        .or(qRole.exposeKind.eq(ExposeKind.TENANT).and(qRole.relTenantId.eq(tenantId)))
                        .or(qRole.exposeKind.eq(ExposeKind.GLOBAL)))
                : sqlBuilder.select(qRole.id)
                .from(qRole)
                .where(qRole.id.in(roleIds))
                .where((qRole.relAppId.eq(appId).and(qRole.relTenantId.eq(tenantId)))
                        .or(qRole.exposeKind.eq(ExposeKind.TENANT).and(qRole.relTenantId.eq(tenantId)))
                        .or(qRole.exposeKind.eq(ExposeKind.GLOBAL)));
        if (checkQuery.fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_ROLE, "角色不合法");
        }
        return Resp.success(null);
    }

    public Resp<Void> checkGroupNodeMembership(Long groupNodeId, Long tenantId) {
        return checkGroupNodeMembership(new ArrayList<>() {
            {
                add(groupNodeId);
            }
        }, Constant.OBJECT_UNDEFINED, tenantId);
    }

    public Resp<Void> checkGroupNodeMembership(Long groupNodeId, Long appId, Long tenantId) {
        return checkGroupNodeMembership(new ArrayList<>() {
            {
                add(groupNodeId);
            }
        }, appId, tenantId);
    }

    public Resp<Void> checkGroupNodeMembership(List<Long> groupNodeIds, Long appId, Long tenantId) {
        var qGroup = QGroup.group;
        var qGroupNode = QGroupNode.groupNode;
        var checkQuery = appId == Constant.OBJECT_UNDEFINED
                ? sqlBuilder
                .select(qGroupNode.id)
                .from(qGroupNode)
                .innerJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qGroupNode.id.in(groupNodeIds))
                .where(qGroup.relTenantId.eq(tenantId)
                        .or(qGroup.exposeKind.eq(ExposeKind.TENANT).and(qGroup.relTenantId.eq(tenantId)))
                        .or(qGroup.exposeKind.eq(ExposeKind.GLOBAL)))
                : sqlBuilder
                .select(qGroupNode.id)
                .from(qGroupNode)
                .innerJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qGroupNode.id.in(groupNodeIds))
                .where((qGroup.relAppId.eq(appId).and(qGroup.relTenantId.eq(tenantId)))
                        .or(qGroup.exposeKind.eq(ExposeKind.TENANT).and(qGroup.relTenantId.eq(tenantId)))
                        .or(qGroup.exposeKind.eq(ExposeKind.GLOBAL)));
        if (checkQuery.fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_GROUP_NODE, "群组节点不合法");
        }
        return Resp.success(null);
    }

    public Set<IdentOptInfo.RoleInfo> findRoleInfo(Long accountId) {
        var qAccountRole = QAccountRole.accountRole;
        var qRole = QRole.role;
        return sqlBuilder.select(qRole.id, qRole.name)
                .from(qAccountRole)
                .innerJoin(qRole)
                .on(qRole.id.eq(qAccountRole.relRoleId))
                .where(qAccountRole.relAccountId.eq(accountId))
                .fetch()
                .stream()
                .map(roleInfo -> {
                    var role = new OptInfo.RoleInfo();
                    role.setCode(roleInfo.get(0, Long.class) + "");
                    role.setName(roleInfo.get(1, String.class));
                    return role;
                })
                .collect(Collectors.toSet());
    }

    public Set<IdentOptInfo.GroupInfo> findGroupInfo(Long accountId) {
        var qAccountGroup = QAccountGroup.accountGroup;
        var qGroup = QGroup.group;
        var qGroupNode = QGroupNode.groupNode;
        return sqlBuilder.select(
                qGroup.code,
                qGroup.name,
                qGroupNode.code,
                qGroupNode.name,
                qGroupNode.busCode)
                .from(qAccountGroup)
                .innerJoin(qGroupNode).on(qGroupNode.id.eq(qAccountGroup.relGroupNodeId))
                .innerJoin(qGroup)
                .on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qAccountGroup.relAccountId.eq(accountId))
                .fetch()
                .stream()
                .map(groupInfo ->
                        IdentOptInfo.GroupInfo.builder()
                                .groupCode(groupInfo.get(0, String.class))
                                .groupName(groupInfo.get(1, String.class))
                                .groupNodeCode(groupInfo.get(2, String.class))
                                .groupNodeName(groupInfo.get(3, String.class))
                                .groupNodeBusCode(groupInfo.get(4, String.class))
                                .build())
                .collect(Collectors.toSet());
    }

}