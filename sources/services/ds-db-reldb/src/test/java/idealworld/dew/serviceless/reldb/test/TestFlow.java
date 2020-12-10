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

package idealworld.dew.serviceless.reldb.test;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.exchange.ExchangeData;
import idealworld.dew.framework.fun.cache.RedisClient;
import idealworld.dew.framework.fun.httpclient.HttpClient;
import idealworld.dew.framework.fun.sql.MysqlClient;
import idealworld.dew.framework.util.YamlHelper;
import idealworld.dew.serviceless.common.CommonConfig;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.reldb.RelDBApplication;
import idealworld.dew.serviceless.reldb.RelDBConfig;
import idealworld.dew.serviceless.reldb.exchange.ExchangeProcessor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFlow extends BasicTest {

    private static RelDBConfig relDBConfig;

    private Map<String, String> rsaKeys = $.security.asymmetric.generateKeys("RSA", 1024);

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        relDBConfig = YamlHelper.toObject(RelDBConfig.class, $.file.readAllByClassPath("application-test.yml", StandardCharsets.UTF_8));
        System.getProperties().put("dew.profile", "test");
        RedisTestHelper.start();
        HttpClient.init("", vertx, HttpClient.HttpConfig.builder().build());
        vertx.deployVerticle(new RelDBApplication(), testContext.succeedingThenComplete());
    }

    private String encrypt(String sql) {
        return $.security.encodeBytesToBase64($.security.asymmetric.encrypt(sql.getBytes(), $.security.asymmetric.getPublicKey(rsaKeys.get("PublicKey"), "RSA"), 1024, "RSA/ECB/OAEPWithSHA1AndMGF1Padding"));
    }

    @Test
    public void testFlow(Vertx vertx, VertxTestContext testContext) {
        var errorResult = $.http.postWrap("http://127.0.0.1:" + relDBConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG,
                "{\"sql\":\"select name from iam_account\",\"parameters\":[]}");
        Assertions.assertEquals("请求格式不合法", Resp.generic(errorResult.result, Void.class).getMessage());

        var identOptCacheInfo = $.json.toJsonString(IdentOptCacheInfo.builder()
                .accountCode("a01")
                .appId(1000L)
                .tenantId(2000L)
                .token("token01")
                .build());

        errorResult = $.http.postWrap("http://127.0.0.1:" + relDBConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG,
                "{\"sql\":\"select name from iam_account\",\"parameters\":[]}", new HashMap<>() {
                    {
                        put(Constant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(identOptCacheInfo, StandardCharsets.UTF_8));
                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                    }
                });
        Assertions.assertEquals("请求的资源主题[subjectCodexx]不存在", Resp.generic(errorResult.result, Void.class).getMessage());

        Future.succeededFuture()
                .compose(resp -> RedisClient.choose("").del(Constant.CACHE_APP_INFO + "1000"))
                .compose(resp -> RedisClient.choose("").del(Constant.CACHE_AUTH_POLICY + "reldb:subjectCodexx/iam_account/name:fetch"))
                .compose(resp -> MysqlClient.init("subjectCodexx", vertx, CommonConfig.JDBCConfig.builder()
                        .uri("mysql://root:123456@127.0.0.1:3306/test").build()))
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
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + relDBConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG,
                                Buffer.buffer("{\"sql\":\"select name from iam_account\",\"parameters\":[]}"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(identOptCacheInfo, StandardCharsets.UTF_8));
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("认证错误，AppId不合法", Resp.generic(resp.bodyAsString(), Void.class).getMessage());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        RedisClient.choose("").set(Constant.CACHE_APP_INFO + "1000", "2000\n" + rsaKeys.get("PublicKey") + "\n" + rsaKeys.get("PrivateKey"))
                )
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + relDBConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG,
                                Buffer.buffer("{\"sql\":\"" + encrypt("select1 name from iam_account1") + "\",\"parameters\":[]}"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(identOptCacheInfo, StandardCharsets.UTF_8));
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("请求的SQL解析错误", Resp.generic(resp.bodyAsString(), Void.class).getMessage());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + relDBConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG,
                                Buffer.buffer("{\"sql\":\"" + encrypt("select name from iam_account1") + "\",\"parameters\":[]}"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(identOptCacheInfo, StandardCharsets.UTF_8));
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("数据查询错误", Resp.generic(resp.bodyAsString(), Void.class).getMessage());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + relDBConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG,
                                Buffer.buffer("{\"sql\":\"" + encrypt("select name from iam_account") + "\",\"parameters\":[]}"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(identOptCacheInfo, StandardCharsets.UTF_8));
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("[{\"name\":\"孤岛旭日1\"}]", Resp.generic(resp.bodyAsString(), String.class).getBody());
                    return Future.succeededFuture();
                })
                .compose(resp -> ExchangeProcessor.init(relDBConfig))
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
                    RedisClient.choose("").publish(Constant.EVENT_NOTIFY_TOPIC_BY_IAM,
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
                                vertx.setTimer(5000, h -> {
                                    HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + relDBConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG,
                                            Buffer.buffer("{\"sql\":\"" + encrypt("select name from iam_account") + "\",\"parameters\":[]}"),
                                            new HashMap<>() {
                                                {
                                                    put(Constant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(identOptCacheInfo, StandardCharsets.UTF_8));
                                                    put(Constant.REQUEST_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                                }
                                            })
                                            .onSuccess(r -> {
                                                Assertions.assertEquals("鉴权错误，没有权限访问对应的资源", Resp.generic(r.bodyAsString(), Void.class).getMessage());
                                                promise.complete(null);
                                            });
                                })
                        )
                )
                .compose(resp -> RedisClient.choose("").del(Constant.CACHE_AUTH_POLICY + "reldb:subjectCodexx/iam_account/name:fetch"))
                .compose(resp -> {
                    RedisClient.choose("").publish(Constant.EVENT_NOTIFY_TOPIC_BY_IAM,
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
                                vertx.setTimer(2000, h -> {
                                    HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + relDBConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG,
                                            Buffer.buffer("{\"sql\":\"" + encrypt("select name from iam_account") + "\",\"parameters\":[]}"),
                                            new HashMap<>() {
                                                {
                                                    put(Constant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(identOptCacheInfo, StandardCharsets.UTF_8));
                                                    put(Constant.REQUEST_RESOURCE_URI_FLAG, "reldb://subjectCodexx");
                                                }
                                            })
                                            .onSuccess(r -> {
                                                Assertions.assertEquals("[{\"name\":\"孤岛旭日1\"}]", Resp.generic(r.bodyAsString(), String.class).getBody());
                                                promise.complete(null);
                                            });
                                })
                        )
                )
                .onSuccess(resp -> testContext.completeNow());
    }

}
