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
import {JsonMap} from "./domain/Basic";
import * as fileHelper from './util/FileHelper';

const TEMPLATE_SIMPLE_GIT_ADDR = 'https://github.com/ideal-world/dew-serviceless-template-simple.git'
let _answers: JsonMap<any> = {}

const createAppWithNewTenantSteps: any[] = [
    {
        type: 'input',
        message: '请输入租户名称:',
        name: 'tenantName',
        default: _answers.appName,
        when: function (answers: any) {
            return answers.kind === '创建应用'
                && !answers.createTenant
        },
        validate: function (val) {
            _answers['tenantName'] = val.trim()
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
                && !answers.createTenant
        },
        validate: function (val) {
            _answers['tenantAdminUsername'] = val.trim()
            return true
        },
        filter: function (val: string) {
            return val.trim()
        }
    }, {
        type: 'password',
        message: '请输入管理员密码:',
        name: 'tenantAdminPassword',
        default: 'Dew93Xi2@s!',
        when: function (answers: any) {
            return answers.kind === '创建应用'
                && !answers.createTenant
        },
        validate: function (val) {
            _answers['tenantAdminPassword'] = val
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
                && answers.createTenant
        },
        validate: function (val) {
            _answers['tenantAdminUsername'] = val.trim()
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
                && answers.createTenant
        },
        validate: function (val) {
            _answers['tenantAdminPassword'] = val
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
            _answers['tenantAdminAppId'] = val.trim()
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
            _answers['appName'] = val.trim()
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
        },
        validate: function (val) {
            _answers['createTenant'] = val.trim()
            return true
        },
    }
].concat(createAppWithExistTenantSteps, createAppWithNewTenantSteps)

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
        ],
        validate: function (val) {
            _answers['kind'] = val.trim()
            return true
        },
    }
].concat(createAppSteps)

async function createApp() {
    let confirmMessage
    if (!_answers.createTenant) {
        let identOptInfo = await request.req<IdentOptInfo>('login', 'http://iam/common/login', 'CREATE', {
            ak: _answers.tenantAdminUsername,
            sk: _answers.tenantAdminPassword,
            relAppId: _answers.tenantAdminAppId
        })
        request.setToken(identOptInfo.token)
        let tenantName = (await request.req<any>('getTenant', 'http://iam/console/tenant/tenant', 'FETCH')).name
        confirmMessage = '即将在租户 [' + tenantName + '] 中创建应用 [' + _answers.appName + ']'
    } else {
        confirmMessage = '即将创建应用 [' + _answers.appName + ']'
    }
    let confirmAnswer = await inquirer.prompt([{
        type: 'confirm',
        message: confirmMessage + '，是否确认？',
        name: "confirm"
    }])
    if (!confirmAnswer) {
        return;
    }
    if (_answers.createTenant) {
        let identOptInfo = await request.req<IdentOptInfo>('createTenant', 'http://iam/common/tenant', 'CREATE', {
            tenantName: _answers.tenantName,
            appName: _answers.appName,
            accountUserName: _answers.tenantAdminUsername,
            accountPassword: _answers.tenantAdminPassword,
        })
        request.setToken(identOptInfo.token)
    } else {
        await request.req<any>('createApp', 'http://iam/console/tenant/app', 'CREATE', {
            name: _answers.appName,
        })
    }
    let publicKey = await request.req<string>('getAppPublicKey', 'http://iam/console/app/app/publicKey', 'FETCH')
    let path = fileHelper.pwd() + '/' + _answers.appName
    if (fileHelper.exists(path)) {
        throw 'The path [' + path + '] already exists.'
    }
    console.log('Create template project to %s', path)
    // TODO package.json 内容替换
    await gitHelper.clone(TEMPLATE_SIMPLE_GIT_ADDR, path, 1)
    fileHelper.writeFile(path + '/Dew.key', publicKey)
    console.log('Template project created.')
}

export async function run(){
    clear()
    console.log(chalk.yellow(figlet.textSync('Dew CLI', {horizontalLayout: 'full'})))
    await inquirer.prompt(allSteps)
    switch (_answers.kind) {
        case '创建应用':
            await createApp()
            break;
    }
}
