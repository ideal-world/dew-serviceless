package idealworld.dew.baas.gateway.util;

import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.baas.gateway.GatewayConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author gudaoxuri
 */
@Slf4j
public class RedisClient {

    private static final String CACHE_KEY_PREFIX = "redis:";
    private static RedisAPI redisClient;
    private static RedisConnection subRedisConn;
    private static RedisConnection pubRedisConn;
    private static Vertx innerVertx;

    public static void init(Vertx vertx, GatewayConfig.RedisConfig config) {
        innerVertx = vertx;
        var redis = Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setPassword(config.getPassword())
                        .setMaxPoolSize(config.getMaxPoolSize())
                        .setMaxPoolWaiting(config.getMaxPoolWaiting()));
        redisClient = RedisAPI.api(redis);
        redis.connect()
                .onSuccess(conn -> log.info("[Redis]Connected {}", config.getUri()))
                .onFailure(e -> {
                    log.error("[Redis]Connection error: {}", e.getMessage(), e);
                    throw new RTException(e);
                });
        var subRedis = Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setPassword(config.getPassword()))
                .connect(conn -> {
                    if (conn.succeeded()) {
                        log.info("[Redis]Subscribe connected {}", config.getUri());
                        subRedisConn = conn.result();
                        return;
                    }
                    log.error("[Redis]Subscribe connection error: {}", conn.cause().getMessage(), conn.cause());
                    throw new RTException(conn.cause());
                });
        var pubRedis = Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setPassword(config.getPassword()))
                .connect(conn -> {
                    if (conn.succeeded()) {
                        log.info("[Redis]Publish connected {}", config.getUri());
                        pubRedisConn = conn.result();
                        return;
                    }
                    log.error("[Redis]Publish connection error: {}", conn.cause().getMessage(), conn.cause());
                    throw new RTException(conn.cause());
                });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            redis.close();
            subRedis.close();
            pubRedis.close();
        }));
    }

    public static Future<Void> set(String key, String value) {
        return Future.future(promise ->
                redisClient.set(new ArrayList<>() {
                    {
                        add(key);
                        add(value);
                    }
                }).onSuccess(response ->
                        promise.complete()
                ).onFailure(e -> {
                    log.error("[Redis]Set [{}:{}] error: {}", key, value, e.getMessage(), e);
                    promise.fail(e.getCause());
                })
        );
    }

    public static Future<Void> del(String... keys) {
        return Future.future(promise ->
                redisClient.del(Arrays.asList(keys)).onSuccess(response ->
                        promise.complete()
                ).onFailure(e -> {
                    log.error("[Redis]Del [{}] error: {}", String.join(",", keys), e.getMessage(), e);
                    promise.fail(e.getCause());
                })
        );
    }

    public static Future<Boolean> exists(String... keys) {
        return Future.future(promise ->
                redisClient.exists(Arrays.asList(keys))
                        .onSuccess(response -> promise.complete(response.toInteger() > 0))
                        .onFailure(e -> {
                            log.error("[Redis]Exists [{}] error: {}", String.join(",", keys), e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public static Future<String> get(String key) {
        return Future.future(promise ->
                redisClient.get(key)
                        .onSuccess(response -> promise.complete(
                                response != null ? response.toString(StandardCharsets.UTF_8) : null))
                        .onFailure(e -> {
                            log.error("[Redis]Get [{}] error: {}", key, e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public static Future<String> get(String key, Integer cacheSec) {
        if (cacheSec == null || cacheSec <= 0) {
            return get(key);
        }
        return FutureCacheHelper.getSetF(CACHE_KEY_PREFIX + key + "-get", cacheSec,
                () -> get(key)
        );
    }

    public static void scan(String key, Consumer<String> fun) {
        doScan(0, key, fun);
    }

    public static Future<Void> publish(String key, String message) {
        return Future.future(promise ->
                pubRedisConn.send(Request.cmd(Command.PUBLISH).arg(key).arg(message), reply -> {
                    if (reply.succeeded()) {
                        promise.complete();
                        return;
                    }
                    log.error("[Redis]Publish [{}] error: {}", key, reply.cause().getMessage(), reply.cause());
                    promise.fail(reply.cause());
                })
        );
    }

    public static Future<Void> subscribe(String key, Consumer<String> fun) {
        return Future.future(promise ->
                subRedisConn.send(Request.cmd(Command.SUBSCRIBE).arg(key), reply -> {
                    if (reply.succeeded()) {
                        innerVertx.eventBus().consumer("io.vertx.redis." + key, msg -> {
                            fun.accept(((JsonObject) msg.body()).getJsonObject("value").getString("message"));
                        });
                        promise.complete();
                        return;
                    }
                    log.error("[Redis]Subscribe [{}] error: {}", key, reply.cause().getMessage(), reply.cause());
                    promise.fail(reply.cause());
                })
        );
    }

    private static void doScan(Integer cursor, String key, Consumer<String> fun) {
        redisClient.scan(new ArrayList<>() {
            {
                add(cursor + "");
                add("MATCH");
                add(key + "*");
            }
        }).onSuccess(response -> {
            response.get(1).forEach(returnKey -> fun.accept(returnKey.toString()));
            var newCursor = response.get(0).toInteger();
            if (newCursor != 0) {
                doScan(newCursor, key, fun);
            }
        }).onFailure(e -> log.error("[Redis]Scan [{}] error: {}", key, e.getMessage(), e));
    }

}
