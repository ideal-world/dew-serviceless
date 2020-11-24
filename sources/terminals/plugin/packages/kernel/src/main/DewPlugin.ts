import {Node, Parser} from "acorn";
import NodeRSA from "node-rsa";
import fs from "fs";
import path from "path";

let nodeRSA: NodeRSA

export function checkAndReplace(fileContent: string): string {
    let ast = Parser.parse(fileContent, {
        ecmaVersion: 6,
        sourceType: 'script'
    })
    return replaceSql(fileContent, ast)
}

function replaceSql(fileContent: string, ast: Node): string {
    findAstOffsetByType(ast, ['Literal', 'TemplateElement']).reverse()
        .forEach(offset => {
            let startOffset = offset[2] === 'Literal' ? offset[0] + 1 : offset[0]
            let endOffset = offset[2] === 'Literal' ? offset[1] - 1 : offset[1]
            let text = fileContent.substring(startOffset, endOffset).trim()
            let action = text.startsWith('select') ? 'FETCH'
                : text.startsWith('insert') ? 'CREATE'
                    : text.startsWith('update') ? 'MODIFY'
                        : text.startsWith('delete') ? 'DELETE' : null
            if (action) {
                fileContent = fileContent.substring(0, startOffset) + action + '|' + encrypt(text) + fileContent.substring(endOffset)
            }
        })
    return fileContent

}

function findAstOffsetByType(ast: Node, types: string[]): [number, number, string][] {
    let offsets: [number, number, string][] = []
    if (types.some(t => t === ast.type)) {
        offsets.push([ast.start, ast.end, ast.type])
    } else {
        switch (ast.type) {
            // 入口
            case 'Program':
                // @ts-ignore
                ast.body && ast.body.forEach(node => offsets = offsets.concat(findAstOffsetByType(node, types)))
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

function encrypt(text: string): string {
    if (!nodeRSA) {
        let publicKey = fs.readFileSync(path.resolve(__dirname, '../../', 'Dew.key'), {encoding: 'utf8'})
        nodeRSA = new NodeRSA();
        nodeRSA.importKey(Buffer.from(getPublicKey(publicKey)), 'pkcs8-public-pem');
    }
    return nodeRSA.encrypt(text, 'base64')
}

function insertStr(str: string, insertStr: string, sn: number): string {
    let newstr = ''
    for (var i = 0; i < str.length; i += sn) {
        var tmp = str.substring(i, i + sn)
        newstr += tmp + insertStr
    }
    return newstr
}

const getPrivateKey = function (key: string): string {
    const result = insertStr(key.trim(), '\n', 64)
    return '-----BEGIN PRIVATE KEY-----\n' + result + '-----END PRIVATE KEY-----'
}

const getPublicKey = function (key: string): string {
    const result = insertStr(key.trim(), '\n', 64)
    return '-----BEGIN PUBLIC KEY-----\n' + result + '-----END PUBLIC KEY-----'
}
