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

export function reldbSDK() {
    return new RelDBSDK("default")
}

export class RelDBSDK {

    constructor(codePostfix: string) {
        this.resourceSubjectCode = ".reldb." + codePostfix;
    }

    private readonly resourceSubjectCode: string

    exec<T>(sql: string, parameters: any[]): Promise<T[]> {
        return doExec<T>(_appId + this.resourceSubjectCode, sql, parameters)
    }

    subject(codePostfix: string) {
        return new RelDBSDK(codePostfix)
    }

}

function doExec<T>(resourceSubjectCode: string, rawSql: string, parameters: any[]): Promise<T[]> {
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

