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

package idealworld.dew.serviceless.reldb.test;

import idealworld.dew.framework.DewAuthApplication;
import idealworld.dew.framework.DewAuthConfig;
import io.vertx.core.Future;

/**
 * 关系型数据库测试类.
 *
 * @author gudaoxuri
 */
public class RelDBApplicationTest extends DewAuthApplication<RelDBApplicationTest.TestDewRelDBConfig> {

    @Override
    protected Future<?> start(TestDewRelDBConfig config) {
        return super.start(config);
    }

    @Override
    protected Future<?> stop(TestDewRelDBConfig config) {
        return Future.succeededFuture();
    }

    public static class TestDewRelDBConfig extends DewAuthConfig {

    }


}
