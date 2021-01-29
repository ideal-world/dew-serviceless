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

package idealworld.dew.framework.test;

import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.fun.cache.FunCacheClient;
import idealworld.dew.framework.fun.test.DewTest;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 缓存操作入口测试.
 *
 * @author gudaoxuri
 */
@Slf4j
public class FunCacheClientTest extends DewTest {

    static {
        enableRedis();
    }

    private FunCacheClient funCacheClient;

    @BeforeEach
    public void before(Vertx vertx, VertxTestContext testContext) {
        FunCacheClient.init("", vertx, DewConfig.FunConfig.CacheConfig.builder()
                .uri("redis://localhost:" + redisConfig.getFirstMappedPort()).build());
        funCacheClient = FunCacheClient.choose("");
        testContext.completeNow();
    }

    @Test
    public void testGetSet(Vertx vertx, VertxTestContext testContext) {
        funCacheClient.set("a:b", "a1")
                .compose(resp -> funCacheClient.get("a:b", 0))
                .onSuccess(value -> {
                    Assertions.assertEquals("a1", value);
                    testContext.completeNow();
                });
    }

    @Test
    public void testGetSetWithCache(Vertx vertx, VertxTestContext testContext) {
        Future.succeededFuture()
                // 设置为 a1
                .compose(resp -> funCacheClient.set("a:b", "a1"))
                // 第一次带缓存获取
                .compose(resp -> funCacheClient.get("a:b", 1))
                .compose(resp -> {
                    Assertions.assertEquals("a1", resp);
                    return Future.succeededFuture();
                })
                // 修改为 a2
                .compose(resp -> funCacheClient.set("a:b", "a2"))
                // 不带缓存获取，拿到 a2
                .compose(resp -> funCacheClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals("a2", resp);
                    return Future.succeededFuture();
                })
                // 带缓存获取，拿到a1
                .compose(resp -> funCacheClient.get("a:b", 1))
                .compose(resp -> {
                    Assertions.assertEquals("a1", resp);
                    return Future.succeededFuture();
                })
                // 延时 1s
                .compose(resp -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                        // ignore
                    }
                    return Future.succeededFuture();
                })
                // 带缓存获取，拿到 a2
                .compose(resp -> funCacheClient.get("a:b", 1))
                .compose(resp -> {
                    Assertions.assertEquals("a2", resp);
                    return Future.succeededFuture();
                })
                // 是否存在
                .compose(resp -> funCacheClient.exists("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals(true, resp);
                    return Future.succeededFuture();
                })
                // 删除
                .compose(resp -> funCacheClient.del("a:b"))
                .compose(resp -> funCacheClient.exists("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals(false, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> funCacheClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertNull(resp);
                    return Future.succeededFuture();
                })
                // 递增
                .compose(resp -> funCacheClient.del("incr"))
                .compose(resp -> funCacheClient.incrby("incr", 1))
                .compose(resp -> {
                    Assertions.assertEquals(1L, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> funCacheClient.incrby("incr", 10))
                .compose(resp -> {
                    Assertions.assertEquals(11L, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> funCacheClient.incrby("incr", -1))
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
        var sets = IntStream.range(0, 1000).mapToObj(i -> (Future) funCacheClient.set("a:" + i, "a" + i)).collect(Collectors.toList());
        CompositeFuture.all(sets)
                .onSuccess(resp -> {
                    funCacheClient.scan("a:", key -> count.countDown());
                });
        count.await();
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testPubSub(Vertx vertx, VertxTestContext testContext) {
        var count = new CountDownLatch(3);
        vertx.setTimer(1000, h -> {
            funCacheClient.subscribe("topic:1", value -> count.countDown())
                    .onSuccess(resp ->
                            vertx.setTimer(1000, h2 -> {
                                funCacheClient.publish("topic:1", "a1");
                                funCacheClient.publish("topic:1", "a2");
                            }));
            funCacheClient.subscribe("topic:2", value -> count.countDown()).onSuccess(resp ->
                    funCacheClient.publish("topic:2", "b1")
            );
        });
        count.await();
        testContext.completeNow();
    }

    @Test
    public void testExpire(Vertx vertx, VertxTestContext testContext) {
        Future.succeededFuture()
                .compose(resp -> funCacheClient.setex("a:b", "风雨逐梦", 1L))
                .compose(resp -> funCacheClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals("风雨逐梦", resp);
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        Future.future(promise -> vertx.setTimer(1000, h -> promise.complete()))
                )
                .compose(resp -> funCacheClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertNull(resp);
                    return Future.succeededFuture();
                })

                .compose(resp -> funCacheClient.set("a:b", "风雨逐梦"))
                .compose(resp -> funCacheClient.expire("a:b", 1L))
                .compose(resp -> funCacheClient.get("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals("风雨逐梦", resp);
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        Future.future(promise -> vertx.setTimer(1000, h -> promise.complete()))
                )
                .compose(resp -> funCacheClient.get("a:b"))
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
                .compose(resp -> funCacheClient.hset("a:b", "f1", "孤岛旭日"))
                .compose(resp -> {
                    Assertions.assertNull(resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> funCacheClient.hset("a:b", "f2", "风雨逐梦"))
                .compose(resp -> funCacheClient.hset("a:b", "字段3", "风雨逐梦"))
                .compose(resp -> funCacheClient.hget("a:b", "f2"))
                .compose(resp -> {
                    Assertions.assertEquals("风雨逐梦", resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> funCacheClient.hgetall("a:b"))
                .compose(resp -> {
                    Assertions.assertEquals("孤岛旭日", resp.get("f1"));
                    Assertions.assertEquals("风雨逐梦", resp.get("f2"));
                    Assertions.assertEquals("风雨逐梦", resp.get("字段3"));
                    return Future.succeededFuture();
                })
                .compose(resp -> funCacheClient.hexists("a:b", "f1"))
                .compose(resp -> {
                    Assertions.assertEquals(true, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> funCacheClient.hexists("a:b", "f0"))
                .compose(resp -> {
                    Assertions.assertEquals(false, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> funCacheClient.hdel("a:b", "f1"))
                .compose(resp -> funCacheClient.hexists("a:b", "f1"))
                .compose(resp -> {
                    Assertions.assertEquals(false, resp);
                    return Future.succeededFuture();
                })
                .compose(resp -> funCacheClient.hset("a:b", "incr", "0"))
                .compose(resp -> funCacheClient.hincrby("a:b", "incr", 1))
                .compose(resp -> funCacheClient.hincrby("a:b", "incr", 10))
                .compose(resp -> {
                    Assertions.assertEquals(11L, resp);
                    return Future.succeededFuture();
                })
                .onSuccess(resp -> testContext.completeNow());
    }

    @Test
    public void testElection(Vertx vertx, VertxTestContext testContext) {
        final FunCacheClient[] funCacheClient1 = {null};
        FunCacheClient.init("election", vertx, DewConfig.FunConfig.CacheConfig.builder()
                .uri("redis://localhost:" + redisConfig.getFirstMappedPort())
                .electionPeriodSec(1)
                .build())
                .compose(resp -> {
                    var promise = Promise.promise();
                    vertx.setTimer(3000, t -> promise.complete());
                    return promise.future();
                })
                .compose(resp -> {
                    funCacheClient1[0] = FunCacheClient.choose("election");
                    Assertions.assertTrue(funCacheClient1[0].isLeader());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        FunCacheClient.init("election", vertx, DewConfig.FunConfig.CacheConfig.builder()
                                .uri("redis://localhost:" + redisConfig.getFirstMappedPort())
                                .electionPeriodSec(1)
                                .build()))
                .compose(resp -> {
                    var promise = Promise.promise();
                    vertx.setTimer(1000, t -> promise.complete());
                    return promise.future();
                })
                .compose(resp -> {
                    Assertions.assertFalse(FunCacheClient.choose("election").isLeader());
                    return funCacheClient1[0].close();
                })
                .compose(resp -> {
                    var promise = Promise.promise();
                    vertx.setTimer(3000, t -> promise.complete());
                    return promise.future();
                })
                .onSuccess(resp -> {
                    Assertions.assertTrue(FunCacheClient.choose("election").isLeader());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

}
