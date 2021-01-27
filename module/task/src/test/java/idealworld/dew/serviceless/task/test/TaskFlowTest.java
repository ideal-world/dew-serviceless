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

package idealworld.dew.serviceless.task.test;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.serviceless.task.TaskModule;
import idealworld.dew.serviceless.task.domain.TaskDef;
import idealworld.dew.serviceless.task.domain.TaskInst;
import idealworld.dew.serviceless.task.process.TaskProcessor;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 任务流程测试.
 *
 * @author gudaoxuri
 */
public class TaskFlowTest extends DewTest {

    static {
        enableRedis();
        enableMysql();
    }

    private static final String MODULE_NAME = new TaskModule().getModuleName();

    private static final String testJS = new BufferedReader(new InputStreamReader(TaskProcessor.class.getResourceAsStream("/test.js")))
            .lines().collect(Collectors.joining("\n"));

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new TaskApplicationTest(), testContext.succeedingThenComplete());
    }

    @SneakyThrows
    @Test
    public void testTimer(Vertx vertx, VertxTestContext testContext) {
        request(OptActionKind.CREATE, "http://xxx/task", testJS);
        Assertions.assertNull(request(OptActionKind.CREATE, "http://xxx/task/codexx?cron=" + URLEncoder.encode("/5 * * * * ?", StandardCharsets.UTF_8), "Dewxx.xx()")._1);
        Assertions.assertNull(request(OptActionKind.CREATE, "http://xxx/task/codeyy?cron=" + URLEncoder.encode("/5 * * * * ?", StandardCharsets.UTF_8), "1+1")._1);
        var taskDefs = await(FunSQLClient.choose(MODULE_NAME).list(new HashMap<>(), TaskDef.class))._0;
        Assertions.assertEquals(3, taskDefs.size());
        Assertions.assertEquals("codexx", taskDefs.get(1).getCode());
        Thread.sleep(50000);
        var taskInsts = await(FunSQLClient.choose(MODULE_NAME).list(new HashMap<>(), TaskInst.class))._0;
        Assertions.assertTrue(taskInsts.stream().anyMatch(TaskInst::getSuccess));
        Assertions.assertTrue(taskInsts.stream().anyMatch(i -> !i.getSuccess()));
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testGrammar(Vertx vertx, VertxTestContext testContext) {
        request(OptActionKind.CREATE, "http://xxx/task", testJS);
        Assertions.assertEquals("测试", request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestStr", "[\"测试\",100,[\"1\",\"2\",\"3\"],\"ddddd\"]")._0.toString());
        Assertions.assertEquals(100, Integer.parseInt(request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestNum", "[\"测试\",100,[\"1\",\"2\",\"3\"],\"ddddd\"]")._0.toString()));
        Assertions.assertEquals("3", request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestArr", "[\"测试\",100,[\"1\",\"2\",\"3\"],\"ddddd\"]")._0.toJsonArray().getString(2));
        Assertions.assertEquals("ddddd", request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestObj", "[\"测试\",100,[\"1\",\"2\",\"3\"],\"ddddd\"]")._0.toString());
        Assertions.assertEquals("xx", request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestMap", "[{\"xx\":\"xx\"}]")._0.toJsonObject().getString("xx"));
        Assertions.assertEquals("add", request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestMap", "[{\"xx\":\"xx\"}]")._0.toJsonObject().getString("add"));
        Assertions.assertEquals("xx", request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestDto", "[{\"content\":\"xx\"}]")._0.toJsonObject().getString("content"));
        Assertions.assertEquals("100", request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestDto", "[{\"content\":\"xx\"}]")._0.toJsonObject().getString("createUserId"));
        Assertions.assertEquals("yy", request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestDtos", "[[{\"content\":\"xx\"},{\"content\":\"yy\"}]]")._0.toJsonArray().getJsonObject(1).getString("content"));
        Assertions.assertEquals("100", request(OptActionKind.CREATE, "http://xxx/exec/TodoAction2_test.ioTestDtos", "[[{\"content\":\"xx\"},{\"content\":\"yy\"}]]")._0.toJsonArray().getJsonObject(0).getString("createUserId"));
        testContext.completeNow();
    }

    private static Tuple2<Buffer, Throwable> request(OptActionKind actionKind, String uri, String body) {
        var header = new HashMap<String, String>();
        var identOptInfo = IdentOptExchangeInfo.builder()
                .tenantId(1L)
                .unauthorizedTenantId(1L)
                .appId(1L)
                .appId(1L)
                .accountId(DewAuthConstant.AK_SK_IDENT_ACCOUNT_FLAG)
                .build();
        header.put(DewAuthConstant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(JsonObject.mapFrom(identOptInfo).toString(), StandardCharsets.UTF_8));
        var req = FunEventBus.choose(MODULE_NAME)
                .request(MODULE_NAME,
                        actionKind,
                        uri,
                        Buffer.buffer(body),
                        header);
        return awaitRequest(req);
    }
}
