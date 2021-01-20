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


import axios from "axios";
import {JsonMap} from "../domain/Basic";
import moment from "moment";
import Base64 from "crypto-js/enc-base64";
import utf8 from "crypto-js/enc-utf8";
import hmacSHA1 from "crypto-js/hmac-sha1";

export function axiosReq(url: string, headers?: JsonMap<any>, data?: any): Promise<any> {
    return axios.request<any>({
        url: url,
        method: 'POST',
        headers: headers,
        data: data
    })
}

export function currentTime(): string {
    return moment.utc().format('ddd, DD MMM YYYY HH:mm:ss [GMT]')
}

export function signature(text: string, key: string): string {
    return Base64.stringify(utf8.parse(hmacSHA1((text).toLowerCase(), key).toString()))
}
