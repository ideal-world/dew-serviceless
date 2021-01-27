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

package idealworld.dew.serviceless.gateway.test;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.auth.dto.ResourceSubjectExchange;
import idealworld.dew.framework.fun.cache.FunCacheClient;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.gateway.GatewayConfig;
import idealworld.dew.serviceless.gateway.GatewayModule;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 网关流程测试.
 *
 * @author gudaoxuri
 */
public class GatewayFlowTest extends DewTest {

    static {
        enableRedis();
    }

    private static final String MODULE_NAME = new GatewayModule().getModuleName();
    private static final GatewayConfig.Security DEW_SECURITY_CONFIG = GatewayConfig.Security.builder().build();

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new GatewayApplicationTest(), testContext.succeedingThenComplete());
    }

    @Test
    public void testError(Vertx vertx, VertxTestContext testContext) {
        Assertions.assertTrue($.http.post("http://127.0.0.1:9000/err-path", "").contains("Resource not found"));
        var result = Resp.generic($.http.post("http://127.0.0.1:9000/exec", ""), Void.class);
        Assertions.assertEquals("请求格式不合法，缺少query", result.getMessage());
        result = Resp.generic($.http.post("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG,
                ""), Void.class);
        Assertions.assertTrue(result.getMessage().contains("请求格式不合法，缺少"));
        result = Resp.generic($.http.post("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                        "=&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=",
                ""), Void.class);
        Assertions.assertTrue(result.getMessage().contains("请求格式不合法，缺少"));
        result = Resp.generic($.http.post("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http//iam.service/console/tenant/account", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                ""), Void.class);
        Assertions.assertTrue(result.getMessage().contains("资源URI错误"));
        result = Resp.generic($.http.post("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://iam.service/console/tenant/account", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=createx",
                ""), Void.class);
        Assertions.assertTrue(result.getMessage().contains("资源类型或操作类型不存在"));
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testPublic(Vertx vertx, VertxTestContext testContext) {
        // 添加资源主体
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resourcesubject.http/httpbin",
                JsonObject.mapFrom(ResourceSubjectExchange.builder()
                        .code("1.http.httpbin")
                        .name("测试API")
                        .kind(ResourceKind.HTTP)
                        .uri("https://httpbin.org")
                        .ak("")
                        .sk("")
                        .platformAccount("")
                        .platformProjectId("")
                        .timeoutMs(10000L)
                        .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);

        var result = $.http.postWrap("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://1.http.httpbin/post", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容");
        Assertions.assertEquals(200, result.statusCode);
        var data = new JsonObject(result.result);
        Assertions.assertEquals("测试内容", data.getJsonObject("body").getString("data"));
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testToken(Vertx vertx, VertxTestContext testContext) {
        // 添加资源主体
        FunEventBus.choose(MODULE_NAME).publish("", OptActionKind.CREATE, "eb://iam/resourcesubject.http/httpbin",
                JsonObject.mapFrom(ResourceSubjectExchange.builder()
                        .code("1.http.httpbin")
                        .name("测试API")
                        .kind(ResourceKind.HTTP)
                        .uri("https://httpbin.org")
                        .ak("")
                        .sk("")
                        .platformAccount("")
                        .platformProjectId("")
                        .timeoutMs(10000L)
                        .build()).toBuffer(), new HashMap<>());
        Thread.sleep(1000);

        var errorResult = $.http.postWrap("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://1.http.httpbin/post", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(DEW_SECURITY_CONFIG.getTokenFieldName(), "tokenxxx");
                    }
                });
        Assertions.assertEquals("认证错误，Token不合法", Resp.generic(errorResult.result, Void.class).getMessage());

        FunCacheClient.choose(MODULE_NAME).set(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + "tokenxxx", "{\"accountCode\":\"testCode\"}");
        vertx.setTimer(1000, h -> {
            var result = $.http.postWrap("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                            "=" + URLEncoder.encode("http://1.http.httpbin/post", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                    "测试内容", new HashMap<>() {
                        {
                            put(DEW_SECURITY_CONFIG.getTokenFieldName(), "tokenxxx");
                        }
                    });
            Assertions.assertEquals(200, result.statusCode);
            var data = new JsonObject(result.result);
            Assertions.assertEquals("测试内容", data.getJsonObject("body").getString("data"));
            testContext.completeNow();
        });
    }

    @Test
    public void testAkSk(Vertx vertx, VertxTestContext testContext) {
        var errorResult = $.http.postWrap("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(DEW_SECURITY_CONFIG.getAkSkFieldName(), "xx");
                    }
                });
        Assertions.assertEquals("请求格式不合法，HTTP Header[" + DEW_SECURITY_CONFIG.getAkSkDateFieldName() + "]不存在", Resp.generic(errorResult.result, Void.class).getMessage());
        var sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        var date = new Date();
        date.setTime(System.currentTimeMillis() - 6000);
        errorResult = $.http.postWrap("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(DEW_SECURITY_CONFIG.getAkSkFieldName(), "xxxx");
                        put(DEW_SECURITY_CONFIG.getAkSkDateFieldName(), sdf.format(date));
                    }
                });
        Assertions.assertEquals("请求格式不合法，HTTP Header[" + DEW_SECURITY_CONFIG.getAkSkFieldName() + "]格式错误", Resp.generic(errorResult.result, Void.class).getMessage());
        errorResult = $.http.postWrap("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(DEW_SECURITY_CONFIG.getAkSkFieldName(), "xx:xxx");
                        put(DEW_SECURITY_CONFIG.getAkSkDateFieldName(), sdf.format(date));
                    }
                });
        Assertions.assertEquals("认证错误，请求时间已过期", Resp.generic(errorResult.result, Void.class).getMessage());
        errorResult = $.http.postWrap("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(DEW_SECURITY_CONFIG.getAkSkFieldName(), "xx:xxx");
                        put(DEW_SECURITY_CONFIG.getAkSkDateFieldName(), sdf.format(new Date()));
                    }
                });
        Assertions.assertEquals("认证错误，AK不存在", Resp.generic(errorResult.result, Void.class).getMessage());

        FunCacheClient.choose(MODULE_NAME).set(DewAuthConstant.CACHE_APP_AK + "xx", "skxx:1123456:789:acxxxx");
        vertx.setTimer(1000, h -> {
            var result = $.http.postWrap("http://127.0.0.1:9000/exec?" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                            "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                    "测试内容", new HashMap<>() {
                        {
                            put(DEW_SECURITY_CONFIG.getAkSkFieldName(), "xx:xxx");
                            put(DEW_SECURITY_CONFIG.getAkSkDateFieldName(), sdf.format(new Date()));
                        }
                    });
            Assertions.assertEquals("认证错误，签名不合法", Resp.generic(result.result, Void.class).getMessage());

            var d = sdf.format(new Date());
            var query = DewAuthConstant.REQUEST_RESOURCE_URI_FLAG +
                    "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "=create";
            query = URIHelper.sortQuery(query);
            var signature = $.security.encodeStringToBase64(
                    $.security.digest.digest(("POST\n" + d + "\n/exec\n" + query).toLowerCase(),
                            "skxx", "HmacSHA1"), StandardCharsets.UTF_8);
            result = $.http.postWrap("http://127.0.0.1:9000/exec?" + query,
                    "测试内容", new HashMap<>() {
                        {
                            put(DEW_SECURITY_CONFIG.getAkSkFieldName(), "xx:" + signature);
                            put(DEW_SECURITY_CONFIG.getAkSkDateFieldName(), d);
                        }
                    });
            Assertions.assertEquals(200, result.statusCode);
            Assertions.assertEquals("找不到请求的资源主体[httpbin.org]", Resp.generic(result.result, Object.class).getMessage());
            testContext.completeNow();
        });

    }

}
