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

package idealworld.dew.framework.test;

import com.ecfront.dew.common.Resp;
import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.framework.fun.test.DewTest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class FunEventBusTest extends DewTest {

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        FunEventBus.init("", vertx, DewConfig.FunConfig.EventBusConfig.builder().build());
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testComplexReqResp(Vertx vertx, VertxTestContext testContext) {
        var count = new CountDownLatch(4);
        ReceiveProcessor.addProcessor(OptActionKind.MODIFY, "/app/{name}/{kind}/enabled", context -> {
            Assertions.assertEquals("xxxx", context.req.header.get("App-Id"));
            Assertions.assertEquals("n1", context.req.params.get("name"));
            Assertions.assertEquals("k1", context.req.params.get("kind"));
            Assertions.assertEquals("测试", context.req.params.get("q"));
            Assertions.assertEquals("孤岛旭日", context.req.body(User.class).getName());
            Assertions.assertEquals("xxxx", context.req.body(User.class).getOpenId());
            if (context.req.body(User.class).getDetail() != null) {
                Assertions.assertEquals("中国", context.req.body(User.class).getDetail().getAddr());
                Assertions.assertEquals(30, context.req.body(User.class).getDetail().getAge());
            }
            count.countDown();
            return Future.succeededFuture(Resp.success("/app/{name}/{kind}/enabled"));
        });
        FunEventBus.choose("").consumer("", (actionKind, uri, header, body) ->
                ReceiveProcessor.chooseProcess("",null, actionKind, uri.getPath(), uri.getQuery(), header, body));

        FunEventBus.choose("").request("", OptActionKind.MODIFY, "http://iam/app/n1/k1/enabled?q=测试",
                JsonObject.mapFrom(User.builder().name("孤岛旭日").build()).toBuffer(), new HashMap<>() {
                    {
                        put("App-Id", "xxxx");
                    }
                })
                .onSuccess(resp -> {
                    count.countDown();
                    Assertions.assertEquals("/app/{name}/{kind}/enabled", Resp.generic(resp._0.toString(StandardCharsets.UTF_8), String.class).getBody());
                });
        FunEventBus.choose("").request("", OptActionKind.MODIFY, "http://iam/app/n1/k1/enabled?q=测试",
                JsonObject.mapFrom(User.builder()
                        .name(" 孤岛旭日 ")
                        .detail(User.Detail.builder()
                                .addr("   中国   ")
                                .age(30)
                                .build())
                        .build()).toBuffer(), new HashMap<>() {
                    {
                        put("App-Id", "xxxx");
                    }
                })
                .onSuccess(resp -> {
                    count.countDown();
                    Assertions.assertEquals("/app/{name}/{kind}/enabled", Resp.generic(resp._0.toString(StandardCharsets.UTF_8), String.class).getBody());
                });
        count.await();
        testContext.completeNow();
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {

        @NotNull
        private String name;
        @Builder.Default
        private String openId = "xxxx";
        private String status;
        private Detail detail;

        @Data
        @SuperBuilder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Detail {

            @NotNull
            @NotBlank
            private String addr;
            @NotNull
            private Integer age;

        }

    }

}
