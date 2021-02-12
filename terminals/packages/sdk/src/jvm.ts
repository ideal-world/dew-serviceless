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

import {SDK, setAjax, setCurrentTime, setSignature} from "./DewSDK";

/**
 * SDK调用入口.
 */
export let DewSDK: SDK

/**
 * 初始化默认的SDK
 * @param serverUrl 服务网关地址
 * @param appId 当前应用Id
 */
export function initDefaultSDK(serverUrl: string, appId: string): void {
    DewSDK = new SDK(serverUrl, appId)
}

setAjax((url, headers, data) => {
    return new Promise((resolve, reject) => {
        try {
            // @ts-ignore
            resolve({data: JSON.parse($.req(url, headers, data))})
        } catch (e) {
            reject({"message": e.getMessage(), "stack": []})
        }
    })
})

setCurrentTime(() => {
    // @ts-ignore
    return $.currentTime()
})

setSignature((text, key) => {
    // @ts-ignore
    return $.signature(text, key)
})
