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

package idealworld.dew.framework.fun.auth.exchange;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewAuthConfig;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.auth.dto.ResourceSubjectExchange;
import idealworld.dew.framework.fun.eventbus.ConsumerFun;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeHelper {

    public static Future<Void> loadAndWatchResourceSubject(String moduleName, DewAuthConfig config, ResourceKind kind, Consumer<ResourceSubjectExchange> addFun, Consumer<String> removeFun) {
        var header = new HashMap<String, String>();
        var identOptInfo = IdentOptCacheInfo.builder()
                .tenantId(config.getIam().getTenantId())
                .appId(config.getIam().getAppId())
                .build();
        header.put(DewAuthConstant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64($.json.toJsonString(identOptInfo), StandardCharsets.UTF_8));
        FunEventBus.choose(moduleName).request(config.getIam().getModuleName(), OptActionKind.FETCH, "/console/app/resource/subject?qKind=" + kind.toString(), null, header)
                .onSuccess(result -> {
                    var resourceSubjects = new JsonArray(result._0.toString(StandardCharsets.UTF_8));
                    for (var resourceSubject : resourceSubjects) {
                        var resourceSubjectExchange = ((JsonObject)resourceSubject).mapTo(ResourceSubjectExchange.class);
                        addFun.accept(resourceSubjectExchange);
                        log.info("[Exchange]Init [resourceSubject.code={}] data", resourceSubjectExchange.getCode());
                    }
                })
                .onFailure(e -> log.error("[Exchange]Init resourceSubjects error: {}", e.getMessage(), e.getCause()));
        return watch(moduleName, new HashSet<>() {
            {
                add("http://"+DewAuthConstant.MODULE_IAM_NAME+"/resourcesubject." + kind.toString().toLowerCase());
            }
        }, exchangeInfo -> {
            var resourceSubjectExchange = JsonObject.mapFrom(exchangeInfo._1).mapTo(ResourceSubjectExchange.class);
            if (exchangeInfo._0 == OptActionKind.CREATE
                    || exchangeInfo._0 == OptActionKind.MODIFY) {
                removeFun.accept(resourceSubjectExchange.getCode());
                addFun.accept(resourceSubjectExchange);
                log.info("[Exchange]Updated [resourceSubject.code={}] data", resourceSubjectExchange.getCode());
            } else if (exchangeInfo._0 == OptActionKind.DELETE) {
                removeFun.accept(resourceSubjectExchange.getCode());
                log.error("[Exchange]Removed [resourceSubject.code={}]", resourceSubjectExchange.getCode());
            }
        });
    }

    public static Future<Void> watch(String moduleName, Set<String> uris, Consumer<Tuple2<OptActionKind,Buffer>> fun) {
        FunEventBus.choose(moduleName).consumer(moduleName, (ConsumerFun<Void>) (actionKind, uri, header, body) -> {
            var strUri = uri.toString().toLowerCase();
            if(uris.stream().anyMatch(u -> u.toLowerCase().startsWith(strUri))){
                log.trace("[Exchange]Received {}", body.toString());
                fun.accept(new Tuple2<>(actionKind,body));
            }
            return Future.succeededFuture();
        });
        return Future.succeededFuture();
    }

}
