import axios, {Method} from "axios";
import hmacSHA1 from 'crypto-js/hmac-sha1';
import Base64 from 'crypto-js/enc-base64';
import utf8 from 'crypto-js/enc-utf8';
import moment from "moment";

const AUTHENTICATION_HEAD_NAME = 'Authentication'
const DATE_HEAD_NAME = 'Dew-Date'
let _serverUrl: string = ''
let _ak: string = ''
let _sk: string = ''

export function setAkSk(ak: string, sk: string): void {
    _ak = ak
    _sk = sk
}

export function setServerUrl(serverUrl: string): void {
    if (serverUrl.trim().endsWith("/")) {
        serverUrl = serverUrl.trim().substring(0, serverUrl.trim().length - 1)
    }
    _serverUrl = serverUrl
}

export function req<T>(name: string, method: Method, pathAndQuery: string, body?: any, headers?: any): Promise<T> {
    headers = headers ? headers : {}
    generateAuthentication(method, pathAndQuery, headers)
    console.log('[Dew]Request [%s]%s', method, pathAndQuery)
    return new Promise((resolve, reject) => {
        axios.request<any>({
            url: _serverUrl + pathAndQuery,
            method: method,
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
