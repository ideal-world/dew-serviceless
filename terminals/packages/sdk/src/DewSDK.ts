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

import * as request from "./util/Request";
import * as iamSDK from "./module/IAMSDK";
import * as cacheSDK from "./module/CacheSDK";
import * as httpSDK from "./module/HttpSDK";
import * as reldbSDK from "./module/RelDBSDK";
import * as taskSDK from "./module/TaskSDK";
import {JsonMap} from "./domain/Basic";

export const SDK = {
    init: init,
    iam: iamSDK.iamSDK,
    reldb: reldbSDK.reldbSDK(),
    cache: cacheSDK.cacheSDK(),
    http: httpSDK.httpSDK(),
    task: taskSDK.taskSDK(),
    setting: {
        serverUrl: function (serverUrl: string): void {
            request.setServerUrl(serverUrl)
        },
        appId: function (appId: string): void {
            request.setAppId(appId)
            iamSDK.init(appId)
            reldbSDK.init(appId)
            cacheSDK.init(appId)
            httpSDK.init(appId)
            taskSDK.init(appId)
            // 重新赋值一次
            SDK.reldb = reldbSDK.reldbSDK()
            SDK.cache = cacheSDK.cacheSDK()
            SDK.http = httpSDK.httpSDK()
            SDK.task = taskSDK.taskSDK()
        },
        aksk: function (ak: string, sk: string): void {
            request.setAkSk(ak, sk)
        },
        ajax: function (impl: (url: string, headers?: JsonMap<any>, data?: any) => Promise<any>): void {
            request.setAjaxImpl(impl)
        },
        currentTime: function (impl: () => string): void {
            request.setCurrentTime(impl)
        },
        signature: function (impl: (text: string, key: string) => string): void {
            request.setSignature(impl)
        }
    }
}

/**
 * 初始化SDK
 * @param serverUrl 服务网关地址
 * @param appId 当前应用Id
 */
function init(serverUrl: string, appId: string): void {
    SDK.setting.serverUrl(serverUrl)
    SDK.setting.appId(appId)
}






