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

package idealworld.dew.serviceless.http.test;

import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.auth.dto.ResourceSubjectExchange;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.serviceless.http.HttpModule;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class HttpFlowTest extends DewTest {

    static {
    }

    private static final String MODULE_NAME = new HttpModule().getModuleName();

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new HttpApplicationTest(), testContext.succeedingThenComplete());
    }

    private Tuple2<Buffer, Throwable> request(OptActionKind actionKind, String uri, String body) {
        var req = FunEventBus.choose(MODULE_NAME)
                .request(MODULE_NAME,
                        actionKind,
                        uri,
                        Buffer.buffer(body),
                        new HashMap<>());
        return awaitRequest(req);
    }

    @SneakyThrows
    @Test
    public void testFlow(Vertx vertx, VertxTestContext testContext) {
        Assertions.assertEquals("找不到请求的资源主体[1.http.httpbin]", request(OptActionKind.CREATE, "http://1.http.httpbin/post", "测试内容")._1.getMessage());

        // 添加资源主体
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resourcesubject.http/httpbin",
                JsonObject.mapFrom(ResourceSubjectExchange.builder()
                        .code("1.http.httpbin")
                        .name("测试API")
                        .kind(ResourceKind.HTTP)
                        .uri("https://httpbin.org")
                        .ak("")
                        .sk("")
                        .platformAccount("")
                        .platformProjectId("")
                        .timeoutMs(10000L)
                        .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);

        var response = request(OptActionKind.CREATE, "http://1.http.httpbin/post", "测试内容")._0.toJsonObject();
        Assertions.assertEquals("测试内容", response.getString("data"));
        testContext.completeNow();
    }

}
