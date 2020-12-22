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

import idealworld.dew.framework.fun.auth.exchange.ExchangeHelper;
import io.vertx.core.Future;

public abstract class DewAuthApplication<C extends DewAuthConfig> extends DewApplication<C> {

    protected Future<?> start(C config) {
        ExchangeHelper.init(config.iam);
        return Future.succeededFuture();
    }

}
