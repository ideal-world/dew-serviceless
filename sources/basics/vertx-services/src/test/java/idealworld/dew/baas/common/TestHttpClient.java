package idealworld.dew.baas.common;

import idealworld.dew.baas.common.funs.httpclient.HttpClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

public class TestHttpClient extends BasicTest {

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        HttpClient.init(vertx);
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testHttp(Vertx vertx, VertxTestContext testContext) {
        var count = new CountDownLatch(2);
        HttpClient.request(HttpMethod.GET, "http://www.baidu.com", null, null, null)
                .onSuccess(response -> {
                    var html = response.body().toString("UTF-8");
                    Assertions.assertTrue(html.contains("百度"));
                    count.countDown();
                });
        HttpClient.request(HttpMethod.GET, "www.baidu.com", 80, "/s", "ie=UTF-8&wd=dew", null, null, null)
                .onSuccess(response -> {
                    var html = response.body().toString("UTF-8");
                    Assertions.assertTrue(html.contains("dew"));
                    count.countDown();
                });
        count.await();
        testContext.completeNow();
    }

}
