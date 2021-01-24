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

import idealworld.dew.framework.DewApplication;
import idealworld.dew.framework.DewConfig;
import io.vertx.core.Future;

/**
 * 网关测试启动类.
 *
 * @author gudaoxuri
 */
public class GatewayApplicationTest extends DewApplication<GatewayApplicationTest.TestDewGatewayConfig> {

    @Override
    protected Future<?> start(TestDewGatewayConfig config) {
        return Future.succeededFuture();
    }

    @Override
    protected Future<?> stop(TestDewGatewayConfig config) {
        return Future.succeededFuture();
    }

    public static class TestDewGatewayConfig extends DewConfig {


    }


}
