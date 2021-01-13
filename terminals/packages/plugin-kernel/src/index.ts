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

import {Node} from "acorn";
import * as JSASTHelper from "./util/JSASTHelper";
import {DewSDK} from "@idealworld/sdk";

const fs = require('fs')
const path = require('path')
const config = JSON.parse(fs.readFileSync('./package.json'))['dew']
const serverUrl: string = config.serverUrl
const appId: number = config.appId
const ak: string = config.ak
const sk: string = config.sk

export function replaceImport(fileContent: string, toJVM: boolean): string {
    if (toJVM) {
        if (fileContent.indexOf('require("@idealworld/sdk")') !== -1) {
            return fileContent.replace(/@idealworld\/sdk/g, '@idealworld/sdk/dist/jvm')
        }
    }
    return fileContent.replace(/@idealworld\/sdk\/dist\/jvm/g, '@idealworld/sdk')
}

export function generateJVMFile(basePath: string, filePath: string) {
    let jvmPath = path.join(basePath, 'JVM.js')
    if (!fs.existsSync(jvmPath)) {
        fs.writeFileSync(jvmPath, `"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const sdk = require("@idealworld/sdk/dist/jvm");
exports.DewSDK = sdk.DewSDK`)
    }
    let rawFileName = filePath.substring(basePath.length + 1, filePath.lastIndexOf('.'));
    let fileName = rawFileName.replace(/\./g, '_')
    fs.appendFileSync(jvmPath, `
const ` + fileName + ` = require("./` + rawFileName + `");
exports.` + fileName + ` = ` + fileName);
}

export function deleteJVMFile(basePath: string) {
    let file = path.join(basePath, 'JVM.js')
    if (fs.existsSync(file)) {
        fs.unlinkSync(file)
    }
}

export function sendTask(fileContent: string): Promise<void> {
    DewSDK.init(serverUrl, appId)
    DewSDK.setting.aksk(ak, sk)
    return DewSDK.task.initTasks(fileContent)
}

export function initDewSDK(fileContent: string): string {
    return fileContent + '\n' +
        'const {DewSDK} = require("@idealworld/sdk");\n' +
        'DewSDK.init("' + serverUrl + '", ' + appId + ');'
}

export function rewriteAction(fileContent: string, moduleName: string): string {
    moduleName = moduleName.replace(/\./g, '_')
    let ast = JSASTHelper.parse(fileContent)
    return replaceFunction(fileContent, ast, moduleName)
}

function replaceFunction(fileContent: string, ast: Node, moduleName: string): string {
    JSASTHelper.findAstOffsetByType(ast, ['FunctionDeclaration']).reverse()
        .forEach(node => {
            // @ts-ignore
            let funName = node.ast.id.name
            if (fileContent.indexOf('exports.' + funName + ' = ' + funName + ';') === -1) {
                // 内部方法，删除
                fileContent = fileContent.substring(0, node.start) + fileContent.substring(node.end + 1)
            } else {
                // @ts-ignore
                let funBody = '{\n  return DewSDK.task.execute("' + moduleName + '.' + funName + '", [' + (node.ast.params ? node.ast.params.map(p => p.name).join(', ') : '') + ']);\n}'
                // @ts-ignore
                fileContent = fileContent.substring(0, node.ast.body.start) + funBody + fileContent.substring(node.ast.body.end)
            }
        })
    return fileContent
}
