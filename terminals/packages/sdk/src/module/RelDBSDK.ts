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
    return RelDBSDK(_appId + ".reldb.default")
}

function RelDBSDK(resourceSubjectCode: string) {

    return {
        exec<T>(encryptedSql: string, parameters: any[]): Promise<T[]> {
            return doExec<T>(resourceSubjectCode, encryptedSql, parameters)
        },
        subject(resourceSubject: string) {
            return RelDBSDK(resourceSubject)
        }
    }

}

function doExec<T>(resourceSubjectCode: string, encryptedSql: string, parameters: any[]): Promise<T[]> {
    let item = encryptedSql.split('|')
    if (item.length !== 2) {
        throw "该SQL语句没有加密 : " + encryptedSql
    }
    return request.req<T[]>('reldb', 'reldb://' + resourceSubjectCode, OptActionKind[item[0]], {
        sql: item[1],
        parameters: parameters
    })
}

