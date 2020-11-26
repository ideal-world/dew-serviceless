import axios from "axios";
import hmacSHA1 from 'crypto-js/hmac-sha1';
import Base64 from 'crypto-js/enc-base64';
import utf8 from 'crypto-js/enc-utf8';
import moment from "moment";
import {JsonMap} from "../domain/Basic";

const MOCK_DATA: JsonMap<Function> = {}

const TOKEN_FLAG = 'Dew-Token'
const REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri"
const REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action"
const APP_ID_FLAG = "Dew-App-Id"

const AUTHENTICATION_HEAD_NAME = 'Authentication'
const DATE_HEAD_NAME = 'Dew-Date'

let _serverUrl: string = ''
let _token: string = ''
let _ak: string = ''
let _sk: string = ''

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

export function setAkSk(ak: string, sk: string): void {
    _ak = ak
    _sk = sk
}

export function setServerUrl(serverUrl: string): void {
    if (serverUrl.trim().endsWith("/")) {
        serverUrl = serverUrl.trim().substring(0, serverUrl.trim().length - 1)
    }
    _serverUrl = serverUrl + '/exec'
}

export function req<T>(name: string, resourceUri: string, resourceAction: string, body?: any, headers?: JsonMap<any>): Promise<T> {
    if (MOCK_DATA.hasOwnProperty(name.toLowerCase())) {
        console.log('[Dew]Mock request [%s]%s', resourceAction, resourceUri)
        return new Promise<T>((resolve) => {
            resolve(MOCK_DATA[name.toLowerCase()].call(null, resourceAction, resourceUri, body))
        })
    }
    headers = headers ? headers : {}
    headers[TOKEN_FLAG] = _token ? _token : ''
    let pathAndQuery = REQUEST_RESOURCE_URI_FLAG + '=' + resourceUri + '&' + REQUEST_RESOURCE_ACTION_FLAG + '=' + resourceAction
    generateAuthentication('post', pathAndQuery, headers)
    console.log('[Dew]Request [%s]%s', resourceAction, resourceUri)
    return new Promise((resolve, reject) => {
        axios.request<any>({
            url: _serverUrl + '?' + pathAndQuery,
            method: 'post',
            headers: headers,
            data: body
        })
            .then(res => {
                let data = res.data
                if (data.code === '200') {
                    resolve(data.body)
                } else {
                    reject('[' + data.code + ']' + data.message)
                }
            })
            .catch(error => {
                reject(error)
            })
    })
}

function generateAuthentication(method: string, pathAndQuery: string, headers: any): void {
    if (!_ak || !_sk) {
        return
    }
    let item = pathAndQuery.split('?')
    let path = item[0]
    let query = item.length === 2 ? item[1] : ''
    if (query) {
        query = query.split('&').sort((a, b) => a < b ? 1 : -1).join("&")
    }
    let date = moment.utc().format('ddd, DD MMM YYYY HH:mm:ss [GMT]')
    let signature = Base64.stringify(utf8.parse(hmacSHA1((method + '\n' + date + '\n' + path + '\n' + query).toLowerCase(), _sk).toString()))
    headers[AUTHENTICATION_HEAD_NAME] = _ak + ':' + signature
    headers[DATE_HEAD_NAME] = date
}
