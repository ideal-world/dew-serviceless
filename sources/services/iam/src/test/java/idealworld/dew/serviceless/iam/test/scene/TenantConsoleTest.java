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

package idealworld.dew.serviceless.iam.test.scene;

import idealworld.dew.serviceless.common.enumeration.CommonStatus;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.scene.systemconsole.dto.TenantAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.account.*;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.app.AppModifyReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.app.AppResp;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.tenant.*;
import idealworld.dew.serviceless.iam.test.BasicTest;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TenantConsoleTest extends BasicTest {

    @BeforeEach
    public void before() {
        loginBySystemAdmin();
    }

    @Order(1)
    @Test
    public void testTenant() {
        // 添加租户
        Assertions.assertTrue(postToEntity("/console/system/tenant", TenantAddReq.builder()
                .name("xyy")
                .build(), Long.class).ok());
        // 修改当前租户
        Assertions.assertTrue(patchToEntity("/console/tenant/tenant", TenantModifyReq.builder()
                .name("xyy_new")
                .allowAccountRegister(true)
                .build(), Void.class).ok());
        // 获取当前租户信息
        var tenantResp = getToEntity("/console/tenant/tenant", TenantResp.class).getBody();
        Assertions.assertEquals("xyy_new", tenantResp.getName());
        Assertions.assertTrue(tenantResp.getAllowAccountRegister());

        // --------------------------------------------------------------------

        // 添加当前租户的认证
        Assertions.assertEquals("租户认证类型已存在", postToEntity("/console/tenant/tenant/ident", TenantIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .validTimeSec(60 * 60 * 24 * 10L)
                .build(), Long.class).getMessage());
        var tenantIdentId = postToEntity("/console/tenant/tenant/ident", TenantIdentAddReq.builder()
                .kind(AccountIdentKind.PHONE)
                .validTimeSec(60 * 60 * 24 * 10L)
                .build(), Long.class).getBody();

        // 修改当前租户的某个认证
        patchToEntity("/console/tenant/tenant/ident/" + tenantIdentId, TenantIdentModifyReq.builder()
                .validAKRuleNote("手机号码校验规则")
                .validAKRule("^[1](([3][0-9])|([4][5-9])|([5][0-3,5-9])|([6][5,6])|([7][0-8])|([8][0-9])|([9][1,8,9]))[0-9]{8}$")
                .build(), Void.class);

        // 获取当前租户的某个认证信息
        var tenantIdentResp = getToEntity("/console/tenant/tenant/ident/" + tenantIdentId, TenantIdentResp.class).getBody();
        Assertions.assertEquals("^[1](([3][0-9])|([4][5-9])|([5][0-3,5-9])|([6][5,6])|([7][0-8])|([8][0-9])|([9][1,8,9]))[0-9]{8}$", tenantIdentResp.getValidAKRule());

        // 获取当前租户的认证列表信息
        var tenantIdentResps = getToPage("/console/tenant/tenant/ident", 1L, 1, TenantIdentResp.class).getBody();
        Assertions.assertEquals(2, tenantIdentResps.getRecordTotal());
        Assertions.assertEquals(2, tenantIdentResps.getPageTotal());
        Assertions.assertEquals(AccountIdentKind.PHONE, tenantIdentResps.getObjects().get(0).getKind());
        tenantIdentResps = getToPage("/console/tenant/tenant/ident", 2L, 1, TenantIdentResp.class).getBody();
        Assertions.assertEquals(2, tenantIdentResps.getRecordTotal());
        Assertions.assertEquals(2, tenantIdentResps.getPageTotal());
        Assertions.assertEquals(AccountIdentKind.USERNAME, tenantIdentResps.getObjects().get(0).getKind());

        // 删除当前租户的某个认证
        delete("/console/tenant/tenant/ident/" + tenantIdentId);
        Assertions.assertFalse(getToEntity("/console/tenant/tenant/ident/" + tenantIdentId, TenantIdentResp.class).ok());

        // --------------------------------------------------------------------

        // 添加当前租户的凭证
        var tenantCertId = postToEntity("/console/tenant/tenant/cert", TenantCertAddReq.builder()
                .category("app")
                .version(2)
                .build(), Long.class).getBody();
        postToEntity("/console/tenant/tenant/cert", TenantCertAddReq.builder()
                .category("pc")
                .version(2)
                .build(), Long.class);
        Assertions.assertFalse(postToEntity("/console/tenant/tenant/cert", TenantCertAddReq.builder()
                .category("app")
                .version(2)
                .build(), Long.class).ok());

        // 修改当前租户的某个凭证
        patchToEntity("/console/tenant/tenant/cert/" + tenantCertId, TenantCertModifyReq.builder()
                .version(3)
                .build(), Void.class);
        Assertions.assertFalse(patchToEntity("/console/tenant/tenant/cert/" + tenantCertId, TenantCertModifyReq.builder()
                .category("pc")
                .build(), Void.class).ok());

        // 获取当前租户的某个凭证信息
        var tenantCertResp = getToEntity("/console/tenant/tenant/cert/" + tenantCertId, TenantCertResp.class).getBody();
        Assertions.assertEquals("app", tenantCertResp.getCategory());
        Assertions.assertEquals(3, tenantCertResp.getVersion());

        // 获取当前租户的认证列表信息
        var tenantCertResps = getToPage("/console/tenant/tenant/cert", 1L, 10, TenantCertResp.class).getBody();
        Assertions.assertEquals(3, tenantCertResps.getRecordTotal());
        Assertions.assertEquals(1, tenantCertResps.getPageTotal());
        Assertions.assertEquals("", tenantCertResps.getObjects().get(0).getCategory());

        // 删除当前租户的某个凭证
        delete("/console/tenant/tenant/cert/" + tenantCertId);
        Assertions.assertFalse(getToEntity("/console/tenant/tenant/cert/" + tenantCertId, TenantCertResp.class).ok());

    }

    @Order(2)
    @Test
    public void testApp() {
        // 添加当前租户的应用
        var appId = postToEntity("/console/tenant/app", AppAddReq.builder()
                .name("testAPP11")
                .build(), Long.class).getBody();
        Assertions.assertTrue(postToEntity("/console/tenant/app", AppAddReq.builder()
                .name("testAPP22")
                .build(), Long.class).ok());
        Assertions.assertEquals("应用名称已存在", postToEntity("/console/tenant/app", AppAddReq.builder()
                .name("testAPP11")
                .build(), Long.class).getMessage());
        Assertions.assertEquals("应用名称已存在", postToEntity("/console/tenant/app", AppAddReq.builder()
                .name(" testapp11 ")
                .build(), Long.class).getMessage());

        // 修改当前租户的某个应用
        Assertions.assertEquals("应用名称已存在", patchToEntity("/console/tenant/app/" + appId, AppModifyReq.builder()
                .name("testAPp22 ")
                .build(), Void.class).getMessage());
        Assertions.assertTrue(patchToEntity("/console/tenant/app/" + appId, AppModifyReq.builder()
                .name("testAPP33")
                .status(CommonStatus.DISABLED)
                .build(), Void.class).ok());

        // 获取当前租户的某个应用信息
        var appResp = getToEntity("/console/tenant/app/" + appId, AppResp.class).getBody();
        Assertions.assertEquals("testAPP33", appResp.getName());
        Assertions.assertEquals(CommonStatus.DISABLED, appResp.getStatus());

        // 获取当前租户的应用列表信息
        var appResps = getToPage("/console/tenant/app", 1L, 10, AppResp.class).getBody();
        Assertions.assertEquals(1, appResps.getPageTotal());
        Assertions.assertTrue(appResps.getObjects().stream().anyMatch(app -> app.getName().equalsIgnoreCase("testapp33")));

    }

    @Order(3)
    @Test
    public void testAccount() {
        // 添加当前租户的账号
        var accountId = postToEntity("/console/tenant/account", AccountAddReq.builder()
                .name("孤岛旭日")
                .build(), Long.class).getBody();

        // 修改当前租户的某个账号
        Assertions.assertTrue(patchToEntity("/console/tenant/account/" + accountId, AccountModifyReq.builder()
                .name("风雨逐梦")
                .build(), Void.class).ok());

        // 获取当前租户的某个账号信息
        var accountResp = getToEntity("/console/tenant/account/" + accountId, AccountResp.class).getBody();
        Assertions.assertEquals("风雨逐梦", accountResp.getName());
        Assertions.assertNotNull(accountResp.getOpenId());

        // 获取当前租户的账号列表信息
        var accountResps = getToPage("/console/tenant/account", 1L, 10, AccountResp.class).getBody();
        Assertions.assertEquals(1, accountResps.getPageTotal());
        Assertions.assertTrue(accountResps.getObjects().stream().anyMatch(account -> account.getName().equals("风雨逐梦")));

        // --------------------------------------------------------------------

        // 添加当前租户某个账号的认证
        Assertions.assertEquals("认证名规则不合法", postToEntity("/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fy")
                .build(), Long.class).getMessage());
        Assertions.assertEquals("认证密钥规则不合法", postToEntity("/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fyzm123")
                .build(), Long.class).getMessage());
        Assertions.assertEquals("认证密钥规则不合法", postToEntity("/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fyzm123")
                .sk("123456")
                .build(), Long.class).getMessage());
        Assertions.assertEquals("认证类型不存在或已禁用", postToEntity("/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.EMAIL)
                .ak("fyzm123")
                .sk("sox3@4352")
                .build(), Long.class).getMessage());
        var accountIdentId = postToEntity("/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fyzm123")
                .sk("sox3@4352")
                .build(), Long.class).getBody();
        Assertions.assertEquals("账号认证类型与AK已存在", postToEntity("/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("fyzm123")
                .sk("sox3@4352")
                .build(), Long.class).getMessage());
        postToEntity("/console/tenant/account/" + accountId + "/ident", AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak("gdxr123")
                .sk("sox3@4352")
                .build(), Long.class);

        // 修改当前租户某个账号的某个认证
        Assertions.assertEquals("账号认证类型与AK已存在", patchToEntity("/console/tenant/account/" + accountId + "/ident/" + accountIdentId, AccountIdentModifyReq.builder()
                .ak("gdxr123 ")
                .build(), Void.class).getMessage());
        Assertions.assertEquals("认证名规则不合法", patchToEntity("/console/tenant/account/" + accountId + "/ident/" + accountIdentId, AccountIdentModifyReq.builder()
                .ak("12")
                .sk("4352")
                .build(), Void.class).getMessage());
        Assertions.assertEquals("认证密钥规则不合法", patchToEntity("/console/tenant/account/" + accountId + "/ident/" + accountIdentId, AccountIdentModifyReq.builder()
                .ak("gdxr456")
                .sk("4352")
                .build(), Void.class).getMessage());
        Assertions.assertTrue(patchToEntity("/console/tenant/account/" + accountId + "/ident/" + accountIdentId, AccountIdentModifyReq.builder()
                .ak("gdxr456")
                .sk("sox3@4352")
                .build(), Void.class).ok());

        // 获取当前租户某个账号的认证列表信息
        var accountIdentResp = getToList("/console/tenant/account/" + accountId + "/ident", AccountIdentResp.class).getBody();
        Assertions.assertEquals(2, accountIdentResp.size());
        Assertions.assertEquals(AccountIdentKind.USERNAME, accountIdentResp.get(0).getKind());
        Assertions.assertEquals("gdxr123", accountIdentResp.get(0).getAk());


        // 删除当前租户某个账号的某个认证
        delete("/console/tenant/account/" + accountId + "/ident/" + accountIdentId);
        accountIdentResp = getToList("/console/tenant/account/" + accountId + "/ident", AccountIdentResp.class).getBody();
        Assertions.assertEquals(1, accountIdentResp.size());


        // --------------------------------------------------------------------

        // 添加当前租户某个账号的关联应用
        Assertions.assertTrue(postToEntity("/console/tenant/account/" + accountId + "/app/1", "", Long.class).ok());
        Assertions.assertFalse(postToEntity("/console/tenant/account/" + accountId + "/app/10", "", Long.class).ok());

        // 删除当前租户某个账号的某个关联应用
        Assertions.assertFalse(delete("/console/tenant/account/" + accountId + "/app/10").ok());

        // --------------------------------------------------------------------

        // 添加当前租户某个账号的关联群组
        Assertions.assertTrue(postToEntity("/console/tenant/account/" + accountId + "/group/1", "", Long.class).ok());
        Assertions.assertFalse(postToEntity("/console/tenant/account/" + accountId + "/group/100", "", Long.class).ok());

        // 删除当前租户某个账号的某个关联群组
        Assertions.assertTrue(delete("/console/tenant/account/" + accountId + "/group/1").ok());
        Assertions.assertFalse(delete("/console/tenant/account/" + accountId + "/group/100").ok());

        // --------------------------------------------------------------------

        // 添加当前租户某个账号的关联角色
        Assertions.assertTrue(postToEntity("/console/tenant/account/" + accountId + "/role/1", "", Long.class).ok());
        Assertions.assertFalse(postToEntity("/console/tenant/account/" + accountId + "/role/100", "", Long.class).ok());

        // 删除当前租户某个账号的某个关联群组
        Assertions.assertTrue(delete("/console/tenant/account/" + accountId + "/role/1").ok());
        Assertions.assertFalse(delete("/console/tenant/account/" + accountId + "/role/100").ok());

        // --------------------------------------------------------------------

        // 删除当前租户的某个账号
        Assertions.assertTrue(delete("/console/tenant/account/" + accountId).ok());
        Assertions.assertFalse(getToEntity("/console/tenant/account/" + accountId, AccountResp.class).ok());


    }

}
