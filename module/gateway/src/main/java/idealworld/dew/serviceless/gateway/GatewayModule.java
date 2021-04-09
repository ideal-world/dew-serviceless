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

package idealworld.dew.serviceless.gateway;

import idealworld.dew.framework.DewModule;
import idealworld.dew.framework.fun.auth.exchange.ExchangeHelper;
import idealworld.dew.framework.fun.httpserver.FunHttpServer;
import idealworld.dew.serviceless.gateway.process.GatewayAuthHandler;
import idealworld.dew.serviceless.gateway.process.GatewayAuthPolicy;
import idealworld.dew.serviceless.gateway.process.GatewayDistributeHandler;
import idealworld.dew.serviceless.gateway.process.GatewayIdentHandler;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;

import java.util.Arrays;

/**
 * 网关模块.
 *
 * @author gudaoxuri
 */
public class GatewayModule extends DewModule<GatewayConfig> {

    @Override
    public String getModuleName() {
        return "gateway";
    }

    @Override
    protected Future<Void> start(GatewayConfig config) {
        var authPolicy = new GatewayAuthPolicy(config.getSecurity().getResourceCacheExpireSec(),
                config.getSecurity().getGroupNodeLength());
        return ExchangeHelper.loadAndWatchResources(getModuleName(), "")
                .compose(resp -> {
                    var identHttpHandler = new GatewayIdentHandler(getModuleName(), config.getSecurity());
                    var authHttpHandler = new GatewayAuthHandler(getModuleName(), authPolicy);
                    var distributeHandler = new GatewayDistributeHandler(getModuleName(), config.getDistribute());
                    FunHttpServer.choose(getModuleName()).addRoute(FunHttpServer.Route.builder()
                            .method(HttpMethod.POST)
                            .path(config.getDistribute().getGatewayRequestPath())
                            .handlers(Arrays.asList(identHttpHandler, authHttpHandler, distributeHandler))
                            .build());
                    return Future.succeededFuture();
                });
    }

    @Override
    protected Future<Void> stop(GatewayConfig config) {
        return Future.succeededFuture();
    }

    @Override
    protected boolean enabledCacheFun() {
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
    protected boolean enabledSQLFun() {
        return false;
    }

}
