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

import axios from 'axios';
import {JsonMap} from "../domain/Basic";
import {OptActionKind} from "../domain/Enum";

const TOKEN_FLAG = 'Dew-Token'
const REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri"
const REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action"
const APP_ID_FLAG = "Dew-App-Id"
let _token: string = ''
let _serverUrl: string = ''
let _appId: number = 0

const MOCK_DATA: JsonMap<Function> = {}

export function mock(items: JsonMap<Function>) {
    for (let k in items) {
        MOCK_DATA[k.toLowerCase()] = items[k]
    }
}

export function addMock(code: string, fun: Function) {
    MOCK_DATA[code.toLowerCase()] = fun
}

export function setToken(token: string): void {
    _token = token
}

export function setAppId(appId: number): void {
    _appId = appId
}

export function setServerUrl(serverUrl: string): void {
    if (serverUrl.trim().endsWith("/")) {
        serverUrl = serverUrl.trim().substring(0, serverUrl.trim().length - 1)
    }
    _serverUrl = serverUrl + '/exec'
}

export function req<T>(name: string, resourceUri: string, optActionKind: OptActionKind, body?: any, headers?: JsonMap<any>, rawResult?: boolean): Promise<T> {
    if (MOCK_DATA.hasOwnProperty(name.toLowerCase())) {
        console.log('[Dew]Mock request [%s]%s', optActionKind, resourceUri)
        return new Promise<T>((resolve) => {
            resolve(MOCK_DATA[name.toLowerCase()].call(null, optActionKind, resourceUri, body))
        })
    }
    if (!_appId) {
        throw 'The Http Head must contain [' + APP_ID_FLAG + ']'
    }
    headers = headers ? headers : {}
    headers[APP_ID_FLAG] = _appId + ''
    headers[TOKEN_FLAG] = _token ? _token : ''
    console.log('[Dew]Request [%s]%s , GW = [%s]', optActionKind, resourceUri, _serverUrl)
    return new Promise<T>((resolve, reject) => {
        axios.request<any>({
            url: _serverUrl + '?' + REQUEST_RESOURCE_URI_FLAG + '=' + encodeURIComponent(resourceUri) + '&' + REQUEST_RESOURCE_ACTION_FLAG + '=' + optActionKind,
            method: 'post',
            headers: headers,
            data: body
        })
            .then(res => {
                let data = res.data
                if (rawResult) {
                    resolve(data)
                } else {
                    if (data.code === '200') {
                        resolve(data.body.trim() == "" ? "" : JSON.parse(data.body))
                    } else {
                        console.error('请求错误 : [' + data.code + ']' + data.message)
                        reject('[' + data.code + ']' + data.message)
                    }
                }
            })
            .catch(error => {
                console.error('请求错误 : [' + error.response.status + ']' + error.stack)
                reject(error)
            })
    })
}
