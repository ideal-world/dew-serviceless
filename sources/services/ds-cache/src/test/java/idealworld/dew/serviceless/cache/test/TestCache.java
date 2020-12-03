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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import idealworld.dew.serviceless.cache.CacheApplication;
import idealworld.dew.serviceless.cache.CacheConfig;
import idealworld.dew.serviceless.cache.CacheConstant;
import idealworld.dew.serviceless.common.CommonConfig;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.funs.cache.RedisClient;
import idealworld.dew.serviceless.common.funs.httpclient.HttpClient;
import idealworld.dew.serviceless.common.util.YamlHelper;
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
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("someValue"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.CREATE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("请求的资源主题[" + resourceSubjectCode + "]不存在", Resp.generic(resp.bodyAsString(), Void.class).getMessage());
                    return Future.succeededFuture();
                })
                .compose(resp -> {
                    RedisClient.init(resourceSubjectCode, vertx, CommonConfig.RedisConfig.builder()
                            .uri("redis://localhost:6379/10").build());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("someValue"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.CREATE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.FETCH.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("someValue", Resp.generic(resp.bodyAsString(), String.class).getBody());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.EXISTS.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("true", Resp.generic(resp.bodyAsString(), String.class).getBody());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.MODIFY.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey?" + CacheConstant.REQUEST_ATTR_EXPIRE + "=1");
                                    }
                                })
                )
                .compose(resp ->
                        Future.future(promise -> vertx.setTimer(1000L, h ->
                                HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                        new HashMap<>() {
                                            {
                                                put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.EXISTS.toString());
                                                put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                            }
                                        }).onSuccess(promise::complete))
                        ))
                .compose(resp -> {
                    Assertions.assertEquals("false", Resp.generic(((HttpResponseImpl) resp).body().toString(), String.class).getBody());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.DELETE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.EXISTS.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("false", Resp.generic(resp.bodyAsString(), String.class).getBody());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("孤岛旭日"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.CREATE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey/f1");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.FETCH.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/someKey/f1");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("孤岛旭日", Resp.generic(resp.bodyAsString(), String.class).getBody());
                    return Future.succeededFuture();
                })
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, null,
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.DELETE.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/incr/f1");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("5"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.MODIFY.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/incr/f1?" + CacheConstant.REQUEST_ATTR_INCR + "=true");
                                    }
                                })
                )
                .compose(resp ->
                        HttpClient.choose("").request(HttpMethod.POST, "http://127.0.0.1:" + cacheConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, Buffer.buffer("10"),
                                new HashMap<>() {
                                    {
                                        put(Constant.REQUEST_RESOURCE_ACTION_FLAG, OptActionKind.MODIFY.toString());
                                        put(Constant.REQUEST_RESOURCE_URI_FLAG, "cache://" + resourceSubjectCode + "/incr/f1?" + CacheConstant.REQUEST_ATTR_INCR + "=true");
                                    }
                                })
                )
                .compose(resp -> {
                    Assertions.assertEquals("15", Resp.generic(resp.bodyAsString(), String.class).getBody());
                    return Future.succeededFuture();
                })
                .onSuccess(resp -> testContext.completeNow());
    }

}
