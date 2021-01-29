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

package idealworld.dew.framework.fun.auth.exchange;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewAuthConfig;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.LocalResourceCache;
import idealworld.dew.framework.fun.auth.dto.ResourceExchange;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.auth.dto.ResourceSubjectExchange;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.util.URIHelper;
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
 * 模块间数据交互处理器.
 *
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeHelper {

    private static DewAuthConfig.IAMConfig config;

    public static void init(DewAuthConfig.IAMConfig _config) {
        config = _config;
    }

    public static Future<Void> loadAndWatchResources(String moduleName, String kind) {
        return Future.succeededFuture()
                .compose(resp -> {
                    if (config == null || config.getAppId() == null) {
                        log.warn("Cannot connect to the iam service because the iam configuration does not exist!");
                        return Future.succeededFuture();
                    }
                    var header = new HashMap<String, String>();
                    var identOptInfo = IdentOptExchangeInfo.builder()
                            .tenantId(config.getTenantId())
                            .appId(config.getAppId())
                            .unauthorizedTenantId(config.getTenantId())
                            .unauthorizedAppId(config.getAppId())
                            .build();
                    header.put(DewAuthConstant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(JsonObject.mapFrom(identOptInfo).toString(),
                            StandardCharsets.UTF_8));
                    return FunEventBus.choose(moduleName).request(config.getModuleName(), OptActionKind.FETCH,
                            DewAuthConstant.REQUEST_INNER_PATH_PREFIX + "resource?kind=" + kind, null, header)
                            .compose(result -> {
                                var resources = new JsonArray(result._0.toString(StandardCharsets.UTF_8));
                                for (var resource : resources) {
                                    var resourceExchange = ((JsonObject) resource).mapTo(ResourceExchange.class);
                                    LocalResourceCache.addLocalResource(URIHelper.newURI(resourceExchange.getUri()),
                                            resourceExchange.getActionKind().toLowerCase());
                                    log.info("[Exchange]Init [resource.actionKind={}:uri={}] data", resourceExchange.getActionKind(),
                                            resourceExchange.getUri());
                                }
                                return Future.succeededFuture();
                            });
                })
                .compose(resp -> watch(moduleName, new HashSet<>() {
                    {
                        add("eb://" + DewAuthConstant.MODULE_IAM_NAME + "/resource." + kind);
                    }
                }, exchangeInfo -> {
                    var resourceExchange = exchangeInfo._1.toJsonObject().mapTo(ResourceExchange.class);
                    var resourceActionKind = resourceExchange.getActionKind().toLowerCase();
                    var resourceUri = URIHelper.newURI(resourceExchange.getUri());
                    switch (exchangeInfo._0) {
                        case CREATE:
                            LocalResourceCache.addLocalResource(resourceUri, resourceActionKind);
                            log.info("[Exchange]Created [resource.actionKind={},uri={}] data", resourceActionKind, resourceExchange.getUri());
                            break;
                        case MODIFY:
                            LocalResourceCache.removeLocalResource(resourceUri, resourceActionKind);
                            LocalResourceCache.addLocalResource(resourceUri, resourceActionKind);
                            log.info("[Exchange]Modify [resource.actionKind={},uri={}] data", resourceActionKind, resourceExchange.getUri());
                            break;
                        case DELETE:
                            LocalResourceCache.removeLocalResource(resourceUri, resourceActionKind);
                            log.info("[Exchange]Delete [resource.actionKind={},uri={}] data", resourceActionKind, resourceExchange.getUri());
                            break;
                        default:
                            log.warn("[Exchange]Not found action kind");
                    }
                }));
    }

    public static Future<Void> loadAndWatchResourceSubjects(String moduleName, ResourceKind kind, Consumer<ResourceSubjectExchange> addFun,
                                                            Consumer<String> removeFun) {
        return Future.succeededFuture()
                .compose(resp -> {
                    if (config == null || config.getAppId() == null) {
                        log.warn("Cannot connect to the iam service because the iam configuration does not exist!");
                        return Future.succeededFuture();
                    }
                    var header = new HashMap<String, String>();
                    var identOptInfo = IdentOptExchangeInfo.builder()
                            .tenantId(config.getTenantId())
                            .appId(config.getAppId())
                            .unauthorizedTenantId(config.getTenantId())
                            .unauthorizedAppId(config.getAppId())
                            .build();
                    header.put(DewAuthConstant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(JsonObject.mapFrom(identOptInfo).toString(),
                            StandardCharsets.UTF_8));
                    return FunEventBus.choose(moduleName).request(config.getModuleName(), OptActionKind.FETCH,
                            DewAuthConstant.REQUEST_INNER_PATH_PREFIX + "resource/subject?kind=" + kind.toString(), null, header)
                            .compose(result -> {
                                var resourceSubjects = new JsonArray(result._0.toString(StandardCharsets.UTF_8));
                                for (var resourceSubject : resourceSubjects) {
                                    var resourceSubjectExchange = ((JsonObject) resourceSubject).mapTo(ResourceSubjectExchange.class);
                                    addFun.accept(resourceSubjectExchange);
                                    log.info("[Exchange]Init [resourceSubject.code={}] data", resourceSubjectExchange.getCode());
                                }
                                return Future.succeededFuture();
                            });
                })
                .compose(resp -> watch(moduleName, new HashSet<>() {
                    {
                        add("eb://" + DewAuthConstant.MODULE_IAM_NAME + "/resourcesubject." + kind.toString().toLowerCase());
                    }
                }, exchangeInfo -> {
                    var resourceSubjectExchange = exchangeInfo._1.toJsonObject().mapTo(ResourceSubjectExchange.class);
                    if (exchangeInfo._0 == OptActionKind.CREATE
                            || exchangeInfo._0 == OptActionKind.MODIFY) {
                        removeFun.accept(resourceSubjectExchange.getCode());
                        addFun.accept(resourceSubjectExchange);
                        log.info("[Exchange]Updated [resourceSubject.code={}] data", resourceSubjectExchange.getCode());
                    } else if (exchangeInfo._0 == OptActionKind.DELETE) {
                        removeFun.accept(resourceSubjectExchange.getCode());
                        log.error("[Exchange]Removed [resourceSubject.code={}]", resourceSubjectExchange.getCode());
                    }
                }));
    }

    public static Future<Void> watch(String moduleName, Set<String> uris, Consumer<Tuple2<OptActionKind, Buffer>> fun) {
        FunEventBus.choose(moduleName).consumer("", (FunEventBus.ConsumerFun<Void>) (actionKind, uri, header, body) -> {
            var strUri = uri.toString().toLowerCase();
            if (uris.stream().anyMatch(u -> strUri.startsWith(u.toLowerCase()))) {
                log.trace("[Exchange]Received {}", body.toString());
                fun.accept(new Tuple2<>(actionKind, body));
            }
            return Future.succeededFuture();
        });
        return Future.succeededFuture();
    }

}
