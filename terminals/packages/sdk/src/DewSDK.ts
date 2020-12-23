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

import * as request from "./util/Request";
import * as iamSDK from "./module/IAMSDK";
import * as cacheSDK from "./module/CacheSDK";
import * as httpSDK from "./module/HttpSDK";
import * as reldbSDK from "./module/RelDBSDK";

export const DewSDK = {
    init: init,
    iam: iamSDK.iamSDK,
    reldb: reldbSDK.reldbSDK(),
    cache: cacheSDK.cacheSDK(),
    http: httpSDK.httpSDK(),
}

function init(serverUrl: string, appId: number): void {
    request.setServerUrl(serverUrl)
    request.setAppId(appId)
    iamSDK.init(appId)
    reldbSDK.init(appId)
    cacheSDK.init(appId)
    httpSDK.init(appId)
    // 重新赋值一次
    DewSDK.reldb = reldbSDK.reldbSDK()
    DewSDK.cache = cacheSDK.cacheSDK()
    DewSDK.http = httpSDK.httpSDK()
}






