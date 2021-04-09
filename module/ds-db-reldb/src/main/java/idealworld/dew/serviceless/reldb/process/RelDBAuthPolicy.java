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

package idealworld.dew.serviceless.reldb.process;

import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.fun.auth.AuthenticationProcessor;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 关系型数据库鉴权策略.
 *
 * @author gudaoxuri
 */
@Slf4j
public class RelDBAuthPolicy {

    public RelDBAuthPolicy(Integer resourceCacheExpireSec, Integer groupNodeLength) {
        AuthenticationProcessor.init(resourceCacheExpireSec, groupNodeLength);
    }

    public Future<AuthResultKind> authentication(
            String moduleName,
            Map<String, List<URI>> resourceInfo,
            IdentOptExchangeInfo identOptExchangeInfo
    ) {
        Promise<AuthResultKind> promise = Promise.promise();
        if (resourceInfo.isEmpty()) {
            promise.complete(AuthResultKind.ACCEPT);
            return promise.future();
        }
        authentication(moduleName, resourceInfo, identOptExchangeInfo, promise);
        return promise.future();
    }

    private void authentication(
            String moduleName,
            Map<String, List<URI>> resourceInfo,
            IdentOptExchangeInfo identOptExchangeInfo,
            Promise<AuthResultKind> promise
    ) {
        var currentProcessInfo = resourceInfo.entrySet().iterator().next();
        AuthenticationProcessor.authentication(moduleName, currentProcessInfo.getKey(), currentProcessInfo.getValue(), identOptExchangeInfo)
                .onSuccess(authResultKind -> {
                    if (authResultKind == AuthResultKind.REJECT) {
                        promise.complete(authResultKind);
                        return;
                    }
                    resourceInfo.remove(currentProcessInfo.getKey());
                    if (!resourceInfo.isEmpty()) {
                        authentication(moduleName, resourceInfo, identOptExchangeInfo, promise);
                        return;
                    }
                    promise.complete(authResultKind);
                })
                .onFailure(e -> {
                    log.error("[Auth]Resource fetch error: {}", e.getMessage(), e.getCause());
                    promise.complete(AuthResultKind.REJECT);
                });
    }


}

