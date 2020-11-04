package idealworld.dew.baas.gateway;

import com.ecfront.dew.common.$;
import com.google.common.collect.Lists;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.baas.gateway.exchange.ExchangeProcessor;
import idealworld.dew.baas.gateway.process.ReadonlyAuthPolicy;
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
        Async async = testContext.async();
        RedisClient.init(rule.vertx(), GatewayConfig.RedisConfig.builder()
                .uri("redis://localhost:6379").build());
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
                .onSuccess(resp -> async.complete());
    }

    @SneakyThrows
    @Test
    public void testPolicy(TestContext testContext) {
        Async async = testContext.async();
        var authPolicy = new ReadonlyAuthPolicy(60, 5);
        ExchangeProcessor.register(GatewayConfig.Exchange.builder().build(), authPolicy);
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

    @SneakyThrows
    private URI uri(String uri) {
        return new URI(uri);
    }

}
