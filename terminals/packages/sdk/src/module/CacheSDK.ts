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

export function cacheSDK(request: DewRequest) {
    return new CacheSDK("default", request)
}

export class CacheSDK {

    constructor(codePostfix: string, request: DewRequest) {
        this.resourceSubjectCode = ".cache." + codePostfix;
        this.request = request
    }

    private readonly resourceSubjectCode: string
    private readonly request: DewRequest

    exists(key: string): Promise<boolean> {
        checkKey(key)
        return cache<boolean>(this.request, "existsCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.EXISTS, key)
    }

    hexists(key: string, fieldName: string): Promise<boolean> {
        checkKey(key)
        return cache<boolean>(this.request, "hexistsCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.EXISTS, key + "/" + fieldName)
    }

    get<T>(key: string): Promise<T> {
        checkKey(key)
        return cache<T>(this.request, "getCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.FETCH, key)
    }

    hgetall<T>(key: string): Promise<T> {
        checkKey(key)
        return cache<T>(this.request, "hgetallCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.FETCH, key + '/*')
    }

    hget<T>(key: string, fieldName: string): Promise<T> {
        checkKey(key)
        return cache<T>(this.request, "hgetCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.FETCH, key + '/' + fieldName)
    }

    del(key: string): Promise<void> {
        checkKey(key)
        return cache<void>(this.request, "delCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.DELETE, key)
    }

    hdel(key: string, fieldName: string): Promise<void> {
        checkKey(key)
        return cache<void>(this.request, "hdelCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.DELETE, key + '/' + fieldName)
    }

    incrby(key: string, step: number): Promise<number> {
        checkKey(key)
        return cache<number>(this.request, "incrbyCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, key + '?incr=true', step + '')
    }

    hincrby(key: string, fieldName: string, step: number): Promise<number> {
        checkKey(key)
        return cache<number>(this.request, "hincrbyCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, key + '/' + fieldName + '?incr=true', step + '')
    }

    set(key: string, value: any): Promise<void> {
        checkKey(key)
        return cache<void>(this.request, "setCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, key, value)
    }

    hset(key: string, fieldName: string, value: any): Promise<void> {
        checkKey(key)
        return cache<void>(this.request, "hsetCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, key + '/' + fieldName, value)
    }

    setex(key: string, value: any, expireSec: number): Promise<void> {
        checkKey(key)
        return cache<void>(this.request, "setexCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, key + '?expire=' + expireSec, value)
    }

    expire(key: string, expireSec: number): Promise<void> {
        checkKey(key)
        return cache<void>(this.request, "expireCache", this.request.getAppId() + this.resourceSubjectCode, OptActionKind.CREATE, key + '?expire=' + expireSec)
    }

    subject(codePostfix: string) {
        return new CacheSDK(codePostfix, this.request)
    }

}

function checkKey(key: string): void {
    if (key.indexOf('/') !== -1) {
        throw 'key不能包含[/]'
    }
}

function cache<T>(request: DewRequest, name: string, resourceSubjectCode: string, optActionKind: OptActionKind, pathAndQuery: string, body?: any): Promise<T> {
    return request.req<T>(name, 'cache://' + resourceSubjectCode + '/' + pathAndQuery, optActionKind, body)
}
