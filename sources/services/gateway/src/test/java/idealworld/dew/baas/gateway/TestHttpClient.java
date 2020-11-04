package idealworld.dew.baas.gateway;

import idealworld.dew.baas.gateway.util.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

public class TestHttpClient extends BasicTest {

    @Before
    public void before(TestContext testContext) {
        HttpClient.init(rule.vertx());
    }

    @Test
    public void testHttp(TestContext testContext) {
        Async async = testContext.async(2);
        HttpClient.request(HttpMethod.GET, "http://www.baidu.com", null, null, null)
                .onSuccess(response -> {
                    var html = response.body().toString("UTF-8");
                    testContext.assertTrue(html.contains("百度"));
                    async.countDown();
                });

        HttpClient.request(HttpMethod.GET, "www.baidu.com", 80, "/s", "ie=UTF-8&wd=dew", null, null, null)
                .onSuccess(response -> {
                    var html = response.body().toString("UTF-8");
                    testContext.assertTrue(html.contains("dew"));
                    async.countDown();
                });
    }

}
