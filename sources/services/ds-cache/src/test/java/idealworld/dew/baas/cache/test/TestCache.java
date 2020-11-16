package idealworld.dew.baas.cache.test;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.cache.CacheApplication;
import idealworld.dew.baas.cache.CacheConfig;
import idealworld.dew.baas.cache.CacheConstant;
import idealworld.dew.baas.common.CommonConfig;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.OptActionKind;
import idealworld.dew.baas.common.funs.cache.RedisClient;
import idealworld.dew.baas.common.funs.httpclient.HttpClient;
import idealworld.dew.baas.common.util.YamlHelper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.impl.HttpResponseImpl;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TestCache extends BasicTest {

    private static CacheConfig cacheConfig;

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        cacheConfig = YamlHelper.toObject(CacheConfig.class, $.file.readAllByClassPath("application-test.yml", StandardCharsets.UTF_8));
        System.getProperties().put("dew.profile", "test");
        RedisTestHelper.start();
        vertx.deployVerticle(new CacheApplication(), testContext.succeedingThenComplete());
    }

    @Test
    public void testCache(Vertx vertx, VertxTestContext testContext) {

        var resourceSubjectCode = "cxxx";

        Future.succeededFuture()
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("someValue"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.CREATE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("请求的资源主题不存在", resp.bodyAsString());
                    return Future.succeededFuture();
                })
                .compose(resp -> {
                    RedisClient.init(resourceSubjectCode, vertx, CommonConfig.RedisConfig.builder()
                            .uri("redis://localhost:6379/10").build());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("someValue"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.CREATE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.FETCH.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("someValue", resp.bodyAsString());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.EXISTS.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("true", resp.bodyAsString());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.MODIFY.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey?" + CacheConstant.REQUEST_ATTR_EXPIRE + "=1");
                                    }
                                })
                )
                .compose(resp ->
                        Future.future(promise -> vertx.setTimer(1000L, h ->
                                HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                        new HashMap<>() {
                                            {
                                                put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.EXISTS.toString());
                                                put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                            }
                                        }).onSuccess(promise::complete))
                        ))
                .compose(resp -> {
                    Assertions.assertEquals("false", ((HttpResponseImpl) resp).body().toString());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.DELETE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.EXISTS.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("false", resp.bodyAsString());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("孤岛旭日"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.CREATE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey/f1");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.FETCH.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey/f1");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("孤岛旭日", resp.bodyAsString());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.DELETE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/incr/f1");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("5"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.MODIFY.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/incr/f1?" + CacheConstant.REQUEST_ATTR_INCR + "=true");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("10"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.MODIFY.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/incr/f1?" + CacheConstant.REQUEST_ATTR_INCR + "=true");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("15", resp.bodyAsString());
                    return Future.succeededFuture();
                })
                .onSuccess(resp -> testContext.completeNow());
    }

}
