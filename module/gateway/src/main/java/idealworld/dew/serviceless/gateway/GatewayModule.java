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

package idealworld.dew.serviceless.gateway;

import idealworld.dew.framework.DewModule;
import idealworld.dew.framework.fun.httpserver.FunHttpServer;
import idealworld.dew.serviceless.gateway.exchange.GatewayExchangeProcessor;
import idealworld.dew.serviceless.gateway.process.GatewayAuthHandler;
import idealworld.dew.serviceless.gateway.process.GatewayDistributeHandler;
import idealworld.dew.serviceless.gateway.process.GatewayIdentHandler;
import idealworld.dew.serviceless.gateway.process.GatewayAuthPolicy;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;

import java.util.Arrays;

/**
 * @author gudaoxuri
 */
public class GatewayModule extends DewModule<GatewayConfig> {

    @Override
    protected void start(GatewayConfig config, Promise<Void> startPromise) {
        var authPolicy = new GatewayAuthPolicy(getModuleName(), config.getSecurity().getResourceCacheExpireSec(), config.getSecurity().getGroupNodeLength());
        GatewayExchangeProcessor.init(getModuleName());
        var identHttpHandler = new GatewayIdentHandler(getModuleName(), config.getSecurity());
        var authHttpHandler = new GatewayAuthHandler(getModuleName(), authPolicy);
        var distributeHandler = new GatewayDistributeHandler(getModuleName(), config.getDistribute());
        FunHttpServer.choose(getModuleName()).addRoute(FunHttpServer.Route.builder()
                .method(HttpMethod.POST)
                .path(config.getDistribute().getGatewayRequestPath())
                .handlers(Arrays.asList(identHttpHandler, authHttpHandler, distributeHandler))
                .build());
        startPromise.complete();
    }

    @Override
    protected void stop(GatewayConfig config, Promise<Void> stopPromise) {
        stopPromise.complete();
    }

    @Override
    protected boolean enabledRedisFun() {
        return true;
    }

    @Override
    protected boolean enabledHttpServerFun() {
        return true;
    }

    @Override
    protected boolean enabledHttpClientFun() {
        return false;
    }

    @Override
    protected boolean enabledJDBCFun() {
        return false;
    }

}
