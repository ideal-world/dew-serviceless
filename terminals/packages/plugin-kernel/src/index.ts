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
const browserify = require("browserify")

const INIT_FUN_NAME = '_'

const config = JSON.parse(fs.readFileSync('./package.json'))['dew']
const serverUrl: string = config.serverUrl
const appId: number = config.appId
const ak: string = config.ak
const sk: string = config.sk

export async function dewBuild(relativeBasePath: string, testToDist?: string): Promise<void> {
    let basePath = path.join(process.cwd(), relativeBasePath)
    fs.readdirSync(basePath).forEach(fileName => {
        let filePath = path.join(basePath, fileName)
        generateJVMFile(basePath, filePath)
        fs.writeFileSync(filePath, replaceImport(fs.readFileSync(filePath).toString('utf8'), true))
    })
    return new Promise((resolve, reject) => {
        browserify({
            entries: path.join(basePath, "JVM.js"),
            standalone: "JVM"
        })
            .plugin('tinyify', {flat: true})
            .bundle(async (err, buf) => {
                if (err) throw err
                let content = buf.toString()
                if (!testToDist) {
                    await sendTask(content)
                } else {
                    fs.writeFileSync(testToDist, content)
                }
                deleteJVMFile(basePath)
                fs.readdirSync(basePath).forEach(fileName => {
                    let filePath = path.join(basePath, fileName)
                    let content = replaceImport(fs.readFileSync(filePath).toString('utf8'), false)
                    content = rewriteAction(content, fileName.substring(0, fileName.lastIndexOf('.')))
                    content = initDewSDK(content)
                    fs.writeFileSync(filePath, content)
                })
                resolve()
            })
    });
}

function generateJVMFile(basePath: string, filePath: string) {
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
exports.` + fileName + ` = ` + fileName + `
` + fileName + `._ && ` + fileName + `._()
`
    );
}

function replaceImport(fileContent: string, toJVM: boolean): string {
    if (toJVM) {
        if (fileContent.indexOf('require("@idealworld/sdk")') !== -1) {
            return fileContent.replace(/@idealworld\/sdk/g, '@idealworld/sdk/dist/jvm')
        }
    }
    return fileContent.replace(/@idealworld\/sdk\/dist\/jvm/g, '@idealworld/sdk')
}

function sendTask(fileContent: string): Promise<void> {
    DewSDK.init(serverUrl, appId)
    DewSDK.setting.aksk(ak, sk)
    return DewSDK.task.initTasks(fileContent)
}

function deleteJVMFile(basePath: string) {
    let file = path.join(basePath, 'JVM.js')
    if (fs.existsSync(file)) {
        fs.unlinkSync(file)
    }
}

function initDewSDK(fileContent: string): string {
    return 'const {DewSDK} = require("@idealworld/sdk");\n' +
        'DewSDK.init("' + serverUrl + '", ' + appId + ');\n' +
        fileContent
}

function rewriteAction(fileContent: string, fileName: string): string {
    let moduleName = fileName.replace(/\./g, '_')
    let ast = JSASTHelper.parse(fileContent)
    return replaceFunction(fileContent, ast, moduleName)
}

function replaceFunction(fileContent: string, ast: Node, moduleName: string): string {
    JSASTHelper.findAstOffsetByType(ast, ['FunctionDeclaration']).reverse()
        .forEach(node => {
            // @ts-ignore
            let funName = node.ast.id.name
            if (funName === INIT_FUN_NAME || fileContent.indexOf('exports.' + funName + ' = ' + funName + ';') === -1) {
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
