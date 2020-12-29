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

package idealworld.dew.serviceless.task.test;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.serviceless.task.TaskModule;
import idealworld.dew.serviceless.task.domain.TaskDef;
import idealworld.dew.serviceless.task.domain.TaskInst;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TaskFlowTest extends DewTest {

    static {
        enableRedis();
        enableMysql();
    }

    private static final String MODULE_NAME = new TaskModule().getModuleName();

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new TaskApplicationTest(), testContext.succeedingThenComplete());
    }

    private Tuple2<Buffer, Throwable> request(OptActionKind actionKind, String uri, String body) {
        var header = new HashMap<String, String>();
        var identOptInfo = IdentOptCacheInfo.builder()
                .tenantId(1L)
                .appId(1L)
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

    @SneakyThrows
    @Test
    public void testFlow(Vertx vertx, VertxTestContext testContext) {
        Assertions.assertNull(request(OptActionKind.CREATE, "http://xxx/task/codexx?cron=" + URLEncoder.encode("/5 * * * * ?", StandardCharsets.UTF_8), "Dewxx.xx()")._1);
        Assertions.assertNull(request(OptActionKind.CREATE, "http://xxx/task/codeyy?cron=" + URLEncoder.encode("/5 * * * * ?", StandardCharsets.UTF_8), "1+1")._1);
        var taskDefs = await(FunSQLClient.choose(MODULE_NAME).list(new HashMap<>(), TaskDef.class))._0;
        Assertions.assertEquals(2, taskDefs.size());
        Assertions.assertEquals("codexx", taskDefs.get(0).getCode());
        Thread.sleep(60000);
        var taskInsts = await(FunSQLClient.choose(MODULE_NAME).list(new HashMap<>(), TaskInst.class))._0;
        Assertions.assertTrue(taskInsts.stream().anyMatch(TaskInst::getSuccess));
        Assertions.assertTrue(taskInsts.stream().anyMatch(i -> !i.getSuccess()));
        testContext.completeNow();
    }

}
