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

import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.process.systemconsole.dto.TenantAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.account.*;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppModifyReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppResp;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.*;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TenantConsoleTest extends IAMBasicTest {

    @BeforeEach
    public void before(Vertx vertx, VertxTestContext testContext) {
        loginBySystemAdmin();
        testContext.completeNow();
    }

    @Order(1)
    @Test
    public void testTenant(Vertx vertx, VertxTestContext testContext) {
        // 添加租户
        Assertions.assertNull(req(OptActionKind.CREATE, "/console/system/tenant", TenantAddReq.builder()
                .name("xyy")
                .build(), Long.class)._1);
        // 修改当前租户
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/tenant/tenant", TenantModifyReq.builder()
                .name("xyy_new")
                .allowAccountRegister(true)
                .build(), Void.class)._1);
        // 获取当前租户信息
        var tenantResp = req(OptActionKind.FETCH, "/console/tenant/tenant", null, TenantResp.class)._0;
        Assertions.assertEquals("xyy_new", tenantResp.getName());
        Assertions.assertTrue(tenantResp.getAllowAccountRegister());

        // --------------------------------------------------------------------

        // 添加当前租户的认证
        Assertions.assertEquals("租户认证类型已存在", req(OptActionKind.CREATE, "/console/tenant/tenant/ident", TenantIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .validTimeSec(60 * 60 * 24 * 10L)
                .build(), Long.class)._1.getMessage());
        var tenantIdentId = req(OptActionKind.CREATE, "/console/tenant/tenant/ident", TenantIdentAddReq.builder()
                .kind(AccountIdentKind.PHONE)
                .validTimeSec(60 * 60 * 24 * 10L)
                .build(), Long.class)._0;

        // 修改当前租户的某个认证
        req(OptActionKind.PATCH, "/console/tenant/tenant/ident/" + tenantIdentId, TenantIdentModifyReq.builder()
                .validAkRuleNote("手机号码校验规则")
                .validAkRule("^[1](([3][0-9])|([4][5-9])|([5][0-3,5-9])|([6][5,6])|([7][0-8])|([8][0-9])|([9][1,8,9]))[0-9]{8}$")
                .build(), Void.class);

        // 获取当前租户的某个认证信息
        var tenantIdentResp = req(OptActionKind.FETCH, "/console/tenant/tenant/ident/" + tenantIdentId, null, TenantIdentResp.class)._0;
        Assertions.assertEquals("^[1](([3][0-9])|([4][5-9])|([5][0-3,5-9])|([6][5,6])|([7][0-8])|([8][0-9])|([9][1,8,9]))[0-9]{8}$", tenantIdentResp.getValidAkRule());

        // 获取当前租户的认证列表信息
        var tenantIdentResps = reqPage("/console/tenant/tenant/ident", 1L, 1L, TenantIdentResp.class)._0;
        Assertions.assertEquals(2, tenantIdentResps.getRecordTotal());
        Assertions.assertEquals(2, tenantIdentResps.getPageTotal());
        Assertions.assertEquals(AccountIdentKind.PHONE, tenantIdentResps.getObjects().get(0).getKind());
        tenantIdentResps = reqPage("/console/tenant/tenant/ident", 2L, 1L, TenantIdentResp.class)._0;
        Assertions.assertEquals(2, tenantIdentResps.getRecordTotal());
        Assertions.assertEquals(2, tenantIdentResps.getPageTotal());
        Assertions.assertEquals(AccountIdentKind.USERNAME, tenantIdentResps.getObjects().get(0).getKind());

        // 删除当前租户的某个认证
        req(OptActionKind.DELETE, "/console/tenant/tenant/ident/" + tenantIdentId, null, Void.class);
        Assertions.assertNull(req(OptActionKind.FETCH, "/console/tenant/tenant/ident/" + tenantIdentId, null, TenantIdentResp.class)._0);

        // --------------------------------------------------------------------

        // 添加当前租户的凭证
        var tenantCertId = req(OptActionKind.CREATE, "/console/tenant/tenant/cert", TenantCertAddReq.builder()
                .category("app")
                .version(2)
                .build(), Long.class)._0;
        req(OptActionKind.CREATE, "/console/tenant/tenant/cert", TenantCertAddReq.builder()
                .category("pc")
                .version(2)
                .build(), Long.class);
        Assertions.assertNotNull(req(OptActionKind.CREATE, "/console/tenant/tenant/cert", TenantCertAddReq.builder()
                .category("app")
                .version(2)
                .build(), Long.class)._1);

        // 修改当前租户的某个凭证
        req(OptActionKind.PATCH, "/console/tenant/tenant/cert/" + tenantCertId, TenantCertModifyReq.builder()
                .version(3)
                .build(), Void.class);
        Assertions.assertNotNull(req(OptActionKind.PATCH, "/console/tenant/tenant/cert/" + tenantCertId, TenantCertModifyReq.builder()
                .category("pc")
                .build(), Void.class)._1);

        // 获取当前租户的某个凭证信息
        var tenantCertResp = req(OptActionKind.FETCH, "/console/tenant/tenant/cert/" + tenantCertId, null, TenantCertResp.class)._0;
        Assertions.assertEquals("app", tenantCertResp.getCategory());
        Assertions.assertEquals(3, tenantCertResp.getVersion());

        // 获取当前租户的认证列表信息
        var tenantCertResps = reqPage("/console/tenant/tenant/cert", 1L, 10L, TenantCertResp.class)._0;
        Assertions.assertEquals(3, tenantCertResps.getRecordTotal());
        Assertions.assertEquals(1, tenantCertResps.getPageTotal());
        Assertions.assertEquals("", tenantCertResps.getObjects().get(0).getCategory());

        // 删除当前租户的某个凭证
        req(OptActionKind.DELETE, "/console/tenant/tenant/cert/" + tenantCertId, null, Void.class);
        Assertions.assertNotNull(req(OptActionKind.FETCH, "/console/tenant/tenant/cert/" + tenantCertId, null, TenantCertResp.class)._1);

        testContext.completeNow();
    }

    @Order(2)
    @Test
    public void testApp(Vertx vertx, VertxTestContext testContext) {
        // 添加当前租户的应用
        var appId = req(OptActionKind.CREATE, "/console/tenant/app", AppAddReq.builder()
                .name("testAPP11")
                .build(), Long.class)._0;
        Assertions.assertNull(req(OptActionKind.CREATE, "/console/tenant/app", AppAddReq.builder()
                .name("testAPP22")
                .build(), Long.class)._1);
        Assertions.assertEquals("应用名称已存在", req(OptActionKind.CREATE, "/console/tenant/app", AppAddReq.builder()
                .name("testAPP11")
                .build(), Long.class)._1.getMessage());
        Assertions.assertEquals("应用名称已存在", req(OptActionKind.CREATE, "/console/tenant/app", AppAddReq.builder()
                .name(" testapp11 ")
                .build(), Long.class)._1.getMessage());

        // 修改当前租户的某个应用
        Assertions.assertEquals("应用名称已存在", req(OptActionKind.PATCH, "/console/tenant/app/" + appId, AppModifyReq.builder()
                .name("testAPp22 ")
                .build(), Void.class)._1.getMessage());
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/tenant/app/" + appId, AppModifyReq.builder()
                .name("testAPP33")
                .status(CommonStatus.DISABLED)
                .build(), Void.class)._1);

        // 获取当前租户的某个应用信息
        var appResp = req(OptActionKind.FETCH, "/console/tenant/app/" + appId, null, AppResp.class)._0;
        Assertions.assertEquals("testAPP33", appResp.getName());
        Assertions.assertEquals(CommonStatus.DISABLED, appResp.getStatus());

        // 获取当前租户的应用列表信息
        var appResps = reqPage("/console/tenant/app", 1L, 10L, AppResp.class)._0;
        Assertions.assertEquals(1, appResps.getPageTotal());
        Assertions.assertTrue(appResps.getObjects().stream().anyMatch(app -> app.getName().equalsIgnoreCase("testapp33")));

        testContext.completeNow();
    }

    @Order(3)
    @Test
    public void testAccount(Vertx vertx, VertxTestContext testContext) {
        // 添加当前租户的账号
        var accountId = req(OptActionKind.CREATE, "/console/tenant/account", AccountAddReq.builder()
                .name("孤岛旭日")
                .build(), Long.class)._0;

        // 修改当前租户的某个账号
        Assertions.assertTrue(req(OptActionKind.PATCH, "/console/tenant/account/" + accountId, AccountModifyReq.builder()
                .name("风雨逐梦")
                .build(), Void.class)._1 == null);

        // 获取当前租户的某个账号信息
        var accountResp = req(OptActionKind.FETCH, "/console/tenant/account/" + accountId, null, AccountResp.class)._0;
        Assertions.assertEquals("风雨逐梦", accountResp.getName());
        Assertions.assertNotNull(accountResp.getOpenId());

        // 获取当前租户的账号列表信息
        var accountResps = reqPage("/console/tenant/account", 1L, 10L, AccountResp.class)._0;
        Assertions.assertEquals(1, accountResps.getPageTotal());
        Assertions.assertTrue(accountResps.getObjects().stream().anyMatch(account -> account.getName().equals("风雨逐梦")));

        // --------------------------------------------------------------------

        // 添加当前租户某个账号的认证
        Assertions.assertEquals("认证名规则不合法", req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fy")
                .build(), Long.class)._1.getMessage());
        Assertions.assertEquals("认证密钥规则不合法", req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fyzm123")
                .build(), Long.class)._1.getMessage());
        Assertions.assertEquals("认证密钥规则不合法", req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fyzm123")
                .sk("123456")
                .build(), Long.class)._1.getMessage());
        Assertions.assertEquals("认证类型不存在或已禁用", req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.EMAIL)
                .ak("fyzm123")
                .sk("sox3@4352")
                .build(), Long.class)._1.getMessage());
        var accountIdentId = req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fyzm123")
                .sk("sox3@4352")
                .build(), Long.class)._0;
        Assertions.assertEquals("账号认证类型与AK已存在", req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fyzm123")
                .sk("sox3@4352")
                .build(), Long.class)._1.getMessage());
        req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("gdxr123")
                .sk("sox3@4352")
                .build(), Long.class);

        // 修改当前租户某个账号的某个认证
        Assertions.assertEquals("账号认证类型与AK已存在", req(OptActionKind.PATCH, "/console/tenant/account/" + accountId + "/ident/" + accountIdentId, AccountIdentModifyReq.builder()
                .ak("gdxr123 ")
                .build(), Void.class)._1.getMessage());
        Assertions.assertEquals("认证名规则不合法", req(OptActionKind.PATCH, "/console/tenant/account/" + accountId + "/ident/" + accountIdentId, AccountIdentModifyReq.builder()
                .ak("12")
                .sk("4352")
                .build(), Void.class)._1.getMessage());
        Assertions.assertEquals("认证密钥规则不合法", req(OptActionKind.PATCH, "/console/tenant/account/" + accountId + "/ident/" + accountIdentId, AccountIdentModifyReq.builder()
                .ak("gdxr456")
                .sk("4352")
                .build(), Void.class)._1.getMessage());
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/tenant/account/" + accountId + "/ident/" + accountIdentId, AccountIdentModifyReq.builder()
                .ak("gdxr456")
                .sk("sox3@4352")
                .build(), Void.class)._1);

        // 获取当前租户某个账号的认证列表信息
        var accountIdentResp = reqList("/console/tenant/account/" + accountId + "/ident", AccountIdentResp.class)._0;
        Assertions.assertEquals(2, accountIdentResp.size());
        Assertions.assertEquals(AccountIdentKind.USERNAME, accountIdentResp.get(0).getKind());
        Assertions.assertTrue(accountIdentResp.stream().anyMatch(r -> r.getAk().contains("gdxr456")));


        // 删除当前租户某个账号的某个认证
        req(OptActionKind.DELETE, "/console/tenant/account/" + accountId + "/ident/" + accountIdentId, null, Void.class);
        accountIdentResp = reqList("/console/tenant/account/" + accountId + "/ident", AccountIdentResp.class)._0;
        Assertions.assertEquals(1, accountIdentResp.size());


        // --------------------------------------------------------------------

        // 添加当前租户某个账号的关联应用
        Assertions.assertNull(req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/app/1", "", Long.class)._1);
        Assertions.assertNotNull(req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/app/10", "", Long.class)._1);

        // 删除当前租户某个账号的某个关联应用
        Assertions.assertNotNull(req(OptActionKind.DELETE, "/console/tenant/account/" + accountId + "/app/10", null, Void.class)._1);

        // --------------------------------------------------------------------

        // 添加当前租户某个账号的关联群组
        Assertions.assertNull(req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/group/1", "", Long.class)._1);
        Assertions.assertNotNull(req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/group/100", "", Long.class)._1);

        // 删除当前租户某个账号的某个关联群组
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/tenant/account/" + accountId + "/group/1", null, Void.class)._1);
        Assertions.assertNotNull(req(OptActionKind.DELETE, "/console/tenant/account/" + accountId + "/group/100", null, Void.class)._1);

        // --------------------------------------------------------------------

        // 添加当前租户某个账号的关联角色
        Assertions.assertNull(req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/role/1", "", Long.class)._1);
        Assertions.assertNotNull(req(OptActionKind.CREATE, "/console/tenant/account/" + accountId + "/role/100", "", Long.class)._1);

        // 删除当前租户某个账号的某个关联群组
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/tenant/account/" + accountId + "/role/1", null, Void.class)._1);
        Assertions.assertNotNull(req(OptActionKind.DELETE, "/console/tenant/account/" + accountId + "/role/100", null, Void.class)._1);

        // --------------------------------------------------------------------

        // 删除当前租户的某个账号
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/tenant/account/" + accountId, null, Void.class)._1);
        Assertions.assertNotNull(req(OptActionKind.FETCH, "/console/tenant/account/" + accountId, null, AccountResp.class)._1);

        testContext.completeNow();
    }

}
