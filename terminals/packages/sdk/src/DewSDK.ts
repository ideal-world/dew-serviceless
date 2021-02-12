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

import * as iamSDK from "./module/IAMSDK";
import * as cacheSDK from "./module/CacheSDK";
import * as httpSDK from "./module/HttpSDK";
import * as reldbSDK from "./module/RelDBSDK";
import * as taskSDK from "./module/TaskSDK";
import {JsonMap} from "./domain/Basic";
import * as request from "./util/DewRequest";
import {DewRequest} from "./util/DewRequest";

/**
 * Dew SDK.
 */
export class SDK {

    /**
     * 初始化SDK.
     * @param serverUrl 服务网关地址
     * @param appId 当前应用Id
     */
    constructor(serverUrl: string, appId: string) {
        this.serverUrl = serverUrl;
        this.appId = appId
        this.request.setServerUrl(serverUrl)
        this.request.setAppId(appId)
    }

    private request: DewRequest = new DewRequest()
    private serverUrl: string
    private appId: string
    private _setting = () => {
        const that = this

        function aksk(ak: string, sk: string): void {
            that.request.setToken("")
            that.request.setAkSk(ak, sk)
        }

        return {
            aksk: aksk,
            conf: loadConfig
        }
    }

    iam = new iamSDK.IAMSDK(this.request)
    reldb = reldbSDK.reldbSDK(this.request)
    cache = cacheSDK.cacheSDK(this.request)
    http = httpSDK.httpSDK(this.request)
    task = taskSDK.taskSDK(this.request)
    setting = this._setting()
}

export function setAjax(impl: (url: string, headers?: JsonMap<any>, data?: any) => Promise<any>): void {
    request.setAjaxImpl(impl)
}

export function setCurrentTime(impl: () => string): void {
    request.setCurrentTime(impl)
}

export function setSignature(impl: (text: string, key: string) => string): void {
    request.setSignature(impl)
}

export function loadConfig(confContext: string | any, envName: string): any {
    const config = typeof confContext === "string" ? JSON.parse(confContext.trim()) : confContext
    console.log("Load config " + envName + " ENV ")
    if (config.hasOwnProperty('env') && config['env'].hasOwnProperty(envName)) {
        let envConfig = config['env'][envName]
        delete config['env']
        for (let k in config) {
            if (!envConfig.hasOwnProperty(k)) {
                envConfig[k] = config[k]
            }
        }
        return envConfig
    } else {
        delete config['env']
        return config
    }
}






