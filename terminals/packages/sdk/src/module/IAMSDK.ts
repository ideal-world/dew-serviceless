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

import * as request from "../util/Request";
import {IdentOptInfo} from "../domain/IdentOptInfo";
import {AccountIdentKind, AuthSubjectKind, OptActionKind, ResourceKind} from "../domain/Enum";
import {TenantResp} from "../domain/Tenant";
import {AppIdentResp} from "../domain/App";
import {Page} from "../domain/Basic";

const iamModuleName: string = 'iam'

let _identOptInfo: IdentOptInfo | null = null

let _appId: string = ''

export function init(appId: string): void {
    _appId = appId
}

const auth = {
    fetchLoginInfo(): IdentOptInfo | null {
        return _identOptInfo
    },
    createLoginInfo(identOptInfo: string): void {
        if (identOptInfo === null || identOptInfo.trim() === '') {
            auth.setLoginInfo(null)
        } else {
            auth.setLoginInfo(JSON.parse(identOptInfo))
        }
    },
    setLoginInfo(identOptInfo: IdentOptInfo | null): void {
        _identOptInfo = identOptInfo
        if (_identOptInfo === null) {
            request.setToken('')
        } else {
            request.setToken(_identOptInfo.token)
        }
    },
}
const account = {

    login(userName: string, password: string): Promise<IdentOptInfo> {
        request.setToken('')
        return request.req<IdentOptInfo>('login', 'http://' + iamModuleName + '/common/login', OptActionKind.CREATE, {
            ak: userName,
            sk: password,
            relAppCode: _appId
        })
            .then(identOptInfo => {
                _identOptInfo = identOptInfo
                request.setToken(identOptInfo.token)
                return identOptInfo
            })
    },
    logout(): Promise<void> {
        return request.req<void>('logout', 'http://' + iamModuleName + '/common/logout', OptActionKind.DELETE)
            .then(() => {
                request.setToken('')
            })
    },
    registerAccount(accountName: string): Promise<number> {
        return request.req<number>('registerAccount', 'http://' + iamModuleName + '/console/tenant/account', OptActionKind.CREATE, {
            name: accountName
        })
    },
    createAccountIdent(accountId: number, identKind: AccountIdentKind, ak: string, sk: string): Promise<number> {
        return request.req<number>('addAccountIdent', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/ident', OptActionKind.CREATE, {
            kind: identKind,
            ak: ak,
            sk: sk
        })
    },
    bindApp(accountId: number, appId: number): Promise<number> {
        return request.req<number>('bindApp', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/app/' + appId, OptActionKind.CREATE)
    },
    bindRole(accountId: number, roleId: number): Promise<number> {
        return request.req<number>('bindRole', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/role/' + roleId, OptActionKind.CREATE)
    }

}

const resource = {

    fetchMenu(): Promise<any[]> {
        return request.req('fetchMenu', 'menu://' + iamModuleName + '/common/resource?kind=menu', OptActionKind.FETCH)
    },
    fetchEle(): Promise<any[]> {
        return request.req('fetchEle', 'element://' + iamModuleName + '/common/resource?kind=element', OptActionKind.FETCH)
    },
    createResourceSubject(code: string, kind: ResourceKind, name: string, uri: string, ak: string, sk: string): Promise<number> {
        return request.req<number>('createResourceSubject', 'http://' + iamModuleName + '/console/app/resource/subject', OptActionKind.CREATE, {
            codePostfix: code,
            kind: kind,
            name: name,
            uri: uri,
            ak: ak,
            sk: sk
        })
    },
    createResource(name: string, pathAndQuery: string, resourceSubjectId: number): Promise<number> {
        return request.req<number>('createResource', 'http://' + iamModuleName + '/console/app/resource', OptActionKind.CREATE, {
            name: name,
            pathAndQuery: pathAndQuery,
            relResourceSubjectId: resourceSubjectId
        })
    },

}

const authPolicy = {
    createAuthPolicy(subjectKind: AuthSubjectKind, subjectId: number, resourceId: number): Promise<void> {
        return request.req<void>('createAuthPolicy', 'http://' + iamModuleName + '/console/app/authpolicy', OptActionKind.CREATE, {
            relSubjectKind: subjectKind,
            relSubjectIds: subjectId,
            subjectOperator: "EQ",
            relResourceId: resourceId,
            resultKind: "ACCEPT"
        })
    },
}

const tenant = {

    registerTenant(tenantName: string, appName: string, tenantAdminUsername: string, tenantAdminPassword: string): Promise<IdentOptInfo> {
        return request.req<IdentOptInfo>('createTenant', 'http://' + iamModuleName + '/common/tenant', OptActionKind.CREATE, {
            tenantName: tenantName,
            appName: appName,
            accountUserName: tenantAdminUsername,
            accountPassword: tenantAdminPassword,
        })
    },
    fetchTenant(): Promise<TenantResp> {
        return request.req<TenantResp>('fetchTenant', 'http://' + iamModuleName + '/console/tenant/tenant', OptActionKind.FETCH)
    }

}

const app = {

    createApp(appName: string): Promise<number> {
        return request.req<number>('createApp', 'http://' + iamModuleName + '/console/tenant/app', OptActionKind.CREATE, {
            name: appName
        })
    },
    fetchPublicKey(): Promise<string> {
        return request.req<string>('fetchPublicKey', 'http://' + iamModuleName + '/console/app/app/publicKey', OptActionKind.FETCH)
    },
    listAppIdents(): Promise<Page<AppIdentResp>> {
        return request.req<Page<AppIdentResp>>('listAppIdents', 'http://' + iamModuleName + '/console/app/app/ident', OptActionKind.FETCH)
    },
    fetchAppIdentSk(identId: number): Promise<string> {
        return request.req<string>('fetchAppIdentSk', 'http://' + iamModuleName + '/console/app/app/ident/' + identId + '/sk', OptActionKind.FETCH)
    },

}

const role = {

    createRoleDef(roleDefCode: string, roleDefName: string): Promise<number> {
        return request.req<number>('createRoleDef', 'http://' + iamModuleName + '/console/app/role/def', OptActionKind.CREATE, {
            code: roleDefCode,
            name: roleDefName
        })
    },

    createRole(relRoleDefId: number): Promise<number> {
        return request.req<number>('createRole', 'http://' + iamModuleName + '/console/app/role', OptActionKind.CREATE, {
            relRoleDefId: relRoleDefId
        })
    }

}

export const iamSDK = {
    auth: {
        fetch: auth.fetchLoginInfo,
        create: auth.createLoginInfo,
        set: auth.setLoginInfo
    },
    account: {
        login: account.login,
        logout: account.logout,
        register: account.registerAccount,
        bindApp: account.bindApp,
        bindRole: account.bindRole,
        ident: {
            create: account.createAccountIdent
        }
    },
    resource: {
        create: resource.createResource,
        subject: {
            create: resource.createResourceSubject
        },
        menu: {
            fetch: resource.fetchMenu
        },
        ele: {
            fetch: resource.fetchEle
        },
    },
    tenant: {
        register: tenant.registerTenant,
        fetch: tenant.fetchTenant
    },
    app: {
        create: app.createApp,
        ident: {
            list: app.listAppIdents,
            fetchSk: app.fetchAppIdentSk,
        },
        key: {
            fetchPublicKey: app.fetchPublicKey
        }
    },
    role: {
        createRoleDef: role.createRoleDef,
        createRole: role.createRole
    },
    policy: {
        create: authPolicy.createAuthPolicy
    }
}
