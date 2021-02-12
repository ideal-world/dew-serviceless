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
import {JsonMap} from "../domain/Basic";
import {DewRequest} from "../util/DewRequest";

export function httpSDK(request: DewRequest): HttpSDK {
    return new HttpSDK("default", request)
}

export class HttpSDK {

    constructor(codePostfix: string, request: DewRequest) {
        this.resourceSubjectCode = ".http." + codePostfix;
        this.request = request
    }

    private readonly resourceSubjectCode: string
    private readonly request: DewRequest

    get<T>(pathAndQuery: string, header?: JsonMap<any>): Promise<T> {
        return http<T>(this.request, this.request.getAppId() + this.resourceSubjectCode, OptActionKind.FETCH, pathAndQuery, header)
    }

    delete(pathAndQuery: string, header?: JsonMap<any>): Promise<void> {
        return http<void>(this.request, this.request.getAppId() + this.resourceSubjectCode, OptActionKind.DELETE, pathAndQuery, header)
    }

    post<T>(pathAndQuery: string, body: any, header?: JsonMap<any>): Promise<T> {
        return http<T>(this.request, this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, pathAndQuery, header, body)
    }

    put<T>(pathAndQuery: string, body: any, header?: JsonMap<any>): Promise<T> {
        return http<T>(this.request, this.request.getAppId() + this.resourceSubjectCode, OptActionKind.MODIFY, pathAndQuery, header, body)
    }

    patch<T>(pathAndQuery: string, body: any, header?: JsonMap<any>): Promise<T> {
        return http<T>(this.request, this.request.getAppId() + this.resourceSubjectCode, OptActionKind.PATCH, pathAndQuery, header, body)
    }

    subject(codePostfix: string): HttpSDK {
        return new HttpSDK(codePostfix, this.request)
    }

}

function http<T>(request: DewRequest, resourceSubjectCode: string, optActionKind: OptActionKind, pathAndQuery: string, header?: JsonMap<any>, body?: any): Promise<T> {
    if (!pathAndQuery.startsWith('/')) {
        pathAndQuery = '/' + pathAndQuery
    }
    if (!header) {
        header = {}
    }
    header["Content-Type"] = "application/json charset=utf-8"
    return request.req<T>('http', 'http://' + resourceSubjectCode + pathAndQuery, optActionKind, body, header, true)
}

