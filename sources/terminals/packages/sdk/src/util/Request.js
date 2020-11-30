"use strict";
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
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.req = exports.setServerUrl = exports.setAppId = exports.setToken = exports.addMock = exports.mock = void 0;
const axios_1 = __importDefault(require("axios"));
const TOKEN_FLAG = 'Dew-Token';
const REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri";
const REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action";
const APP_ID_FLAG = "Dew-App-Id";
let _token = '';
let _serverUrl = '';
let _appId = '';
const MOCK_DATA = {};
function mock(items) {
    for (let k in items) {
        MOCK_DATA[k.toLowerCase()] = items[k];
    }
}
exports.mock = mock;
function addMock(code, fun) {
    MOCK_DATA[code.toLowerCase()] = fun;
}
exports.addMock = addMock;
function setToken(token) {
    _token = token;
}
exports.setToken = setToken;
function setAppId(appId) {
    _appId = appId;
}
exports.setAppId = setAppId;
function setServerUrl(serverUrl) {
    if (serverUrl.trim().endsWith("/")) {
        serverUrl = serverUrl.trim().substring(0, serverUrl.trim().length - 1);
    }
    _serverUrl = serverUrl + '/exec';
}
exports.setServerUrl = setServerUrl;
function req(name, resourceUri, resourceAction, body, headers) {
    if (MOCK_DATA.hasOwnProperty(name.toLowerCase())) {
        console.log('[Dew]Mock request [%s]%s', resourceAction, resourceUri);
        return new Promise((resolve) => {
            resolve(MOCK_DATA[name.toLowerCase()].call(null, resourceAction, resourceUri, body));
        });
    }
    if (!_appId) {
        throw 'The Http Head must contain [' + APP_ID_FLAG + ']';
    }
    headers = headers ? headers : {};
    headers[APP_ID_FLAG] = _appId;
    headers[TOKEN_FLAG] = _token ? _token : '';
    console.log('[Dew]Request [%s]%s', resourceAction, resourceUri);
    return new Promise((resolve, reject) => {
        axios_1.default.request({
            url: _serverUrl + '?' + REQUEST_RESOURCE_URI_FLAG + '=' + resourceUri + '&' + REQUEST_RESOURCE_ACTION_FLAG + '=' + resourceAction,
            method: 'post',
            headers: headers,
            data: body
        })
            .then(res => {
            let data = res.data;
            if (data.code === '200') {
                resolve(data.body);
            }
            else {
                reject('[' + data.code + ']' + data.message);
            }
        })
            .catch(error => {
            reject(error);
        });
    });
}
exports.req = req;
