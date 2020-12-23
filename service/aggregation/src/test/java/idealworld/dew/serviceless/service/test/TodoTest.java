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

package idealworld.dew.serviceless.service.test;

import com.ecfront.dew.common.$;
import idealworld.dew.framework.dto.OptActionKind;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TodoTest extends ITBasicTest {

    private static final Integer APP_ID = 2;

    private String publicKey;

    private String encrypt(String sql) {
        return $.security.encodeBytesToBase64($.security.asymmetric.encrypt(sql.getBytes(), $.security.asymmetric.getPublicKey(publicKey, "RSA"), 1024, "RSA/ECB/OAEPWithSHA1AndMGF1Padding"));
    }

    @Test
    public void testTodo(Vertx vertx, VertxTestContext testContext) {
        // 注册租户
        var appAdminIdentOpt = req(OptActionKind.CREATE, "http://iam/common/tenant", new HashMap<String, Object>() {
            {
                put("tenantName", "测试租户1");
                put("appName", "测试应用");
                put("accountUserName", "gudaoxuri");
                put("accountPassword", "o2dd^!fdd");
            }
        }, null, null, Map.class);
        Assertions.assertNotNull(appAdminIdentOpt);
        var appAdminToken = appAdminIdentOpt.get("token").toString();
        // 获取当前应用的公钥
        publicKey = req(OptActionKind.FETCH, "http://iam/console/app/app/publicKey", "", appAdminToken, null, String.class);
        // 添加角色定义
        var roleDefId = req(OptActionKind.CREATE, "http://iam/console/app/role/def", new HashMap<String, Object>() {
            {
                put("code", "user");
                put("name", "普通用户");
            }
        }, appAdminToken, null, Long.class);
        // 添加角色
        var roleId = req(OptActionKind.CREATE, "http://iam/console/app/role", new HashMap<String, Object>() {
            {
                put("relRoleDefId", roleDefId);
            }
        }, appAdminToken, null, Long.class);
        // 注册普通用户
        var accountId = req(OptActionKind.CREATE, "http://iam/console/tenant/account", new HashMap<String, Object>() {
            {
                put("name", "孤岛旭日");
            }
        }, appAdminToken, null, Long.class);
        // 添加普通用户的认证
        req(OptActionKind.CREATE, "http://iam/console/tenant/account/" + accountId + "/ident", new HashMap<String, Object>() {
            {
                put("kind", "USERNAME");
                put("ak", "gdxr");
                put("sk", "sossd#@x3");
            }
        }, appAdminToken, null, Long.class);
        // 添加普通用户到应用
        req(OptActionKind.CREATE, "http://iam/console/tenant/account/" + accountId + "/app/" + APP_ID, "", appAdminToken, null, Long.class);
        // 添加普通用户到角色
        req(OptActionKind.CREATE, "http://iam/console/tenant/account/" + accountId + "/role/" + roleId, "", appAdminToken, null, Long.class);
        // 普通用户登录
        var userIdentOpt = req(OptActionKind.CREATE, "http://iam/common/login", new HashMap<String, Object>() {
            {
                put("ak", "gdxr");
                put("sk", "sossd#@x3");
                put("relAppId", APP_ID);
            }
        }, null, null, Map.class);
        var userToken = userIdentOpt.get("token").toString();
        // 应用管理员添加数据库资源主体
        try {
            req(OptActionKind.CREATE, "http://iam/console/app/resource/subject", new HashMap<String, Object>() {
                {
                    put("codePostfix", "todoDB");
                    put("kind", "RELDB");
                    put("name", "todo应用数据库");
                    put("uri", "mysql://127.0.0.1:" + mysqlConfig.getFirstMappedPort() + "/" + mysqlConfig.getDatabaseName());
                    put("ak", mysqlConfig.getUsername());
                    put("sk", mysqlConfig.getPassword());
                }
            }, null, null, Map.class);
            Assertions.fail();
        } catch (Exception ignored) {
        }
        var resourceSubjectId = req(OptActionKind.CREATE, "http://iam/console/app/resource/subject", new HashMap<String, Object>() {
            {
                put("codePostfix", "todoDB");
                put("kind", "RELDB");
                put("name", "todo应用数据库");
                put("uri", "mysql://127.0.0.1:" + mysqlConfig.getFirstMappedPort() + "/" + mysqlConfig.getDatabaseName());
                put("ak", mysqlConfig.getUsername());
                put("sk", mysqlConfig.getPassword());
            }
        }, appAdminToken, null, Long.class);
        // 应用管理员添加数据库资源
        var resourceId = req(OptActionKind.CREATE, "http://iam/console/app/resource", new HashMap<String, Object>() {
            {
                put("name", "todoDB Task表");
                put("pathAndQuery", "/task");
                put("relResourceSubjectId", resourceSubjectId);
            }
        }, appAdminToken, null, Long.class);
        // 普通用户可以操作task表
        req(OptActionKind.CREATE, "http://iam/console/app/authpolicy", new HashMap<String, Object>() {
            {
                put("relSubjectKind", "ROLE");
                put("relSubjectIds", roleId);
                put("subjectOperator", "EQ");
                put("relResourceId", resourceId);
                put("resultKind", "ACCEPT");
            }
        }, appAdminToken, null, String.class);
        // 未注册用户不能添加记录
        try {
            req(OptActionKind.CREATE, "reldb://" + APP_ID + ".reldb.todoDB", new HashMap<String, Object>() {
                {
                    put("sql", encrypt("insert into task(content, create_user, update_user) values (?, ?, ?)"));
                    put("parameters", new ArrayList<>() {
                        {
                            add(userIdentOpt.get("accountCode"));
                            add(userIdentOpt.get("accountCode"));
                            add("还款");
                        }
                    });
                }
            }, null, null, Map.class);
            Assertions.fail();
        } catch (Exception ignored) {
        }
        // 注册用户添加记录
        reqList(OptActionKind.CREATE, "reldb://" + APP_ID + ".reldb.todoDB", new HashMap<String, Object>() {
            {
                put("sql", encrypt("insert into task(content, create_user, update_user) values (?, ?, ?)"));
                put("parameters", new ArrayList<>() {
                    {
                        add("还款");
                        add(userIdentOpt.get("accountCode"));
                        add(userIdentOpt.get("accountCode"));
                    }
                });
            }
        }, userToken, null, Map.class);
        // 注册用户获取所有记录
        var taskList = reqList(OptActionKind.FETCH, "reldb://" + APP_ID + ".reldb.todoDB", new HashMap<String, Object>() {
            {
                put("sql", encrypt("select * from task where create_user = ?"));
                put("parameters", new ArrayList<>() {
                    {
                        add(userIdentOpt.get("accountCode"));
                    }
                });
            }
        }, userToken, null, Map.class);
        Assertions.assertEquals(1, taskList.size());
        Assertions.assertEquals("还款", taskList.get(0).get("content"));
        testContext.completeNow();
    }

}
