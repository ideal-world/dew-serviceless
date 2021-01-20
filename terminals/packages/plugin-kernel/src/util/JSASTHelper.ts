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

import {Node, Parser} from "acorn";

let _ecmaVersion: 3 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 2015 | 2016 | 2017 | 2018 | 2019 | 2020 = 2020
let _sourceType: 'script' | 'module' = 'script'

export function option(ecmaVersion: 3 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 2015 | 2016 | 2017 | 2018 | 2019 | 2020,
                       sourceType: 'script' | 'module'): void {
    _ecmaVersion = ecmaVersion
    _sourceType = sourceType
}

export type DewNode = {
    start: number
    end: number
    type: string
    ast: Node
}

export function parse(fileContent: string): Node {
    return Parser.parse(fileContent, {
        ecmaVersion: _ecmaVersion,
        sourceType: _sourceType
    })
}

export function findAstOffsetByType(ast: Node, types: string[]): DewNode[] {
    let offsets: DewNode[] = []
    if (types.some(t => t === ast.type)) {
        offsets.push({
            start: ast.start,
            end: ast.end,
            type: ast.type,
            ast: ast
        })
    } else {
        switch (ast.type) {
            // 入口
            case 'Program':
                // @ts-ignore
                ast.body && ast.body.forEach(node => offsets = offsets.concat(findAstOffsetByType(node, types)))
                break;
            // function
            case 'FunctionDeclaration':
                // @ts-ignore
                if (ast.id) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.id, types))
                }
                // @ts-ignore
                if (ast.body) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.body, types))
                }
                break;
            // let a = 'xxx'
            case 'VariableDeclaration':
                // @ts-ignore
                ast.declarations && ast.declarations.forEach(node => offsets = offsets.concat(findAstOffsetByType(node, types)))
                break;
            // let a = 'xxx'
            case 'VariableDeclarator':
                // @ts-ignore
                if (ast.id) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.id, types))
                }
                // @ts-ignore
                if (ast.init) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.init, types))
                }
                break;
            // {}
            case 'ObjectExpression':
                // @ts-ignore
                ast.properties && ast.properties.forEach(node => offsets = offsets.concat(findAstOffsetByType(node, types)))
                break;
            // {s1: 'xxxx'}
            case 'Property':
                // @ts-ignore
                if (ast.key) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.key, types))
                }
                // @ts-ignore
                if (ast.value) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.value, types))
                }
                break;
            // `xxx`
            case 'TemplateLiteral':
                // @ts-ignore
                ast.expressions && ast.expressions.forEach(node => offsets = offsets.concat(findAstOffsetByType(node, types)))
                // @ts-ignore
                ast.quasis && ast.quasis.forEach(node => offsets = offsets.concat(findAstOffsetByType(node, types)))
                break;
            case 'ExpressionStatement':
                // @ts-ignore
                if (ast.expression) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.expression, types))
                }
                break;
            case 'CallExpression':
                // @ts-ignore
                if (ast.callee) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.callee, types))
                }
                // @ts-ignore
                ast.arguments && ast.arguments.forEach(node => offsets = offsets.concat(findAstOffsetByType(node, types)))
                break;
            // a.b
            case 'MemberExpression':
                // @ts-ignore
                if (ast.object) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.object, types))
                }
                // @ts-ignore
                if (ast.property) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.property, types))
                }
                break;
            // 1+ 2
            case 'BinaryExpression':
                // @ts-ignore
                if (ast.left) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.left, types))
                }
                // @ts-ignore
                if (ast.right) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.right, types))
                }
                break;
            // s = 'xxxx'
            case 'AssignmentExpression':
                // @ts-ignore
                if (ast.left) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.left, types))
                }
                // @ts-ignore
                if (ast.right) {
                    // @ts-ignore
                    offsets = offsets.concat(findAstOffsetByType(ast.right, types))
                }
                break;
        }
    }
    return offsets
}
