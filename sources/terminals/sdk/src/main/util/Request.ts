import axios from 'axios';
import {JsonMap} from "../domain/Basic";

const TOKEN_FLAG = 'Dew-Token'
const REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri"
const REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action"
const APP_ID_FLAG = "Dew-App-Id"
let _token: string = ''
let _serverUrl: string = ''
let _appId: string = ''

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

export function setAppId(appId: string): void {
    _appId = appId
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
    if(!_appId){
        throw 'The Http Head must contain ['+APP_ID_FLAG+']'
    }
    headers = headers ? headers : {}
    headers[APP_ID_FLAG] = _appId
    headers[TOKEN_FLAG] = _token ? _token : ''
    console.log('[Dew]Request [%s]%s', resourceAction, resourceUri)
    return new Promise<T>((resolve, reject) => {
        axios.request<any>({
            url: _serverUrl + '?' + REQUEST_RESOURCE_URI_FLAG + '=' + resourceUri + '&' + REQUEST_RESOURCE_ACTION_FLAG + '=' + resourceAction,
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
