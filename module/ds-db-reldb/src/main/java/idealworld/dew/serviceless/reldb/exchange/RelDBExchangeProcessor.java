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

package idealworld.dew.serviceless.reldb.exchange;

import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.auth.exchange.ExchangeHelper;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.serviceless.reldb.RelDBConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

/**
 * 关系型数据库数据交互处理器.
 *
 * @author gudaoxuri
 */
@Slf4j
public class RelDBExchangeProcessor {

    public static Future<Void> init(String moduleName, RelDBConfig relDBConfig, Vertx vertx) {
        return ExchangeHelper.loadAndWatchResourceSubjects(moduleName, ResourceKind.RELDB,
                resourceSubjectExchange ->
                        FunSQLClient.init(resourceSubjectExchange.getCode(), vertx,
                                DewConfig.FunConfig.SQLConfig.builder()
                                        .uri(resourceSubjectExchange.getUri())
                                        .userName(resourceSubjectExchange.getAk())
                                        .password(resourceSubjectExchange.getSk())
                                        .build()),
                FunSQLClient::remove)
                .compose(resp ->
                        ExchangeHelper.loadAndWatchResources(moduleName, ResourceKind.RELDB.toString().toLowerCase()));
    }

}
