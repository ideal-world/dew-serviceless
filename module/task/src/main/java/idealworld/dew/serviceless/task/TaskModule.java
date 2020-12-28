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

package idealworld.dew.serviceless.task;

import idealworld.dew.framework.DewModule;
import idealworld.dew.serviceless.task.process.TaskProcessor;
import io.vertx.core.Future;

/**
 * @author gudaoxuri
 */
public class TaskModule extends DewModule<TaskConfig> {

    @Override
    public String getModuleName() {
        return "task";
    }

    @Override
    protected Future<Void> start(TaskConfig config) {
        new TaskProcessor(getModuleName(),config,vertx);
        return Future.succeededFuture();
    }

    @Override
    protected Future<Void> stop(TaskConfig config) {
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
