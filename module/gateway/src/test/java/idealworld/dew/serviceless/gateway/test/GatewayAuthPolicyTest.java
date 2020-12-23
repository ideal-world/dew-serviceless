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
import idealworld.dew.framework.fun.cache.FunCacheClient;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.gateway.GatewayModule;
import idealworld.dew.serviceless.gateway.exchange.GatewayExchangeProcessor;
import idealworld.dew.serviceless.gateway.process.GatewayAuthPolicy;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
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
import java.util.concurrent.CountDownLatch;

public class GatewayAuthPolicyTest extends DewTest {

    static {
        enableRedis();
    }

    private static final String MODULE_NAME = new GatewayModule().getModuleName();
    private static FunCacheClient cacheClient;
    private static FunEventBus eventBus;

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        FunCacheClient.init(MODULE_NAME, vertx, DewConfig.FunConfig.CacheConfig.builder()
                .uri("redis://localhost:" + redisConfig.getFirstMappedPort()).build());
        FunEventBus.init(MODULE_NAME, vertx, DewConfig.FunConfig.EventBusConfig.builder().build())
                .onSuccess(resp -> {
                    cacheClient = FunCacheClient.choose(MODULE_NAME);
                    eventBus = FunEventBus.choose(MODULE_NAME);
                    testContext.completeNow();
                });
    }

    @SneakyThrows
    @Test
    public void testBasic(Vertx vertx, VertxTestContext testContext) {
        var count = new CountDownLatch(1);
        // 先向redis中添加一些资源
        var addAccountAllByRole = cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/**:create",
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
                }).toString());
        var deleteAccountAllByRole = cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/**:delete",
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
                }).toString());
        var deleteAccountIdentAllByRoleAndAccount = cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/ident/**:delete",
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
                }).toString());
        var deleteAccountIdentItemByAccount = cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/ident/1:delete",
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
                }).toString());
        CompositeFuture.all(addAccountAllByRole, deleteAccountAllByRole, deleteAccountIdentAllByRoleAndAccount, deleteAccountIdentItemByAccount)
                .onSuccess(resp -> count.countDown());
        count.await();
        var authPolicy = new GatewayAuthPolicy(MODULE_NAME, 60, 5);
        vertx.setTimer(1000, h -> {
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
            Future.succeededFuture()
                    // 资源不需要认证
                    .compose(resp -> authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/public"), subjectInfo))
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，但没有权限主体
                    .compose(resp -> authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/tenant/account"), new HashMap<>()))
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，但没有匹配到权限主体
                    .compose(resp -> authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/tenant/account"), subjectInfo))
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，且匹配到权限主体：角色r01
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.ROLE).add("r01");
                        return authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/tenant/account"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，且匹配到权限主体：角色r01
                    .compose(resp -> authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/tenant/account/ident/1"), subjectInfo))
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，没有匹配到权限主体
                    .compose(resp -> authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/tenant/account/ident/001"), subjectInfo))
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，没有匹配到权限主体：账号a01
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.ACCOUNT).clear();
                        subjectInfo.get(AuthSubjectKind.ACCOUNT).add("a01");
                        return authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/tenant/account/ident/1"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，且匹配到权限主体：账号a01
                    .compose(resp -> {
                        return authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/tenant/account/ident/2"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    .onSuccess(resp -> testContext.completeNow());
        });
    }

    @SneakyThrows
    @Test
    public void testGroup(Vertx vertx, VertxTestContext testContext) {
        var count = new CountDownLatch(1);
        // 先向redis中添加一些资源
        var addAccountAllByGroup = cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:create",
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
                }).toString());
        var deleteAccountAllByGroup = cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:delete",
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
                }).toString());
        var modifyAccountAllByGroup = cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:modify",
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
                }).toString());
        var patchAccountAllByGroup = cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:patch",
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
                }).toString());
        CompositeFuture.all(addAccountAllByGroup, deleteAccountAllByGroup, modifyAccountAllByGroup, patchAccountAllByGroup)
                .onSuccess(resp -> count.countDown());
        count.await();
        var authPolicy = new GatewayAuthPolicy(MODULE_NAME, 60, 5);
        vertx.setTimer(1000, h -> {
            Map<AuthSubjectKind, List<String>> subjectInfo = new HashMap<>() {
                {
                    put(AuthSubjectKind.GROUP_NODE, new ArrayList<>() {
                        {
                            add("1000010000");
                        }
                    });
                }
            };
            Future.succeededFuture()
                    // NEQ
                    .compose(resp -> authPolicy.authentication(MODULE_NAME, "patch", URIHelper.newURI("http://iam.service/console/app/group"), subjectInfo))
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // EQ
                    .compose(resp -> authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/app/group"), subjectInfo))
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // INCLUDE, 当前级
                    .compose(resp -> authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo))
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // INCLUDE, 下级，拒绝
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("100001000010000");
                        return authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // INCLUDE, 上级
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("10000");
                        return authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // INCLUDE, 非上级，拒绝
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("10001");
                        return authPolicy.authentication(MODULE_NAME, "delete", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // LIKE, 当前级
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("1000010000");
                        return authPolicy.authentication(MODULE_NAME, "modify", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // LIKE, 下级
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("100001000010000");
                        return authPolicy.authentication(MODULE_NAME, "modify", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // LIKE, 非下级，拒绝
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("100001000110000");
                        return authPolicy.authentication(MODULE_NAME, "modify", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // LIKE, 上级，拒绝
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("10000");
                        return authPolicy.authentication(MODULE_NAME, "modify", URIHelper.newURI("http://iam.service/console/app/group/ident/1"), subjectInfo);
                    })
                    .compose(result -> {
                        Assertions.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    .onSuccess(resp -> testContext.completeNow());
        });
    }

    @Test
    public void testDynamicModifyResources(Vertx vertx, VertxTestContext testContext) {
        GatewayExchangeProcessor.init(MODULE_NAME);

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
        Future.succeededFuture()
                .compose(resp ->
                        Future.future(promise -> vertx.setTimer(1000, h -> {
                            promise.complete();
                        }))
                )
                .compose(resp -> GatewayExchangeProcessor.init(MODULE_NAME))
                // 资源不存在
                .compose(resp -> authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/app/ident"), subjectInfo))
                .compose(result -> {
                    Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        // 添加资源
                        cacheClient.set(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/ident/**:create",
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
                                }).toString()))
                .compose(resp -> {
                    // 通知资源变更
                    eventBus.publish("", OptActionKind.CREATE, "eb://iam/resource.http/xxxx",
                            JsonObject.mapFrom(ResourceExchange.builder()
                                    .resourceUri("http://iam.service/console/app/ident/**")
                                    .resourceActionKind(OptActionKind.CREATE.toString().toLowerCase())
                                    .build()).toBuffer(), new HashMap<>());
                    return Future.succeededFuture();
                })
                // 资源已存在且没有匹配主体
                .compose(resp ->
                        Future.future(promise ->
                                vertx.setTimer(2000, h ->
                                        authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/app/ident"), subjectInfo)
                                                .onSuccess(promise::complete)
                                )
                        )
                )
                .compose(result -> {
                    Assertions.assertEquals(AuthResultKind.REJECT, result);
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        // 删除资源
                        cacheClient.del(DewAuthConstant.CACHE_AUTH_POLICY + "http:iam.service/console/app/ident/**:create"))
                .compose(resp -> {
                    // 通知资源变更
                    eventBus.publish("", OptActionKind.DELETE, "eb://iam/resource.http/xxxx",
                            JsonObject.mapFrom(ResourceExchange.builder()
                                    .resourceUri("http://iam.service/console/app/ident/**")
                                    .resourceActionKind(OptActionKind.CREATE.toString().toLowerCase())
                                    .build()).toBuffer(), new HashMap<>());
                    return Future.succeededFuture();
                })
                // 资源已存在且没有匹配主体
                .compose(resp ->
                        Future.future(promise ->
                                vertx.setTimer(2000, h ->
                                        authPolicy.authentication(MODULE_NAME, "create", URIHelper.newURI("http://iam.service/console/app/ident"), subjectInfo)
                                                .onSuccess(promise::complete)
                                )
                        )
                )
                .compose(result -> {
                    Assertions.assertEquals(AuthResultKind.ACCEPT, result);
                    return Future.succeededFuture();
                })
                .onSuccess(resp -> testContext.completeNow())
                .onFailure(e -> testContext.failNow(e));
    }

}
