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

import * as request from "../util/Request";
import {OptActionKind} from "../domain/Enum";


let _appId: number = 0

export function init(appId: number): void {
    _appId = appId
}

export function taskSDK() {
    return new TaskSDK("default")
}

export class TaskSDK {

    constructor(codePostfix: string) {
        this.resourceSubjectCode = _appId + ".task." + codePostfix;
    }

    private readonly resourceSubjectCode: string

    create(taskCode: string, fun: string, cron?: string): Promise<void> {
        let query = ''
        if (cron) {
            query = '?cron=' + encodeURIComponent(cron)
        }
        return task<void>("createTask", this.resourceSubjectCode, OptActionKind.CREATE, 'task/' + taskCode + query, fun)
    }

    modify(taskCode: string, fun: string, cron?: string): Promise<void> {
        let query = ''
        if (cron) {
            query = '?cron=' + encodeURIComponent(cron)
        }
        return task<void>("modifyTask", this.resourceSubjectCode, OptActionKind.MODIFY, 'task/' + taskCode + query, fun)
    }

    delete(taskCode: string): Promise<void> {
        return task<void>("deleteTask", this.resourceSubjectCode, OptActionKind.DELETE, 'task/' + taskCode)
    }

    execute(taskCode: string, parameters: any[]): Promise<any> {
        return task<any>("executeTask", this.resourceSubjectCode, OptActionKind.CREATE, 'exec/' + taskCode, parameters)
    }

    subject(codePostfix: string) {
        return new TaskSDK(codePostfix)
    }

}

function task<T>(name: string, resourceSubjectCode: string, optActionKind: OptActionKind, pathAndQuery: string, body?: any): Promise<T> {
    return request.req<T>(name, 'task://' + resourceSubjectCode + '/' + pathAndQuery, optActionKind, body)
}
