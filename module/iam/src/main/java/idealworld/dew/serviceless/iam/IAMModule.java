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

package idealworld.dew.serviceless.iam;

import idealworld.dew.framework.DewModule;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.*;
import idealworld.dew.serviceless.iam.process.common.CommonProcessor;
import idealworld.dew.serviceless.iam.process.systemconsole.SCTenantProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.TCAccountProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.TCAppProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.TCTenantProcessor;
import io.vertx.core.Promise;

/**
 * @author gudaoxuri
 */
public class IAMModule extends DewModule<IAMConfig> {

    @Override
    protected void start(IAMConfig config, Promise<Void> startPromise) {
        new CommonProcessor();
        new SCTenantProcessor();
        new TCTenantProcessor();
        new TCAppProcessor();
        new TCAccountProcessor();
        new ACAppProcessor();
        new ACRoleProcessor();
        new ACGroupProcessor();
        new ACResourceProcessor();
        new ACAuthPolicyProcessor();
        ReceiveProcessor.watch(getModuleName(), config)
                .onSuccess(startResult -> startPromise.complete())
                .onFailure(startPromise::fail);
    }

    @Override
    protected void stop(IAMConfig config, Promise<Void> stopPromise) {
        stopPromise.complete();
    }

    @Override
    protected boolean enabledRedisFun() {
        return true;
    }

    @Override
    protected boolean enabledHttpServerFun() {
        return false;
    }

    @Override
    protected boolean enabledHttpClientFun() {
        return true;
    }

    @Override
    protected boolean enabledJDBCFun() {
        return true;
    }

}
