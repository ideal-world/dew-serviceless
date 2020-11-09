package idealworld.dew.baas.common;

import idealworld.dew.baas.common.funs.cache.RedisClient;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestRedisClient extends BasicTest {

    @Before
    public void before(TestContext testContext) {
        RedisTestHelper.start();
        RedisClient.init(rule.vertx(), CommonConfig.RedisConfig.builder()
                .uri("redis://localhost:6379").build());
    }

    @Test
    public void testGetSet(TestContext testContext) {
        Async async = testContext.async();
        RedisClient.set("a:b", "a1")
                .compose(resp -> RedisClient.get("a:b", 0))
                .onSuccess(value -> {
                    testContext.assertEquals("a1", value);
                    async.complete();
                });
    }

    @Test
    public void testGetSetWithCache(TestContext testContext) {
        Async async = testContext.async();
        // 设置为 a1
        RedisClient.set("a:b", "a1")
                // 第一次带缓存获取
                .compose(resp -> RedisClient.get("a:b", 1))
                .compose(resp -> {
                    testContext.assertEquals("a1", resp);
                    return Future.succeededFuture();
                })
                // 修改为 a2
                .compose(resp -> RedisClient.set("a:b", "a2"))
                // 不带缓存获取，拿到 a2
                .compose(resp -> RedisClient.get("a:b"))
                .compose(resp -> {
                    testContext.assertEquals("a2", resp);
                    return Future.succeededFuture();
                })
                // 带缓存获取，拿到a1
                .compose(resp -> RedisClient.get("a:b", 1))
                .compose(resp -> {
                    testContext.assertEquals("a1", resp);
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
                .compose(resp -> RedisClient.get("a:b", 1))
                .onSuccess(resp -> {
                    testContext.assertEquals("a2", resp);
                    async.complete();
                });
    }

    @Test
    public void testScan(TestContext testContext) {
        Async async = testContext.async(1000);
        var sets = IntStream.range(0, async.count()).mapToObj(i -> (Future) RedisClient.set("a:" + i, "a" + i)).collect(Collectors.toList());
        CompositeFuture.all(sets)
                .onSuccess(resp -> {
                    RedisClient.scan("a:", key -> {
                        async.countDown();
                    });
                });
    }

    @Test
    public void testPubSub(TestContext testContext) {
        Async async = testContext.async(3);
        rule.vertx().setTimer(1000, h -> {
            RedisClient.subscribe("topic:1", value -> async.countDown())
                    .onSuccess(resp ->
                            rule.vertx().setTimer(1000, h2 -> {
                                RedisClient.publish("topic:1", "a1");
                                RedisClient.publish("topic:1", "a2");
                            }));
            RedisClient.subscribe("topic:2", value -> {
                async.countDown();
            }).onSuccess(resp ->
                    RedisClient.publish("topic:2", "b1")
            );
        });
    }

}
