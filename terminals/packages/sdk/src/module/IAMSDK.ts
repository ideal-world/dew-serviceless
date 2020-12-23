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
import {IdentOptInfo} from "../domain/IdentOptInfo";
import {OptActionKind} from "../domain/Enum";

const iamModuleName: string = 'iam'
let _appId: number = 0

export function init(appId: number): void {
    _appId = appId
}

export const iamSDK = {
    login: login,
    logout: logout,
    menu: {
        fetch: fetchMenu
    },
    ele: {
        fetch: fetchEle
    },
}

function fetchMenu(): Promise<any[]> {
    return request.req('fetchMenu', 'menu://' + iamModuleName + '/common/resource?kind=menu', OptActionKind.FETCH)
}

function fetchEle(): Promise<any[]> {
    return request.req('fetchEle', 'element://' + iamModuleName + '/common/resource?kind=element', OptActionKind.FETCH)
}

function login(userName: string, password: string): Promise<IdentOptInfo> {
    return request.req<IdentOptInfo>('login', 'http://' + iamModuleName + '/common/login', OptActionKind.CREATE, {
        ak: userName,
        sk: password,
        relAppId: _appId
    })
        .then(identOptInfo => {
            request.setToken(identOptInfo.token)
            return identOptInfo
        })
}

function logout(): Promise<void> {
    return request.req<void>('logout', 'http://' + iamModuleName + '/common/logout', OptActionKind.DELETE)
        .then(v => {
            request.setToken('')
        })
}
