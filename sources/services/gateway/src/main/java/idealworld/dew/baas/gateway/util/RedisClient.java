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
import java.util.List;
import java.util.function.Consumer;

/**
 * @author gudaoxuri
 */
@Slf4j
public class RedisClient {

    private static final String CACHE_KEY_PREFIX = "redis:";
    private static RedisAPI redisClient;
    private static Redis subRedis;
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
                .onSuccess(conn -> log.info("Redis has connected {}", config.getUri()))
                .onFailure(e -> {
                    log.error("Redis connection error {}", e.getMessage(), e);
                    throw new RTException(e);
                });
        subRedis = Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setPassword(config.getPassword()));
        subRedis.connect()
                .onSuccess(conn -> log.info("Redis subscribe has connected {}", config.getUri()))
                .onFailure(e -> {
                    log.error("Redis subscribe connection error {}", e.getMessage(), e);
                    throw new RTException(e);
                });
        Runtime.getRuntime().addShutdownHook(new Thread(redis::close));
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
                    log.error("Redis set [{}:{}] error {}", key, value, e.getMessage(), e);
                    promise.fail(e.getCause());
                })
        );
    }

    public static Future<Boolean> exists(List<String> keys) {
        return Future.future(promise ->
                redisClient.exists(keys)
                        .onSuccess(response -> promise.complete(response.toInteger() > 0))
                        .onFailure(e -> {
                            log.error("Redis exists [{}] error {}", String.join(",", keys), e.getMessage(), e);
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
                            log.error("Redis get [{}] error {}", key, e.getMessage(), e);
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
                redisClient.publish(key, message)
                        .onSuccess(response ->
                                promise.complete())
                        .onFailure(e -> {
                            log.error("Redis publish [{}] error {}", key, e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public static Future<Void> subscribe(String key, Consumer<String> fun) {
        return Future.future(promise -> {
            subRedis.send(Request.cmd(Command.SUBSCRIBE).arg(key), reply -> {
                innerVertx.eventBus().consumer("io.vertx.redis." + key, msg -> {
                    fun.accept(((JsonObject) msg.body()).getJsonObject("value").getString("message"));
                });
                promise.complete();
            });
        });
    }

    private static void doScan(Integer cursor, String key, Consumer<String> fun) {
        redisClient.scan(new ArrayList<>() {
            {
                add(cursor + "");
                add("MATCH");
                add(key + "*");
            }
        }).onSuccess(response -> {
            response.get(1).forEach(returnKey -> {
                fun.accept(returnKey.toString());
            });
            var newCursor = response.get(0).toInteger();
            if (newCursor != 0) {
                doScan(newCursor, key, fun);
            }
        }).onFailure(e -> log.error("Redis scan [{}] error {}", key, e.getMessage(), e));
    }

}
