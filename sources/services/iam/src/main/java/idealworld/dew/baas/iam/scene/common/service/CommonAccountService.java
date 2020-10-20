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

package idealworld.dew.baas.iam.scene.common.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.ident.Account;
import idealworld.dew.baas.iam.domain.ident.AccountIdent;
import idealworld.dew.baas.iam.domain.ident.QAccount;
import idealworld.dew.baas.iam.domain.ident.QAccountIdent;
import idealworld.dew.baas.iam.scene.common.dto.IAMOptInfo;
import idealworld.dew.baas.iam.scene.common.dto.account.AccountChangeReq;
import idealworld.dew.baas.iam.scene.common.dto.account.AccountIdentChangeReq;
import idealworld.dew.baas.iam.scene.common.dto.account.AccountLoginReq;
import idealworld.dew.baas.iam.scene.common.dto.account.AccountRegisterReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 公共账号服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class CommonAccountService extends IAMBasicService {

    @Autowired
    private CommonFunctionService commonFunctionService;

    @Transactional
    public Resp<IAMOptInfo> register(AccountRegisterReq accountRegisterReq) {
        var tenantIdR = commonFunctionService.getEnabledTenantIdByAppId(accountRegisterReq.getRelAppId());
        if (!tenantIdR.ok()) {
            return Resp.error(tenantIdR);
        }
        var qAccountIdent = QAccountIdent.accountIdent;
        if (sqlBuilder.select(qAccountIdent.id)
                .from(qAccountIdent)
                .where(qAccountIdent.relTenantId.eq(tenantIdR.getBody()))
                .where(qAccountIdent.kind.eq(accountRegisterReq.getKind()))
                .where(qAccountIdent.ak.eq(accountRegisterReq.getAk()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ACCOUNT_IDENT, "账号凭证[" + accountRegisterReq.getAk() + "]已存在");
        }
        var accountIdR = saveEntity(Account.builder()
                .name(accountRegisterReq.getName())
                .avatar(accountRegisterReq.getAvatar())
                .parameters(accountRegisterReq.getParameters())
                .openId($.field.createUUID())
                .parentId(Constant.OBJECT_UNDEFINED)
                .status(CommonStatus.ENABLED)
                .relTenantId(tenantIdR.getBody())
                .build());
        if (!accountIdR.ok()) {
            return Resp.error(accountIdR);
        }
        var validRuleAndGetValidEndTimeR = commonFunctionService.validRuleAndGetValidEndTime(
                accountRegisterReq.getKind(),
                accountRegisterReq.getAk(),
                accountRegisterReq.getSk(),
                tenantIdR.getBody());
        if (!validRuleAndGetValidEndTimeR.ok()) {
            return Resp.error(validRuleAndGetValidEndTimeR);
        }
        var processIdentSkR = commonFunctionService.processIdentSk(accountRegisterReq.getKind(),
                accountRegisterReq.getAk(),
                accountRegisterReq.getSk(),
                tenantIdR.getBody());
        if (!processIdentSkR.ok()) {
            return StandardResp.error(processIdentSkR);
        }
        saveEntity(AccountIdent.builder()
                .kind(accountRegisterReq.getKind())
                .ak(accountRegisterReq.getAk())
                .sk(processIdentSkR.getBody())
                .validStartTime(new Date())
                .validEndTime(validRuleAndGetValidEndTimeR.getBody())
                .relAccountId(accountIdR.getBody())
                .relTenantId(tenantIdR.getBody())
                .build());
        return login(AccountLoginReq.builder()
                .ak(accountRegisterReq.getAk())
                .sk(accountRegisterReq.getSk())
                .relAppId(accountRegisterReq.getRelAppId())
                .build());
    }

    @Transactional
    public Resp<IAMOptInfo> login(AccountLoginReq accountLoginReq) {
        // TODO
        return Resp.success(null);
    }

    @Transactional
    public Resp<Void> logout(String token) {
        // TODO
        return Resp.success(null);
    }

    @Transactional
    public Resp<Void> changeIdent(AccountIdentChangeReq accountIdentChangeReq, Long relAccountId) {
        var tenantIdR = commonFunctionService.getEnabledTenantIdByAccountId(relAccountId);
        if (!tenantIdR.ok()) {
            return Resp.error(tenantIdR);
        }
        var validRuleAndGetValidEndTimeR = commonFunctionService.validRuleAndGetValidEndTime(
                accountIdentChangeReq.getKind(),
                accountIdentChangeReq.getAk(),
                accountIdentChangeReq.getSk(),
                tenantIdR.getBody());
        if (!validRuleAndGetValidEndTimeR.ok()) {
            return Resp.error(validRuleAndGetValidEndTimeR);
        }
        var processIdentSkR = commonFunctionService.processIdentSk(accountIdentChangeReq.getKind(),
                accountIdentChangeReq.getAk(),
                accountIdentChangeReq.getSk(),
                tenantIdR.getBody());
        if (!processIdentSkR.ok()) {
            return StandardResp.error(processIdentSkR);
        }
        var qAccountIdent = QAccountIdent.accountIdent;
        return updateEntity(sqlBuilder.update(qAccountIdent)
                .set(qAccountIdent.sk, processIdentSkR.getBody())
                .where(qAccountIdent.relTenantId.eq(tenantIdR.getBody()))
                .where(qAccountIdent.kind.eq(accountIdentChangeReq.getKind()))
                .where(qAccountIdent.ak.eq(accountIdentChangeReq.getAk())));

    }

    @Transactional
    public Resp<Void> changeInfo(AccountChangeReq accountChangeReq, Long relAccountId) {
        var qAccount = QAccount.account;
        var accountUpdate = sqlBuilder.update(qAccount)
                .where(qAccount.id.eq(relAccountId));
        if (accountChangeReq.getName() != null) {
            accountUpdate.set(qAccount.name, accountChangeReq.getName());
        }
        if (accountChangeReq.getAvatar() != null) {
            accountUpdate.set(qAccount.avatar, accountChangeReq.getAvatar());
        }
        if (accountChangeReq.getParameters() != null) {
            accountUpdate.set(qAccount.parameters, accountChangeReq.getParameters());
        }
        return updateEntity(accountUpdate);
    }

    @Transactional
    public Resp<Void> unRegister(Long relAccountId) {
        var qAccount = QAccount.account;
        var accountUpdate = sqlBuilder.update(qAccount)
                .set(qAccount.status, CommonStatus.DISABLED)
                .where(qAccount.id.eq(relAccountId));
        return updateEntity(accountUpdate);
    }

}
