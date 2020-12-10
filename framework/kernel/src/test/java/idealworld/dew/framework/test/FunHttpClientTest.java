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

package idealworld.dew.framework.test;

import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.fun.httpclient.FunHttpClient;
import idealworld.dew.framework.fun.test.DewTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

public class FunHttpClientTest extends DewTest {

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        FunHttpClient.init("", vertx, DewConfig.FunConfig.HttpClientConfig.builder().build());
        testContext.completeNow();
    }

    @SneakyThrows
    @Test
    public void testHttp(Vertx vertx, VertxTestContext testContext) {
        var count = new CountDownLatch(2);
        FunHttpClient.choose("").request(HttpMethod.GET, "http://www.baidu.com", null, null, null)
                .onSuccess(response -> {
                    var html = response.body().toString("UTF-8");
                    Assertions.assertTrue(html.contains("百度"));
                    count.countDown();
                });
        FunHttpClient.choose("").request(HttpMethod.GET, "www.baidu.com", 80, "/s", "ie=UTF-8&wd=dew", null, null, null)
                .onSuccess(response -> {
                    var html = response.body().toString("UTF-8");
                    Assertions.assertTrue(html.contains("dew"));
                    count.countDown();
                });
        count.await();
        testContext.completeNow();
    }

}
