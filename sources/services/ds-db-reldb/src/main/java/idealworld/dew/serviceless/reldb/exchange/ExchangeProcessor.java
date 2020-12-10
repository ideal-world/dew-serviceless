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

import com.ecfront.dew.common.$;
import idealworld.dew.serviceless.common.CommonApplication;
import idealworld.dew.serviceless.common.CommonConfig;
import idealworld.dew.framework.auth.LocalResourceCache;
import idealworld.dew.framework.dto.exchange.ResourceExchange;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.framework.fun.exchange.ExchangeHelper;
import idealworld.dew.framework.fun.sql.MysqlClient;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.reldb.RelDBConfig;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeProcessor {

    public static Future<Void> init(RelDBConfig relDBConfig) {
        ExchangeHelper.loadAndWatchResourceSubject(relDBConfig, ResourceKind.RELDB,
                resourceSubject ->
                        MysqlClient.init(resourceSubject.getCode(), CommonApplication.VERTX,
                                CommonConfig.JDBCConfig.builder()
                                        .uri(resourceSubject.getUri())
                                        .userName(resourceSubject.getAk())
                                        .password(resourceSubject.getSk())
                                        .build()),
                MysqlClient::remove);
        return ExchangeHelper.watch(new HashSet<>() {
            {
                add("resource." + ResourceKind.RELDB.toString().toLowerCase());
            }
        }, exchangeData -> {
            var resourceExchange = $.json.toObject(exchangeData.getDetailData(), ResourceExchange.class);
            var resourceActionKind = resourceExchange.getResourceActionKind();
            var resourceUri = URIHelper.newURI(resourceExchange.getResourceUri());
            switch (exchangeData.getActionKind()) {
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
        });
    }

}
