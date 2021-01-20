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

package idealworld.dew.framework.fun.eventbus;

import idealworld.dew.framework.dto.OptActionKind;

/**
 * @author gudaoxuri
 */
public class EventBusProcessor {

    private String moduleName;

    public EventBusProcessor(String moduleName) {
        this.moduleName = moduleName;
    }

    public void addProcessor(String pathPattern, ProcessFun processFun) {
        EventBusDispatcher.addProcessor(moduleName, pathPattern, processFun);
    }

    public void addProcessor(OptActionKind actionKind, String pathPattern, ProcessFun processFun) {
        EventBusDispatcher.addProcessor(moduleName, actionKind, pathPattern, processFun);
    }

}
