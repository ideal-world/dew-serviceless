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

package idealworld.dew.serviceless.reldb.exchange;

import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.fun.auth.LocalResourceCache;
import idealworld.dew.framework.fun.auth.dto.ResourceExchange;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.auth.exchange.ExchangeHelper;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.reldb.RelDBConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

/**
 * @author gudaoxuri
 */
@Slf4j
public class RelDBExchangeProcessor {

    public static Future<Void> init(String moduleName, RelDBConfig relDBConfig, Vertx vertx) {
        return ExchangeHelper.loadAndWatchResourceSubject(moduleName, ResourceKind.RELDB,
                resourceSubjectExchange ->
                        FunSQLClient.init(resourceSubjectExchange.getCode(), vertx,
                                DewConfig.FunConfig.SQLConfig.builder()
                                        .uri(resourceSubjectExchange.getUri())
                                        .userName(resourceSubjectExchange.getAk())
                                        .password(resourceSubjectExchange.getSk())
                                        .build()),
                FunSQLClient::remove)
                .compose(resp ->
                        ExchangeHelper.watch(moduleName, new HashSet<>() {
                            {
                                add("eb://" + DewAuthConstant.MODULE_IAM_NAME + "/resource." + ResourceKind.RELDB.toString().toLowerCase());
                            }
                        }, exchangeData -> {
                            var resourceExchange = new JsonObject(exchangeData._1).mapTo(ResourceExchange.class);
                            var resourceActionKind = resourceExchange.getResourceActionKind();
                            var resourceUri = URIHelper.newURI(resourceExchange.getResourceUri());
                            switch (exchangeData._0) {
                                case CREATE:
                                    LocalResourceCache.addLocalResource(resourceUri, resourceActionKind);
                                    log.info("[Exchange]Created [resource.actionKind={},uri={}] data", resourceActionKind, resourceExchange.getResourceUri());
                                    break;
                                case MODIFY:
                                    LocalResourceCache.removeLocalResource(resourceUri, resourceActionKind);
                                    LocalResourceCache.addLocalResource(resourceUri, resourceActionKind);
                                    log.info("[Exchange]Modify [resource.actionKind={},uri={}] data", resourceActionKind, resourceExchange.getResourceUri());
                                    break;
                                case DELETE:
                                    LocalResourceCache.removeLocalResource(resourceUri, resourceActionKind);
                                    log.info("[Exchange]Delete [resource.actionKind={},uri={}] data", resourceActionKind, resourceExchange.getResourceUri());
                                    break;
                            }
                        }));
    }

}
