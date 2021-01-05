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
import fs from "fs";
import path from "path";
import * as JSASTHelper from "./util/JSASTHelper";
import {RSA} from "./util/RSAHelper";
import {SQLParser} from "./util/SQLASTHelper";

let rsa: RSA
let sqlParser: SQLParser = new SQLParser()

export function checkAndReplace(fileContent: string): string {
    let ast = JSASTHelper.parse(fileContent)
    fileContent = replaceSql(fileContent, ast)
    ast = JSASTHelper.parse(fileContent)
    let items = replaceAndExtractInitFun(fileContent, ast)
    let initFun = items[1]
    return items[0]
}

function replaceSql(fileContent: string, ast: Node): string {
    JSASTHelper.findAstOffsetByType(ast, ['Literal', 'TemplateElement']).reverse()
        .forEach(offset => {
            let startOffset = offset[2] === 'Literal' ? offset[0] + 1 : offset[0]
            let endOffset = offset[2] === 'Literal' ? offset[1] - 1 : offset[1]
            let text = fileContent.substring(startOffset, endOffset).trim().toLowerCase()
            let action = text.startsWith('select ') && text.indexOf(' from ') !== -1 ? 'FETCH'
                : text.startsWith('insert ') && text.indexOf(' into ') !== -1 && text.indexOf(' values ') !== -1 ? 'CREATE'
                    : text.startsWith('update ') && text.indexOf(' set ') !== -1 && text.indexOf(' where ') !== -1 ? 'MODIFY'
                        : text.startsWith('delete ') && text.indexOf(' where ') !== -1 ? 'DELETE' : null
            if (action) {
                try {
                    sqlParser.astify(text)
                    fileContent = fileContent.substring(0, startOffset) + action + '|' + encrypt(text) + fileContent.substring(endOffset)
                } catch (e) {
                    console.log('[DewPlugin]Non-SQL type text : %s', text)
                }
            }
        })
    return fileContent
}

function replaceAndExtractInitFun(fileContent: string, ast: Node): string[] {
    console.log(fileContent)
    return []
}

function encrypt(text: string): string {
    initRSA()
    return rsa.encryptByPub(text)
}

export function initRSA(publicKeyContent?: string): void {
    if (!rsa) {
        let publicKey = publicKeyContent ? publicKeyContent : fs.readFileSync(path.resolve(__dirname, '../', 'Dew.public'), {encoding: 'utf8'})
        rsa = new RSA()
        rsa.loadKey(publicKey)
    }
}
