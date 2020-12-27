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

package idealworld.dew.serviceless.gateway.test;

import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectOperatorKind;
import idealworld.dew.framework.fun.auth.dto.ResourceExchange;
import idealworld.dew.framework.fun.auth.exchange.ExchangeHelper;
import idealworld.dew.framework.fun.cache.FunCacheClient;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.gateway.GatewayModule;
import idealworld.dew.serviceless.gateway.process.GatewayAuthPolicy;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GatewayAuthPolicyTest extends DewTest {

    static {
        enableRedis();
    }

    private static final String MODULE_NAME = new GatewayModule().getModuleName();
    private static FunCacheClient cacheClient;
    private static FunEventBus eventBus;

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        await(FunCacheClient.init(MODULE_NAME, vertx, DewConfig.FunConfig.CacheConfig.builder()
                .uri("redis://localhost:" + redisConfig.getFirstMappedPort()).build()));
        await(FunEventBus.init(MODULE_NAME, vertx, DewConfig.FunConfig.EventBusConfig.builder().build()));
        cacheClient = FunCacheClient.choose(MODULE_NAME);
        eventBus = FunEventBus.choose(MODULE_NAME);
        await(ExchangeHelper.loadAndWatchResources(MODULE_NAME, ""));
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testBasic(Vertx vertx, VertxTestContext testContext) {
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resource.http", JsonObject.mapFrom(ResourceExchange.builder()
                .actionKind(OptActionKind.CREATE.toString().toLowerCase())
                .uri("http://iam.service/console/tenant/account")
                .build()).toBuffer(), new HashMap<>());
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resource.http", JsonObject.mapFrom(ResourceExchange.builder()
                .actionKind(OptActionKind.DELETE.toString().toLowerCase())
                .uri("http://iam.service/console/tenant/account/*")
                .build()).toBuffer(), new HashMap<>());
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resource.http", JsonObject.mapFrom(ResourceExchange.builder()
                .actionKind(OptActionKind.DELETE.toString().toLowerCase())
                .uri("http://iam.service/console/tenant/account/ident/*")
                .build()).toBuffer(), new HashMap<>());
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resource.http", JsonObject.mapFrom(ResourceExchange.builder()
                .actionKind(OptActionKind.DELETE.toString().toLowerCase())
                .uri("http://iam.service/console/tenant/account/ident/1")
                .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);
        // 先向redis中添加一些资源
        await(cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account:create",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
                    {
                        put(AuthSubjectOperatorKind.EQ.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.ROLE.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("r01");
                                    }
                                });
                            }
                        });
                    }
                }).toString()));
        await(cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/*:delete",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
                    {
                        put(AuthSubjectOperatorKind.EQ.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.ROLE.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("r01");
                                    }
                                });
                            }
                        });
                    }
                }).toString()));
        await(cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/ident/*:delete",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
                    {
                        put(AuthSubjectOperatorKind.NEQ.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.ROLE.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("r01");
                                    }
                                });
                            }
                        });
                        put(AuthSubjectOperatorKind.EQ.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.ACCOUNT.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("a01");
                                    }
                                });
                            }
                        });
                    }
                }).toString()));
        await(cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/ident/1:delete",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
                    {
                        put(AuthSubjectOperatorKind.NEQ.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.ACCOUNT.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("a01");
                                    }
                                });
                            }
                        });
                    }
                }).toString()));
        var authPolicy = new GatewayAuthPolicy(MODULE_NAME, 60, 5);
        Thread.sleep(1000);
        Map<AuthSubjectKind, List<String>> subjectInfo = new HashMap<>() {
            {
                put(AuthSubjectKind.ACCOUNT, new ArrayList<>() {
                    {
                        add("a00");
                    }
                });
                put(AuthSubjectKind.ROLE, new ArrayList<>() {
                    {
                        add("r00");
                    }
                });
            }
        };
        // 资源不需要认证
        var result = await(authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/public"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        // 资源需要认证，但没有权限主体
        result = await(authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/tenant/account"), new HashMap<>()));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        // 资源需要认证，但没有匹配到权限主体
        result = await(authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/tenant/account"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        // 资源需要认证，且匹配到权限主体：角色r01
        subjectInfo.get(AuthSubjectKind.ROLE).add("r01");
        result = await(authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/tenant/account"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        // 资源需要认证，且匹配到权限主体：角色r01
        result = await(authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/tenant/account/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        // 资源需要认证，没有匹配到权限主体
        result = await(authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/tenant/account/ident/001"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        // 资源需要认证，没有匹配到权限主体：账号a01
        subjectInfo.get(AuthSubjectKind.ACCOUNT).clear();
        subjectInfo.get(AuthSubjectKind.ACCOUNT).add("a01");
        result = await(authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/tenant/account/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        // 资源需要认证，且匹配到权限主体：账号a01
        result = await(authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/tenant/account/ident/2"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testGroup(Vertx vertx, VertxTestContext testContext) {
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resource.http", JsonObject.mapFrom(ResourceExchange.builder()
                .actionKind(OptActionKind.CREATE.toString().toLowerCase())
                .uri("http://iam.service/console/app/group/**")
                .build()).toBuffer(), new HashMap<>());
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resource.http", JsonObject.mapFrom(ResourceExchange.builder()
                .actionKind(OptActionKind.DELETE.toString().toLowerCase())
                .uri("http://iam.service/console/app/group/**")
                .build()).toBuffer(), new HashMap<>());
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resource.http", JsonObject.mapFrom(ResourceExchange.builder()
                .actionKind(OptActionKind.MODIFY.toString().toLowerCase())
                .uri("http://iam.service/console/app/group/**")
                .build()).toBuffer(), new HashMap<>());
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resource.http", JsonObject.mapFrom(ResourceExchange.builder()
                .actionKind(OptActionKind.PATCH.toString().toLowerCase())
                .uri("http://iam.service/console/app/group/**")
                .build()).toBuffer(), new HashMap<>());
        // 先向redis中添加一些资源
        await(cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:create",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
                    {
                        put(AuthSubjectOperatorKind.EQ.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.GROUP_NODE.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("1000010000");
                                    }
                                });
                            }
                        });
                    }
                }).toString()));
        await(cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:delete",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
                    {
                        put(AuthSubjectOperatorKind.INCLUDE.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.GROUP_NODE.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("1000010000");
                                    }
                                });
                            }
                        });
                    }
                }).toString()));
        await(cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:modify",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
                    {
                        put(AuthSubjectOperatorKind.LIKE.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.GROUP_NODE.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("1000010000");
                                    }
                                });
                            }
                        });
                    }
                }).toString()));
        await(cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:patch",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
                    {
                        put(AuthSubjectOperatorKind.NEQ.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.GROUP_NODE.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("1000010000");
                                    }
                                });
                            }
                        });
                    }
                }).toString()));
        var authPolicy = new GatewayAuthPolicy(MODULE_NAME, 60, 5);
        Thread.sleep(1000);
        Map<AuthSubjectKind, List<String>> subjectInfo = new HashMap<>() {
            {
                put(AuthSubjectKind.GROUP_NODE, new ArrayList<>() {
                    {
                        add("1000010000");
                    }
                });
            }
        };
        // NEQ
        var result = await(authPolicy.authentication(MODULE_NAME, "patch", URIHelper.newURI("http://iam.service/console/app/group"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        // EQ
        result = await(authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/app/group"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        // INCLUDE, 当前级
        result = await(authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        // INCLUDE, 下级，拒绝
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("100001000010000");
        result = await(authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        // INCLUDE, 上级
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("10000");
        result = await(authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        // INCLUDE, 非上级，拒绝
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("10001");
        result = await(authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        // LIKE, 当前级
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("1000010000");
        result = await(authPolicy.authentication(MODULE_NAME, "modify", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        // LIKE, 下级
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("100001000010000");
        result = await(authPolicy.authentication(MODULE_NAME, "modify", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        // LIKE, 非下级，拒绝
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("100001000110000");
        result = await(authPolicy.authentication(MODULE_NAME, "modify", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        // LIKE, 上级，拒绝
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("10000");
        result = await(authPolicy.authentication(MODULE_NAME, "modify", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        testContext.completeNow();
    }

    @Test
    public void testDynamicModifyResources(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        Map<AuthSubjectKind, List<String>> subjectInfo = new HashMap<>() {
            {
                put(AuthSubjectKind.ROLE, new ArrayList<>() {
                    {
                        add("r00");
                    }
                });
            }
        };
        var authPolicy = new GatewayAuthPolicy(MODULE_NAME, 60, 5);
        Thread.sleep(1000);
        // 资源不存在
        var result = await(authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/app/ident"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        // 添加资源
        await(cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/ident/**:create",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
                    {
                        put(AuthSubjectOperatorKind.EQ.toString().toLowerCase(), new HashMap<>() {
                            {
                                put(AuthSubjectKind.ROLE.toString().toLowerCase(), new ArrayList<>() {
                                    {
                                        add("r01");
                                    }
                                });
                            }
                        });
                    }
                }).toString()));
        // 通知资源变更
        eventBus.publish("", OptActionKind.CREATE, "eb://iam/resource.http/xxxx",
                JsonObject.mapFrom(ResourceExchange.builder()
                        .uri("http://iam.service/console/app/ident/**")
                        .actionKind(OptActionKind.CREATE.toString().toLowerCase())
                        .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);
        // 资源已存在且没有匹配主体
        result = await(authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/app/ident"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.REJECT, result._0);
        // 删除资源
        await(cacheClient.del(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/ident/**:create"));
        // 通知资源变更
        eventBus.publish("", OptActionKind.DELETE, "eb://iam/resource.http/xxxx",
                JsonObject.mapFrom(ResourceExchange.builder()
                        .uri("http://iam.service/console/app/ident/**")
                        .actionKind(OptActionKind.CREATE.toString().toLowerCase())
                        .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);
        // 资源已存在且没有匹配主体
        result = await(authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/app/ident"), subjectInfo));
        Assertions.assertEquals(AuthResultKind.ACCEPT, result._0);
        testContext.completeNow();
    }

}
