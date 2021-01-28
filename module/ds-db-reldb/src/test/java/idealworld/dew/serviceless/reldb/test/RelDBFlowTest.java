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

package idealworld.dew.serviceless.reldb.test;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.*;
import idealworld.dew.framework.fun.cache.FunCacheClient;
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

/**
 * 关系型数据库流程测试.
 *
 * @author gudaoxuri
 */
public class RelDBFlowTest extends DewTest {

    private static final String MODULE_NAME = new RelDBModule().getModuleName();

    static {
        enableRedis();
        enableMysql();
    }

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new RelDBApplicationTest(), testContext.succeedingThenComplete());
    }

    private Tuple2<Buffer, Throwable> request(String subjectCode, String body, IdentOptExchangeInfo identOptCacheInfo) {
        var header = new HashMap<String, String>();
        identOptCacheInfo.setUnauthorizedTenantId(identOptCacheInfo.getUnauthorizedTenantId());
        identOptCacheInfo.setUnauthorizedAppId(identOptCacheInfo.getUnauthorizedAppId());
        header.put(DewAuthConstant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(JsonObject.mapFrom(identOptCacheInfo).toString(), StandardCharsets.UTF_8));
        var req = FunEventBus.choose(MODULE_NAME)
                .request(MODULE_NAME,
                        OptActionKind.FETCH,
                        "reldb://" + subjectCode,
                        Buffer.buffer(body),
                        header);
        return awaitRequest(req);
    }

    @SneakyThrows
    @Test
    public void testFlow(Vertx vertx, VertxTestContext testContext) {
        var identOptCacheInfo = IdentOptExchangeInfo.builder()
                .accountCode("a01")
                .appId(1000L)
                .tenantId(2000L)
                .unauthorizedAppId(1000L)
                .unauthorizedTenantId(2000L)
                .token("token01")
                .build();
        Assertions.assertEquals("找不到请求的资源主体[1.reldb.subjectCodexx]", request("1.reldb.subjectCodexx", "{\"sql\":\"select name from iam_account\",\"parameters\":[]}", identOptCacheInfo)._1.getMessage());

        // 添加资源主体
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resourcesubject.reldb/subjectCodexx",
                JsonObject.mapFrom(ResourceSubjectExchange.builder()
                        .code("1.reldb.subjectCodexx")
                        .name("测试数据库")
                        .kind(ResourceKind.RELDB)
                        .uri("mysql://127.0.0.1:" + mysqlConfig.getFirstMappedPort() + "/" + mysqlConfig.getDatabaseName())
                        .ak(mysqlConfig.getUsername())
                        .sk(mysqlConfig.getPassword())
                        .platformAccount("")
                        .platformProjectId("")
                        .timeoutMs(1000L)
                        .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);

        await(FunSQLClient.choose("1.reldb.subjectCodexx").ddl("create table if not exists iam_account\n" +
                "(\n" +
                "\tid bigint auto_increment primary key,\n" +
                "\tname varchar(255) not null comment '账号名称',\n" +
                "\topen_id varchar(100) not null comment 'Open Id',\n" +
                "\tstatus varchar(50) not null comment '账号状态'\n" +
                ")\n" +
                "comment '账号'"));
        await(FunSQLClient.choose("1.reldb.subjectCodexx").rawExec("insert into iam_account(name, open_id, status) values (?, ?, ?)", new ArrayList<>() {
            {
                add("孤岛旭日1");
                add("xxxx");
                add("ENABLED");
            }
        }));

        Assertions.assertEquals("请求的SQL解析错误", request("1.reldb.subjectCodexx", "{\"sql\":\"" + "select1 name from iam_account1" + "\",\"parameters\":[]}", identOptCacheInfo)._1.getMessage());
        Assertions.assertEquals("Table 'test.iam_account1' doesn't exist", request("1.reldb.subjectCodexx", "{\"sql\":\"" + "select name from iam_account1" + "\",\"parameters\":[]}", identOptCacheInfo)._1.getMessage());
        Assertions.assertEquals("[{\"name\":\"孤岛旭日1\"}]", request("1.reldb.subjectCodexx", "{\"sql\":\"" + "select name from iam_account" + "\",\"parameters\":[]}", identOptCacheInfo)._0.toString("utf-8"));

        await(FunCacheClient.choose(MODULE_NAME).set(DewConstant.CACHE_AUTH_POLICY + "reldb:1.reldb.subjectCodexx/iam_account/name:fetch",
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
                .actionKind(OptActionKind.FETCH.toString().toLowerCase())
                .uri("reldb://1.reldb.subjectCodexx/iam_account/name")
                .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);

        Assertions.assertEquals("鉴权错误，没有权限访问对应的资源", request("1.reldb.subjectCodexx", "{\"sql\":\"" + "select name from iam_account" + "\",\"parameters\":[]}", identOptCacheInfo)._1.getMessage());

        await(FunCacheClient.choose(MODULE_NAME).del(DewConstant.CACHE_AUTH_POLICY + "reldb:1.reldb.subjectCodexx/iam_account/name:fetch"));
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.DELETE, "eb://iam/resource.reldb", JsonObject.mapFrom(ResourceExchange.builder()
                .actionKind(OptActionKind.FETCH.toString().toLowerCase())
                .uri("reldb://1.reldb.subjectCodexx/iam_account/name")
                .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);

        Assertions.assertEquals("[{\"name\":\"孤岛旭日1\"}]", request("1.reldb.subjectCodexx", "{\"sql\":\"" + "select name from iam_account" + "\",\"parameters\":[]}", identOptCacheInfo)._0.toString("utf-8"));
        testContext.completeNow();
    }

}
