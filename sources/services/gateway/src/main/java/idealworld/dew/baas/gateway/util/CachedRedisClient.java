package idealworld.dew.baas.gateway.util;

import idealworld.dew.baas.gateway.GatewayConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * @author gudaoxuri
 */
@Slf4j
public class CachedRedisClient {

    private static final String CACHE_KEY_PREFIX = "redis:";
    private static RedisAPI redisClient;

    public static void init(Vertx vertx, GatewayConfig.RedisConfig config) {
        var redis = Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setPassword(config.getPassword())
                        .setMaxPoolSize(config.getMaxPoolSize())
                        .setMaxWaitingHandlers(config.getMaxPoolWaiting()));
        log.info("Created redis client: {}", config.getUri());
        redisClient = RedisAPI.api(redis);
    }

    public static Future<String> get(String key, Integer cacheSec) {
        return FutureCacheHelper.getSetF(CACHE_KEY_PREFIX + key, cacheSec,
                () -> Future.future(promise ->
                        redisClient.get(key)
                                .onSuccess(resp -> promise.complete(
                                        resp != null ? resp.toString() : null))
                                .onFailure(e -> {
                                    log.error("Redis get [{}] error {}", key, e.getMessage(), e);
                                    promise.fail(e.getCause());
                                })
                ));
    }

    public static void scan(String key, Consumer<String> fun) {
        doScan(0, key, fun);
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
        }).onFailure(e -> {
            log.error("Redis scan [{}] error {}", key, e.getMessage(), e);
        });
    }

}
