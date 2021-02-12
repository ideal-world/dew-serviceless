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

import {JsonMap} from "../domain/Basic";
import {OptActionKind} from "../domain/Enum";

const TOKEN_FLAG = 'Dew-Token'
const APP_FLAG = 'Dew-App-Id'
const REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri"
const REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action"

const AUTHENTICATION_HEAD_NAME = 'Authorization'
const DATE_HEAD_NAME = 'Dew-Date'

let ajax: (url: string, headers?: JsonMap<any>, data?: any) => Promise<any>
let currentTime: () => string
let signature: (text: string, key: string) => string

export function setAjaxImpl(impl: (url: string, headers?: JsonMap<any>, data?: any) => Promise<any>) {
    ajax = impl
}

export function setCurrentTime(impl: () => string) {
    currentTime = impl
}

export function setSignature(impl: (text: string, key: string) => string) {
    signature = impl
}

export class DewRequest {

    private appId: string = ''
    private token: string = ''
    private serverUrl: string = ''
    private ak: string = ''
    private sk: string = ''

    setAppId(appId: string): void {
        this.appId = appId
    }

    getAppId(): string {
        return this.appId
    }

    setToken(token: string): void {
        this.token = token
    }

    setAkSk(ak: string, sk: string): void {
        this.ak = ak
        this.sk = sk
    }

    setServerUrl(serverUrl: string): void {
        if (serverUrl.trim().endsWith("/")) {
            serverUrl = serverUrl.trim().substring(0, serverUrl.trim().length - 1)
        }
        this.serverUrl = serverUrl
    }

    req<T>(name: string, resourceUri: string, optActionKind: OptActionKind, body?: any, headers?: JsonMap<any>, rawResult?: boolean): Promise<T> {
        headers = headers ? headers : {}
        if (this.appId) {
            headers[APP_FLAG] = this.appId
        }
        headers[TOKEN_FLAG] = this.token ? this.token : ''
        let pathAndQuery = '/exec?' + REQUEST_RESOURCE_ACTION_FLAG + '=' + optActionKind + '&' + REQUEST_RESOURCE_URI_FLAG + '=' + encodeURIComponent(resourceUri)
        generateAuthentication('post', pathAndQuery, headers, this.ak, this.sk)
        let url = this.serverUrl + pathAndQuery
        console.log('[Dew]Request [%s]%s , GW = [%s]', optActionKind, resourceUri, url)
        return new Promise<T>((resolve, reject) => {
            ajax(
                url,
                headers,
                typeof body === "undefined" ? "" : body
            )
                .then(res => {
                    let data = res.data
                    if (rawResult) {
                        resolve(data)
                    } else {
                        if (data.code === '200') {
                            resolve(data.body)
                        } else {
                            console.error('请求错误 : [' + data.code + ']' + data.message)
                            reject('[' + data.code + ']' + data.message)
                        }
                    }
                })
                .catch(error => {
                    console.error('服务错误 : [' + error.message + ']' + error.stack)
                    reject(error.message)
                })
        })
    }

}

function generateAuthentication(method: string, pathAndQuery: string, headers: any, ak: string, sk: string): void {
    if (!ak || !sk) {
        return
    }
    let item = pathAndQuery.split('?')
    let path = item[0]
    let query = item.length === 2 ? item[1] : ''
    if (query) {
        query = query.split('&').sort((a, b) => a < b ? -1 : 1).join("&")
    }
    let date = currentTime()
    headers[AUTHENTICATION_HEAD_NAME] = ak + ':' + signature(method + '\n' + date + '\n' + path + '\n' + query, sk)
    headers[DATE_HEAD_NAME] = date
}
