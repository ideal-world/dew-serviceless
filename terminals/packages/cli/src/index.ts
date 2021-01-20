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
import * as gitHelper from './util/GitHelper'
import chalk from "chalk";
import clear from "clear";
import figlet from "figlet";
import * as fileHelper from './util/FileHelper';
import fsPath from "path";
import {DewSDK} from "@idealworld/sdk";
import pack from "../package.json";

const TEMPLATE_SIMPLE_GIT_ADDR = 'https://github.com/ideal-world/dew-serviceless-template-simple.git'

const GATEWAY_SERVER_URL = "https://gateway.serviceless.org";
const SDK_VERSION = pack.version;

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
            DewSDK.setting.serverUrl(val)
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
        DewSDK.setting.appId(answers.tenantAdminAppId)
        await DewSDK.iam.account.login(answers.tenantAdminUsername, answers.tenantAdminPassword)
        let tenantName = (await DewSDK.iam.tenant.fetch()).name
        confirmMessage = '即将在租户 [' + tenantName + '] 中创建应用 [' + answers.appName + ']'
    } else {
        DewSDK.setting.appId('')
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
    let appId
    if (answers.createTenant) {
        let identOptInfo = await DewSDK.iam.tenant.register(answers.tenantName, answers.appName, answers.tenantAdminUsername, answers.tenantAdminPassword)
        DewSDK.iam.auth.set(identOptInfo)
        appId = identOptInfo.appCode
    } else {
        appId = await DewSDK.iam.app.create(answers.appName)
    }
    let identAKInfo = (await DewSDK.iam.app.ident.list()).objects[0]
    let identSk = await DewSDK.iam.app.ident.fetchSk(identAKInfo.id)

    let path = fsPath.resolve(fileHelper.pwd(), answers.appName)
    console.log(chalk.yellow('正在创建模板到 [' + path + ']'))
    await gitHelper.clone(TEMPLATE_SIMPLE_GIT_ADDR, path, 1)
    let packagePath = fsPath.resolve(path, 'package.json')
    console.log(chalk.yellow('正在添加基本信息到 [' + packagePath + ']'))
    let packageJsonFile = JSON.parse(fileHelper.readFile(packagePath))
    if (!packageJsonFile.hasOwnProperty('dependencies')) {
        packageJsonFile['dependencies'] = {}
    }
    if (!packageJsonFile.hasOwnProperty('devDependencies')) {
        packageJsonFile['devDependencies'] = {}
    }
    packageJsonFile['dependencies']['@idealworld/sdk'] = SDK_VERSION
    packageJsonFile['devDependencies']['@idealworld/plugin-gulp'] = SDK_VERSION
    packageJsonFile['dew'] = {
        "serverUrl": answers.serverUrl,
        "appId": appId
    }
    fileHelper.writeFile(packagePath, JSON.stringify(packageJsonFile, null, 2))
    let dewCrtPath = fsPath.resolve(path, 'dew.crt')
    console.log(chalk.yellow('正在添加认证信息到 [' + dewCrtPath + ']'))
    fileHelper.writeFile(dewCrtPath, JSON.stringify({
        "ak": identAKInfo.ak,
        "sk": identSk
    }, null, 2))
    let gitignorePath = fsPath.resolve(path, '.gitignore')
    if (fileHelper.exists(gitignorePath)) {
        if (fileHelper.readFile(gitignorePath).indexOf('dew.crt') === -1) {
            fileHelper.append(gitignorePath, '\ndew.crt')
        }
    } else {
        fileHelper.writeFile(gitignorePath, '\ndew.crt')
    }
    console.log(chalk.green.bold.bgWhite('应用创建完成，请到 [' + path + '] 中查看。\r\n' +
        '===================\r\n' +
        '应用Id(AppId): ' + appId + '\r\n' +
        '应用管理员: ' + answers.tenantAdminUsername + '\r\n' +
        '管理员密码: ' + answers.tenantAdminPassword + '\r\n' +
        '[dew.crt]存放了密钥数据，请妥善保存！\r\n' +
        '==================='))
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
