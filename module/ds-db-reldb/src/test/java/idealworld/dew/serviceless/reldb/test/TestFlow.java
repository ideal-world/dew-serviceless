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
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectOperatorKind;
import idealworld.dew.framework.fun.auth.dto.ResourceExchange;
import idealworld.dew.framework.fun.cache.FunRedisClient;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.serviceless.reldb.RelDBModule;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFlow extends DewTest {

    static {
        enableRedis();
        enableMysql();
    }

    private static final String MODULE_NAME = new RelDBModule().getModuleName();
    private Map<String, String> rsaKeys = $.security.asymmetric.generateKeys("RSA", 1024);

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new RelDBApplicationTest(), testContext.succeedingThenComplete());
    }

    private String encrypt(String sql) {
        return $.security.encodeBytesToBase64($.security.asymmetric.encrypt(sql.getBytes(), $.security.asymmetric.getPublicKey(rsaKeys.get("PublicKey"), "RSA"), 1024, "RSA/ECB/OAEPWithSHA1AndMGF1Padding"));
    }

    private Tuple2<Buffer, Throwable> request(String subjectCode, String body, IdentOptCacheInfo identOptCacheInfo) {
        var header = new HashMap<String, String>();
        header.put(DewAuthConstant.REQUEST_RESOURCE_URI_FLAG, "reldb://" + subjectCode);
        header.put(DewAuthConstant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(JsonObject.mapFrom(identOptCacheInfo).toString(), StandardCharsets.UTF_8));
        var req = FunEventBus.choose(MODULE_NAME)
                .request(MODULE_NAME,
                        OptActionKind.FETCH,
                        "reldb://" + MODULE_NAME,
                        Buffer.buffer(body),
                        header);
        return awaitRequest(req);
    }

    @SneakyThrows
    @Test
    public void testFlow(Vertx vertx, VertxTestContext testContext) {
        var identOptCacheInfo = IdentOptCacheInfo.builder()
                .accountCode("a01")
                .appId(1000L)
                .tenantId(2000L)
                .token("token01")
                .build();
        Assertions.assertEquals("请求的资源主题[subjectCodexx]不存在", request("subjectCodexx", "{\"sql\":\"select name from iam_account\",\"parameters\":[]}", identOptCacheInfo)._1.getMessage());

        await(FunSQLClient.init("subjectCodexx", vertx, DewConfig.FunConfig.SQLConfig.builder()
                .host("127.0.0.1")
                .port(mysqlConfig.getFirstMappedPort())
                .db(mysqlConfig.getDatabaseName())
                .userName(mysqlConfig.getUsername())
                .password(mysqlConfig.getPassword())
                .build()));
        await(FunSQLClient.choose("subjectCodexx").ddl("create table if not exists iam_account\n" +
                "(\n" +
                "\tid bigint auto_increment primary key,\n" +
                "\tname varchar(255) not null comment '账号名称',\n" +
                "\topen_id varchar(100) not null comment 'Open Id',\n" +
                "\tstatus varchar(50) not null comment '账号状态'\n" +
                ")\n" +
                "comment '账号'"));
        await(FunSQLClient.choose("subjectCodexx").rawExec("insert into iam_account(name, open_id, status) values (?, ?, ?)", new ArrayList<>() {
            {
                add("孤岛旭日1");
                add("xxxx");
                add("ENABLED");
            }
        }));

        Assertions.assertEquals("认证错误，AppId不合法", request("subjectCodexx", "{\"sql\":\"select name from iam_account\",\"parameters\":[]}", identOptCacheInfo)._1.getMessage());

        await(FunRedisClient.choose(MODULE_NAME).set(DewConstant.CACHE_APP_INFO + "1000", "2000\n" + rsaKeys.get("PublicKey") + "\n" + rsaKeys.get("PrivateKey")));

        Assertions.assertEquals("请求的SQL解析错误", request("subjectCodexx", "{\"sql\":\"" + encrypt("select1 name from iam_account1") + "\",\"parameters\":[]}", identOptCacheInfo)._1.getMessage());
        Assertions.assertEquals("Table 'test.iam_account1' doesn't exist", request("subjectCodexx", "{\"sql\":\"" + encrypt("select name from iam_account1") + "\",\"parameters\":[]}", identOptCacheInfo)._1.getMessage());
        Assertions.assertEquals("[{\"name\":\"孤岛旭日1\"}]", request("subjectCodexx", "{\"sql\":\"" + encrypt("select name from iam_account") + "\",\"parameters\":[]}", identOptCacheInfo)._0.toString("utf-8"));

        await(FunRedisClient.choose(MODULE_NAME).set(DewConstant.CACHE_AUTH_POLICY + "reldb:subjectCodexx/iam_account/name:fetch",
                JsonObject.mapFrom(new HashMap<String, Map<String, List<String>>>() {
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
                }).toString()));
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resource.reldb", JsonObject.mapFrom(ResourceExchange.builder()
                .resourceActionKind(OptActionKind.FETCH.toString().toLowerCase())
                .resourceUri("reldb://subjectCodexx/iam_account/name")
                .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);

        Assertions.assertEquals("鉴权错误，没有权限访问对应的资源", request("subjectCodexx", "{\"sql\":\"" + encrypt("select name from iam_account") + "\",\"parameters\":[]}", identOptCacheInfo)._1.getMessage());

        await(FunRedisClient.choose(MODULE_NAME).del(DewConstant.CACHE_AUTH_POLICY + "reldb:subjectCodexx/iam_account/name:fetch"));
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.DELETE, "eb://iam/resource.reldb", JsonObject.mapFrom(ResourceExchange.builder()
                .resourceActionKind(OptActionKind.FETCH.toString().toLowerCase())
                .resourceUri("reldb://subjectCodexx/iam_account/name")
                .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);

        Assertions.assertEquals("[{\"name\":\"孤岛旭日1\"}]", request("subjectCodexx", "{\"sql\":\"" + encrypt("select name from iam_account") + "\",\"parameters\":[]}", identOptCacheInfo)._0.toString("utf-8"));
        testContext.completeNow();
    }

}
