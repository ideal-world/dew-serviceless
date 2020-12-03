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

package idealworld.dew.serviceless.http;

import idealworld.dew.serviceless.common.CommonApplication;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.funs.httpserver.HttpServer;
import idealworld.dew.serviceless.http.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.http.process.HttpHandler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class HttpApplication extends CommonApplication<HttpConfig> {

    @Override
    protected void doStart(HttpConfig config, Promise<Void> startPromise) {
        initRedis(config);
        initHttpClient(config);
        var httpHandler = new HttpHandler();
        ExchangeProcessor.init(config);
        initHttpServer(config, HttpServer.Route.builder()
                .method(HttpMethod.POST)
                .path(Constant.REQUEST_PATH_FLAG)
                .parseBody(true)
                .handlers(Collections.singletonList(httpHandler))
                .build())
                .onSuccess(resp -> startPromise.complete())
                .onFailure(e -> startPromise.fail(e.getCause()));
    }

}
