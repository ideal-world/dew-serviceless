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

package idealworld.dew.baas.iam.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import group.idealworld.dew.Dew;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.auth.AccountGroup;
import idealworld.dew.baas.iam.domain.auth.AccountRole;
import idealworld.dew.baas.iam.domain.auth.QAccountGroup;
import idealworld.dew.baas.iam.domain.auth.QAccountRole;
import idealworld.dew.baas.iam.domain.ident.*;
import idealworld.dew.baas.iam.dto.account.*;
import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * Account service.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class AccountService extends IAMBasicService {

    private static final String SK_KIND_VCODE_TMP_REL = "iam:sk-kind:vocde:tmp-rel:";
    private static final String SK_KIND_VCODE_ERROR_TIMES = "iam:sk-kind:vocde:error-times:";

    private static final String BUSINESS_ACCOUNT = "ACCOUNT";
    private static final String BUSINESS_ACCOUNT_IDENT = "ACCOUNT_IDENT";

    @Autowired
    private TenantService tenantService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private RoleService roleService;

    @Transactional
    public Resp<Long> addAccount(AccountAddReq accountAddReq, Long relTenantId) {
        var account = $.bean.copyProperties(accountAddReq, Account.class);
        account.setOpenId($.field.createUUID());
        account.setParentId(Constant.OBJECT_UNDEFINED);
        account.setRelTenantId(relTenantId);
        account.setStatus(CommonStatus.ENABLED);
        return saveEntity(account);
    }

    @Transactional
    public Resp<Void> modifyAccount(Long accountId, AccountModifyReq accountModifyReq, Long relTenantId) {
        var qAccount = QAccount.account;
        if (accountModifyReq.getParentId() != null
                && !checkAccountMembership(accountModifyReq.getParentId(), relTenantId).ok()) {
            return StandardResp.unAuthorized(BUSINESS_ACCOUNT, "父账号Id不合法");
        }
        var accountUpdate = sqlBuilder.update(qAccount)
                .where(qAccount.id.eq(accountId))
                .where(qAccount.relTenantId.eq(relTenantId));
        if (accountModifyReq.getName() != null) {
            accountUpdate.set(qAccount.name, accountModifyReq.getName());
        }
        if (accountModifyReq.getAvatar() != null) {
            accountUpdate.set(qAccount.avatar, accountModifyReq.getAvatar());
        }
        if (accountModifyReq.getParameters() != null) {
            accountUpdate.set(qAccount.parameters, accountModifyReq.getParameters());
        }
        if (accountModifyReq.getParentId() != null) {
            accountUpdate.set(qAccount.parentId, accountModifyReq.getParentId());
        }
        if (accountModifyReq.getStatus() != null) {
            accountUpdate.set(qAccount.status, accountModifyReq.getStatus());
        }
        return updateEntity(accountUpdate);
    }

    public Resp<AccountResp> getAccount(Long accountId, Long relTenantId) {
        var qAccount = QAccount.account;
        return getDTO(sqlBuilder.select(Projections.bean(AccountResp.class,
                qAccount.id,
                qAccount.openId,
                qAccount.name,
                qAccount.avatar,
                qAccount.parameters,
                qAccount.parentId,
                qAccount.status,
                qAccount.relTenantId))
                .from(qAccount)
                .where(qAccount.id.eq(accountId))
                .where(qAccount.relTenantId.eq(relTenantId)));
    }

    public Resp<Page<AccountResp>> pageAccounts(Long pageNumber, Integer pageSize, Long relTenantId) {
        var qAccount = QAccount.account;
        return pageDTOs(sqlBuilder.select(Projections.bean(AccountResp.class,
                qAccount.id,
                qAccount.openId,
                qAccount.name,
                qAccount.avatar,
                qAccount.parameters,
                qAccount.parentId,
                qAccount.status,
                qAccount.relTenantId))
                .from(qAccount)
                .where(qAccount.relTenantId.eq(relTenantId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteAccount(Long accountId, Long relTenantId) {
        var qAccount = QAccount.account;
        return softDelEntity(sqlBuilder
                .selectFrom(qAccount)
                .where(qAccount.id.eq(accountId))
                .where(qAccount.relTenantId.eq(relTenantId)));
    }

    private Resp<Void> checkAccountMembership(Long accountId, Long tenantId) {
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

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addAccountIdent(AccountIdentAddReq accountIdentAddReq, Long relTenantId) {
        var qAccountIdent = QAccountIdent.accountIdent;
        if (sqlBuilder.select(qAccountIdent.id)
                .from(qAccountIdent)
                .where(qAccountIdent.id.eq(accountIdentAddReq.getRelAccountId()))
                .where(qAccountIdent.relTenantId.eq(relTenantId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_ACCOUNT_IDENT, "关联账号不合法");
        }
        if (sqlBuilder.select(qAccountIdent.id)
                .from(qAccountIdent)
                .where(qAccountIdent.relTenantId.eq(relTenantId))
                .where(qAccountIdent.kind.eq(accountIdentAddReq.getKind()))
                .where(qAccountIdent.ak.eq(accountIdentAddReq.getAk()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ACCOUNT_IDENT, "账号认证类型与AK已存在");
        }
        var validRuleAndGetValidEndTimeR = tenantService.validRuleAndGetValidEndTime(
                accountIdentAddReq.getKind(),
                accountIdentAddReq.getAk(),
                accountIdentAddReq.getSk(),
                relTenantId);
        if (!validRuleAndGetValidEndTimeR.ok()) {
            return Resp.error(validRuleAndGetValidEndTimeR);
        }
        var processIdentSkR = processIdentSk(accountIdentAddReq.getKind(),
                accountIdentAddReq.getAk(),
                accountIdentAddReq.getSk(),
                relTenantId);
        if (!processIdentSkR.ok()) {
            return StandardResp.error(processIdentSkR);
        }
        var accountIdent = $.bean.copyProperties(accountIdentAddReq, AccountIdent.class);
        accountIdent.setRelTenantId(relTenantId);
        if (accountIdent.getValidStartTime() == null) {
            accountIdent.setValidStartTime(new Date());
        }
        if (accountIdent.getValidEndTime() == null) {
            accountIdent.setValidEndTime(validRuleAndGetValidEndTimeR.getBody());
        }
        accountIdent.setSk(processIdentSkR.getBody());
        return saveEntity(accountIdent);
    }

    @Transactional
    public Resp<Void> modifyAccountIdent(Long accountIdentId, AccountIdentModifyReq accountIdentModifyReq, Long relTenantId) {
        var qAccountIdent = QAccountIdent.accountIdent;
        var accountIdentKindAndAk = sqlBuilder.select(qAccountIdent.kind, qAccountIdent.ak)
                .from(qAccountIdent)
                .where(qAccountIdent.id.eq(accountIdentId))
                .where(qAccountIdent.relTenantId.eq(relTenantId))
                .fetchOne();
        if (accountIdentKindAndAk == null) {
            return StandardResp.unAuthorized(BUSINESS_ACCOUNT_IDENT, "账号认证不合法");
        }
        var accountIdentKind = accountIdentKindAndAk.get(0, AccountIdentKind.class);
        var accountIdentAk = accountIdentModifyReq.getAk() != null ? accountIdentModifyReq.getAk() : accountIdentKindAndAk.get(1, String.class);
        if (sqlBuilder.select(qAccountIdent.id)
                .from(qAccountIdent)
                .where(qAccountIdent.relTenantId.eq(relTenantId))
                .where(qAccountIdent.kind.eq(accountIdentKind))
                .where(qAccountIdent.ak.eq(accountIdentAk))
                .where(qAccountIdent.id.ne(accountIdentId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ACCOUNT_IDENT, "账号认证类型与AK已存在");
        }
        var validRuleAndGetValidEndTimeR = tenantService.validRuleAndGetValidEndTime(
                accountIdentKind,
                accountIdentModifyReq.getAk(),
                accountIdentModifyReq.getSk(),
                relTenantId);
        if (!validRuleAndGetValidEndTimeR.ok()) {
            return Resp.error(validRuleAndGetValidEndTimeR);
        }
        var accountIdentUpdate = sqlBuilder.update(qAccountIdent)
                .where(qAccountIdent.id.eq(accountIdentId))
                .where(qAccountIdent.relTenantId.eq(relTenantId));
        if (accountIdentModifyReq.getAk() != null) {
            accountIdentUpdate.set(qAccountIdent.ak, accountIdentModifyReq.getAk());
        }
        if (accountIdentModifyReq.getSk() != null) {
            var processIdentSkR = processIdentSk(accountIdentKind,
                    accountIdentAk,
                    accountIdentModifyReq.getSk(),
                    relTenantId);
            if (!processIdentSkR.ok()) {
                return StandardResp.error(processIdentSkR);
            }
            accountIdentUpdate.set(qAccountIdent.sk, processIdentSkR.getBody());
        }
        if (accountIdentModifyReq.getValidStartTime() != null) {
            accountIdentUpdate.set(qAccountIdent.validStartTime, accountIdentModifyReq.getValidStartTime());
        }
        if (accountIdentModifyReq.getValidEndTime() != null) {
            accountIdentUpdate.set(qAccountIdent.validEndTime, accountIdentModifyReq.getValidEndTime());
        }
        return updateEntity(accountIdentUpdate);
    }

    public Resp<List<AccountIdentResp>> findAccountIdents(Long relAccountId, Long relTenantId) {
        var qAccountIdent = QAccountIdent.accountIdent;
        return findDTOs(sqlBuilder.select(Projections.bean(AccountIdentResp.class,
                qAccountIdent.kind,
                qAccountIdent.ak,
                qAccountIdent.validStartTime,
                qAccountIdent.validEndTime,
                qAccountIdent.relAccountId))
                .from(qAccountIdent)
                .where(qAccountIdent.relAccountId.eq(relAccountId))
                .where(qAccountIdent.relTenantId.eq(relTenantId))
        );
    }

    @Transactional
    public Resp<Void> deleteAccountIdent(Long accountIdentId, Long relTenantId) {
        var qAccountIdent = QAccountIdent.accountIdent;
        return softDelEntity(sqlBuilder
                .selectFrom(qAccountIdent)
                .where(qAccountIdent.id.eq(accountIdentId))
                .where(qAccountIdent.relTenantId.eq(relTenantId)));
    }

    private Resp<String> processIdentSk(AccountIdentKind identKind, String ak, String sk, Long relTenantId) {
        switch (identKind) {
            case EMAIL:
            case PHONE:
                String tmpSk = Dew.cluster.cache.get(SK_KIND_VCODE_TMP_REL + relTenantId + ":" + ak);
                if (tmpSk == null) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "验证码不存在或已过期");
                }
                if (!tmpSk.equalsIgnoreCase(sk)) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "验证码错误");
                }
                return StandardResp.success("");
            case USERNAME:
                if (StringUtils.isEmpty(sk)) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "密码必填");
                }
                return StandardResp.success($.security.digest.digest(ak + sk, "SHA-512"));
            default:
                return StandardResp.success("");
        }
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addAccountApp(Long accountId, Long relAppId, Long relTenantId) {
        var checkAccountMembershipR = checkAccountMembership(accountId, relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        return saveEntity(AccountApp.builder()
                .relAccountId(accountId)
                .relAppId(relAppId)
                .build());
    }

    @Transactional
    public Resp<Void> deleteAccountApp(Long accountAppId, Long relAppId, Long relTenantId) {
        var qAccountApp = QAccountApp.accountApp;
        var accountApp = sqlBuilder
                .selectFrom(qAccountApp)
                .where(qAccountApp.id.eq(accountAppId))
                .where(qAccountApp.relAppId.eq(relAppId))
                .fetchOne();
        var checkAccountMembershipR = checkAccountMembership(accountApp.getRelAccountId(), relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        return softDelEntity(accountApp);
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addAccountGroup(Long accountId, Long groupNodeId, Long relAppId, Long relTenantId) {
        var checkAccountMembershipR = checkAccountMembership(accountId, relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkGroupNodeMembership = groupService.checkGroupNodeMembership(groupNodeId, relAppId, relTenantId);
        if (!checkGroupNodeMembership.ok()) {
            return Resp.error(checkGroupNodeMembership);
        }
        return saveEntity(AccountGroup.builder()
                .relAccountId(accountId)
                .relGroupNodeId(groupNodeId)
                .build());
    }

    @Transactional
    public Resp<Void> deleteAccountGroup(Long accountGroupId, Long relAppId, Long relTenantId) {
        var qAccountGroup = QAccountGroup.accountGroup;
        var accountGroup = sqlBuilder
                .selectFrom(qAccountGroup)
                .where(qAccountGroup.id.eq(accountGroupId))
                .fetchOne();
        var checkAccountMembershipR = checkAccountMembership(accountGroup.getRelAccountId(), relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkGroupNodeMembership = groupService.checkGroupNodeMembership(accountGroup.getRelGroupNodeId(), relAppId, relTenantId);
        if (!checkGroupNodeMembership.ok()) {
            return Resp.error(checkGroupNodeMembership);
        }
        return softDelEntity(accountGroup);
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addAccountRole(Long accountId, Long RoleId, Long relAppId, Long relTenantId) {
        var checkAccountMembershipR = checkAccountMembership(accountId, relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkRoleMembership = roleService.checkRoleMembership(RoleId, relAppId, relTenantId);
        if (!checkRoleMembership.ok()) {
            return Resp.error(checkRoleMembership);
        }
        return saveEntity(AccountRole.builder()
                .relAccountId(accountId)
                .relRoleId(RoleId)
                .build());
    }

    @Transactional
    public Resp<Void> deleteAccountRole(Long accountRoleId, Long relAppId, Long relTenantId) {
        var qAccountRole = QAccountRole.accountRole;
        var accountRole = sqlBuilder
                .selectFrom(qAccountRole)
                .where(qAccountRole.id.eq(accountRoleId))
                .fetchOne();
        var checkAccountMembershipR = checkAccountMembership(accountRole.getRelAccountId(), relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkRoleMembership = roleService.checkRoleMembership(accountRole.getRelRoleId(), relAppId, relTenantId);
        if (!checkRoleMembership.ok()) {
            return Resp.error(checkRoleMembership);
        }
        return softDelEntity(accountRole);
    }

}
