"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.req = exports.setServerUrl = exports.setAkSk = void 0;
const axios_1 = __importDefault(require("axios"));
const hmac_sha1_1 = __importDefault(require("crypto-js/hmac-sha1"));
const enc_base64_1 = __importDefault(require("crypto-js/enc-base64"));
const enc_utf8_1 = __importDefault(require("crypto-js/enc-utf8"));
const moment_1 = __importDefault(require("moment"));
const AUTHENTICATION_HEAD_NAME = 'Authentication';
const DATE_HEAD_NAME = 'Dew-Date';
let _serverUrl = '';
let _ak = '';
let _sk = '';
function setAkSk(ak, sk) {
    _ak = ak;
    _sk = sk;
}
exports.setAkSk = setAkSk;
function setServerUrl(serverUrl) {
    if (serverUrl.trim().endsWith("/")) {
        serverUrl = serverUrl.trim().substring(0, serverUrl.trim().length - 1);
    }
    _serverUrl = serverUrl;
}
exports.setServerUrl = setServerUrl;
function req(name, method, pathAndQuery, body, headers) {
    headers = headers ? headers : {};
    generateAuthentication(method, pathAndQuery, headers);
    console.log('[Dew]Request [%s]%s', method, pathAndQuery);
    return new Promise((resolve, reject) => {
        axios_1.default.request({
            url: _serverUrl + pathAndQuery,
            method: method,
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
function generateAuthentication(method, pathAndQuery, headers) {
    if (!_ak || !_sk) {
        return;
    }
    let item = pathAndQuery.split('?');
    let path = item[0];
    let query = item.length === 2 ? item[1] : '';
    if (query) {
        query = query.split('&').sort((a, b) => a < b ? 1 : -1).join("&");
    }
    let date = moment_1.default.utc().format('ddd, DD MMM YYYY HH:mm:ss [GMT]');
    let signature = enc_base64_1.default.stringify(enc_utf8_1.default.parse(hmac_sha1_1.default((method + '\n' + date + '\n' + path + '\n' + query).toLowerCase(), _sk).toString()));
    headers[AUTHENTICATION_HEAD_NAME] = _ak + ':' + signature;
    headers[DATE_HEAD_NAME] = date;
}
