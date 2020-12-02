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
import {JsonMap} from "../domain/Basic";


let _appId: number = 0

export function init(appId: number): void {
    _appId = appId
}

export function httpSDK(){
    return HttpSDK(_appId + ".http.default")
}

function HttpSDK(resourceSubjectCode: string) {

    return {
        get<T>(pathAndQuery: string, header?: JsonMap<any>): Promise<T> {
            return http<T>(resourceSubjectCode, OptActionKind.FETCH, pathAndQuery, header)
        },
        delete(pathAndQuery: string, header?: JsonMap<any>): Promise<void> {
            return http<void>(resourceSubjectCode, OptActionKind.DELETE, pathAndQuery, header)
        },
        post<T>(pathAndQuery: string, body: any, header?: JsonMap<any>): Promise<T> {
            return http<T>(resourceSubjectCode, OptActionKind.CREATE, pathAndQuery, header, body)
        },
        put<T>(pathAndQuery: string, body: any, header?: JsonMap<any>): Promise<T> {
            return http<T>(resourceSubjectCode, OptActionKind.MODIFY, pathAndQuery, header, body)
        },
        patch<T>(pathAndQuery: string, body: any, header?: JsonMap<any>): Promise<T> {
            return http<T>(resourceSubjectCode, OptActionKind.PATCH, pathAndQuery, header, body)
        },
        subject(resourceSubject: string) {
            return HttpSDK(resourceSubject)
        }
    }

}

function http<T>(resourceSubjectCode: string, optActionKind: OptActionKind, pathAndQuery: string, header?: JsonMap<any>, body?: any): Promise<T> {
    if (!pathAndQuery.startsWith('/')) {
        pathAndQuery = '/' + pathAndQuery
    }
    return request.req<T>('http', 'http://' + resourceSubjectCode + pathAndQuery, optActionKind, body, header, true)
}

