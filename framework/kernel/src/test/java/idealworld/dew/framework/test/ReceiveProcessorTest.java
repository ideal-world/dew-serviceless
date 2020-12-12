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
import com.ecfront.dew.common.StandardCode;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.framework.fun.test.DewTest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.NotNull;
import java.util.HashMap;

public class ReceiveProcessorTest extends DewTest {

    @Test
    public void testProcess(Vertx vertx, VertxTestContext testContext) {
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/app", MockProcessor1());
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/app/{name}", MockProcessor2());
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/app/{name}/{kind}", MockProcessor3());
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/app/{name}/{kind}/enabled", MockProcessor4());

        Future.succeededFuture()
                .compose(resp -> ReceiveProcessor.chooseProcess("", OptActionKind.MODIFY, "/app", "q=测试", new HashMap<>(), null))
                .compose(resp -> {
                    Assertions.assertEquals(StandardCode.BAD_REQUEST.toString(), resp.getCode());
                    return Future.succeededFuture();
                })
                .compose(resp -> ReceiveProcessor.chooseProcess("", OptActionKind.CREATE, "/app", "q=测试", new HashMap<>(), null))
                .compose(resp -> {
                    Assertions.assertEquals("/app", resp.getBody());
                    return Future.succeededFuture();
                })
                .compose(resp -> ReceiveProcessor.chooseProcess("", OptActionKind.CREATE, "/app/n1", "q=测试", new HashMap<>(), null))
                .compose(resp -> {
                    Assertions.assertEquals("/app/{name}", resp.getBody());
                    return Future.succeededFuture();
                })
                .compose(resp -> ReceiveProcessor.chooseProcess("", OptActionKind.CREATE, "/app/n1/k1", "q=测试", new HashMap<>(), null))
                .compose(resp -> {
                    Assertions.assertEquals("/app/{name}/{kind}", resp.getBody());
                    return Future.succeededFuture();
                })
                .compose(resp -> ReceiveProcessor.chooseProcess("", OptActionKind.CREATE, "/app/n1/k1/enabled", "q=测试", new HashMap<>(),
                        JsonObject.mapFrom(User.builder().name("孤岛旭日").build()).toBuffer()))
                .compose(resp -> {
                    Assertions.assertEquals("/app/{name}/{kind}/enabled", resp.getBody());
                    return Future.succeededFuture();
                })
                .compose(resp -> ReceiveProcessor.chooseProcess("", OptActionKind.CREATE, "/app/n1/k1/disabled", "q=测试", new HashMap<>(),
                        JsonObject.mapFrom(User.builder().name("孤岛旭日").build()).toBuffer()))
                .compose(resp -> {
                    Assertions.assertEquals(StandardCode.BAD_REQUEST.toString(), resp.getCode());
                    return Future.succeededFuture();
                })
                .onSuccess(resp -> testContext.completeNow())
                .onFailure(testContext::failNow);
    }

    private ProcessFun<String> MockProcessor1() {
        return context ->
                Future.succeededFuture(Resp.success("/app"));
    }

    private ProcessFun<String> MockProcessor2() {
        return context -> {
            Assertions.assertEquals("n1", context.req.params.get("name"));
            Assertions.assertEquals("测试", context.req.params.get("q"));
            return Future.succeededFuture(Resp.success("/app/{name}"));
        };
    }

    private ProcessFun<String> MockProcessor3() {
        return context -> {
            Assertions.assertEquals("n1", context.req.params.get("name"));
            Assertions.assertEquals("k1", context.req.params.get("kind"));
            Assertions.assertEquals("测试", context.req.params.get("q"));
            return Future.succeededFuture(Resp.success("/app/{name}/{kind}"));
        };
    }

    private ProcessFun<String> MockProcessor4() {
        return context -> {
            var userR = context.helper.parseBody(context.req.body, User.class);
            Assertions.assertEquals("n1", context.req.params.get("name"));
            Assertions.assertEquals("k1", context.req.params.get("kind"));
            Assertions.assertEquals("测试", context.req.params.get("q"));
            Assertions.assertEquals("孤岛旭日", userR.getBody().getName());
            return Future.succeededFuture(Resp.success("/app/{name}/{kind}/enabled"));
        };
    }


    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {

        @NotNull
        private String name;
        private String openId;
        private String status;

    }

}