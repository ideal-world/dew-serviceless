#!/usr/bin/env node
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

import inquirer from 'inquirer'
import {IdentOptInfo} from './domain/IdentOptInfo'
import * as request from './util/Request'
import * as gitHelper from './util/GitHelper'
import chalk from "chalk";
import clear from "clear";
import figlet from "figlet";
import * as fileHelper from './util/FileHelper';
import fsPath from "path";

const TEMPLATE_SIMPLE_GIT_ADDR = 'https://github.com/ideal-world/dew-serviceless-template-simple.git'

// TODO
const GATEWAY_SERVER_URL = "http://127.0.0.1:9000";
const IAM_URL = "http://127.0.0.1:8081";

const createAppWithNewTenantSteps: any[] = [
    {
        type: 'input',
        message: '请输入租户名称:',
        name: 'tenantName',
        when: function (answers: any) {
            return answers.kind === '创建应用'
                && answers.createTenant
        },
        validate: function (val) {
            if (!val.trim()) {
                return '请输入租户名称'
            }
            return true
        },
        filter: function (val: string) {
            return val.trim()
        }
    }, {
        type: 'input',
        message: '请输入管理员用户名:',
        name: 'tenantAdminUsername',
        default: 'admin',
        when: function (answers: any) {
            return answers.kind === '创建应用'
                && answers.createTenant
        },
        validate: function (val) {
            if (!val.trim()) {
                return '请输入管理员用户名'
            }
            return true
        },
        filter: function (val: string) {
            return val.trim()
        }
    }, {
        type: 'input',
        message: '请输入管理员密码:',
        name: 'tenantAdminPassword',
        default: 'Dew93Xi2@s!',
        when: function (answers: any) {
            return answers.kind === '创建应用'
                && answers.createTenant
        },
        validate: function (val) {
            if (!val.trim()) {
                return '请输入管理员密码'
            }
            return true
        },
    },
]

const createAppWithExistTenantSteps: any[] = [
    {
        type: 'input',
        message: '请输入要创建应用的租户管理员用户名:',
        name: 'tenantAdminUsername',
        when: function (answers: any) {
            return answers.kind === '创建应用'
                && !answers.createTenant
        },
        validate: function (val) {
            if (!val.trim()) {
                return '请输入要创建应用的租户管理员用户名'
            }
            return true
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
                && !answers.createTenant
        },
        validate: function (val) {
            if (!val.trim()) {
                return '请输入要创建应用的租户管理员密码'
            }
            return true
        },
    }, {
        type: 'input',
        message: '请输入要创建应用的租户管理员的应用Id:',
        name: 'tenantAdminAppId',
        when: function (answers: any) {
            return answers.kind === '创建应用'
                && !answers.createTenant
        },
        validate: function (val) {
            if (!val.trim()) {
                return '请输入要创建应用的租户管理员的应用Id'
            }
            if (isNaN(parseInt(val.trim()))) {
                return '应用Id为数值类型'
            }
            return true
        },
        filter: function (val: string) {
            return val.trim()
        }
    },
]

const createAppSteps: any[] = [
    {
        type: 'input',
        message: '请输入应用名:',
        name: 'appName',
        when: function (answers: any) {
            return answers.kind === '创建应用'
        },
        validate: function (val) {
            if (!val.trim()) {
                return '请输入应用名称'
            }
            let path = fsPath.resolve(fileHelper.pwd(), val.trim())
            if (fileHelper.exists(path)) {
                return '该应用名称在当前路径下已存在，请更换应用名称或路径'
            }
            return true
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
        }
    }
].concat(createAppWithExistTenantSteps, createAppWithNewTenantSteps)

const allSteps: any[] = [
    {
        type: 'input',
        message: '请输入服务地址:',
        name: 'serverUrl',
        default: GATEWAY_SERVER_URL,
        validate: function (val) {
            if (!val.trim()) {
                return '请输入服务地址'
            }
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

async function createApp(answers: any) {
    let confirmMessage
    if (!answers.createTenant) {
        let identOptInfo = await request.req<IdentOptInfo>('login', IAM_URL + '/common/login', 'CREATE', {
            ak: answers.tenantAdminUsername,
            sk: answers.tenantAdminPassword,
            relAppId: answers.tenantAdminAppId
        })
        request.setToken(identOptInfo.token)
        let tenantName = (await request.req<any>('getTenant', IAM_URL + '/console/tenant/tenant', 'FETCH')).name
        confirmMessage = '即将在租户 [' + tenantName + '] 中创建应用 [' + answers.appName + ']'
    } else {
        confirmMessage = '即将创建应用 [' + answers.appName + ']'
    }
    let confirmAnswer = await inquirer.prompt([{
        type: 'confirm',
        message: confirmMessage + '，是否确认？',
        name: "confirm"
    }])
    if (!confirmAnswer) {
        return;
    }
    if (answers.createTenant) {
        let identOptInfo = await request.req<IdentOptInfo>('createTenant', IAM_URL + '/common/tenant', 'CREATE', {
            tenantName: answers.tenantName,
            appName: answers.appName,
            accountUserName: answers.tenantAdminUsername,
            accountPassword: answers.tenantAdminPassword,
        })
        request.setToken(identOptInfo.token)
    } else {
        await request.req<any>('createApp', IAM_URL + '/console/tenant/app', 'CREATE', {
            name: answers.appName,
        })
    }
    let publicKey = await request.req<string>('getAppPublicKey', IAM_URL + '/console/app/app/publicKey', 'FETCH')
    let path = fsPath.resolve(fileHelper.pwd(), answers.appName)
    console.log(chalk.green('正在创建模板到 [' + path + ']'))
    // TODO package.json 内容替换
    await gitHelper.clone(TEMPLATE_SIMPLE_GIT_ADDR, path, 1)
    fileHelper.writeFile(path + '/Dew.key', publicKey)
    console.log(chalk.green.bold.bgWhite('应用创建完成，请到 [' + path + '] 中查看。'))
}

export async function run() {
    clear()
    console.log(chalk.green(figlet.textSync('Dew CLI', {horizontalLayout: 'full'})))
    let answers = await inquirer.prompt(allSteps)
    try {
        switch (answers.kind) {
            case '创建应用':
                await createApp(answers)
                break;
        }
    } catch (e) {
        console.log(chalk.red.bold('执行错误，请检查修正。'))
    }
}
