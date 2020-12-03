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

package idealworld.dew.serviceless.reldb.test;

import idealworld.dew.serviceless.reldb.RelDBApplication;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

public class TestRelDBApplication extends BasicTest {

    @SneakyThrows
    @Test
    public void start(Vertx vertx, VertxTestContext testContext) {
        RedisTestHelper.start();
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new RelDBApplication(), testContext.succeedingThenComplete());
        new CountDownLatch(1).await();
    }

}
