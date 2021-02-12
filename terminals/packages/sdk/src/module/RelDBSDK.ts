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

export function reldbSDK(request: DewRequest) {
    return new RelDBSDK("default", request)
}

export class RelDBSDK {

    constructor(codePostfix: string, request: DewRequest) {
        this.resourceSubjectCode = ".reldb." + codePostfix;
        this.request = request
    }

    private readonly resourceSubjectCode: string
    private readonly request: DewRequest

    exec<T>(sql: string, parameters: any[]): Promise<T[]> {
        return doExec<T>(this.request, this.request.getAppId() + this.resourceSubjectCode, sql, parameters)
    }

    subject(codePostfix: string) {
        return new RelDBSDK(codePostfix, this.request)
    }

}

function doExec<T>(request: DewRequest, resourceSubjectCode: string, rawSql: string, parameters: any[]): Promise<T[]> {
    let sql = rawSql.trim().toLocaleLowerCase()
    let action = sql.startsWith('select ') && sql.indexOf(' from ') !== -1 ? 'FETCH'
        : sql.startsWith('insert ') && sql.indexOf(' into ') !== -1 && sql.indexOf(' values ') !== -1 ? 'CREATE'
            : sql.startsWith('update ') && sql.indexOf(' set ') !== -1 && sql.indexOf(' where ') !== -1 ? 'MODIFY'
                : sql.startsWith('delete ') && sql.indexOf(' where ') !== -1 ? 'DELETE'
                    : sql.startsWith('create table ') ? 'CREATE' : null
    if (action == null) {
        throw 'SQL操作不合法:' + rawSql.trim()
    }
    return request.req<T[]>('reldb', 'reldb://' + resourceSubjectCode, OptActionKind[action], JSON.stringify({
        sql: rawSql.trim(),
        parameters: parameters
    }))
}

