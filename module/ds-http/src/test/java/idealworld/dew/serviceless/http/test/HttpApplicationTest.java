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

package idealworld.dew.serviceless.http.test;

import idealworld.dew.framework.DewAuthApplication;
import idealworld.dew.framework.DewAuthConfig;
import io.vertx.core.Future;

/**
 * HTTP测试启动类.
 *
 * @author gudaoxuri
 */
public class HttpApplicationTest extends DewAuthApplication<HttpApplicationTest.TestDewHttpConfig> {

    @Override
    protected Future<?> start(TestDewHttpConfig config) {
        return super.start(config);
    }

    @Override
    protected Future<?> stop(TestDewHttpConfig config) {
        return Future.succeededFuture();
    }

    public static class TestDewHttpConfig extends DewAuthConfig {

    }


}
