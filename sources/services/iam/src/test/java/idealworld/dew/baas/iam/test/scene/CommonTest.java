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

package idealworld.dew.baas.iam.test.scene;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.dto.IdentOptInfo;
import idealworld.dew.baas.common.enumeration.ResourceKind;
import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
import idealworld.dew.baas.iam.scene.appconsole.dto.resource.ResourceAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.resource.ResourceSubjectAddReq;
import idealworld.dew.baas.iam.scene.common.dto.account.*;
import idealworld.dew.baas.iam.scene.common.dto.tenant.TenantRegisterReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.tenant.TenantIdentAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.tenant.TenantModifyReq;
import idealworld.dew.baas.iam.test.BasicTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CommonTest extends BasicTest {

    private Long appId;

    @BeforeEach
    public void before() {
        loginBySystemAdmin();
        // 添加当前租户的应用
        appId = postToEntity("/console/tenant/app", AppAddReq.builder()
                .name("testAPP")
                .build(), Long.class).getBody();
        removeToken();
    }

    @Test
    public void testCommon() {
        // 注册租户
        Assertions.assertEquals("认证名规则不合法", postToEntity("/common/tenant", TenantRegisterReq.builder()
                .tenantName("测试租户")
                .appName("默认应用")
                .accountUserName("jy")
                .accountPassword("sss")
                .build(), IdentOptInfo.class).getMessage());
        var identOptInfo = postToEntity("/common/tenant", TenantRegisterReq.builder()
                .tenantName("测试租户")
                .appName("默认应用")
                .accountUserName("jzy1")
                .accountPassword("si2nc$@2")
                .build(), IdentOptInfo.class).getBody();
        Assertions.assertNotNull(identOptInfo.getAccountCode());
        identOptInfo = postToEntity("/common/tenant", TenantRegisterReq.builder()
                .tenantName("测试租户")
                .appName("默认应用")
                .accountUserName("jzy1")
                .accountPassword("si2nc$@2")
                .build(), IdentOptInfo.class).getBody();
        Assertions.assertNotNull(identOptInfo.getAccountCode());
        // 注册账号
        Assertions.assertTrue(postToEntity("/common/account", AccountRegisterReq.builder()
                .name("孤岛旭日")
                .kind(AccountIdentKind.EMAIL)
                .ak("1")
                .sk("s")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getMessage().contains("对应租户不存在、未启用或禁止注册"));
        loginBySystemAdmin();
        patchToEntity("/console/tenant/tenant", TenantModifyReq.builder()
                .allowAccountRegister(true)
                .build(), Void.class);
        removeToken();
        Assertions.assertEquals("认证类型不存在或已禁用", postToEntity("/common/account", AccountRegisterReq.builder()
                .name("孤岛旭日")
                .kind(AccountIdentKind.EMAIL)
                .ak("1")
                .sk("s")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getMessage());
        Assertions.assertEquals("认证名规则不合法", postToEntity("/common/account", AccountRegisterReq.builder()
                .name("孤岛旭日")
                .kind(AccountIdentKind.USERNAME)
                .ak("1")
                .sk("s")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getMessage());
        Assertions.assertEquals("认证密钥规则不合法", postToEntity("/common/account", AccountRegisterReq.builder()
                .name("ss")
                .kind(AccountIdentKind.USERNAME)
                .ak("gdxr")
                .sk("s")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getMessage());
        identOptInfo = postToEntity("/common/account", AccountRegisterReq.builder()
                .name("ss")
                .kind(AccountIdentKind.USERNAME)
                .ak("gdxr")
                .sk("ownc3@ds")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getBody();
        Assertions.assertEquals("账号凭证[gdxr]已存在", postToEntity("/common/account", AccountRegisterReq.builder()
                .name("ss")
                .kind(AccountIdentKind.USERNAME)
                .ak("gdxr")
                .sk("ownc3@ds")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getMessage());
        Assertions.assertNotNull(identOptInfo.getAccountCode());

        // 登录
        Assertions.assertEquals("登录认证[gdx]不存在或已过期或是（应用关联）账号不存在", postToEntity("/common/login", AccountLoginReq.builder()
                .ak("gdx")
                .sk("ownc3@s")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getMessage());
        Assertions.assertEquals("密码错误", postToEntity("/common/login", AccountLoginReq.builder()
                .ak("gdxr")
                .sk("ownc3@s")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getMessage());
        identOptInfo = postToEntity("/common/login", AccountLoginReq.builder()
                .ak("gdxr")
                .sk("ownc3@ds")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getBody();
        Assertions.assertNotNull(identOptInfo.getAccountCode());


        // 修改账号
        removeToken();
        Assertions.assertEquals("[Internal Server Error]用户未登录", patchToEntity("/common/account", AccountChangeReq.builder()
                .name("xxxx")
                .build(), Void.class).getMessage());
        setToken(identOptInfo.getToken());
        Assertions.assertTrue(patchToEntity("/common/account", AccountChangeReq.builder()
                .name("孤岛旭日")
                .build(), Void.class).ok());

        // 修改账号认证
        removeToken();
        Assertions.assertEquals("[Internal Server Error]用户未登录", patchToEntity("/common/account/ident", AccountIdentChangeReq.builder()
                .ak("gdxrxxx")
                .build(), Void.class).getMessage());
        setToken(identOptInfo.getToken());
        Assertions.assertEquals("认证名规则不合法", patchToEntity("/common/account/ident", AccountIdentChangeReq.builder()
                .ak("dd")
                .sk("sd")
                .build(), Void.class).getMessage());
        Assertions.assertEquals("认证密钥规则不合法", patchToEntity("/common/account/ident", AccountIdentChangeReq.builder()
                .ak("gdxr01")
                .sk("sd")
                .build(), Void.class).getMessage());
        Assertions.assertTrue(patchToEntity("/common/account/ident", AccountIdentChangeReq.builder()
                .ak("gdxr")
                .sk("sds240!#")
                .build(), Void.class).ok());

        // 退出登录
        removeToken();
        Assertions.assertEquals("[Internal Server Error]用户未登录", postToEntity("/common/logout", "", Void.class).getMessage());
        setToken(identOptInfo.getToken());
        Assertions.assertTrue(postToEntity("/common/logout", "", Void.class).ok());

        // 注销账号
        removeToken();
        Assertions.assertEquals("[Internal Server Error]用户未登录", delete("/common/account").getMessage());
        identOptInfo = postToEntity("/common/login", AccountLoginReq.builder()
                .ak("gdxr")
                .sk("sds240!#")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getBody();
        setToken(identOptInfo.getToken());
        Assertions.assertTrue(delete("/common/account").ok());
        Assertions.assertEquals("登录认证[gdxr]不存在或已过期或是（应用关联）账号不存在", postToEntity("/common/login", AccountLoginReq.builder()
                .ak("gdxr")
                .sk("ownc3@ds")
                .relAppId(appId)
                .build(), IdentOptInfo.class).getMessage());

    }

    @Test
    @Disabled
    public void testOAuth() {
        var oauth = $.file.readAllByClassPath("oauth-info.secret", "UTF-8");
        var oauthJson = $.json.toJson(oauth);

        Assertions.assertEquals("对应的OAuth资源主体不存在", postToEntity("/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_XCX)
                .code("ownc3@s")
                .relAppId(1L)
                .build(), IdentOptInfo.class).getMessage());
        loginBySystemAdmin();
        var resourceSubjectId = postToEntity("/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .code(AccountIdentKind.WECHAT_XCX.toString())
                .kind(ResourceKind.OAUTH)
                .name("微信OAuth")
                .uri("oauth://" + AccountIdentKind.WECHAT_XCX.toString())
                .ak(oauthJson.get("wechat-xcx").get("ak").asText())
                .sk(oauthJson.get("wechat-xcx").get("sk").asText())
                .build(), Long.class).getBody();
        Assertions.assertTrue(postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("微信OAuth")
                .uri("oauth://" + AccountIdentKind.WECHAT_XCX.toString())
                .relResourceSubjectId(resourceSubjectId)
                .build(), Long.class).ok());
        removeToken();
        Assertions.assertTrue(postToEntity("/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_XCX)
                .code("ownc3@s")
                .relAppId(1L)
                .build(), IdentOptInfo.class).getMessage().contains("invalid code"));
        // code只能用一次
        /*Assertions.assertEquals("应用[1]对应租户不存在、未启用或禁止注册", postToEntity("/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_MP)
                .code(oauthJson.get("wechat-xcx").get("code").asText())
                .relAppId(1L)
                .build(), IdentOptInfo.class).getMessage());*/
        loginBySystemAdmin();
        patchToEntity("/console/tenant/tenant", TenantModifyReq.builder()
                .allowAccountRegister(true)
                .build(), Void.class);
        removeToken();
        // code只能用一次
       /* Assertions.assertEquals("认证类型不存在或已禁用", postToEntity("/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_MP)
                .code(oauthJson.get("wechat-xcx").get("code").asText())
                .relAppId(1L)
                .build(), IdentOptInfo.class).getMessage());*/
        loginBySystemAdmin();
        Assertions.assertTrue(postToEntity("/console/tenant/tenant/ident", TenantIdentAddReq.builder()
                .kind(AccountIdentKind.WECHAT_XCX)
                .validTimeSec(60 * 60 * 24 * 10L)
                .build(), Long.class).ok());
        removeToken();
        // 注册
        var identOptInfo = postToEntity("/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_XCX)
                .code(oauthJson.get("wechat-xcx").get("code").asText())
                .relAppId(1L)
                .build(), IdentOptInfo.class).getBody();
        Assertions.assertNotNull(identOptInfo.getAccountCode());
        // 登录 code只能用一次
        /*identOptInfo = postToEntity("/common/oauth/login", AccountOAuthLoginReq.builder()
                .kind(AccountIdentKind.WECHAT_MP)
                .code(oauthJson.get("wechat-xcx").get("code").asText())
                .relAppId(1L)
                .build(), IdentOptInfo.class).getBody();
        Assertions.assertNotNull(identOptInfo.getAccountCode());*/

    }

}
