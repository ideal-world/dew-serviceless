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

package idealworld.dew.serviceless.iam.test;

import com.ecfront.dew.common.$;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.ResourceAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.ResourceSubjectAddReq;
import idealworld.dew.serviceless.iam.process.common.dto.account.*;
import idealworld.dew.serviceless.iam.process.common.dto.tenant.TenantRegisterReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.TenantIdentAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.TenantModifyReq;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CommonTest extends IAMBasicTest {

    private Long appId;

    @BeforeEach
    public void before(Vertx vertx, VertxTestContext testContext) {
        loginBySystemAdmin();
        // 添加当前租户的应用
        appId = req(OptActionKind.CREATE, "/console/tenant/app", AppAddReq.builder()
                .name("testAPP")
                .build(), Long.class)._0;
        removeToken();
        testContext.completeNow();
    }

    @Test
    public void testCommon(Vertx vertx, VertxTestContext testContext) {
        // 注册租户
        Assertions.assertEquals("认证名规则不合法", req(OptActionKind.CREATE, "/common/tenant", TenantRegisterReq.builder()
                .tenantName("测试租户")
                .appName("默认应用")
                .accountUserName("jy")
                .accountPassword("sss")
                .build(), IdentOptInfo.class)._1.getMessage());
        var identOptInfo = req(OptActionKind.CREATE, "/common/tenant", TenantRegisterReq.builder()
                .tenantName("测试租户")
                .appName("默认应用")
                .accountUserName("jzy1")
                .accountPassword("si2nc$@2")
                .build(), IdentOptInfo.class)._0;
        Assertions.assertNotNull(identOptInfo.getAccountCode());
        identOptInfo = req(OptActionKind.CREATE, "/common/tenant", TenantRegisterReq.builder()
                .tenantName("测试租户")
                .appName("默认应用")
                .accountUserName("jzy1")
                .accountPassword("si2nc$@2")
                .build(), IdentOptInfo.class)._0;
        Assertions.assertNotNull(identOptInfo.getAccountCode());
        // 注册账号
        loginBySystemAdmin();
        req(OptActionKind.PATCH, "/console/tenant/tenant", TenantModifyReq.builder()
                .allowAccountRegister(false)
                .build(), Void.class);
        Assertions.assertTrue(req(OptActionKind.CREATE, "/common/account", AccountRegisterReq.builder()
                .name("孤岛旭日")
                .kind(AccountIdentKind.EMAIL)
                .ak("1")
                .sk("s")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._1.getMessage().contains("对应租户不存在、未启用或禁止注册"));
        req(OptActionKind.PATCH, "/console/tenant/tenant", TenantModifyReq.builder()
                .allowAccountRegister(true)
                .build(), Void.class);
        removeToken();
        Assertions.assertEquals("认证类型不存在或已禁用", req(OptActionKind.CREATE, "/common/account", AccountRegisterReq.builder()
                .name("孤岛旭日")
                .kind(AccountIdentKind.EMAIL)
                .ak("1")
                .sk("s")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._1.getMessage());
        Assertions.assertEquals("认证名规则不合法", req(OptActionKind.CREATE, "/common/account", AccountRegisterReq.builder()
                .name("孤岛旭日")
                .kind(AccountIdentKind.USERNAME)
                .ak("1")
                .sk("s")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._1.getMessage());
        Assertions.assertEquals("认证密钥规则不合法", req(OptActionKind.CREATE, "/common/account", AccountRegisterReq.builder()
                .name("ss")
                .kind(AccountIdentKind.USERNAME)
                .ak("gdxr")
                .sk("s")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._1.getMessage());
        identOptInfo = req(OptActionKind.CREATE, "/common/account", AccountRegisterReq.builder()
                .name("ss")
                .kind(AccountIdentKind.USERNAME)
                .ak("gdxr")
                .sk("ownc3@ds")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._0;
        Assertions.assertEquals("账号凭证[gdxr]已存在", req(OptActionKind.CREATE, "/common/account", AccountRegisterReq.builder()
                .name("ss")
                .kind(AccountIdentKind.USERNAME)
                .ak("gdxr")
                .sk("ownc3@ds")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._1.getMessage());
        Assertions.assertNotNull(identOptInfo.getAccountCode());

        // 登录
        Assertions.assertEquals("登录认证[gdx]不存在或已过期或是（应用关联）账号不存在", req(OptActionKind.CREATE, "/common/login", AccountLoginReq.builder()
                .ak("gdx")
                .sk("ownc3@s")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._1.getMessage());
        Assertions.assertEquals("用户名或密码错误", req(OptActionKind.CREATE, "/common/login", AccountLoginReq.builder()
                .ak("gdxr")
                .sk("ownc3@s")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._1.getMessage());
        identOptInfo = req(OptActionKind.CREATE, "/common/login", AccountLoginReq.builder()
                .ak("gdxr")
                .sk("ownc3@ds")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._0;
        Assertions.assertNotNull(identOptInfo.getAccountCode());


        // 修改账号
        removeToken();
        Assertions.assertEquals("用户未登录", req(OptActionKind.PATCH, "/common/account", AccountChangeReq.builder()
                .name("xxxx")
                .build(), Void.class)._1.getMessage());
        setToken(identOptInfo.getToken());
        Assertions.assertNull(req(OptActionKind.PATCH, "/common/account", AccountChangeReq.builder()
                .name("孤岛旭日")
                .build(), Void.class)._1);

        // 修改账号认证
        removeToken();
        Assertions.assertEquals("用户未登录", req(OptActionKind.PATCH, "/common/account/ident", AccountIdentChangeReq.builder()
                .ak("gdxrxxx")
                .build(), Void.class)._1.getMessage());
        setToken(identOptInfo.getToken());
        Assertions.assertEquals("用户[" + identOptInfo.getAccountCode() + "]对应的认证方式不存在", req(OptActionKind.PATCH, "/common/account/ident", AccountIdentChangeReq.builder()
                .ak("dd")
                .sk("sd")
                .build(), Void.class)._1.getMessage());
        Assertions.assertEquals("认证密钥规则不合法", req(OptActionKind.PATCH, "/common/account/ident", AccountIdentChangeReq.builder()
                .ak("gdxr")
                .sk("sd")
                .build(), Void.class)._1.getMessage());
        Assertions.assertNull(req(OptActionKind.PATCH, "/common/account/ident", AccountIdentChangeReq.builder()
                .ak("gdxr")
                .sk("sds240!#")
                .build(), Void.class)._1);

        // 退出登录
        removeToken();
        Assertions.assertEquals("用户未登录", req(OptActionKind.DELETE, "/common/logout", null, Void.class)._1.getMessage());
        setToken(identOptInfo.getToken());
        Assertions.assertNotNull(req(OptActionKind.CREATE, "/common/logout", "", Void.class)._1);

        // 注销账号
        removeToken();
        Assertions.assertEquals("用户未登录", req(OptActionKind.DELETE, "/common/account", null, Void.class)._1.getMessage());
        identOptInfo = req(OptActionKind.CREATE, "/common/login", AccountLoginReq.builder()
                .ak("gdxr")
                .sk("sds240!#")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._0;
        setToken(identOptInfo.getToken());
        Assertions.assertNotNull(req(OptActionKind.DELETE, "/common/account", null, Void.class));
        Assertions.assertEquals("登录认证[gdxr]不存在或已过期或是（应用关联）账号不存在", req(OptActionKind.CREATE, "/common/login", AccountLoginReq.builder()
                .ak("gdxr")
                .sk("ownc3@ds")
                .relAppId(appId)
                .build(), IdentOptInfo.class)._1.getMessage());

        testContext.completeNow();
    }

    @Test
    @Disabled
    public void testOAuth(Vertx vertx, VertxTestContext testContext) {
        var oauth = $.file.readAllByClassPath("oauth-info.secret", "UTF-8");
        var oauthJson = $.json.toJson(oauth);

        Assertions.assertEquals("对应的OAuth资源主体不存在", req(OptActionKind.CREATE, "/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_XCX)
                .code("ownc3@s")
                .relAppId(1L)
                .build(), IdentOptInfo.class)._1.getMessage());
        loginBySystemAdmin();
        var resourceSubjectId = req(OptActionKind.CREATE, "/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix(AccountIdentKind.WECHAT_XCX.toString())
                .kind(ResourceKind.OAUTH)
                .name("微信OAuth")
                .uri("oauth://" + AccountIdentKind.WECHAT_XCX.toString())
                .ak(oauthJson.get("wechat-xcx").get("ak").asText())
                .sk(oauthJson.get("wechat-xcx").get("sk").asText())
                .build(), Long.class)._0;
        Assertions.assertNotNull(req(OptActionKind.CREATE, "/console/app/resource", ResourceAddReq.builder()
                .name("微信OAuth")
                .pathAndQuery(AccountIdentKind.WECHAT_XCX.toString())
                .relResourceSubjectId(resourceSubjectId)
                .build(), Long.class)._1);
        removeToken();
        Assertions.assertTrue(req(OptActionKind.CREATE, "/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_XCX)
                .code("ownc3@s")
                .relAppId(1L)
                .build(), IdentOptInfo.class)._1.getMessage().contains("invalid code"));
        // code只能用一次
        /*Assertions.assertEquals("应用[1]对应租户不存在、未启用或禁止注册", req(OptActionKind.CREATE,"/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_MP)
                .code(oauthJson.get("wechat-xcx").get("code").asText())
                .relAppId(1L)
                .build(), IdentOptInfo.class)._1.getMessage());*/
        loginBySystemAdmin();
        req(OptActionKind.PATCH, "/console/tenant/tenant", TenantModifyReq.builder()
                .allowAccountRegister(true)
                .build(), Void.class);
        removeToken();
        // code只能用一次
       /* Assertions.assertEquals("认证类型不存在或已禁用", req(OptActionKind.CREATE,"/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_MP)
                .code(oauthJson.get("wechat-xcx").get("code").asText())
                .relAppId(1L)
                .build(), IdentOptInfo.class)._1.getMessage());*/
        loginBySystemAdmin();
        Assertions.assertNotNull(req(OptActionKind.CREATE, "/console/tenant/tenant/ident", TenantIdentAddReq.builder()
                .kind(AccountIdentKind.WECHAT_XCX)
                .validTimeSec(60 * 60 * 24 * 10L)
                .build(), Long.class));
        removeToken();
        // 注册
        var identOptInfo = req(OptActionKind.CREATE, "/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_XCX)
                .code(oauthJson.get("wechat-xcx").get("code").asText())
                .relAppId(1L)
                .build(), IdentOptInfo.class)._0;
        Assertions.assertNotNull(identOptInfo.getAccountCode());
        // 登录 code只能用一次
        /*identOptInfo = req(OptActionKind.CREATE,"/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_MP)
                .code(oauthJson.get("wechat-xcx").get("code").asText())
                .relAppId(1L)
                .build(), IdentOptInfo.class).getBody();
        Assertions.assertNotNull(identOptInfo.getAccountCode());*/
        testContext.completeNow();
    }

}
