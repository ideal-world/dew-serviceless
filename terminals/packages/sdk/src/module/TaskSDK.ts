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

import {OptActionKind} from "../domain/Enum";
import {DewRequest} from "../util/DewRequest";

export function taskSDK(request: DewRequest) {
    return new TaskSDK("default", request)
}

export class TaskSDK {

    constructor(codePostfix: string, request: DewRequest) {
        this.resourceSubjectCode = ".task." + codePostfix;
        this.request = request
    }

    private readonly resourceSubjectCode: string
    private readonly request: DewRequest

    initTasks(funs: string): Promise<void> {
        return task<void>(this.request, "initTasks", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, 'task', funs)
    }

    create(taskCode: string, fun: string, cron?: string): Promise<void> {
        let query = ''
        if (cron) {
            query = '?cron=' + encodeURIComponent(cron)
        }
        return task<void>(this.request, "createTask", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, 'task/' + taskCode + query, fun)
    }

    modify(taskCode: string, fun: string, cron?: string): Promise<void> {
        let query = ''
        if (cron) {
            query = '?cron=' + encodeURIComponent(cron)
        }
        return task<void>(this.request, "modifyTask", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.MODIFY, 'task/' + taskCode + query, fun)
    }

    delete(taskCode: string): Promise<void> {
        return task<void>(this.request, "deleteTask", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.DELETE, 'task/' + taskCode)
    }

    execute(taskCode: string, parameters: any[]): Promise<any> {
        return task<any>(this.request, "executeTask", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, 'exec/' + taskCode, parameters)
    }

    subject(codePostfix: string) {
        return new TaskSDK(codePostfix, this.request)
    }

}

function task<T>(request: DewRequest, name: string, resourceSubjectCode: string, optActionKind: OptActionKind, pathAndQuery: string, body?: any): Promise<T> {
    return request.req<T>(name, 'task://' + resourceSubjectCode + '/' + pathAndQuery, optActionKind, body, {
        'Content-Type': 'text/plain'
    })
}
