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

import {IdentOptInfo} from "../domain/IdentOptInfo";
import {DewRequest} from "../util/DewRequest";
import {AccountIdentKind, AuthSubjectKind, OptActionKind, ResourceKind} from "../domain/Enum";
import {TenantResp} from "../domain/Tenant";
import {AppIdentResp, AppResp} from "../domain/App";
import {Page} from "../domain/Basic";
import {ResourceSubjectResp} from "../domain/Resource";

const iamModuleName: string = 'iam.http.iam'

export class IAMSDK {

    constructor(request: DewRequest) {
        this.request = request
    }

    private readonly request: DewRequest

    private identOptInfo: IdentOptInfo | null = null

    private _tenant = () => {
        const that = this

        function registerTenant(tenantName: string, appName: string, tenantAdminUsername: string, tenantAdminPassword: string): Promise<IdentOptInfo> {
            return that.request.req<IdentOptInfo>('createTenant', 'http://' + iamModuleName + '/common/tenant', OptActionKind.CREATE, {
                tenantName: tenantName,
                appName: appName,
                accountUserName: tenantAdminUsername,
                accountPassword: tenantAdminPassword,
            })
        }

        function fetchTenant(): Promise<TenantResp> {
            return that.request.req<TenantResp>('fetchTenant', 'http://' + iamModuleName + '/console/tenant/tenant', OptActionKind.FETCH)
        }

        return {
            register: registerTenant,
            fetch: fetchTenant
        }
    }

    private _app = () => {
        const that = this

        function registerApp(appName: string): Promise<IdentOptInfo> {
            return that.request.req<IdentOptInfo>('registerApp', 'http://' + iamModuleName + '/common/app', OptActionKind.CREATE, {
                appName: appName,
            })
        }

        function createApp(appName: string): Promise<number> {
            return that.request.req<number>('createApp', 'http://' + iamModuleName + '/console/tenant/app', OptActionKind.CREATE, {
                name: appName
            })
        }

        function fetchApp(appId: number): Promise<AppResp> {
            return that.request.req<AppResp>('fetchApp', 'http://' + iamModuleName + '/console/tenant/app/' + appId, OptActionKind.FETCH)
        }

        function fetchPublicKey(): Promise<string> {
            return that.request.req<string>('fetchPublicKey', 'http://' + iamModuleName + '/console/app/app/publicKey', OptActionKind.FETCH)
        }

        function listAppIdents(): Promise<Page<AppIdentResp>> {
            return that.request.req<Page<AppIdentResp>>('listAppIdents', 'http://' + iamModuleName + '/console/app/app/ident', OptActionKind.FETCH)
        }

        function fetchAppIdentSk(identId: number): Promise<string> {
            return that.request.req<string>('fetchAppIdentSk', 'http://' + iamModuleName + '/console/app/app/ident/' + identId + '/sk', OptActionKind.FETCH)
        }

        return {
            register: registerApp,
            create: createApp,
            fetch: fetchApp,
            ident: {
                list: listAppIdents,
                fetchSk: fetchAppIdentSk,
            },
            key: {
                fetchPublicKey: fetchPublicKey
            }
        }
    }

    private _account = () => {
        const that = this

        function registerAccount(accountName: string): Promise<number> {
            return that.request.req<number>('registerAccount', 'http://' + iamModuleName + '/console/tenant/account', OptActionKind.CREATE, {
                name: accountName
            })
        }

        function createAccountIdent(accountId: number, identKind: AccountIdentKind, ak: string, sk: string): Promise<number> {
            return that.request.req<number>('addAccountIdent', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/ident', OptActionKind.CREATE, {
                kind: identKind,
                ak: ak,
                sk: sk
            })
        }

        function bindApp(accountId: number, appId: number): Promise<number> {
            return that.request.req<number>('bindApp', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/app/' + appId, OptActionKind.CREATE)
        }

        function bindRole(accountId: number, roleId: number): Promise<number> {
            return that.request.req<number>('bindRole', 'http://' + iamModuleName + '/console/tenant/account/' + accountId + '/role/' + roleId, OptActionKind.CREATE)
        }

        return {
            register: registerAccount,
            bindApp: bindApp,
            bindRole: bindRole,
            ident: {
                create: createAccountIdent
            }
        }
    }

    private _role = () => {
        const that = this


        function createRoleDef(roleDefCode: string, roleDefName: string): Promise<number> {
            return that.request.req<number>('createRoleDef', 'http://' + iamModuleName + '/console/app/role/def', OptActionKind.CREATE, {
                code: roleDefCode,
                name: roleDefName
            })
        }

        function createRole(relRoleDefId: number): Promise<number> {
            return that.request.req<number>('createRole', 'http://' + iamModuleName + '/console/app/role', OptActionKind.CREATE, {
                relRoleDefId: relRoleDefId
            })
        }

        return {
            createRoleDef: createRoleDef,
            createRole: createRole
        }
    }

    private _resource = () => {
        const that = this

        function fetchMenu(): Promise<any[]> {
            return that.request.req('fetchMenu', 'menu://' + iamModuleName + '/common/resource?kind=menu', OptActionKind.FETCH)
        }

        function fetchEle(): Promise<any[]> {
            return that.request.req('fetchEle', 'element://' + iamModuleName + '/common/resource?kind=element', OptActionKind.FETCH)
        }

        function createResourceSubject(code: string, kind: ResourceKind, name: string, uri: string, ak: string, sk: string): Promise<number> {
            return that.request.req<number>('createResourceSubject', 'http://' + iamModuleName + '/console/app/resource/subject', OptActionKind.CREATE, {
                codePostfix: code,
                kind: kind,
                name: name,
                uri: uri,
                ak: ak,
                sk: sk
            })
        }

        function fetchResourceSubjects(codeLike?: string, nameLike?: string, kind?: string): Promise<ResourceSubjectResp[]> {
            return that.request.req('fetchResourceSubjects', 'http://' + iamModuleName + '/console/app/resource/subject?code='
                + (codeLike ? codeLike : '') + '&name=' + (nameLike ? nameLike : '') + '&kind=' + (kind ? kind : ''), OptActionKind.FETCH)
        }

        function createResource(name: string, pathAndQuery: string, resourceSubjectId: number): Promise<number> {
            return that.request.req<number>('createResource', 'http://' + iamModuleName + '/console/app/resource', OptActionKind.CREATE, {
                name: name,
                pathAndQuery: pathAndQuery,
                relResourceSubjectId: resourceSubjectId
            })
        }

        return {
            create: createResource,
            subject: {
                create: createResourceSubject,
                fetch: fetchResourceSubjects
            },
            menu: {
                fetch: fetchMenu
            },
            ele: {
                fetch: fetchEle
            },
        }
    }

    private _policy = () => {
        const that = this

        function createAuthPolicy(subjectKind: AuthSubjectKind, subjectId: number, resourceId: number): Promise<void> {
            return that.request.req<void>('createAuthPolicy', 'http://' + iamModuleName + '/console/app/authpolicy', OptActionKind.CREATE, {
                relSubjectKind: subjectKind,
                relSubjectIds: subjectId,
                subjectOperator: "EQ",
                relResourceId: resourceId,
                resultKind: "ACCEPT"
            })
        }

        return {
            create: createAuthPolicy
        }
    }

    private _auth = () => {
        const that = this

        function fetchLoginInfo(): IdentOptInfo | null {
            return that.identOptInfo
        }

        function createLoginInfo(identOptInfo: string): void {
            if (identOptInfo === null || identOptInfo.trim() === '') {
                setLoginInfo(null)
            } else {
                setLoginInfo(JSON.parse(identOptInfo))
            }
        }

        function setLoginInfo(identOptInfo: IdentOptInfo | null): void {
            that.identOptInfo = identOptInfo
            if (that.identOptInfo === null) {
                that.request.setToken('')
            } else {
                that.request.setToken(that.identOptInfo.token)
            }
            that.request.setAkSk('', '')
        }

        function login(userName: string, password: string): Promise<IdentOptInfo> {
            that.request.setToken('')
            return that.request.req<IdentOptInfo>('login', 'http://' + iamModuleName + '/common/login', OptActionKind.CREATE, {
                ak: userName,
                sk: password,
                relAppCode: that.request.getAppId()
            })
                .then(identOptInfo => {
                    that.identOptInfo = identOptInfo
                    that.request.setToken(identOptInfo.token)
                    return identOptInfo
                })
        }

        function logout(): Promise<void> {
            return that.request.req<void>('logout', 'http://' + iamModuleName + '/common/logout', OptActionKind.DELETE)
                .then(() => {
                    that.request.setToken('')
                })
        }

        return {
            fetch: fetchLoginInfo,
            create: createLoginInfo,
            set: setLoginInfo,
            login: login,
            logout: logout,
        }
    }

    tenant = this._tenant()
    app = this._app()
    account = this._account()
    role = this._role()
    resource = this._resource()
    policy = this._policy()
    auth = this._auth()

}

