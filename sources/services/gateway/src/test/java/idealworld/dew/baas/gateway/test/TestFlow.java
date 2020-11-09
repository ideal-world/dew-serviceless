package idealworld.dew.baas.gateway.test;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.funs.cache.RedisClient;
import idealworld.dew.baas.common.util.YamlHelper;
import idealworld.dew.baas.gateway.GatewayApplication;
import idealworld.dew.baas.gateway.GatewayConfig;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class TestFlow extends BasicTest {

    private GatewayConfig gatewayConfig;

    @Before
    public void before(TestContext testContext) {
        gatewayConfig = YamlHelper.toObject(GatewayConfig.class, $.file.readAllByClassPath("application-test.yml", StandardCharsets.UTF_8));
        System.getProperties().put("dew.profile", "test");
        RedisTestHelper.start();
        Vertx vertx = rule.vertx();
        vertx.deployVerticle(new GatewayApplication(), testContext.asyncAssertSuccess());
    }

    @Test
    public void testError(TestContext testContext) {
        var result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + "/err-path", "");
        testContext.assertTrue(result.contains("Resource not found"));
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath(), "");
        testContext.assertEquals("请求格式不合法，缺少query", result);
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey(),
                "");
        testContext.assertTrue(result.contains("请求格式不合法，缺少"));
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey() +
                        "=&" + gatewayConfig.getRequest().getActionKey() + "=",
                "");
        testContext.assertTrue(result.contains("请求格式不合法，缺少"));
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey() +
                        "=" + URLEncoder.encode("http//iam.service/console/tenant/account", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create",
                "");
        testContext.assertTrue(result.contains("资源URI错误"));
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey() +
                        "=" + URLEncoder.encode("http://iam.service/console/tenant/account", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=createx",
                "");
        testContext.assertTrue(result.contains("资源类型或操作类型不存在"));
    }


    @Test
    public void testPublic(TestContext testContext) {
        var result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey() +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create",
                "测试内容");
        testContext.assertEquals(200, result.statusCode);
        var data = $.json.toJson(result.result);
        testContext.assertTrue(data.get("headers").has(gatewayConfig.getDistribute().getIdentOptHeaderName()));
        testContext.assertEquals("测试内容", data.get("data").asText());
    }

    @Test
    public void testToken(TestContext testContext) {
        var async = testContext.async();
        var errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey() +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getTokenFieldName(), "tokenxxx");
                    }
                });
        testContext.assertEquals("认证错误，Token不合法", errorResult.result);

        RedisClient.set(gatewayConfig.getSecurity().getCacheTokenInfoKey() + "tokenxxx", "{\"accountCode\":\"testCode\"}");
        rule.vertx().setTimer(1000, h -> {
            var result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                            "?" + gatewayConfig.getRequest().getResourceUriKey() +
                            "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create",
                    "测试内容", new HashMap<>() {
                        {
                            put(gatewayConfig.getSecurity().getTokenFieldName(), "tokenxxx");
                        }
                    });
            testContext.assertEquals(200, result.statusCode);
            var data = $.json.toJson(result.result);
            testContext.assertTrue(data.get("headers").has(gatewayConfig.getDistribute().getIdentOptHeaderName()));
            testContext.assertTrue(data.get("headers").get(gatewayConfig.getDistribute().getIdentOptHeaderName()).asText().contains("testCode"));
            testContext.assertEquals("测试内容", data.get("data").asText());
            async.complete();
        });
    }

    @Test
    public void testAksk(TestContext testContext) {
        var async = testContext.async();
        var errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey() +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx");
                    }
                });
        testContext.assertEquals("请求格式不合法，HTTP Header[" + gatewayConfig.getSecurity().getAkSkDateFieldName() + "]不存在", errorResult.result);
        var sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        var date = new Date();
        date.setTime(System.currentTimeMillis() - 6000);
        errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey() +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getAkSkFieldName(), "xxxx");
                        put(gatewayConfig.getSecurity().getAkSkDateFieldName(), sdf.format(date));
                    }
                });
        testContext.assertEquals("请求格式不合法，HTTP Header[" + gatewayConfig.getSecurity().getAkSkFieldName() + "]格式错误", errorResult.result);
        errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey() +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx:xxx");
                        put(gatewayConfig.getSecurity().getAkSkDateFieldName(), sdf.format(date));
                    }
                });
        testContext.assertEquals("认证错误，请求时间已过期", errorResult.result);
        errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                        "?" + gatewayConfig.getRequest().getResourceUriKey() +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx:xxx");
                        put(gatewayConfig.getSecurity().getAkSkDateFieldName(), sdf.format(new Date()));
                    }
                });
        testContext.assertEquals("认证错误，AK不存在", errorResult.result);

        RedisClient.set(gatewayConfig.getSecurity().getCacheAkSkInfoKey() + "xx", "skxx:1123456:789");
        rule.vertx().setTimer(1000, h -> {
            var result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() +
                            "?" + gatewayConfig.getRequest().getResourceUriKey() +
                            "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create",
                    "测试内容", new HashMap<>() {
                        {
                            put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx:xxx");
                            put(gatewayConfig.getSecurity().getAkSkDateFieldName(), sdf.format(new Date()));
                        }
                    });
            testContext.assertEquals("认证错误，签名不合法", result.result);

            var d = sdf.format(new Date());
            var query = gatewayConfig.getRequest().getResourceUriKey() +
                    "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + gatewayConfig.getRequest().getActionKey() + "=create";
            var signature = $.security.encodeStringToBase64(
                    $.security.digest.digest(("post\n" + d + "\n" + gatewayConfig.getRequest().getPath() + "\n" + query).toLowerCase(),
                            "skxx", "HmacSHA1"), StandardCharsets.UTF_8);
            result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + gatewayConfig.getRequest().getPath() + "?" + query,
                    "测试内容", new HashMap<>() {
                        {
                            put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx:" + signature);
                            put(gatewayConfig.getSecurity().getAkSkDateFieldName(), d);
                        }
                    });
            testContext.assertEquals(200, result.statusCode);
            var data = $.json.toJson(result.result);
            testContext.assertTrue(data.get("headers").has(gatewayConfig.getDistribute().getIdentOptHeaderName()));
            testContext.assertTrue(data.get("headers").get(gatewayConfig.getDistribute().getIdentOptHeaderName()).asText().contains("1123456"));
            testContext.assertEquals("测试内容", data.get("data").asText());
            async.complete();
        });

    }

}
