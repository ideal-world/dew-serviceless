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

package idealworld.dew.serviceless.http.exchange;

import idealworld.dew.serviceless.common.CommonApplication;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.common.funs.exchange.ExchangeHelper;
import idealworld.dew.serviceless.common.funs.httpclient.HttpClient;
import idealworld.dew.serviceless.http.HttpConfig;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeProcessor {

    public static Future<Void> init(HttpConfig httpConfig) {
        return ExchangeHelper.loadAndWatchResourceSubject(httpConfig, ResourceKind.HTTP,
                resourceSubject ->
                        HttpClient.init(resourceSubject.getCode(), CommonApplication.VERTX,
                                HttpClient.HttpConfig.builder()
                                        .uri(resourceSubject.getUri())
                                        .timeoutMS(resourceSubject.getTimeoutMS())
                                        .build()),
                HttpClient::remove);
    }

}
