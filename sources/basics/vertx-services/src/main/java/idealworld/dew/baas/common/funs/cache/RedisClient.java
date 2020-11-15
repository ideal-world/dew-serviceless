package idealworld.dew.baas.common.funs.cache;

import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.baas.common.CommonConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author gudaoxuri
 */
@Slf4j
public class RedisClient {

    private static final String CACHE_KEY_PREFIX = "redis:";
    private static final Map<String, RedisClient> REDIS_CLIENTS = new HashMap<>();
    private Vertx innerVertx;
    private RedisAPI redisAPI;
    private RedisConnection subRedisConn;
    private RedisConnection pubRedisConn;

    public static void init(String code, Vertx vertx, CommonConfig.RedisConfig config) {
        var redisClient = new RedisClient();
        redisClient.innerVertx = vertx;
        var redis = Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setPassword(config.getPassword())
                        .setMaxPoolSize(config.getMaxPoolSize())
                        .setMaxPoolWaiting(config.getMaxPoolWaiting()));
        redisClient.redisAPI = RedisAPI.api(redis);
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
                        redisClient.subRedisConn = conn.result();
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
                        redisClient.pubRedisConn = conn.result();
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
        REDIS_CLIENTS.put(code, redisClient);
    }

    public static RedisClient choose(String code) {
        return REDIS_CLIENTS.get(code);
    }

    public static Boolean contains(String code) {
        return REDIS_CLIENTS.containsKey(code);
    }

    public static void remove(String code) {
        REDIS_CLIENTS.remove(code);
    }

    public Future<Void> set(String key, String value) {
        return Future.future(promise ->
                redisAPI.set(new ArrayList<>() {
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

    public Future<Void> del(String... keys) {
        return Future.future(promise ->
                redisAPI.del(Arrays.asList(keys)).onSuccess(response ->
                        promise.complete()
                ).onFailure(e -> {
                    log.error("[Redis]Del [{}] error: {}", String.join(",", keys), e.getMessage(), e);
                    promise.fail(e.getCause());
                })
        );
    }

    public Future<Boolean> exists(String... keys) {
        return Future.future(promise ->
                redisAPI.exists(Arrays.asList(keys))
                        .onSuccess(response -> promise.complete(response.toInteger() > 0))
                        .onFailure(e -> {
                            log.error("[Redis]Exists [{}] error: {}", String.join(",", keys), e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public Future<String> get(String key) {
        return Future.future(promise ->
                redisAPI.get(key)
                        .onSuccess(response -> promise.complete(
                                response != null ? response.toString(StandardCharsets.UTF_8) : null))
                        .onFailure(e -> {
                            log.error("[Redis]Get [{}] error: {}", key, e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public Future<String> get(String key, Integer cacheSec) {
        if (cacheSec == null || cacheSec <= 0) {
            return get(key);
        }
        return LocalCacheHelper.getSetF(CACHE_KEY_PREFIX + key + "-get", cacheSec,
                () -> get(key)
        );
    }

    public void scan(String key, Consumer<String> fun) {
        doScan(0, key, fun);
    }

    public Future<Void> publish(String key, String message) {
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

    public Future<Void> subscribe(String key, Consumer<String> fun) {
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

    private void doScan(Integer cursor, String key, Consumer<String> fun) {
        redisAPI.scan(new ArrayList<>() {
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
