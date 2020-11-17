package idealworld.dew.baas.gateway.test;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.funs.cache.RedisClient;
import idealworld.dew.baas.common.util.YamlHelper;
import idealworld.dew.baas.gateway.GatewayApplication;
import idealworld.dew.baas.gateway.GatewayConfig;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
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

public class TestFlow extends BasicTest {

    private static GatewayConfig gatewayConfig;

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        gatewayConfig = YamlHelper.toObject(GatewayConfig.class, $.file.readAllByClassPath("application-test.yml", StandardCharsets.UTF_8));
        System.getProperties().put("dew.profile", "test");
        RedisTestHelper.start();
        vertx.deployVerticle(new GatewayApplication(), testContext.succeedingThenComplete());
    }

    @Test
    public void testError(Vertx vertx, VertxTestContext testContext) {
        var result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + "/err-path", "");
        Assertions.assertTrue(result.contains("Resource not found"));
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG, "");
        Assertions.assertEquals("请求格式不合法，缺少query", result);
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG,
                "");
        Assertions.assertTrue(result.contains("请求格式不合法，缺少"));
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                        "=&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=",
                "");
        Assertions.assertTrue(result.contains("请求格式不合法，缺少"));
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http//iam.service/console/tenant/account", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "");
        Assertions.assertTrue(result.contains("资源URI错误"));
        result = $.http.post("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://iam.service/console/tenant/account", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=createx",
                "");
        Assertions.assertTrue(result.contains("资源类型或操作类型不存在"));
        testContext.completeNow();
    }


    @Test
    public void testPublic(Vertx vertx, VertxTestContext testContext) {
        var result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容");
        Assertions.assertEquals(200, result.statusCode);
        var data = $.json.toJson(result.result);
        Assertions.assertTrue(data.get("headers").has(Constant.REQUEST_IDENT_OPT_FLAG));
        Assertions.assertEquals("测试内容", data.get("data").asText());
        testContext.completeNow();
    }

    @Test
    public void testToken(Vertx vertx, VertxTestContext testContext) {
        var errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getTokenFieldName(), "tokenxxx");
                    }
                });
        Assertions.assertEquals("认证错误，Token不合法", errorResult.result);

        RedisClient.choose("").set(gatewayConfig.getSecurity().getCacheTokenInfoKey() + "tokenxxx", "{\"accountCode\":\"testCode\"}");
        vertx.setTimer(1000, h -> {
            var result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                            "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                            "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                    "测试内容", new HashMap<>() {
                        {
                            put(gatewayConfig.getSecurity().getTokenFieldName(), "tokenxxx");
                        }
                    });
            Assertions.assertEquals(200, result.statusCode);
            var data = $.json.toJson(result.result);
            Assertions.assertTrue(data.get("headers").has(Constant.REQUEST_IDENT_OPT_FLAG));
            Assertions.assertTrue($.security.decodeBase64ToString(data.get("headers").get(Constant.REQUEST_IDENT_OPT_FLAG).asText(), StandardCharsets.UTF_8).contains("testCode"));
            Assertions.assertEquals("测试内容", data.get("data").asText());
            testContext.completeNow();
        });
    }

    @Test
    public void testAksk(Vertx vertx, VertxTestContext testContext) {
        var errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx");
                    }
                });
        Assertions.assertEquals("请求格式不合法，HTTP Header[" + gatewayConfig.getSecurity().getAkSkDateFieldName() + "]不存在", errorResult.result);
        var sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        var date = new Date();
        date.setTime(System.currentTimeMillis() - 6000);
        errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getAkSkFieldName(), "xxxx");
                        put(gatewayConfig.getSecurity().getAkSkDateFieldName(), sdf.format(date));
                    }
                });
        Assertions.assertEquals("请求格式不合法，HTTP Header[" + gatewayConfig.getSecurity().getAkSkFieldName() + "]格式错误", errorResult.result);
        errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx:xxx");
                        put(gatewayConfig.getSecurity().getAkSkDateFieldName(), sdf.format(date));
                    }
                });
        Assertions.assertEquals("认证错误，请求时间已过期", errorResult.result);
        errorResult = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                        "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                        "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                "测试内容", new HashMap<>() {
                    {
                        put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx:xxx");
                        put(gatewayConfig.getSecurity().getAkSkDateFieldName(), sdf.format(new Date()));
                    }
                });
        Assertions.assertEquals("认证错误，AK不存在", errorResult.result);

        RedisClient.choose("").set(gatewayConfig.getSecurity().getCacheAkSkInfoKey() + "xx", "skxx:1123456:789");
        vertx.setTimer(1000, h -> {
            var result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG +
                            "?" + Constant.REQUEST_RESOURCE_URI_FLAG +
                            "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create",
                    "测试内容", new HashMap<>() {
                        {
                            put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx:xxx");
                            put(gatewayConfig.getSecurity().getAkSkDateFieldName(), sdf.format(new Date()));
                        }
                    });
            Assertions.assertEquals("认证错误，签名不合法", result.result);

            var d = sdf.format(new Date());
            var query = Constant.REQUEST_RESOURCE_URI_FLAG +
                    "=" + URLEncoder.encode("http://httpbin.org/post", StandardCharsets.UTF_8) + "&" + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=create";
            var signature = $.security.encodeStringToBase64(
                    $.security.digest.digest(("post\n" + d + "\n" + Constant.REQUEST_PATH_FLAG + "\n" + query).toLowerCase(),
                            "skxx", "HmacSHA1"), StandardCharsets.UTF_8);
            result = $.http.postWrap("http://127.0.0.1:" + gatewayConfig.getHttpServer().getPort() + Constant.REQUEST_PATH_FLAG + "?" + query,
                    "测试内容", new HashMap<>() {
                        {
                            put(gatewayConfig.getSecurity().getAkSkFieldName(), "xx:" + signature);
                            put(gatewayConfig.getSecurity().getAkSkDateFieldName(), d);
                        }
                    });
            Assertions.assertEquals(200, result.statusCode);
            var data = $.json.toJson(result.result);
            Assertions.assertTrue(data.get("headers").has(Constant.REQUEST_IDENT_OPT_FLAG));
            Assertions.assertTrue($.security.decodeBase64ToString(data.get("headers").get(Constant.REQUEST_IDENT_OPT_FLAG).asText(), StandardCharsets.UTF_8).contains("testCode"));
            Assertions.assertEquals("测试内容", data.get("data").asText());
            testContext.completeNow();
        });

    }

}
