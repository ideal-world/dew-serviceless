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

package idealworld.dew.serviceless.cache;

import idealworld.dew.framework.DewModule;
import idealworld.dew.serviceless.cache.exchange.CacheExchangeProcessor;
import idealworld.dew.serviceless.cache.process.CacheProcessor;
import io.vertx.core.Future;

/**
 * @author gudaoxuri
 */
public class CacheModule extends DewModule<CacheConfig> {

    @Override
    public String getModuleName() {
        return "cache";
    }

    @Override
    protected Future<Void> start(CacheConfig config) {
        new CacheProcessor(getModuleName());
        return CacheExchangeProcessor.init(getModuleName(), config, vertx);
    }

    @Override
    protected Future<Void> stop(CacheConfig config) {
        return Future.succeededFuture();
    }

    @Override
    protected boolean enabledCacheFun() {
        return true;
    }

    @Override
    protected boolean enabledHttpServerFun() {
        return false;
    }

    @Override
    protected boolean enabledHttpClientFun() {
        return false;
    }

    @Override
    protected boolean enabledSQLFun() {
        return false;
    }

}
