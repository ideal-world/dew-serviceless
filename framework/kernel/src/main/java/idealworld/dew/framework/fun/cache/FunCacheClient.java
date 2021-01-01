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

package idealworld.dew.framework.fun.cache;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.framework.DewConfig;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.*;
import io.vertx.redis.client.impl.types.BulkType;
import io.vertx.redis.client.impl.types.MultiType;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
@Slf4j
public class FunCacheClient {

    private static final String CACHE_KEY_PREFIX = "redis:";
    private static final String CACHE_KEY_ELECTION_PREFIX = "dew:cluster:election:";
    private static final Map<String, FunCacheClient> REDIS_CLIENTS = new ConcurrentHashMap<>();
    private final String instanceId = $.field.createUUID();
    private String code;
    private Integer electionPeriodSec;
    protected AtomicBoolean leader = new AtomicBoolean(false);
    private Vertx innerVertx;
    private RedisAPI redisAPI;
    private Redis subRedis;
    private Redis pubRedis;
    private RedisConnection subRedisConn;
    private RedisConnection pubRedisConn;

    public static Future<Void> init(String code, Vertx vertx, DewConfig.FunConfig.CacheConfig config) {
        Promise<Void> promise = Promise.promise();
        var redisClient = new FunCacheClient();
        if (config.getPassword() != null && config.getPassword().isBlank()) {
            config.setPassword(null);
        }
        redisClient.code = code;
        redisClient.innerVertx = vertx;
        redisClient.electionPeriodSec = config.getElectionPeriodSec();
        var redis = Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setPassword(config.getPassword())
                        .setMaxPoolSize(config.getMaxPoolSize())
                        .setMaxPoolWaiting(config.getMaxPoolWaiting()));
        redisClient.redisAPI = RedisAPI.api(redis);
        REDIS_CLIENTS.put(code, redisClient);
        redis.connect()
                .onSuccess(conn -> {
                    log.info("[Redis][{}]Connected {}", code, config.getUri());
                    promise.complete();
                })
                .onFailure(e -> {
                    log.error("[Redis][{}]Connection error: {}", code, e.getMessage(), e);
                    throw new RTException(e);
                });
        redisClient.subRedis = Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setPassword(config.getPassword()))
                .connect(conn -> {
                    if (conn.succeeded()) {
                        log.info("[Redis][{}]Subscribe connected {}", code, config.getUri());
                        redisClient.subRedisConn = conn.result();
                        return;
                    }
                    log.error("[Redis][{}]Subscribe connection error: {}", code, conn.cause().getMessage(), conn.cause());
                    throw new RTException(conn.cause());
                });
        redisClient.pubRedis = Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setPassword(config.getPassword()))
                .connect(conn -> {
                    if (conn.succeeded()) {
                        log.info("[Redis][{}]Publish connected {}", code, config.getUri());
                        redisClient.pubRedisConn = conn.result();
                        return;
                    }
                    log.error("[Redis][{}]Publish connection error: {}", code, conn.cause().getMessage(), conn.cause());
                    throw new RTException(conn.cause());
                });
        redisClient.election();
        return promise.future();
    }

    public static CompositeFuture destroy() {
        return CompositeFuture.all(REDIS_CLIENTS.values().stream()
                .map(FunCacheClient::close)
                .collect(Collectors.toList()));
    }

    public static FunCacheClient choose(String code) {
        return REDIS_CLIENTS.get(code);
    }

    public static Boolean contains(String code) {
        return REDIS_CLIENTS.containsKey(code);
    }

    public static void remove(String code) {
        REDIS_CLIENTS.remove(code);
    }

    public Future<Void> close() {
        return redisAPI.get(CACHE_KEY_ELECTION_PREFIX + code)
                .compose(getResult -> {
                    // 如果当前是领导者则执行销毁
                    if (getResult != null
                            && getResult.toString(StandardCharsets.UTF_8).equalsIgnoreCase(instanceId)) {
                        return redisAPI.del(new ArrayList<>() {
                            {
                                add(CACHE_KEY_ELECTION_PREFIX + code);
                            }
                        });
                    } else {
                        return Future.succeededFuture();
                    }
                })
                .compose(resp -> {
                    // Auto close
                    /*redisAPI.close();
                    subRedis.close();
                    pubRedis.close();*/
                    return Future.succeededFuture();
                });
    }

    // ---------------------------- String ----------------------------

    public Future<Void> expire(String key, Long expireSec) {
        return Future.future(promise ->
                redisAPI.expire(key, String.valueOf(expireSec)).onSuccess(response ->
                        promise.complete()
                ).onFailure(e -> {
                    log.error("[Redis][{}]Expire [{}] error: {}", code, key, e.getMessage(), e);
                    promise.fail(e.getCause());
                })
        );
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
                    log.error("[Redis][{}]Set [{}:{}] error: {}", code, key, value, e.getMessage(), e);
                    promise.fail(e.getCause());
                })
        );
    }

    public Future<Void> setex(String key, String value, Long expireSec) {
        return Future.future(promise ->
                redisAPI.setex(key, String.valueOf(expireSec), value).onSuccess(response ->
                        promise.complete()
                ).onFailure(e -> {
                    log.error("[Redis][{}]Setex [{}:{}] error: {}", code, key, value, e.getMessage(), e);
                    promise.fail(e.getCause());
                })
        );
    }

    public Future<Boolean> setnx(String key, String value) {
        return Future.future(promise ->
                redisAPI.setnx(key, value).onSuccess(response ->
                        promise.complete(response.toInteger() > 0)
                ).onFailure(e -> {
                    log.error("[Redis][{}]Setnx [{}:{}] error: {}", code, key, value, e.getMessage(), e);
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
                            log.error("[Redis][{}]Get [{}] error: {}", code, key, e.getMessage(), e);
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

    public Future<Boolean> exists(String... keys) {
        return Future.future(promise ->
                redisAPI.exists(Arrays.asList(keys))
                        .onSuccess(response -> promise.complete(response.toInteger() > 0))
                        .onFailure(e -> {
                            log.error("[Redis][{}]Exists [{}] error: {}", code, String.join(",", keys), e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }


    public Future<Void> del(String... keys) {
        return Future.future(promise ->
                redisAPI.del(Arrays.asList(keys)).onSuccess(response ->
                        promise.complete()
                ).onFailure(e -> {
                    log.error("[Redis][{}]Del [{}] error: {}", code, String.join(",", keys), e.getMessage(), e);
                    promise.fail(e.getCause());
                })
        );
    }

    public Future<Long> incrby(String key, Integer step) {
        return Future.future(promise ->
                redisAPI.incrby(key, String.valueOf(step))
                        .onSuccess(response -> promise.complete(
                                response != null ? response.toLong() : null))
                        .onFailure(e -> {
                            log.error("[Redis][{}]Incrby [{}] error: {}", code, key, e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    // ---------------------------- Hash ----------------------------

    public Future<Void> hset(String key, String fieldKey, String value) {
        return Future.future(promise ->
                redisAPI.hset(new ArrayList<>() {
                    {
                        add(key);
                        add(fieldKey);
                        add(value);
                    }
                })
                        .onSuccess(response -> promise.complete(null))
                        .onFailure(e -> {
                            log.error("[Redis][{}]Hset [{}-{}] error: {}", code, key, fieldKey, e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public Future<String> hget(String key, String fieldKey) {
        return Future.future(promise ->
                redisAPI.hget(key, fieldKey)
                        .onSuccess(response -> promise.complete(
                                response != null ? response.toString(StandardCharsets.UTF_8) : null))
                        .onFailure(e -> {
                            log.error("[Redis][{}]Hget [{}-{}] error: {}", code, key, fieldKey, e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public Future<Map<String, String>> hgetall(String key) {
        return Future.future(promise ->
                redisAPI.hgetall(key)
                        .onSuccess(response -> {
                            var result = new HashMap<String, String>();
                            var it = response.stream().iterator();
                            while (it.hasNext()) {
                                var item = it.next();
                                if (item instanceof MultiType) {
                                    result.put(item.get(0).toString(), ((BulkType) item.get(1)).toString());
                                } else {
                                    // TODO 在某些时候会变成单值
                                    result.put(item.toString(), it.next().toString());
                                }
                            }
                            promise.complete(result);
                        })
                        .onFailure(e -> {
                            log.error("[Redis][{}]Hgetall [{}] error: {}", code, key, e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public Future<Boolean> hexists(String key, String fieldKey) {
        return Future.future(promise ->
                redisAPI.hexists(key, fieldKey)
                        .onSuccess(response -> promise.complete(response.toInteger() > 0))
                        .onFailure(e -> {
                            log.error("[Redis][{}]Hexists [{}-{}] error: {}", code, key, fieldKey, e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public Future<Void> hdel(String key, String... fieldKeys) {
        var args = new ArrayList<String>();
        args.add(key);
        args.addAll(Arrays.asList(fieldKeys));
        return Future.future(promise ->
                redisAPI.hdel(args)
                        .onSuccess(response -> promise.complete(null))
                        .onFailure(e -> {
                            log.error("[Redis][{}]Hdel [{}-{}] error: {}", key, code, String.join(",", fieldKeys), e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    public Future<Long> hincrby(String key, String fieldKey, Integer step) {
        return Future.future(promise ->
                redisAPI.hincrby(key, fieldKey, String.valueOf(step))
                        .onSuccess(response -> promise.complete(
                                response != null ? response.toLong() : null))
                        .onFailure(e -> {
                            log.error("[Redis][{}]Hincrby [{}-{}] error: {}", code, key, fieldKey, e.getMessage(), e);
                            promise.fail(e.getCause());
                        })
        );
    }

    // ---------------------------- Others ----------------------------


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
                    log.error("[Redis][{}]Publish [{}] error: {}", code, key, reply.cause().getMessage(), reply.cause());
                    promise.fail(reply.cause());
                })
        );
    }

    public Future<Void> subscribe(String key, Consumer<String> fun) {
        return Future.future(promise ->
                subRedisConn.send(Request.cmd(Command.SUBSCRIBE).arg(key), reply -> {
                    if (reply.succeeded()) {
                        innerVertx.eventBus().consumer("io.vertx.redis." + key, msg ->
                                fun.accept(((JsonObject) msg.body()).getJsonObject("value").getString("message")));
                        promise.complete();
                        return;
                    }
                    log.error("[Redis][{}]Subscribe [{}] error: {}", code, key, reply.cause().getMessage(), reply.cause());
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
            response.get(1).forEach(returnKey -> fun.accept(returnKey.toString(StandardCharsets.UTF_8)));
            var newCursor = response.get(0).toInteger();
            if (newCursor != 0) {
                doScan(newCursor, key, fun);
            }
        }).onFailure(e -> log.error("[Redis][{}]Scan [{}] error: {}", code, key, e.getMessage(), e));
    }

    public Boolean isLeader() {
        return leader.get();
    }

    private void election() {
        innerVertx.setPeriodic(electionPeriodSec * 1000, event -> doElection());
    }

    private void doElection() {
        log.trace("[Redis]Electing...");
        redisAPI.set(new ArrayList<>() {
            {
                add(CACHE_KEY_ELECTION_PREFIX + code);
                add(instanceId);
                add("EX");
                add((electionPeriodSec * 2 + 2) + "");
                add("NX");
            }
        })
                .onSuccess(setResult -> {
                    if (setResult != null && setResult.toString().equalsIgnoreCase("ok")) {
                        leader.set(true);
                    } else {
                        redisAPI.get(CACHE_KEY_ELECTION_PREFIX + code)
                                .onSuccess(getResult -> {
                                    if (getResult == null) {
                                        doElection();
                                    } else {
                                        leader.set(getResult.toString(StandardCharsets.UTF_8).equalsIgnoreCase(instanceId));
                                    }
                                })
                                .onFailure(e ->
                                        log.error("[Redis][{}]Election [{}] error: {}", code, CACHE_KEY_ELECTION_PREFIX + code, e.getMessage(), e)
                                );
                    }
                })
                .onFailure(e ->
                        log.error("[Redis][{}]Election [{}] error: {}", code, CACHE_KEY_ELECTION_PREFIX + code, e.getMessage(), e)
                );
    }

}
