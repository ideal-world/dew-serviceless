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

import * as request from "../util/Request";
import {OptActionKind} from "../domain/Enum";

let _appId: string = ''

export function init(appId: string): void {
    _appId = appId
}

export function taskSDK() {
    return new TaskSDK("default")
}

export class TaskSDK {

    constructor(codePostfix: string) {
        this.resourceSubjectCode = ".task." + codePostfix;
    }

    private readonly resourceSubjectCode: string

    initTasks(funs: string): Promise<void> {
        return task<void>("initTasks", _appId + this.resourceSubjectCode, OptActionKind.CREATE, 'task', funs)
    }

    create(taskCode: string, fun: string, cron?: string): Promise<void> {
        let query = ''
        if (cron) {
            query = '?cron=' + encodeURIComponent(cron)
        }
        return task<void>("createTask", _appId + this.resourceSubjectCode, OptActionKind.CREATE, 'task/' + taskCode + query, fun)
    }

    modify(taskCode: string, fun: string, cron?: string): Promise<void> {
        let query = ''
        if (cron) {
            query = '?cron=' + encodeURIComponent(cron)
        }
        return task<void>("modifyTask", _appId + this.resourceSubjectCode, OptActionKind.MODIFY, 'task/' + taskCode + query, fun)
    }

    delete(taskCode: string): Promise<void> {
        return task<void>("deleteTask", _appId + this.resourceSubjectCode, OptActionKind.DELETE, 'task/' + taskCode)
    }

    execute(taskCode: string, parameters: any[]): Promise<any> {
        return task<any>("executeTask", _appId + this.resourceSubjectCode, OptActionKind.CREATE, 'exec/' + taskCode, parameters)
    }

    subject(codePostfix: string) {
        return new TaskSDK(codePostfix)
    }

}

function task<T>(name: string, resourceSubjectCode: string, optActionKind: OptActionKind, pathAndQuery: string, body?: any): Promise<T> {
    return request.req<T>(name, 'task://' + resourceSubjectCode + '/' + pathAndQuery, optActionKind, body, {
        'Content-Type': 'text/plain'
    })
}
