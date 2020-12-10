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

export function cacheSDK() {
    return CacheSDK(_appId + ".cache.default")
}

function CacheSDK(resourceSubjectCode: string) {

    return {
        exists(key: string): Promise<boolean> {
            checkKey(key)
            return cache<boolean>(resourceSubjectCode, OptActionKind.EXISTS, key)
        },
        hexists(key: string, fieldName: string): Promise<boolean> {
            checkKey(key)
            return cache<boolean>(resourceSubjectCode, OptActionKind.EXISTS, key + "/" + fieldName)
        },
        get<T>(key: string): Promise<T> {
            checkKey(key)
            return cache<T>(resourceSubjectCode, OptActionKind.FETCH, key)
        },
        hgetall<T>(key: string): Promise<T> {
            checkKey(key)
            return cache<T>(resourceSubjectCode, OptActionKind.FETCH, key + '/*')
        },
        hget<T>(key: string, fieldName: string): Promise<T> {
            checkKey(key)
            return cache<T>(resourceSubjectCode, OptActionKind.FETCH, key + '/' + fieldName)
        },
        del(key: string): Promise<void> {
            checkKey(key)
            return cache<void>(resourceSubjectCode, OptActionKind.DELETE, key)
        },
        hdel(key: string, fieldName: string): Promise<void> {
            checkKey(key)
            return cache<void>(resourceSubjectCode, OptActionKind.DELETE, key + '/' + fieldName)
        },
        incrby(key: string, step: number): Promise<number> {
            checkKey(key)
            return cache<number>(resourceSubjectCode, OptActionKind.CREATE, key + '?incr=true', step + '')
        },
        hincrby(key: string, fieldName: string, step: number): Promise<number> {
            checkKey(key)
            return cache<number>(resourceSubjectCode, OptActionKind.CREATE, key + '/' + fieldName + '?incr=true', step + '')
        },
        set(key: string, value: any): Promise<void> {
            checkKey(key)
            return cache<void>(resourceSubjectCode, OptActionKind.CREATE, key, value)
        },
        hset(key: string, fieldName: string, value: any): Promise<void> {
            checkKey(key)
            return cache<void>(resourceSubjectCode, OptActionKind.CREATE, key + '/' + fieldName, value)
        },
        setex(key: string, value: any, expireSec: number): Promise<void> {
            checkKey(key)
            return cache<void>(resourceSubjectCode, OptActionKind.CREATE, key + '?expire=' + expireSec, value)
        },
        expire(key: string, expireSec: number): Promise<void> {
            checkKey(key)
            return cache<void>(resourceSubjectCode, OptActionKind.CREATE, key + '?expire=' + expireSec)
        },
        subject(resourceSubject: string) {
            return CacheSDK(resourceSubject)
        }
    }

}

function checkKey(key: string): void {
    if (key.indexOf('/') !== -1) {
        throw 'key不能包含[/]'
    }
}

function cache<T>(resourceSubjectCode: string, optActionKind: OptActionKind, pathAndQuery: string, body?: any): Promise<T> {
    return request.req<T>('cache', 'cache://' + resourceSubjectCode + '/' + pathAndQuery, optActionKind, body)
}
