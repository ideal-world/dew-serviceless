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

package idealworld.dew.serviceless.gateway.exchange;

import com.ecfront.dew.common.$;
import idealworld.dew.serviceless.common.auth.LocalResourceCache;
import idealworld.dew.serviceless.common.dto.exchange.ResourceExchange;
import idealworld.dew.serviceless.common.funs.exchange.ExchangeHelper;
import idealworld.dew.serviceless.common.util.URIHelper;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeProcessor {

    public static Future<Void> init() {
        return ExchangeHelper.watch(new HashSet<>() {
            {
                add("resource");
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
