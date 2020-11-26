#!/usr/bin/env node

import inquirer from 'inquirer'
import {IdentOptInfo} from './domain/IdentOptInfo'
import * as request from './util/Request'

const createAppSteps: any[] = [
    {
        type: 'input',
        message: '请输入应用名:',
        name: 'appName',
        when: function (answers: any) {
            return answers.kind === '创建应用'
        },
        filter: function (val: string) {
            return val.trim()
        }
    }, {
        type: 'confirm',
        message: "是否新建租户？",
        name: "createTenant",
        when: function (answers: any) {
            return answers.kind === '创建应用'
        },
    }, {
        type: 'input',
        message: '请输入要创建应用的租户名称:',
        name: 'tenantName',
        when: function (answers: any) {
            return answers.kind === '创建应用'
        },
        filter: function (val: string) {
            return val.trim()
        }
    }, {
        type: 'input',
        message: '请输入要创建应用的租户管理员用户名:',
        name: 'tenantAdminUsername',
        when: function (answers: any) {
            return answers.kind === '创建应用'
        },
        filter: function (val: string) {
            return val.trim()
        }
    }, {
        type: 'password',
        message: '请输入要创建应用的租户管理员密码:',
        name: 'tenantAdminPassword',
        when: function (answers: any) {
            return answers.kind === '创建应用'
        },
    }, {
        type: 'appId',
        message: '请输入要创建应用的租户管理员的应用Id:',
        name: 'tenantAdminAppId',
        when: function (answers: any) {
            return answers.kind === '创建应用'
                && !answers.createTenant
        },
        filter: function (val: string) {
            return val.trim()
        }
    },
]

const allSteps: any[] = [
    {
        type: 'input',
        message: '请输入服务地址:',
        name: 'serverUrl',
        validate: function (val) {
            request.setServerUrl(val)
            return true
        }
    }, {
        type: 'list',
        message: '请选择操作:',
        name: 'kind',
        choices: [
            "创建应用",
        ]
    }
].concat(createAppSteps)

inquirer
    .prompt(allSteps)
    .then(async answers => {
        let identOptInfo = await request.req<IdentOptInfo>('login', 'http://iam/common/login', 'CREATE', {
            ak: answers.tenantAdminUsername,
            sk: answers.tenantAdminPassword,
            relAppId: answers.tenantAdminAppId
        })
        request.setToken(identOptInfo.token)
        let tenantResp = await request.req<any>('getTenant', 'http://iam/console/tenant/tenant', 'FETCH')
        let confirm = answers.createTenant
            ? '即将创建应用 [' + answers.appName + ']'
            : '即将在租户 [' + tenantResp.name + '] 中创建应用 [' + answers.appName + ']'
        inquirer
            .prompt([{
                type: 'confirm',
                message: confirm + '，是否确认？',
                name: "confirm"
            }])
            .then(async answer => {
                if (answers.createTenant) {
                    identOptInfo = await request.req<any>('createTenant', 'http://iam/common/tenant', 'CREATE', {
                        tenantName: answers.tenantName,
                        appName: answers.appName,
                        accountUserName: answers.tenantAdminUsername,
                        accountPassword: answers.tenantAdminPassword,
                    })
                } else {
                    await request.req<any>('createApp', 'http://iam/console/tenant/app', 'CREATE', {
                        name: answers.appName,
                    })
                }
            })
            .catch(error => {
                throw error
            })
    })
    .catch(error => {
        throw error
    })
