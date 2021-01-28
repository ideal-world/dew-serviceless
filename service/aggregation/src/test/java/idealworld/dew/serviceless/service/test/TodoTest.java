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

import idealworld.dew.framework.dto.OptActionKind;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TodoTest extends ITBasicTest {

    @Test
    public void testTodo(Vertx vertx, VertxTestContext testContext) {
        // 注册租户
        var appAdminIdentOpt = req(OptActionKind.CREATE, "http://iam.http.iam/common/tenant", new HashMap<String, Object>() {
            {
                put("tenantName", "测试租户1");
                put("appName", "测试应用");
                put("accountUserName", "gudaoxuri");
                put("accountPassword", "o2dd^!fdd");
            }
        }, null, null, Map.class);
        Assertions.assertNotNull(appAdminIdentOpt);
        var appAdminToken = appAdminIdentOpt.get("token").toString();
        var appCode = appAdminIdentOpt.get("appCode").toString();
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
        // 普通用户登录
        req(OptActionKind.CREATE, "http://iam.http.iam/common/login", new HashMap<String, Object>() {
            {
                put("ak", "gdxr");
                put("sk", "sossd#@x3");
                put("relAppCode", appCode);
            }
        }, null, null, Map.class);
        // 测试二次登录时Redis中的Token是否正常
        var userIdentOpt = req(OptActionKind.CREATE, "http://iam.http.iam/common/login", new HashMap<String, Object>() {
            {
                put("ak", "gdxr");
                put("sk", "sossd#@x3");
                put("relAppCode", appCode);
            }
        }, null, null, Map.class);
        var userToken = userIdentOpt.get("token").toString();
        // 应用管理员添加数据库资源主体
        try {
            req(OptActionKind.CREATE, "http://iam.http.iam/console/app/resource/subject", new HashMap<String, Object>() {
                {
                    put("codePostfix", "todoDB");
                    put("kind", "RELDB");
                    put("name", "todo应用数据库");
                    put("uri", "mysql://127.0.0.1:" + mysqlConfig.getFirstMappedPort() + "/" + mysqlConfig.getDatabaseName());
                    put("ak", mysqlConfig.getUsername());
                    put("sk", mysqlConfig.getPassword());
                }
            }, null, null, Long.class);
            Assertions.fail();
        } catch (Exception ignored) {
        }
        var resourceSubjectId = req(OptActionKind.CREATE, "http://iam.http.iam/console/app/resource/subject", new HashMap<String, Object>() {
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
        var resourceId = req(OptActionKind.CREATE, "http://iam.http.iam/console/app/resource", new HashMap<String, Object>() {
            {
                put("name", "todoDB ToDo表");
                put("pathAndQuery", "/TODO/**");
                put("relResourceSubjectId", resourceSubjectId);
            }
        }, appAdminToken, null, Long.class);
        // 普通用户可以操作todo表
        req(OptActionKind.CREATE, "http://iam.http.iam/console/app/authpolicy", new HashMap<String, Object>() {
            {
                put("relSubjectKind", "ROLE");
                put("relSubjectIds", roleId);
                put("subjectOperator", "EQ");
                put("relResourceId", resourceId);
                put("resultKind", "ACCEPT");
            }
        }, appAdminToken, null, Void.class);
        // 未注册用户不能添加记录
        try {
            req(OptActionKind.CREATE, "reldb://" + appCode + ".reldb.todoDB", new HashMap<String, Object>() {
                {
                    put("sql", "insert into todo(content, create_user) values (?, ?)");
                    put("parameters", new ArrayList<>() {
                        {
                            add("还款");
                            add(userIdentOpt.get("accountCode"));
                        }
                    });
                }
            }, null, null, Map.class);
            Assertions.fail();
        } catch (Exception ignored) {
        }
        // 注册用户添加记录
        reqList(OptActionKind.CREATE, "reldb://" + appCode + ".reldb.todoDB", new HashMap<String, Object>() {
            {
                put("sql", "create table if not exists todo\n" +
                        "(\n" +
                        "    id bigint auto_increment primary key,\n" +
                        "    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',\n" +
                        "    create_user varchar(255) not null comment '创建者OpenId',\n" +
                        "    content varchar(255) not null comment '内容'\n" +
                        ")\n" +
                        "comment '任务表'");
                put("parameters", new ArrayList<>());
            }
        }, userToken, null, Map.class);
        reqList(OptActionKind.CREATE, "reldb://" + appCode + ".reldb.todoDB", new HashMap<String, Object>() {
            {
                put("sql", "insert into todo(content, create_user) values (?, ?)");
                put("parameters", new ArrayList<>() {
                    {
                        add("还款");
                        add(userIdentOpt.get("accountCode"));
                    }
                });
            }
        }, userToken, null, Map.class);
        // 注册用户获取所有记录
        var todoList = reqList(OptActionKind.FETCH, "reldb://" + appCode + ".reldb.todoDB", new HashMap<String, Object>() {
            {
                put("sql", "select * from todo where create_user = ?");
                put("parameters", new ArrayList<>() {
                    {
                        add(userIdentOpt.get("accountCode"));
                    }
                });
            }
        }, userToken, null, Map.class);
        Assertions.assertEquals(1, todoList.size());
        Assertions.assertEquals("还款", todoList.get(0).get("content"));
        testContext.completeNow();
    }

}
