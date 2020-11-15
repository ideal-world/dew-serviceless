package idealworld.dew.baas.reldb.test;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.CommonConfig;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.dto.IdentOptCacheInfo;
import idealworld.dew.baas.common.dto.exchange.ExchangeData;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.baas.common.enumeration.OptActionKind;
import idealworld.dew.baas.common.funs.cache.RedisClient;
import idealworld.dew.baas.common.funs.httpclient.HttpClient;
import idealworld.dew.baas.common.funs.mysql.MysqlClient;
import idealworld.dew.baas.common.util.YamlHelper;
import idealworld.dew.baas.reldb.RelDBApplication;
import idealworld.dew.baas.reldb.RelDBConfig;
import idealworld.dew.baas.reldb.exchange.ExchangeProcessor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFlow extends BasicTest {

    private RelDBConfig gatewayConfig;

    @Before
    public void before(TestContext testContext) {
        gatewayConfig = YamlHelper.toObject(RelDBConfig.class, $.file.readAllByClassPath("application-test.yml", StandardCharsets.UTF_8));
        System.getProperties().put("dew.profile", "test");
        RedisTestHelper.start();
        Vertx vertx = rule.vertx();
        HttpClient.init(vertx);
        vertx.deployVerticle(new RelDBApplication(), testContext.asyncAssertSuccess());
    }

    @Test
    public void testFlow(TestContext testContext) {
        var async = testContext.async();
        var errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath(),
                "xxxx|reldb://subjectCodexx");
        testContext.assertEquals("请求格式不合法", errorResult.result);

        var identOptCacheInfo = $.json.toJsonString(IdentOptCacheInfo.builder()
                .accountCode("a01")
                .appId(1000L)
                .tenantId(2000L)
                .token("token01")
                .build());

        errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath(),
                "xxxx|reldb://subjectCodexx", new HashMap<>() {
                    {
                        put(gatewayConfig.getRequest().getIdentOptHeaderName(), identOptCacheInfo);
                        put(Constant.CONFIG_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                    }
                });
        testContext.assertEquals("请求的资源主题不存在", errorResult.result);

        Future.succeededFuture()
                .compose(resp -> MysqlClient.init("subjectCodexx", rule.vertx(), CommonConfig.JDBCConfig.builder()
                        .url("mysql://root:123456@127.0.0.1:3306/test").build()))
                .compose(resp -> MysqlClient.choose("subjectCodexx").exec("drop table if exists iam_account"))
                .compose(resp ->
                        MysqlClient.choose("subjectCodexx").exec("create table if not exists iam_account\n" +
                                "(\n" +
                                "\tid bigint auto_increment primary key,\n" +
                                "\tname varchar(255) not null comment '账号名称',\n" +
                                "\topen_id varchar(100) not null comment 'Open Id',\n" +
                                "\tstatus varchar(50) not null comment '账号状态'\n" +
                                ")\n" +
                                "comment '账号'")
                )
                .compose(resp ->
                        MysqlClient.choose("subjectCodexx").exec("insert into iam_account(name, open_id, status) values (?, ?, ?)", "孤岛旭日1", "xxxx", "ENABLED")
                )
                .compose(resp -> {
                    var error2Result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath(),
                            "xxxx|reldb://subjectCodexx", new HashMap<>() {
                                {
                                    put(gatewayConfig.getRequest().getIdentOptHeaderName(), identOptCacheInfo);
                                    put(Constant.CONFIG_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                }
                            });
                    testContext.assertEquals("请求的SQL不存在", error2Result.result);
                    return Future.succeededFuture();
                })
                .compose(resp -> RedisClient.choose("").set(Constant.CACHE_RELDB_SQL_MAPPING + "xxxx", "select name fromm iam_account"))
                .compose(resp -> {
                    var error2Result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath(),
                            "xxxx|", new HashMap<>() {
                                {
                                    put(gatewayConfig.getRequest().getIdentOptHeaderName(), identOptCacheInfo);
                                    put(Constant.CONFIG_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                }
                            });
                    testContext.assertEquals("请求的SQL解析错误", error2Result.result);
                    return Future.succeededFuture();
                })
                .compose(resp -> RedisClient.choose("").set(Constant.CACHE_RELDB_SQL_MAPPING + "xxxx", "select name from iam_account"))
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath(), Buffer.buffer("xxxx|"),
                                new HashMap<>() {
                                    {
                                        put(gatewayConfig.getRequest().getIdentOptHeaderName(), identOptCacheInfo);
                                        put(Constant.CONFIG_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                    }
                                })
                )
                .compose(resp -> {
                    testContext.assertEquals("[{\"name\":\"孤岛旭日1\"}]", resp.bodyAsString());
                    return Future.succeededFuture();
                })
                .compose(resp -> ExchangeProcessor.init())
                .compose(resp ->
                        RedisClient.choose("").set(Constant.CACHE_AUTH_POLICY + "reldb:subjectCodexx/iam_account/name:fetch",
                                $.json.toJsonString(new HashMap<String, Map<String, List<String>>>() {
                                    {
                                        put(AuthSubjectOperatorKind.EQ.toString().toLowerCase(), new HashMap<>() {
                                            {
                                                put(AuthSubjectKind.ACCOUNT.toString().toLowerCase(), new ArrayList<>() {
                                                    {
                                                        add("a02");
                                                    }
                                                });
                                            }
                                        });
                                    }
                                })))
                .compose(resp -> {
                    RedisClient.choose("").publish(Constant.CONFIG_EVENT_NOTIFY_TOPIC_BY_IAM,
                            "resource.reldb#" + $.json.toJsonString(ExchangeData.builder()
                                    .actionKind(OptActionKind.CREATE)
                                    .subjectCategory("resource")
                                    .subjectId("xxxx")
                                    .detailData(new HashMap<>() {
                                        {
                                            put("resourceUri", "reldb://subjectCodexx/iam_account/name");
                                            put("resourceActionKind", OptActionKind.FETCH.toString().toLowerCase());
                                        }
                                    })
                                    .build()));
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        Future.future(promise ->
                                rule.vertx().setTimer(5000, h -> {
                                    HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath(), Buffer.buffer("xxxx|"),
                                            new HashMap<>() {
                                                {
                                                    put(gatewayConfig.getRequest().getIdentOptHeaderName(), identOptCacheInfo);
                                                    put(Constant.CONFIG_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                                }
                                            })
                                            .onSuccess(r -> {
                                                testContext.assertEquals("鉴权错误，没有权限访问对应的资源", r.bodyAsString());
                                                promise.complete(null);
                                            });
                                })
                        )
                )
                .compose(resp -> RedisClient.choose("").del(Constant.CACHE_AUTH_POLICY + "reldb:subjectCodexx/iam_account/name:fetch"))
                .compose(resp -> {
                    RedisClient.choose("").publish(Constant.CONFIG_EVENT_NOTIFY_TOPIC_BY_IAM,
                            "resource.reldb#" + $.json.toJsonString(ExchangeData.builder()
                                    .actionKind(OptActionKind.DELETE)
                                    .subjectCategory("resource")
                                    .subjectId("xxxx")
                                    .detailData(new HashMap<>() {
                                        {
                                            put("resourceUri", "reldb://subjectCodexx/iam_account/name");
                                            put("resourceActionKind", OptActionKind.FETCH.toString().toLowerCase());
                                        }
                                    })
                                    .build()));
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        Future.future(promise ->
                                rule.vertx().setTimer(2000, h -> {
                                    HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath(), Buffer.buffer("xxxx|"),
                                            new HashMap<>() {
                                                {
                                                    put(gatewayConfig.getRequest().getIdentOptHeaderName(), identOptCacheInfo);
                                                    put(Constant.CONFIG_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                                }
                                            })
                                            .onSuccess(r -> {
                                                testContext.assertEquals("[{\"name\":\"孤岛旭日1\"}]", r.bodyAsString());
                                                promise.complete(null);
                                            });
                                })
                        )
                )
                .onSuccess(resp -> async.complete());
    }

}
