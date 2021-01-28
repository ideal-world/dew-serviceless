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

package idealworld.dew.serviceless.service.test;

import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.framework.dto.OptActionKind;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class AuthTest extends ITBasicTest {

    @Test
    public void testAuth(Vertx vertx, VertxTestContext testContext) {
        // ================= 没有登录的情况下 =================
        // 注册应用
        try {
            req(OptActionKind.CREATE, "http://iam.http.iam/common/app", new HashMap<String, Object>() {
                {
                    put("appName", "测试应用");
                    put("accountUserName", "gudaoxuri");
                    put("accountPassword", "o2dd^!fdd");
                }
            }, null, null, Map.class);
            Assertions.fail();
        } catch (RTException e) {
            Assertions.assertTrue(e.getMessage().contains("[401]:鉴权错误，没有权限访问对应的资源"));
        }
        // 获取应用
        try {
            req(OptActionKind.CREATE, "http://iam.http.iam/console/app/app/ident", new HashMap<String, Object>() {
                {
                    put("note", "临时认证");
                }
            }, null, 1, Long.class);
            Assertions.fail();
        } catch (RTException e) {
            Assertions.assertTrue(e.getMessage().contains("[401]:认证错误，AppId不合法"));
        }
        // 注册租户
        var appAdminIdentOpt = req(OptActionKind.CREATE, "http://iam.http.iam/common/tenant", new HashMap<String, Object>() {
            {
                put("tenantName", "测试租户2");
                put("appName", "测试应用");
                put("accountUserName", "gudaoxuri");
                put("accountPassword", "o2dd^!fdd");
            }
        }, null, null, Map.class);
        Assertions.assertNotNull(appAdminIdentOpt);
        var appAdminToken = appAdminIdentOpt.get("token").toString();

        // ================= 管理员登录的情况下 =================
        // 注册应用
        appAdminIdentOpt = req(OptActionKind.CREATE, "http://iam.http.iam/common/app", new HashMap<String, Object>() {
            {
                put("appName", "测试应用2");
            }
        }, appAdminToken, null, Map.class);
        Assertions.assertNotNull(appAdminIdentOpt);
        appAdminToken = appAdminIdentOpt.get("token").toString();
        var appCode = appAdminIdentOpt.get("appCode").toString();
        // 获取应用
        var appIdentId = req(OptActionKind.CREATE, "http://iam.http.iam/console/app/app/ident", new HashMap<String, Object>() {
            {
                put("note", "临时认证");
            }
        }, appAdminToken, null, Long.class);
        Assertions.assertNotNull(appIdentId);
        // 获取应用Id
        var appId = req(OptActionKind.FETCH, "http://iam.http.iam/console/tenant/app/id?appCode=" + appCode, null, appAdminToken, null, Long.class);
        // 添加角色定义
        var roleDefId = req(OptActionKind.CREATE, "http://iam.http.iam/console/app/role/def", new HashMap<String, Object>() {
            {
                put("code", "user");
                put("name", "普通用户");
            }
        }, appAdminToken, null, Long.class);
        // 添加角色
        var roleId = req(OptActionKind.CREATE, "http://iam.http.iam/console/app/role", new HashMap<String, Object>() {
            {
                put("relRoleDefId", roleDefId);
            }
        }, appAdminToken, null, Long.class);
        // 注册普通用户
        var accountId = req(OptActionKind.CREATE, "http://iam.http.iam/console/tenant/account", new HashMap<String, Object>() {
            {
                put("name", "孤岛旭日");
            }
        }, appAdminToken, null, Long.class);
        // 添加普通用户的认证
        req(OptActionKind.CREATE, "http://iam.http.iam/console/tenant/account/" + accountId + "/ident", new HashMap<String, Object>() {
            {
                put("kind", "USERNAME");
                put("ak", "gdxr");
                put("sk", "sossd#@x3");
            }
        }, appAdminToken, null, Long.class);
        // 添加普通用户到应用
        req(OptActionKind.CREATE, "http://iam.http.iam/console/tenant/account/" + accountId + "/app/" + appId, "", appAdminToken, null, Long.class);
        // 添加普通用户到角色
        req(OptActionKind.CREATE, "http://iam.http.iam/console/tenant/account/" + accountId + "/role/" + roleId, "", appAdminToken, null, Long.class);

        // ================= 普通用户登录的情况下 =================
        // 普通用户登录
        var userIdentOpt = req(OptActionKind.CREATE, "http://iam.http.iam/common/login", new HashMap<String, Object>() {
            {
                put("ak", "gdxr");
                put("sk", "sossd#@x3");
                put("relAppCode", appCode);
            }
        }, null, null, Map.class);
        var userToken = userIdentOpt.get("token").toString();
        // 获取应用Id
        try {
            req(OptActionKind.FETCH, "http://iam.http.iam/console/tenant/app/id?appCode=" + appCode, null, userToken, null, Long.class);
            Assertions.fail();
        } catch (RTException e) {
            Assertions.assertTrue(e.getMessage().contains("[401]:鉴权错误，没有权限访问对应的资源"));
        }
        try {
            req(OptActionKind.CREATE, "http://iam.http.iam/common/app", new HashMap<String, Object>() {
                {
                    put("appName", "测试应用");
                    put("accountUserName", "gudaoxuri");
                    put("accountPassword", "o2dd^!fdd");
                }
            }, userToken, null, Map.class);
            Assertions.fail();
        } catch (RTException e) {
            Assertions.assertTrue(e.getMessage().contains("[401]:鉴权错误，没有权限访问对应的资源"));
        }
        // 获取应用
        try {
            req(OptActionKind.CREATE, "http://iam.http.iam/console/app/app/ident", new HashMap<String, Object>() {
                {
                    put("note", "临时认证");
                }
            }, userToken, 1, Long.class);
            Assertions.fail();
        } catch (RTException e) {
            Assertions.assertTrue(e.getMessage().contains("[401]:鉴权错误，没有权限访问对应的资源"));
        }
        testContext.completeNow();
    }

}
