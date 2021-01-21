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

package idealworld.dew.framework.test;

import com.ecfront.dew.common.$;
import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.fun.auth.AuthCacheProcessor;
import idealworld.dew.framework.fun.cache.FunCacheClient;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.test.DewTest;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * 权限测试.
 *
 * @author gudaoxuri
 */
public class AuthCacheTest extends DewTest {

    static {
        enableRedis();
    }

    @SneakyThrows
    @Test
    public void testAuthCache(Vertx vertx, VertxTestContext testContext) {
        await(FunCacheClient.init("", vertx, DewConfig.FunConfig.CacheConfig.builder()
                .uri("redis://localhost:" + redisConfig.getFirstMappedPort())
                .build()));
        var identCacheOpt = IdentOptExchangeInfo.builder()
                .token($.field.createUUID())
                .accountId(1L)
                .build();
        var context = ProcessContext.builder()
                .moduleName("")
                .funStatus(new HashMap<>() {
                    {
                        put("cache", true);
                    }
                })
                .build().init(identCacheOpt);
        var counter = new AtomicLong(1000);
        IntStream.range(0, 10)
                .mapToObj(i -> new Thread(() -> {
                    while (counter.addAndGet(-1) >= 0) {
                        await(AuthCacheProcessor.setOptInfo(IdentOptExchangeInfo.builder()
                                .token($.field.createUUID())
                                .accountCode("1")
                                .build(), 0L, context));
                    }
                }))
                .forEach(Thread::start);
        while (counter.get() >= 0) {

        }
        testContext.completeNow();
    }

}
