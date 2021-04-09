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

package idealworld.dew.serviceless.reldb;

import idealworld.dew.framework.DewModule;
import idealworld.dew.serviceless.reldb.exchange.RelDBExchangeProcessor;
import idealworld.dew.serviceless.reldb.process.RelDBAuthPolicy;
import idealworld.dew.serviceless.reldb.process.RelDBProcessor;
import io.vertx.core.Future;

/**
 * 关系型数据库模块.
 *
 * @author gudaoxuri
 */
public class RelDBModule extends DewModule<RelDBConfig> {

    @Override
    public String getModuleName() {
        return "reldb";
    }

    @Override
    protected Future<Void> start(RelDBConfig config) {
        var authPolicy = new RelDBAuthPolicy(
                config.getSecurity().getResourceCacheExpireSec(),
                config.getSecurity().getGroupNodeLength());
        new RelDBProcessor(authPolicy, getModuleName());
        return RelDBExchangeProcessor.init(getModuleName(), config, vertx);
    }

    @Override
    protected Future<Void> stop(RelDBConfig config) {
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
        return true;
    }

}
