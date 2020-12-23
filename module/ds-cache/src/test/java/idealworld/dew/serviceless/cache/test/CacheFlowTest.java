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

package idealworld.dew.serviceless.cache.test;

import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.auth.dto.ResourceSubjectExchange;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.serviceless.cache.CacheConstant;
import idealworld.dew.serviceless.cache.CacheModule;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class CacheFlowTest extends DewTest {

    static {
        enableRedis();
    }

    private static final String MODULE_NAME = new CacheModule().getModuleName();

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new CacheApplicationTest(), testContext.succeedingThenComplete());
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
        Assertions.assertEquals("请求的资源主题[1.cache.cxxx]不存在", request(OptActionKind.CREATE, "http://1.cache.cxxx/someKey", "someValue")._1.getMessage());

        // 添加资源主体
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resourcesubject.cache/cachexx",
                JsonObject.mapFrom(ResourceSubjectExchange.builder()
                        .code("1.cache.cxxx")
                        .name("测试缓存")
                        .kind(ResourceKind.CACHE)
                        .uri("redis://localhost:" + redisConfig.getFirstMappedPort() + "/10")
                        .ak("")
                        .sk("")
                        .platformAccount("")
                        .platformProjectId("")
                        .timeoutMs(10000L)
                        .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);

        // 创建
        Assertions.assertNull(request(OptActionKind.CREATE, "http://1.cache.cxxx/someKey", "someValue")._1);
        // 获取
        Assertions.assertEquals("someValue", request(OptActionKind.FETCH, "http://1.cache.cxxx/someKey", "")._0.toString("utf-8"));
        // 是否存在
        Assertions.assertEquals("true", request(OptActionKind.EXISTS, "http://1.cache.cxxx/someKey", "")._0.toString("utf-8"));
        // 修改值
        Assertions.assertNull(request(OptActionKind.MODIFY, "http://1.cache.cxxx/someKey", "someValue2")._1);
        Assertions.assertEquals("someValue2", request(OptActionKind.FETCH, "http://1.cache.cxxx/someKey", "")._0.toString("utf-8"));
        // 修改过期时间
        Assertions.assertNull(request(OptActionKind.MODIFY, "http://1.cache.cxxx/someKey?" + CacheConstant.REQUEST_ATTR_EXPIRE_SEC + "=1", "")._1);
        Thread.sleep(1000);
        Assertions.assertEquals("false", request(OptActionKind.EXISTS, "http://1.cache.cxxx/someKey", "")._0.toString("utf-8"));
        // 删除
        Assertions.assertNull(request(OptActionKind.CREATE, "http://1.cache.cxxx/someKey2", "someValue")._1);
        Assertions.assertEquals("true", request(OptActionKind.EXISTS, "http://1.cache.cxxx/someKey2", "")._0.toString("utf-8"));
        Assertions.assertNull(request(OptActionKind.DELETE, "http://1.cache.cxxx/someKey2", "")._1);
        Assertions.assertEquals("false", request(OptActionKind.EXISTS, "http://1.cache.cxxx/someKey2", "")._0.toString("utf-8"));
        // 创建hash
        Assertions.assertNull(request(OptActionKind.CREATE, "http://1.cache.cxxx/someHash/f1", "孤岛旭日1")._1);
        Assertions.assertNull(request(OptActionKind.CREATE, "http://1.cache.cxxx/someHash/f2", "孤岛旭日2")._1);
        // 获取hash
        Assertions.assertEquals("孤岛旭日1", request(OptActionKind.FETCH, "http://1.cache.cxxx/someHash/f1", "")._0.toString("utf-8"));
        Assertions.assertEquals("{\"f1\":\"孤岛旭日1\",\"f2\":\"孤岛旭日2\"}", request(OptActionKind.FETCH, "http://1.cache.cxxx/someHash/*", "")._0.toString("utf-8"));
        // 是否存在hash
        Assertions.assertEquals("true", request(OptActionKind.EXISTS, "http://1.cache.cxxx/someHash/f1", "")._0.toString("utf-8"));
        Assertions.assertEquals("false", request(OptActionKind.EXISTS, "http://1.cache.cxxx/someHash/f4", "")._0.toString("utf-8"));
        // 修改值hash
        Assertions.assertNull(request(OptActionKind.MODIFY, "http://1.cache.cxxx/someHash/f1", "孤岛旭日new")._1);
        Assertions.assertEquals("孤岛旭日new", request(OptActionKind.FETCH, "http://1.cache.cxxx/someHash/f1", "")._0.toString("utf-8"));
        // 删除hash
        Assertions.assertNull(request(OptActionKind.DELETE, "http://1.cache.cxxx/someHash/f1", "")._1);
        Assertions.assertEquals("false", request(OptActionKind.EXISTS, "http://1.cache.cxxx/someHash/f1", "")._0.toString("utf-8"));
        Assertions.assertNull(request(OptActionKind.DELETE, "http://1.cache.cxxx/someHash", "")._1);
        Assertions.assertEquals("false", request(OptActionKind.EXISTS, "http://1.cache.cxxx/someHash", "")._0.toString("utf-8"));
        // incr
        Assertions.assertNull(request(OptActionKind.CREATE, "http://1.cache.cxxx/incr?" + CacheConstant.REQUEST_ATTR_INCR + "=true", "5")._1);
        Assertions.assertNull(request(OptActionKind.CREATE, "http://1.cache.cxxx/hash/incr?" + CacheConstant.REQUEST_ATTR_INCR + "=true", "5")._1);
        Assertions.assertNull(request(OptActionKind.MODIFY, "http://1.cache.cxxx/incr?" + CacheConstant.REQUEST_ATTR_INCR + "=true", "3")._1);
        Assertions.assertNull(request(OptActionKind.MODIFY, "http://1.cache.cxxx/hash/incr?" + CacheConstant.REQUEST_ATTR_INCR + "=true", "3")._1);
        Assertions.assertNull(request(OptActionKind.MODIFY, "http://1.cache.cxxx/incr?" + CacheConstant.REQUEST_ATTR_INCR + "=true", "-1")._1);
        Assertions.assertNull(request(OptActionKind.MODIFY, "http://1.cache.cxxx/hash/incr?" + CacheConstant.REQUEST_ATTR_INCR + "=true", "-1")._1);
        Assertions.assertEquals("7", request(OptActionKind.FETCH, "http://1.cache.cxxx/incr", "")._0.toString("utf-8"));
        Assertions.assertEquals("7", request(OptActionKind.FETCH, "http://1.cache.cxxx/hash/incr", "")._0.toString("utf-8"));

        testContext.completeNow();
    }

}
