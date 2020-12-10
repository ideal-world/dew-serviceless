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
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.enumeration.CommonStatus;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.scene.common.dto.account.AccountChangeReq;
import idealworld.dew.serviceless.iam.scene.common.dto.account.AccountIdentChangeReq;
import idealworld.dew.serviceless.iam.scene.common.dto.account.AccountLoginReq;
import idealworld.dew.serviceless.iam.scene.common.dto.account.AccountRegisterReq;
import idealworld.dew.serviceless.iam.util.KeyHelper;
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
    private IAMConfig iamConfig;
    @Autowired
    private CommonFunctionService commonFunctionService;

    @Transactional
    public Resp<IdentOptInfo> register(AccountRegisterReq accountRegisterReq) {
        var tenantIdR = commonFunctionService.getEnabledTenantIdByAppId(accountRegisterReq.getRelAppId(), true);
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
        var account = Account.builder()
                .name(accountRegisterReq.getName())
                .avatar(accountRegisterReq.getAvatar())
                .parameters(accountRegisterReq.getParameters())
                .openId($.field.createUUID())
                .parentId(Constant.OBJECT_UNDEFINED)
                .status(CommonStatus.ENABLED)
                .relTenantId(tenantIdR.getBody())
                .build();
        var accountIdR = saveEntity(account);
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
                accountRegisterReq.getRelAppId(),
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
        saveEntity(AccountApp.builder()
                .relAccountId(account.getId())
                .relAppId(accountRegisterReq.getRelAppId())
                .build());
        return loginWithoutAuth(account.getId(), account.getOpenId(), accountRegisterReq.getAk(), accountRegisterReq.getRelAppId(), tenantIdR.getBody());
    }

    @Transactional
    public Resp<IdentOptInfo> login(AccountLoginReq accountLoginReq) {
        var tenantIdR = commonFunctionService.getEnabledTenantIdByAppId(accountLoginReq.getRelAppId(), false);
        if (!tenantIdR.ok()) {
            return Resp.error(tenantIdR);
        }
        log.info("login : [{}-{}] kind = {}, ak = {}", tenantIdR.getBody(), accountLoginReq.getRelAppId(), accountLoginReq.getKind(), accountLoginReq.getAk());
        var qAccount = QAccount.account;
        var qAccountApp = QAccountApp.accountApp;
        var qAccountIdent = QAccountIdent.accountIdent;
        var qTenantIdent = QTenantIdent.tenantIdent;
        var now = new Date();
        var accountInfo = sqlBuilder
                .select(qAccountIdent.sk, qAccount.id, qAccount.openId)
                .from(qAccountIdent)
                .innerJoin(qAccountApp).on(qAccountApp.relAccountId.eq(qAccountIdent.relAccountId))
                .innerJoin(qTenantIdent).on(qAccountIdent.relTenantId.eq(qTenantIdent.relTenantId))
                .innerJoin(qAccount).on(qAccountIdent.relAccountId.eq(qAccount.id))
                .where(qTenantIdent.kind.eq(accountLoginReq.getKind()))
                .where(qAccountIdent.kind.eq(accountLoginReq.getKind()))
                .where(qAccountIdent.ak.eq(accountLoginReq.getAk()))
                .where(qAccountIdent.validStartTime.before(now))
                .where(qAccountIdent.validEndTime.after(now))
                .where(qAccount.relTenantId.eq(tenantIdR.getBody()))
                .where(qAccount.status.eq(CommonStatus.ENABLED))
                .fetchOne();
        if (accountInfo == null) {
            log.warn("Login Fail: [{}-{}] AK {} doesn't exist or has expired or account doesn't exist", tenantIdR.getBody(), accountLoginReq.getRelAppId(), accountLoginReq.getAk());
            return StandardResp.notFound(BUSINESS_ACCOUNT, "登录认证[%s]不存在或已过期或是（应用关联）账号不存在", accountLoginReq.getAk());
        }
        var identSk = accountInfo.get(0, String.class);
        var accountId = accountInfo.get(1, Long.class);
        var openId = accountInfo.get(2, String.class);
        return login(accountId, openId, accountLoginReq.getKind(), accountLoginReq.getAk(), accountLoginReq.getSk(), identSk, accountLoginReq.getRelAppId(), tenantIdR.getBody());
    }

    public Resp<IdentOptInfo> login(Long accountId, String openId, AccountIdentKind kind, String ak, String inputSk, String persistedSk, Long appId, Long tenantId) {
        var validateR = validateSK(kind, ak, inputSk, persistedSk, appId, tenantId);
        if (!validateR.ok()) {
            log.warn("Login Fail: [{}-{}] SK {} error", tenantId, appId, ak);
            return StandardResp.error(validateR);
        }
        return loginWithoutAuth(accountId, openId, ak, appId, tenantId);
    }

    protected Resp<IdentOptInfo> loginWithoutAuth(Long accountId, String openId, String ak, Long appId, Long tenantId) {
        log.info("Login Success:  [{}-{}] ak {}", tenantId, appId, ak);
        String token = KeyHelper.generateToken();
        var optInfo = new IdentOptCacheInfo();
        optInfo.setAccountCode(openId);
        optInfo.setToken(token);
        // TODO
        optInfo.setTokenKind(IdentOptCacheInfo.DEFAULT_TOKEN_KIND_FLAG);
        optInfo.setRoleInfo(commonFunctionService.findRoleInfo(accountId));
        optInfo.setGroupInfo(commonFunctionService.findGroupInfo(accountId));
        optInfo.setAppId(appId);
        optInfo.setTenantId(tenantId);
        Dew.auth.setOptInfo(optInfo);
        return StandardResp.success(new IdentOptInfo()
                .setAccountCode(optInfo.getAccountCode())
                .setToken(optInfo.getToken())
                .setRoleInfo(optInfo.getRoleInfo())
                .setGroupInfo(optInfo.getGroupInfo()));
    }

    @Transactional
    public Resp<Void> logout(String token, String relOpenId) {
        log.info("Logout Account {} by token {}", relOpenId, token);
        Dew.auth.removeOptInfo(token);
        return StandardResp.success(null);
    }

    @Transactional
    public Resp<Void> changeIdent(AccountIdentChangeReq accountIdentChangeReq, String relOpenId, Long relAppId) {
        var tenantIdR = commonFunctionService.getEnabledTenantIdByOpenId(relOpenId);
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
                relAppId,
                tenantIdR.getBody());
        if (!processIdentSkR.ok()) {
            return StandardResp.error(processIdentSkR);
        }
        var qAccountIdent = QAccountIdent.accountIdent;
        return updateEntity(sqlBuilder.update(qAccountIdent)
                .set(qAccountIdent.sk, processIdentSkR.getBody())
                .where(qAccountIdent.relTenantId.eq(tenantIdR.getBody()))
                .where(qAccountIdent.ak.eq(accountIdentChangeReq.getAk())));

    }

    @Transactional
    public Resp<Void> changeInfo(AccountChangeReq accountChangeReq, String relOpenId) {
        var qAccount = QAccount.account;
        var accountUpdate = sqlBuilder.update(qAccount)
                .where(qAccount.openId.eq(relOpenId));
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
    public Resp<Void> unRegister(String relOpenId) {
        var qAccount = QAccount.account;
        var accountUpdate = sqlBuilder.update(qAccount)
                .set(qAccount.status, CommonStatus.DISABLED)
                .where(qAccount.openId.eq(relOpenId));
        return updateEntity(accountUpdate);
    }

    private Resp<Void> validateSK(AccountIdentKind identKind,
                                  String ak, String inputSk, String storageSk, Long relAppId, Long tenantId) {
        switch (identKind) {
            case EMAIL:
            case PHONE:
                String tmpSk = Dew.cluster.cache.get(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + tenantId + ":" + ak);
                if (tmpSk == null) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_CERT, "验证码不存在或已过期，请重新获取");
                }
                if (tmpSk.equalsIgnoreCase(inputSk)) {
                    Dew.cluster.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + tenantId + ":" + ak);
                    Dew.cluster.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_ERROR_TIMES + tenantId + ":" + ak);
                    return StandardResp.success(null);
                }
                if (Dew.cluster.cache.incrBy(IAMConstant.CACHE_ACCOUNT_VCODE_ERROR_TIMES + tenantId + ":" + ak, 1)
                        >= iamConfig.getSecurity().getAccountVCodeMaxErrorTimes()) {
                    Dew.cluster.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_TMP_REL + tenantId + ":" + ak);
                    Dew.cluster.cache.del(IAMConstant.CACHE_ACCOUNT_VCODE_ERROR_TIMES + tenantId + ":" + ak);
                    // TODO 需要特殊标记表示验证码过期
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_CERT, "验证码不存在或已过期，请重新获取");
                }
                return StandardResp.badRequest(BUSINESS_ACCOUNT_CERT, "验证码错误");
            case USERNAME:
                if (!$.security.digest.validate(ak + inputSk, storageSk, "SHA-512")) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_CERT, "密码错误");
                }
                return StandardResp.success(null);
            case WECHAT_XCX:
                String accessToken = Dew.cluster.cache.get(IAMConstant.CACHE_ACCESS_TOKEN + relAppId + ":" + identKind.toString());
                if (accessToken == null) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "Access Token不存在");
                }
                if (!accessToken.equalsIgnoreCase(inputSk)) {
                    return StandardResp.badRequest(BUSINESS_ACCOUNT_IDENT, "Access Token错误");
                }
                return StandardResp.success(null);
            default:
                return StandardResp.success(null);
        }
    }

}
