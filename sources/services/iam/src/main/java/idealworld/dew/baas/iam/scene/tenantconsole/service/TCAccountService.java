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

package idealworld.dew.baas.iam.scene.tenantconsole.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.auth.AccountGroup;
import idealworld.dew.baas.iam.domain.auth.AccountRole;
import idealworld.dew.baas.iam.domain.auth.QAccountGroup;
import idealworld.dew.baas.iam.domain.auth.QAccountRole;
import idealworld.dew.baas.iam.domain.ident.*;
import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
import idealworld.dew.baas.iam.scene.common.service.CommonFunctionService;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.account.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 租户控制台下的账号服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class TCAccountService extends IAMBasicService {

    @Autowired
    private CommonFunctionService commonFunctionService;

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
                && !commonFunctionService.checkAccountMembership(accountModifyReq.getParentId(), relTenantId).ok()) {
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
        var deletedAccountR = softDelEntity(sqlBuilder
                .selectFrom(qAccount)
                .where(qAccount.id.eq(accountId))
                .where(qAccount.relTenantId.eq(relTenantId)));
        if (!deletedAccountR.ok()) {
            return deletedAccountR;
        }
        var qAccountIdent = QAccountIdent.accountIdent;
        softDelEntities(sqlBuilder
                .selectFrom(qAccountIdent)
                .where(qAccountIdent.relAccountId.eq(accountId)));
        var qAccountGroup = QAccountGroup.accountGroup;
        softDelEntities(sqlBuilder
                .selectFrom(qAccountGroup)
                .where(qAccountGroup.relAccountId.eq(accountId)));
        var qAccountRole = QAccountRole.accountRole;
        softDelEntities(sqlBuilder
                .selectFrom(qAccountRole)
                .where(qAccountRole.relAccountId.eq(accountId)));
        var qAccountApp = QAccountApp.accountApp;
        softDelEntities(sqlBuilder
                .selectFrom(qAccountApp)
                .where(qAccountApp.relAccountId.eq(accountId)));
        var qAccountBind = QAccountBind.accountBind;
        softDelEntities(sqlBuilder
                .selectFrom(qAccountBind)
                .where(qAccountBind.fromAccountId.eq(accountId)
                        .or(qAccountBind.toAccountId.eq(accountId))));
        return deletedAccountR;
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addAccountIdent(AccountIdentAddReq accountIdentAddReq, Long accountId, Long relTenantId) {
        var qAccountIdent = QAccountIdent.accountIdent;
        if (sqlBuilder.select(qAccountIdent.id)
                .from(qAccountIdent)
                .where(qAccountIdent.id.eq(accountId))
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
        var validRuleAndGetValidEndTimeR = commonFunctionService.validRuleAndGetValidEndTime(
                accountIdentAddReq.getKind(),
                accountIdentAddReq.getAk(),
                accountIdentAddReq.getSk(),
                relTenantId);
        if (!validRuleAndGetValidEndTimeR.ok()) {
            return Resp.error(validRuleAndGetValidEndTimeR);
        }
        var processIdentSkR = commonFunctionService.processIdentSk(accountIdentAddReq.getKind(),
                accountIdentAddReq.getAk(),
                accountIdentAddReq.getSk(),
                null,
                relTenantId);
        if (!processIdentSkR.ok()) {
            return StandardResp.error(processIdentSkR);
        }
        var accountIdent = $.bean.copyProperties(accountIdentAddReq, AccountIdent.class);
        accountIdent.setRelAccountId(accountId);
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
        var validRuleAndGetValidEndTimeR = commonFunctionService.validRuleAndGetValidEndTime(
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
            var processIdentSkR = commonFunctionService.processIdentSk(
                    accountIdentKind,
                    accountIdentAk,
                    accountIdentModifyReq.getSk(),
                    null,
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

    public Resp<List<AccountIdentResp>> findAccountIdents(Long accountId, Long relTenantId) {
        var qAccountIdent = QAccountIdent.accountIdent;
        return findDTOs(sqlBuilder.select(Projections.bean(AccountIdentResp.class,
                qAccountIdent.kind,
                qAccountIdent.ak,
                qAccountIdent.validStartTime,
                qAccountIdent.validEndTime,
                qAccountIdent.relAccountId))
                .from(qAccountIdent)
                .where(qAccountIdent.relAccountId.eq(accountId))
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


    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addAccountApp(Long accountId, Long appId, Long relTenantId) {
        var checkAccountMembershipR = commonFunctionService.checkAccountMembership(accountId, relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkAppMembershipR = commonFunctionService.checkAppMembership(appId, relTenantId);
        if (!checkAppMembershipR.ok()) {
            return Resp.error(checkAppMembershipR);
        }
        return saveEntity(AccountApp.builder()
                .relAccountId(accountId)
                .relAppId(appId)
                .build());
    }

    @Transactional
    public Resp<Void> deleteAccountApp(Long accountAppId, Long relTenantId) {
        var qAccountApp = QAccountApp.accountApp;
        var accountApp = sqlBuilder
                .selectFrom(qAccountApp)
                .where(qAccountApp.id.eq(accountAppId))
                .fetchOne();
        var checkAccountMembershipR = commonFunctionService.checkAccountMembership(accountApp.getRelAccountId(), relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkAppMembershipR = commonFunctionService.checkAppMembership(accountApp.getRelAppId(), relTenantId);
        if (!checkAppMembershipR.ok()) {
            return Resp.error(checkAppMembershipR);
        }
        return softDelEntity(accountApp);
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addAccountGroup(Long accountId, Long groupNodeId, Long relTenantId) {
        var checkAccountMembershipR = commonFunctionService.checkAccountMembership(accountId, relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkGroupNodeMembership = commonFunctionService.checkGroupNodeMembership(groupNodeId, relTenantId);
        if (!checkGroupNodeMembership.ok()) {
            return Resp.error(checkGroupNodeMembership);
        }
        return saveEntity(AccountGroup.builder()
                .relAccountId(accountId)
                .relGroupNodeId(groupNodeId)
                .build());
    }

    @Transactional
    public Resp<Void> deleteAccountGroup(Long accountGroupId, Long relTenantId) {
        var qAccountGroup = QAccountGroup.accountGroup;
        var accountGroup = sqlBuilder
                .selectFrom(qAccountGroup)
                .where(qAccountGroup.id.eq(accountGroupId))
                .fetchOne();
        var checkAccountMembershipR = commonFunctionService.checkAccountMembership(accountGroup.getRelAccountId(), relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkGroupNodeMembership = commonFunctionService.checkGroupNodeMembership(accountGroup.getRelGroupNodeId(), relTenantId);
        if (!checkGroupNodeMembership.ok()) {
            return Resp.error(checkGroupNodeMembership);
        }
        return softDelEntity(accountGroup);
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addAccountRole(Long accountId, Long roleId, Long relTenantId) {
        var checkAccountMembershipR = commonFunctionService.checkAccountMembership(accountId, relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkRoleMembership = commonFunctionService.checkRoleMembership(roleId, relTenantId);
        if (!checkRoleMembership.ok()) {
            return Resp.error(checkRoleMembership);
        }
        return saveEntity(AccountRole.builder()
                .relAccountId(accountId)
                .relRoleId(roleId)
                .build());
    }

    @Transactional
    public Resp<Void> deleteAccountRole(Long accountRoleId, Long relTenantId) {
        var qAccountRole = QAccountRole.accountRole;
        var accountRole = sqlBuilder
                .selectFrom(qAccountRole)
                .where(qAccountRole.id.eq(accountRoleId))
                .fetchOne();
        var checkAccountMembershipR = commonFunctionService.checkAccountMembership(accountRole.getRelAccountId(), relTenantId);
        if (!checkAccountMembershipR.ok()) {
            return Resp.error(checkAccountMembershipR);
        }
        var checkRoleMembership = commonFunctionService.checkRoleMembership(accountRole.getRelRoleId(), relTenantId);
        if (!checkRoleMembership.ok()) {
            return Resp.error(checkRoleMembership);
        }
        return softDelEntity(accountRole);
    }

}
