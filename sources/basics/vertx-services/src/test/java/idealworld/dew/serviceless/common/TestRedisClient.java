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

package idealworld.dew.serviceless.common;

import idealworld.dew.serviceless.common.funs.cache.RedisClient;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestRedisClient extends BasicTest {

    private RedisClient redisClient;

    @BeforeEach
    public void before(Vertx vertx, VertxTestContext testContext) {
        RedisTestHelper.start();
        RedisClient.init("", vertx, CommonConfig.RedisConfig.builder()
                .uri("redis://localhost:6379").build());
        redisClient = RedisClient.choose("");
        testContext.completeNow();
    }

    @Test
    public void testGetSet(Vertx vertx, VertxTestContext testContext) {
        redisClient.set("a:b", "a1")
                .compose(resp -> redisClient.get("a:b", 0))
                .onSuccess(value -> {
                    Assertions.assertEquals("a1", value);
                    testContext.completeNow();
                });
    }

    @Test
    public void testGetSetWithCache(Vertx vertx, VertxTestContext testContext) {
        Future.succeededFuture()
                // 设置为 a1
                .compose(resp -> redisClient.set("a:b", "a1"))
                // 第一次带缓存获取
                .compose(resp -> redisClient.get("a:b", 1))
                .compose(resp -> {
                    Assertions.assertEquals("a1", resp);
                    return Future.succeededFuture();
                })
                // 修改为 a2
                .compose(resp -> redisClient.set("a:b", "a2"))
                // 不带缓存获取，拿到 a2
                .compose(resp -> redisClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals("a2", resp);
                    return Future.succeededFuture();
                })
                // 带缓存获取，拿到a1
                .compose(resp -> redisClient.get("a:b", 1))
                .compose(resp -> {
                    Assertions.assertEquals("a1", resp);
                    return Future.succeededFuture();
                })
                // 延时 1s
                .compose(resp -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                    return Future.succeededFuture();
                })
                // 带缓存获取，拿到 a2
                .compose(resp -> redisClient.get("a:b", 1))
                .compose(resp -> {
                    Assertions.assertEquals("a2", resp);
                    return Future.succeededFuture();
                })
                // 是否存在
                .compose(resp -> redisClient.exists("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals(true, resp);
                    return Future.succeededFuture();
                })
                // 删除
                .compose(resp -> redisClient.del("a:b"))
                .compose(resp -> redisClient.exists("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals(false, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> redisClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertNull(resp);
                    return Future.succeededFuture();
                })
                // 递增
                .compose(resp -> redisClient.del("incr"))
                .compose(resp -> redisClient.incrby("incr", 1))
                .compose(resp -> {
                    Assertions.assertEquals(1L, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> redisClient.incrby("incr", 10))
                .compose(resp -> {
                    Assertions.assertEquals(11L, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> redisClient.incrby("incr", -1))
                .compose(resp -> {
                    Assertions.assertEquals(10L, resp);
                    return Future.succeededFuture();
                })
                .onSuccess(resp -> testContext.completeNow());
    }

    @SneakyThrows
    @Test
    public void testScan(Vertx vertx, VertxTestContext testContext) {
        var count = new CountDownLatch(1000);
        var sets = IntStream.range(0, 1000).mapToObj(i -> (Future) redisClient.set("a:" + i, "a" + i)).collect(Collectors.toList());
        CompositeFuture.all(sets)
                .onSuccess(resp -> {
                    redisClient.scan("a:", key -> count.countDown());
                });
        count.await();
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testPubSub(Vertx vertx, VertxTestContext testContext) {
        var count = new CountDownLatch(3);
        vertx.setTimer(1000, h -> {
            redisClient.subscribe("topic:1", value -> count.countDown())
                    .onSuccess(resp ->
                            vertx.setTimer(1000, h2 -> {
                                redisClient.publish("topic:1", "a1");
                                redisClient.publish("topic:1", "a2");
                            }));
            redisClient.subscribe("topic:2", value -> count.countDown()).onSuccess(resp ->
                    redisClient.publish("topic:2", "b1")
            );
        });
        count.await();
        testContext.completeNow();
    }

    @Test
    public void testExpire(Vertx vertx, VertxTestContext testContext) {
        Future.succeededFuture()
                .compose(resp -> redisClient.setex("a:b", "风雨逐梦", 1L))
                .compose(resp -> redisClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals("风雨逐梦", resp);
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        Future.future(promise -> vertx.setTimer(1000, h -> promise.complete()))
                )
                .compose(resp -> redisClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertNull(resp);
                    return Future.succeededFuture();
                })

                .compose(resp -> redisClient.set("a:b", "风雨逐梦"))
                .compose(resp -> redisClient.expire("a:b", 1L))
                .compose(resp -> redisClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals("风雨逐梦", resp);
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        Future.future(promise -> vertx.setTimer(1000, h -> promise.complete()))
                )
                .compose(resp -> redisClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertNull(resp);
                    return Future.succeededFuture();
                })
                .onSuccess(resp -> testContext.completeNow());
    }

    @Test
    public void testHash(Vertx vertx, VertxTestContext testContext) {
        Future.succeededFuture()
                // 设置
                .compose(resp -> redisClient.hset("a:b", "f1", "孤岛旭日"))
                .compose(resp -> {
                    Assertions.assertNull(resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> redisClient.hset("a:b", "f2", "风雨逐梦"))
                .compose(resp -> redisClient.hget("a:b", "f2"))
                .compose(resp -> {
                    Assertions.assertEquals("风雨逐梦", resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> redisClient.hgetall("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals("孤岛旭日", resp.get("f1"));
                    Assertions.assertEquals("风雨逐梦", resp.get("f2"));
                    return Future.succeededFuture();
                })
                .compose(resp -> redisClient.hexists("a:b", "f1"))
                .compose(resp -> {
                    Assertions.assertEquals(true, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> redisClient.hexists("a:b", "f0"))
                .compose(resp -> {
                    Assertions.assertEquals(false, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> redisClient.hdel("a:b", "f1"))
                .compose(resp -> redisClient.hexists("a:b", "f1"))
                .compose(resp -> {
                    Assertions.assertEquals(false, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> redisClient.hset("a:b", "incr", "0"))
                .compose(resp -> redisClient.hincrby("a:b", "incr", 1))
                .compose(resp -> redisClient.hincrby("a:b", "incr", 10))
                .compose(resp -> {
                    Assertions.assertEquals(11L, resp);
                    return Future.succeededFuture();
                })
                .onSuccess(resp -> testContext.completeNow());
    }

}
