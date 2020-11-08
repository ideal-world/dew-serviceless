package idealworld.dew.baas.gateway.test;

import com.ecfront.dew.common.$;
import com.google.common.collect.Lists;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.AuthActionKind;
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.baas.gateway.GatewayConfig;
import idealworld.dew.baas.gateway.exchange.ExchangeData;
import idealworld.dew.baas.gateway.exchange.ExchangeProcessor;
import idealworld.dew.baas.gateway.process.ReadonlyAuthPolicy;
import idealworld.dew.baas.gateway.test.helper.BasicTest;
import idealworld.dew.baas.gateway.test.helper.RedisTestHelper;
import idealworld.dew.baas.gateway.util.RedisClient;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestAuthPolicy extends BasicTest {

    @Before
    public void before(TestContext testContext) {
        RedisTestHelper.start();
        RedisClient.init(rule.vertx(), GatewayConfig.RedisConfig.builder()
                .uri("redis://localhost:6379").build());
    }

    @Test
    public void testBasic(TestContext testContext) {
        Async async = testContext.async(2);
        // 先向redis中添加一些资源
        var addAccountAllByRole = RedisClient.set(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/**:create",
                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
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
                }));
        var deleteAccountAllByRole = RedisClient.set(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/**:delete",
                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
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
                }));
        var deleteAccountIdentAllByRoleAndAccount = RedisClient.set(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/ident/**:delete",
                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
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
                }));
        var deleteAccountIdentItemByAccount = RedisClient.set(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/tenant/account/ident/1:delete",
                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
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
                }));
        CompositeFuture.all(Lists.newArrayList(addAccountAllByRole, deleteAccountAllByRole, deleteAccountIdentAllByRoleAndAccount, deleteAccountIdentItemByAccount))
                .onSuccess(resp -> async.countDown());

        var authPolicy = new ReadonlyAuthPolicy(60, 5);
        rule.vertx().setTimer(1000, h -> {
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
                    .compose(resp -> authPolicy.authentication(uri("http://iam.service/console/public"), "create", subjectInfo))
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，但没有权限主体
                    .compose(resp -> authPolicy.authentication(uri("http://iam.service/console/tenant/account"), "create", new HashMap<>()))
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，但没有匹配到权限主体
                    .compose(resp -> authPolicy.authentication(uri("http://iam.service/console/tenant/account"), "create", subjectInfo))
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，且匹配到权限主体：角色r01
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.ROLE).add("r01");
                        return authPolicy.authentication(uri("http://iam.service/console/tenant/account"), "create", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，且匹配到权限主体：角色r01
                    .compose(resp -> authPolicy.authentication(uri("http://iam.service/console/tenant/account/ident/1"), "create", subjectInfo))
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，没有匹配到权限主体
                    .compose(resp -> authPolicy.authentication(uri("http://iam.service/console/tenant/account/ident/001"), "delete", subjectInfo))
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，没有匹配到权限主体：账号a01
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.ACCOUNT).clear();
                        subjectInfo.get(AuthSubjectKind.ACCOUNT).add("a01");
                        return authPolicy.authentication(uri("http://iam.service/console/tenant/account/ident/1"), "delete", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // 资源需要认证，且匹配到权限主体：账号a01
                    .compose(resp -> {
                        return authPolicy.authentication(uri("http://iam.service/console/tenant/account/ident/2"), "delete", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    .onSuccess(resp -> async.complete());
        });
    }

    @Test
    public void testGroup(TestContext testContext) {
        Async async = testContext.async(2);
        // 先向redis中添加一些资源
        var addAccountAllByGroup = RedisClient.set(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:create",
                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
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
                }));
        var deleteAccountAllByGroup = RedisClient.set(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:delete",
                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
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
                }));
        var modifyAccountAllByGroup = RedisClient.set(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:modify",
                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
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
                }));
        var patchAccountAllByGroup = RedisClient.set(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/app/group/**:patch",
                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
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
                }));
        CompositeFuture.all(Lists.newArrayList(addAccountAllByGroup, deleteAccountAllByGroup, modifyAccountAllByGroup, patchAccountAllByGroup))
                .onSuccess(resp -> async.countDown());

        var authPolicy = new ReadonlyAuthPolicy(60, 5);
        rule.vertx().setTimer(1000, h -> {
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
                    .compose(resp -> authPolicy.authentication(uri("http://iam.service/console/app/group"), "patch", subjectInfo))
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // EQ
                    .compose(resp -> authPolicy.authentication(uri("http://iam.service/console/app/group"), "create", subjectInfo))
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // INCLUDE, 当前级
                    .compose(resp -> authPolicy.authentication(uri("http://iam.service/console/app/group/ident/1"), "delete", subjectInfo))
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // INCLUDE, 下级，拒绝
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("100001000010000");
                        return authPolicy.authentication(uri("http://iam.service/console/app/group/ident/1"), "delete", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // INCLUDE, 上级
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("10000");
                        return authPolicy.authentication(uri("http://iam.service/console/app/group/ident/1"), "delete", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // INCLUDE, 非上级，拒绝
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("10001");
                        return authPolicy.authentication(uri("http://iam.service/console/app/group/ident/1"), "delete", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // LIKE, 当前级
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("1000010000");
                        return authPolicy.authentication(uri("http://iam.service/console/app/group/ident/1"), "modify", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // LIKE, 下级
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("100001000010000");
                        return authPolicy.authentication(uri("http://iam.service/console/app/group/ident/1"), "modify", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.ACCEPT, result);
                        return Future.succeededFuture();
                    })
                    // LIKE, 非下级，拒绝
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("100001000110000");
                        return authPolicy.authentication(uri("http://iam.service/console/app/group/ident/1"), "modify", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    // LIKE, 上级，拒绝
                    .compose(resp -> {
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).clear();
                        subjectInfo.get(AuthSubjectKind.GROUP_NODE).add("10000");
                        return authPolicy.authentication(uri("http://iam.service/console/app/group/ident/1"), "modify", subjectInfo);
                    })
                    .compose(result -> {
                        testContext.assertEquals(AuthResultKind.REJECT, result);
                        return Future.succeededFuture();
                    })
                    .onSuccess(resp -> async.complete());
        });
    }

    @Test
    public void testDynamicModifyResources(TestContext testContext) {
        Async async = testContext.async();
        Map<AuthSubjectKind, List<String>> subjectInfo = new HashMap<>() {
            {
                put(AuthSubjectKind.ROLE, new ArrayList<>() {
                    {
                        add("r00");
                    }
                });
            }
        };
        var topic = new GatewayConfig.Exchange().getTopic();
        var authPolicy = new ReadonlyAuthPolicy(60, 5);
        Future.succeededFuture()
                .compose(resp ->
                        Future.future(promise -> {
                            rule.vertx().setTimer(1000, h -> {
                                promise.complete();
                            });
                        })
                )
                .compose(resp -> ExchangeProcessor.register(GatewayConfig.Exchange.builder().build(), authPolicy))
                // 资源不存在
                .compose(resp -> authPolicy.authentication(uri("http://iam.service/console/app/ident"), "create", subjectInfo))
                .compose(result -> {
                    testContext.assertEquals(AuthResultKind.ACCEPT, result);
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        // 添加资源
                        RedisClient.set(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/app/ident/**:create",
                                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
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
                                })))
                .compose(resp -> {
                    // 通知资源变更
                    RedisClient.publish(topic,
                            $.json.toJsonString(ExchangeData.builder()
                                    .addOpt(true)
                                    .actionKind(AuthActionKind.CREATE.toString().toLowerCase())
                                    .resourceUri("http://iam.service/console/app/ident/**")
                                    .build()));
                    return Future.succeededFuture();
                })
                // 资源已存在且没有匹配主体
                .compose(resp ->
                        Future.future(promise ->
                                rule.vertx().setTimer(2000, h ->
                                        authPolicy.authentication(uri("http://iam.service/console/app/ident"), "create", subjectInfo)
                                                .onSuccess(promise::complete)
                                )
                        )
                )
                .compose(result -> {
                    testContext.assertEquals(AuthResultKind.REJECT, result);
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        // 删除资源
                        RedisClient.del(Constant.CACHE_AUTH_POLICY + "http:iam.service/console/app/ident/**:create"))
                .compose(resp -> {
                    // 通知资源变更
                    RedisClient.publish(topic,
                            $.json.toJsonString(ExchangeData.builder()
                                    .addOpt(false)
                                    .actionKind(AuthActionKind.CREATE.toString().toLowerCase())
                                    .resourceUri("http://iam.service/console/app/ident/**")
                                    .build()));
                    return Future.succeededFuture();
                })
                // 资源已存在且没有匹配主体
                .compose(resp ->
                        Future.future(promise ->
                                rule.vertx().setTimer(2000, h ->
                                        authPolicy.authentication(uri("http://iam.service/console/app/ident"), "create", subjectInfo)
                                                .onSuccess(promise::complete)
                                )
                        )
                )
                .compose(result -> {
                    testContext.assertEquals(AuthResultKind.ACCEPT, result);
                    return Future.succeededFuture();
                })
                .onSuccess(resp -> async.complete());
    }

    @SneakyThrows
    private URI uri(String uri) {
        return new URI(uri);
    }

}
