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

package idealworld.dew.framework;

import idealworld.dew.framework.fun.cache.FunCacheClient;
import idealworld.dew.framework.fun.eventbus.EventBusDispatcher;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.httpclient.FunHttpClient;
import idealworld.dew.framework.fun.httpserver.FunHttpServer;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public abstract class DewModule<C extends Object> extends AbstractVerticle {

    private Class<C> configClazz = (Class<C>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    private C moduleConfig;
    private DewConfig.FunConfig funConfig;

    public abstract String getModuleName();

    protected abstract Future<Void> start(C config);

    protected abstract Future<Void> stop(C config);

    protected abstract boolean enabledCacheFun();

    protected abstract boolean enabledHttpServerFun();

    protected abstract boolean enabledHttpClientFun();

    protected abstract boolean enabledSQLFun();

    protected boolean enabledEventbus() {
        return true;
    }

    protected List<Future> loadCustomFuns(DewConfig.FunConfig funConfig) {
        return new ArrayList<>();
    }

    protected List<Future> unLoadCustomFuns(DewConfig.FunConfig funConfig) {
        return new ArrayList<>();
    }

    @SneakyThrows
    @Override
    public final void start(Promise<Void> startPromise) {
        var jsonFunConfig = vertx.getOrCreateContext().config().getJsonObject("funs");
        funConfig = jsonFunConfig != null ? jsonFunConfig.mapTo(DewConfig.FunConfig.class) : DewConfig.FunConfig.builder().build();
        var jsonConfig = vertx.getOrCreateContext().config().getJsonObject("config");
        moduleConfig = jsonConfig != null ? jsonConfig.mapTo(configClazz) : configClazz.getDeclaredConstructor().newInstance();
        CompositeFuture.all(loadFuns(funConfig))
                .onSuccess(funLoadResult ->
                        vertx.setTimer(500, r ->
                                start(moduleConfig)
                                        .onSuccess(resp -> EventBusDispatcher.watch(getModuleName(), moduleConfig, new HashMap<>() {
                                            {
                                                put("cache", enabledCacheFun());
                                                put("httpserver", enabledHttpServerFun());
                                                put("httpclient", enabledHttpClientFun());
                                                put("sql", enabledSQLFun());
                                                put("eventbus", enabledEventbus());
                                            }
                                        })
                                                .onSuccess(rr -> startPromise.complete())
                                                .onFailure(startPromise::fail))
                                        .onFailure(startPromise::fail))
                )
                .onFailure(startPromise::fail);
    }

    @Override
    public final void stop(Promise<Void> stopPromise) {
        CompositeFuture.all(unLoadFuns(funConfig))
                .onSuccess(funUnloadResult -> stop(moduleConfig)
                        .onSuccess(resp -> stopPromise.complete())
                        .onFailure(stopPromise::fail))
                .onFailure(stopPromise::fail);
    }

    private ArrayList<Future> loadFuns(DewConfig.FunConfig funConfig) {
        var funPromises = new ArrayList<Future>();
        if (enabledCacheFun()) {
            funPromises.add(FunCacheClient.init(getModuleName(), vertx, funConfig.getCache()));
        }
        if (enabledHttpServerFun()) {
            funPromises.add(FunHttpServer.init(getModuleName(), vertx, funConfig.getHttpServer()));
        }
        if (enabledHttpClientFun()) {
            funPromises.add(FunHttpClient.init(getModuleName(), vertx, funConfig.getHttpClient()));
        }
        if (enabledSQLFun()) {
            funPromises.add(FunSQLClient.init(getModuleName(), vertx, funConfig.getSql()));
        }
        if (enabledEventbus()) {
            funPromises.add(FunEventBus.init(getModuleName(), vertx, funConfig.getEventBus()));
            EventBusDispatcher.initModule(getModuleName());
        }
        funPromises.addAll(loadCustomFuns(funConfig));
        return funPromises;
    }

    private ArrayList<Future> unLoadFuns(DewConfig.FunConfig funConfig) {
        var funPromises = new ArrayList<Future>();
        if (enabledCacheFun()) {
            funPromises.add(FunCacheClient.destroy());
        }
        if (enabledHttpServerFun()) {
            funPromises.add(FunHttpServer.destroy());
        }
        if (enabledHttpClientFun()) {
            funPromises.add(FunHttpClient.destroy());
        }
        if (enabledSQLFun()) {
            funPromises.add(FunSQLClient.destroy());
        }
        funPromises.addAll(unLoadCustomFuns(funConfig));
        return funPromises;
    }

}
