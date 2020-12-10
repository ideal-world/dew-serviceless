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
import com.ecfront.dew.common.tuple.Tuple2;
import com.ecfront.dew.common.tuple.Tuple3;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.serviceless.common.enumeration.CommonStatus;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.domain.auth.QResource;
import idealworld.dew.serviceless.iam.domain.auth.QResourceSubject;
import idealworld.dew.serviceless.iam.domain.ident.QAccount;
import idealworld.dew.serviceless.iam.domain.ident.QAccountIdent;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.scene.common.dto.account.AccountLoginReq;
import idealworld.dew.serviceless.iam.scene.common.dto.account.AccountOAuthLoginReq;
import idealworld.dew.serviceless.iam.scene.common.dto.account.AccountRegisterReq;
import idealworld.dew.serviceless.iam.scene.common.service.oauthimpl.PlatformAPI;
import idealworld.dew.serviceless.iam.scene.common.service.oauthimpl.WechatXCXAPI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.NonUniqueObjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class OAuthService extends IAMBasicService {

    @Autowired
    private CommonFunctionService commonFunctionService;
    @Autowired
    private CommonAccountService commonAccountService;
    @Autowired
    private WechatXCXAPI wechatXCXService;

    @Transactional
    public Resp<IdentOptInfo> login(AccountOAuthLoginReq accountOAuthLoginReq) {
        var tenantIdR = commonFunctionService.getEnabledTenantIdByAppId(accountOAuthLoginReq.getRelAppId(), false);
        var checkAndGetAKSKR = checkAndGetAKSK(accountOAuthLoginReq.getKind(), accountOAuthLoginReq.getRelAppId(), tenantIdR.getBody());
        if (!checkAndGetAKSKR.ok()) {
            return Resp.error(checkAndGetAKSKR);
        }
        PlatformAPI platformAPI = checkAndGetAKSKR.getBody()._0;
        var oauthAk = checkAndGetAKSKR.getBody()._1;
        var oauthSk = checkAndGetAKSKR.getBody()._2;
        Resp<Tuple2<String, OAuthUserInfo>> oauthUserInfoR = platformAPI.getUserInfo(accountOAuthLoginReq.getCode(), oauthAk, oauthSk, accountOAuthLoginReq.getRelAppId());
        if (!oauthUserInfoR.ok()) {
            return StandardResp.error(oauthUserInfoR);
        }
        var accessToken = oauthUserInfoR.getBody()._0;
        var userInfo = oauthUserInfoR.getBody()._1;
        var qAccountIdent = QAccountIdent.accountIdent;
        var qAccount = QAccount.account;
        var accountInfo = sqlBuilder.select(qAccount.id, qAccount.status)
                .from(qAccountIdent)
                .innerJoin(qAccount).on(qAccount.id.eq(qAccountIdent.relAccountId))
                .where(qAccountIdent.kind.eq(accountOAuthLoginReq.getKind()))
                .where(qAccountIdent.ak.eq(userInfo.getOpenid()))
                .where(qAccountIdent.relTenantId.eq(tenantIdR.getBody()))
                .fetchOne();
        Long accountId = null;
        if (accountInfo != null) {
            accountId = accountInfo.get(0, Long.class);
            if (accountInfo.get(1, CommonStatus.class) == CommonStatus.DISABLED) {
                return StandardResp.badRequest(BUSINESS_OAUTH, "用户状态异常");
            }
        }
        if (accountId == null) {
            log.info("OAuth Register : [{}-{}] {}", tenantIdR.getBody(), accountOAuthLoginReq.getRelAppId(), $.json.toJsonString(accountOAuthLoginReq));
            try {
                return commonAccountService.register(AccountRegisterReq.builder()
                        .name("")
                        .kind(accountOAuthLoginReq.getKind())
                        .ak(userInfo.getOpenid())
                        .sk(accessToken)
                        .relAppId(accountOAuthLoginReq.getRelAppId())
                        .build());
            } catch (NonUniqueObjectException e) {
                // TODO 确定异常
                log.info("OAuth Login : [{}-{}] {}", tenantIdR.getBody(), accountOAuthLoginReq.getRelAppId(), $.json.toJsonString(accountOAuthLoginReq));
                return commonAccountService.login(AccountLoginReq.builder()
                        .kind(accountOAuthLoginReq.getKind())
                        .ak(userInfo.getOpenid())
                        .sk(accessToken)
                        .relAppId(accountOAuthLoginReq.getRelAppId())
                        .build());
            }
        } else {
            log.info("OAuth Login : [{}-{}] {}", tenantIdR.getBody(), accountOAuthLoginReq.getRelAppId(), $.json.toJsonString(accountOAuthLoginReq));
            return commonAccountService.login(AccountLoginReq.builder()
                    .kind(accountOAuthLoginReq.getKind())
                    .ak(userInfo.getOpenid())
                    .sk(accessToken)
                    .relAppId(accountOAuthLoginReq.getRelAppId())
                    .build());
        }
    }

    public Resp<String> getAccessToken(AccountIdentKind oauthKind, Long relAppId, Long relTenantId) {
        var checkAndGetAKSKR = checkAndGetAKSK(oauthKind, relAppId, relTenantId);
        if (!checkAndGetAKSKR.ok()) {
            return Resp.error(checkAndGetAKSKR);
        }
        PlatformAPI platformAPI = checkAndGetAKSKR.getBody()._0;
        var oauthAk = checkAndGetAKSKR.getBody()._1;
        var oauthSk = checkAndGetAKSKR.getBody()._2;
        return platformAPI.getAccessToken(oauthAk, oauthSk, relAppId);
    }

    private Resp<Tuple3<PlatformAPI, String, String>> checkAndGetAKSK(AccountIdentKind kind, Long appId, Long tenantId) {
        PlatformAPI platformAPI;
        switch (kind) {
            case WECHAT_XCX:
                platformAPI = wechatXCXService;
                break;
            default:
                return StandardResp.badRequest(BUSINESS_OAUTH, "认证类型不合法");
        }
        var qResource = QResource.resource;
        var qResourceSubject = QResourceSubject.resourceSubject;
        var oauthResourceSubject = sqlBuilder.select(qResourceSubject.ak, qResourceSubject.sk)
                .from(qResource)
                .innerJoin(qResourceSubject).on(qResourceSubject.id.eq(qResource.relResourceSubjectId))
                .where((qResource.relAppId.eq(appId).and(qResource.relTenantId.eq(tenantId)))
                        .or(qResource.exposeKind.eq(ExposeKind.TENANT).and(qResource.relTenantId.eq(tenantId)))
                        .or(qResource.exposeKind.eq(ExposeKind.GLOBAL)))
                .where(qResourceSubject.kind.eq(ResourceKind.OAUTH))
                .where(qResourceSubject.code.equalsIgnoreCase(kind.toString()))
                .fetchOne();
        if (oauthResourceSubject == null) {
            return StandardResp.notFound(BUSINESS_OAUTH, "对应的OAuth资源主体不存在");
        }
        var oauthAk = oauthResourceSubject.get(0, String.class);
        var oauthSk = oauthResourceSubject.get(1, String.class);
        return Resp.success(new Tuple3<>(platformAPI, oauthAk, oauthSk));
    }

    /**
     * OAuth user info.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OAuthUserInfo {

        /**
         * 用户唯一标识.
         */
        private String openid;
        /**
         * 同一平台下的多个用户共用一个标识.
         */
        private String unionid;

    }

}
