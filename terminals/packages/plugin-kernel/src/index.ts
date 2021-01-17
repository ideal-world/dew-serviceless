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

import * as JSASTHelper from "./util/JSASTHelper";
import {DewNode} from "./util/JSASTHelper";
import {DewSDK} from "@idealworld/sdk";
import {minify} from "terser";

const fs = require('fs')
const path = require('path')
const browserify = require("browserify")

const config = JSON.parse(fs.readFileSync('./package.json'))['dew']
const crt = JSON.parse(fs.readFileSync('./dew.crt'))
const serverUrl: string = config.serverUrl
const appId: number = config.appId
const ak: string = crt.ak
const sk: string = crt.sk

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
            .bundle(async (err, buf) => {
                if (err) throw err
                let content = buf.toString()
                content = (await minify(content,{
                    ecma:2020
                })).code
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

export function generateJVMFile(basePath: string, filePath: string) {
    let jvmPath = path.join(basePath, 'JVM.js')
    if (!fs.existsSync(jvmPath)) {
        fs.writeFileSync(jvmPath, `"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const sdk = require("@idealworld/sdk/dist/jvm");
exports.DewSDK = sdk.DewSDK;
sdk.DewSDK.init("REPLACE_IT", -1);
sdk.DewSDK.setting.ajax((url, headers, data) => {
  return new Promise((resolve, reject) => {
    try{
      resolve({data:JSON.parse($.req(url,headers,data))})
    }catch(e){
      reject({"message": e.getMessage(),"stack": []})
    }
  })
});
sdk.DewSDK.setting.currentTime(() => {
  return $.currentTime()
});
sdk.DewSDK.setting.signature((text, key) => {
  return $.signature(text, key)
});
`)
    }
    let rawFileName = filePath.substring(basePath.length + 1, filePath.lastIndexOf('.'));
    let fileName = rawFileName.replace(/\./g, '_')
    fs.appendFileSync(jvmPath, `
const ` + fileName + ` = require("./` + rawFileName + `");
exports.` + fileName + ` = ` + fileName + `;
` + fileName + `;
`);
}

export function replaceImport(fileContent: string, toJVM: boolean): string {
    if (toJVM) {
        if (fileContent.indexOf('require("@idealworld/sdk")') !== -1) {
            return fileContent.replace(/@idealworld\/sdk/g, '@idealworld/sdk/dist/jvm')
        }
    }
    return fileContent.replace(/@idealworld\/sdk\/dist\/jvm/g, '@idealworld/sdk')
}

export function sendTask(fileContent: string): Promise<void> {
    DewSDK.init(serverUrl, appId)
    DewSDK.setting.aksk(ak, sk)
    return DewSDK.task.initTasks(fileContent)
}

export function deleteJVMFile(basePath: string) {
    let file = path.join(basePath, 'JVM.js')
    if (fs.existsSync(file)) {
        fs.unlinkSync(file)
    }
}

export function initDewSDK(fileContent: string): string {
    return 'const {DewSDK} = require("@idealworld/sdk");\n' +
        fileContent
}

let postRemoveSegments: string[] = []

export function rewriteAction(fileContent: string, fileName: string): string {
    let moduleName = fileName.replace(/\./g, '_')
    let ast = JSASTHelper.parse(fileContent)
    // @ts-ignore
    ast.body.reverse()
        .forEach(node => {
            switch (node.type) {
                case 'FunctionDeclaration':
                    fileContent = replaceFunction(node, fileContent, moduleName)
                    break
                case 'ExpressionStatement':
                    fileContent = deleteExpression(node, fileContent)
                    break
                default:
                    // 删除, E.g.
                    // const sdk_1 = require("@idealworld/sdk");
                    fileContent = fileContent.substring(0, node.start) + fileContent.substring(node.end + 1)
            }
        })
    postRemoveSegments.forEach(segment => {
        fileContent = fileContent.replace(segment, '')
    })
    return fileContent
}

export function replaceFunction(node: DewNode, fileContent: string, moduleName: string): string {
    // @ts-ignore
    let funName = node.id.name
    if (fileContent.indexOf('exports.' + funName + ' = ' + funName + ';') === -1) {
        // 内部方法，删除
        fileContent = fileContent.substring(0, node.start) + fileContent.substring(node.end + 1)
    } else {
        // @ts-ignore
        let funBody = '{\n  return DewSDK.task.execute("' + moduleName + '.' + funName + '", [' + (node.params ? node.params.map(p => p.name).join(', ') : '') + ']);\n}'
        // @ts-ignore
        fileContent = fileContent.substring(0, node.body.start) + funBody + fileContent.substring(node.body.end)
    }
    return fileContent
}

export function deleteExpression(node: DewNode, fileContent: string): string {
    // @ts-ignore
    if (node.expression.type === 'AssignmentExpression'
        // @ts-ignore
        && node.expression.left.type === 'MemberExpression'
        // @ts-ignore
        && node.expression.left.object
        // @ts-ignore
        && node.expression.left.object.type === 'Identifier'
        // @ts-ignore
        && node.expression.left.object.name === 'exports'
        // @ts-ignore
        && node.expression.left.property
        // @ts-ignore
        && node.expression.left.property.type === 'Identifier'
    ) {
        // @ts-ignore
        let exportFunName = node.expression.left.property.name
        if (fileContent.indexOf('function ' + exportFunName + '(') !== -1) {
            // 忽略 exports.<funxx> , E.g.
            // async function addItem(content) {
            //     ...
            // }
            // exports.addItem = addItem;
            return fileContent
        } else {
            // 删除 E.g.
            // exports.db = sdk_1.DewSDK.reldb.subject("todoDB");
            fileContent = fileContent.substring(0, node.start) + fileContent.substring(node.end + 1)
            // 删除类似 exports.addItem = exports.fetchItems = exports.db = void 0; 中元素
            postRemoveSegments.push('exports.' + exportFunName + ' = ')
            return fileContent
        }

    }
    // @ts-ignore
    if (node.expression.type === 'CallExpression'
        // @ts-ignore
        && node.expression.callee.type === 'MemberExpression'
        // @ts-ignore
        && node.expression.callee.object
        // @ts-ignore
        && node.expression.callee.object.type === 'Identifier'
        // @ts-ignore
        && node.expression.callee.object.name === 'Object'
        // @ts-ignore
        && node.expression.callee.property
        // @ts-ignore
        && node.expression.callee.property.type === 'Identifier'
        // @ts-ignore
        && node.expression.callee.property.name === 'defineProperty'
    ) {
        // 忽略 Object.defineProperty(exports, "__esModule", { value: true });
        return fileContent
    }
    // @ts-ignore
    if (node.expression.type === 'Literal'
        // @ts-ignore
        && node.expression.value === 'use strict'
    ) {
        // 忽略 "use strict";
        return fileContent
    }
    // 删除, E.g.
    // async function init() {
    //     ...
    // }
    // init();
    return fileContent.substring(0, node.start) + fileContent.substring(node.end + 1)
}
