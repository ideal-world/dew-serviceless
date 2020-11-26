import * as request from "./util/Request";
import {IdentOptInfo} from "./domain/IdentOptInfo";

let _iamServerName: string = 'iam'
let _appId: string = ''

export default {
    db: db,
    fun: {
        login: login,
        logout: logout
    },
    http: {
        fetch: function (resourceSubjectCode: string, pathAndQuery: string): Promise<any[]> {
            return http(resourceSubjectCode, 'FETCH', pathAndQuery)
        },
        delete: function (resourceSubjectCode: string, pathAndQuery: string): Promise<any[]> {
            return http(resourceSubjectCode, 'DELETE', pathAndQuery)
        },
        create: function (resourceSubjectCode: string, pathAndQuery: string, body: any): Promise<any[]> {
            return http(resourceSubjectCode, 'CREATE', pathAndQuery, body)
        },
        modify: function (resourceSubjectCode: string, pathAndQuery: string, body: any): Promise<any[]> {
            return http(resourceSubjectCode, 'MODIFY', pathAndQuery, body)
        },
        patch: function (resourceSubjectCode: string, pathAndQuery: string, body: any): Promise<any[]> {
            return http(resourceSubjectCode, 'PATCH', pathAndQuery, body)
        }
    },
    cache: {
        fetch: function (resourceSubjectCode: string, pathAndQuery: string): Promise<any[]> {
            return cache(resourceSubjectCode, 'FETCH', pathAndQuery)
        },
        exists: function (resourceSubjectCode: string, pathAndQuery: string): Promise<any[]> {
            return cache(resourceSubjectCode, 'EXISTS', pathAndQuery)
        },
        delete: function (resourceSubjectCode: string, pathAndQuery: string): Promise<any[]> {
            return cache(resourceSubjectCode, 'DELETE', pathAndQuery)
        },
        create: function (resourceSubjectCode: string, pathAndQuery: string, body: any): Promise<any[]> {
            return cache(resourceSubjectCode, 'CREATE', pathAndQuery, body)
        },
        modify: function (resourceSubjectCode: string, pathAndQuery: string, body: any): Promise<any[]> {
            return http(resourceSubjectCode, 'MODIFY', pathAndQuery, body)
        },
        patch: function (resourceSubjectCode: string, pathAndQuery: string, body: any): Promise<any[]> {
            return cache(resourceSubjectCode, 'PATCH', pathAndQuery, body)
        }
    },
    menu: menu,
    ele: ele,
}

function init(serverUrl: string, appId: string, iamServerName?: string): void {
    _appId = appId
    request.setServerUrl(serverUrl)
    request.setAppId(appId)
    if (iamServerName) {
        _iamServerName = iamServerName
    }
}

function db(resourceSubjectCode: string, encryptedSql: string, parameters: string[]): Promise<any[]> {
    let item = encryptedSql.split('|')
    if (item.length !== 2) {
        throw "SQL statements are not encrypted : " + encryptedSql
    }
    return request.req('reldb', 'reldb://' + resourceSubjectCode, item[0], {
        sql: item[1],
        parameters: parameters
    })
}

function http(resourceSubjectCode: string, action: string, pathAndQuery: string, body?: any): Promise<any[]> {
    return request.req('http', 'http://' + resourceSubjectCode + pathAndQuery, action, body)
}

function cache(resourceSubjectCode: string, action: string, pathAndQuery: string, body?: any): Promise<any[]> {
    return request.req('cache', 'cache://' + resourceSubjectCode + pathAndQuery, action, body)
}

function menu(): Promise<any[]> {
    return request.req('menu', 'menu://' + _iamServerName, 'FETCH')
}

function ele(): Promise<any[]> {
    return request.req('element', 'menu://' + _iamServerName, 'FETCH')
}

function login(userName: string, password: string): Promise<IdentOptInfo> {
    return request.req<IdentOptInfo>('http', 'http://' + _iamServerName + '/common/login', 'CREATE', {
        ak: userName,
        sk: password,
        relAppId: _appId
    })
        .then(identOptInfo => {
            request.setToken(identOptInfo.token)
            return identOptInfo
        })
}

function logout(): Promise<void> {
    return request.req<void>('http', 'http://' + _iamServerName + '/common/logout', 'DELETE')
        .then(v => {
            request.setToken('')
        })
}

