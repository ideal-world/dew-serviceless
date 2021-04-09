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

package idealworld.dew.serviceless.iam.process.common;

import com.ecfront.dew.common.tuple.Tuple3;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.exception.NotFoundException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.auth.Resource;
import idealworld.dew.serviceless.iam.domain.auth.ResourceSubject;
import idealworld.dew.serviceless.iam.domain.ident.Account;
import idealworld.dew.serviceless.iam.domain.ident.AccountIdent;
import idealworld.dew.serviceless.iam.domain.ident.App;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
import idealworld.dew.serviceless.iam.process.common.dto.account.AccountLoginReq;
import idealworld.dew.serviceless.iam.process.common.dto.account.AccountOAuthLoginReq;
import idealworld.dew.serviceless.iam.process.common.dto.account.AccountRegisterReq;
import idealworld.dew.serviceless.iam.process.common.oauthimpl.PlatformAPI;
import idealworld.dew.serviceless.iam.process.common.oauthimpl.WechatXCXAPI;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth服务.
 *
 * @author gudaoxuri
 */
@Slf4j
public class OAuthProcessor {

    private static final WechatXCXAPI WECHAT_XCXAPI = new WechatXCXAPI();

    public static Future<IdentOptInfo> login(AccountOAuthLoginReq accountOAuthLoginReq, ProcessContext context) {
        return context.sql.getOne(
                "SELECT id FROM %s WHERE open_id = ?",
                App.class, accountOAuthLoginReq.getRelAppCode())
                .compose(appInfo -> {
                    var appId = appInfo.getLong("id");
                    return IAMBasicProcessor.getEnabledTenantIdByAppId(appId, false, context)
                            .compose(tenantId ->
                                    checkAndGetAkSk(accountOAuthLoginReq.getKind(), appId, tenantId, context)
                                            .compose(checkAndGetAkSk -> {
                                                PlatformAPI platformAPI = checkAndGetAkSk._0;
                                                var oauthAk = checkAndGetAkSk._1;
                                                var oauthSk = checkAndGetAkSk._2;
                                                return platformAPI.getUserInfo(accountOAuthLoginReq.getCode(), oauthAk, oauthSk, appId, context)
                                                        .compose(oauthUserInfo -> {
                                                            var accessToken = oauthUserInfo._0;
                                                            var userInfo = oauthUserInfo._1;
                                                            return context.sql.getOne(
                                                                    "SELECT acc.id, acc.status FROM %s AS ident" +
                                                                            " INNER JOIN %s AS acc ON acc.id = ident.rel_account_id" +
                                                                            " WHERE ident.kind = ? AND ident.ak = ? AND ident.rel_tenant_id = ?",
                                                                    AccountIdent.class, Account.class, accountOAuthLoginReq.getKind(), userInfo.getOpenid(), tenantId)
                                                                    .compose(accountInfo -> {
                                                                        Long accountId = null;
                                                                        if (accountInfo != null) {
                                                                            accountId = accountInfo.getLong("id");
                                                                            if (CommonStatus.parse(accountInfo.getString("status")) == CommonStatus.DISABLED) {
                                                                                context.helper.error(new BadRequestException("用户状态异常"));
                                                                            }
                                                                        }
                                                                        if (accountId == null) {
                                                                            log.info("OAuth Register : [{}-{}] {}", tenantId, appId,
                                                                                    JsonObject.mapFrom(accountOAuthLoginReq).toString());
                                                                            return CommonProcessor.registerAccount(AccountRegisterReq.builder()
                                                                                    .name("")
                                                                                    .kind(accountOAuthLoginReq.getKind())
                                                                                    .ak(userInfo.getOpenid())
                                                                                    .sk(accessToken)
                                                                                    .relAppCode(accountOAuthLoginReq.getRelAppCode())
                                                                                    .build(), context)
                                                                                    .onFailure(e -> {
                                                                                        if (e instanceof MySQLException && e.getMessage()
                                                                                                .startsWith("Duplicate entry")) {
                                                                                            log.info("OAuth Login : [{}-{}] {}", tenantId, appId,
                                                                                                    JsonObject.mapFrom(accountOAuthLoginReq).toString());
                                                                                            CommonProcessor.login(AccountLoginReq.builder()
                                                                                                    .kind(accountOAuthLoginReq.getKind())
                                                                                                    .ak(userInfo.getOpenid())
                                                                                                    .sk(accessToken)
                                                                                                    .relAppCode(accountOAuthLoginReq.getRelAppCode())
                                                                                                    .build(), context);
                                                                                        } else {
                                                                                            context.helper.error(e);
                                                                                        }
                                                                                    })
                                                                                    .onSuccess(identOptInfo -> context.helper.success(identOptInfo));
                                                                        } else {
                                                                            log.info("OAuth Login : [{}-{}] {}", tenantId, appId,
                                                                                    JsonObject.mapFrom(accountOAuthLoginReq).toString());
                                                                            return CommonProcessor.login(AccountLoginReq.builder()
                                                                                    .kind(accountOAuthLoginReq.getKind())
                                                                                    .ak(userInfo.getOpenid())
                                                                                    .sk(accessToken)
                                                                                    .relAppCode(accountOAuthLoginReq.getRelAppCode())
                                                                                    .build(), context);
                                                                        }
                                                                    });
                                                        });
                                            }));
                });
    }

    public static Future<String> getAccessToken(AccountIdentKind oauthKind, Long relAppId, Long relTenantId, ProcessContext context) {
        return checkAndGetAkSk(oauthKind, relAppId, relTenantId, context)
                .compose(checkAndGetAkSk -> {
                    PlatformAPI platformAPI = checkAndGetAkSk._0;
                    var oauthAk = checkAndGetAkSk._1;
                    var oauthSk = checkAndGetAkSk._2;
                    return platformAPI.getAccessToken(oauthAk, oauthSk, relAppId, context);
                });
    }

    @SneakyThrows
    private static Future<Tuple3<PlatformAPI, String, String>> checkAndGetAkSk(AccountIdentKind kind, Long appId, Long tenantId,
                                                                               ProcessContext context) {
        PlatformAPI platformAPI;
        switch (kind) {
            case WECHAT_XCX:
                platformAPI = WECHAT_XCXAPI;
                break;
            default:
                throw context.helper.error(new UnAuthorizedException("认证类型不合法"));
        }
        return context.helper.notExistToError(
                context.sql.getOne(
                        "SELECT subject.ak, subject.sk FROM %s AS resource" +
                                " INNER JOIN %s subject ON subject.id = resource.rel_resource_subject_id" +
                                " WHERE ((resource.rel_app_id = ? AND resource.rel_tenant_id = ?)" +
                                " OR (resource.expose_kind = ? AND resource.rel_tenant_id = ?)" +
                                " OR resource.expose_kind = ?)" +
                                " AND subject.kind = ? AND subject.code = ?",
                        Resource.class, ResourceSubject.class,
                        appId, tenantId, ExposeKind.TENANT, tenantId, ExposeKind.GLOBAL, ResourceKind.OAUTH,
                        appId + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                                + ResourceKind.OAUTH.toString().toLowerCase() + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                                + kind.toString()),
                () -> new NotFoundException("找不到对应的OAuth资源主体"))
                .compose(oauthResourceSubject -> {
                    var oauthAk = oauthResourceSubject.getString("ak");
                    var oauthSk = oauthResourceSubject.getString("sk");
                    return context.helper.success(new Tuple3<>(platformAPI, oauthAk, oauthSk));
                });
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

        private String session_key;

    }

}
