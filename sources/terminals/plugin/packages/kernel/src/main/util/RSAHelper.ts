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

import NodeRSA from "node-rsa";


function formatKey(str: string, insertStr: string, sn: number): string {
    let newstr = ''
    for (var i = 0; i < str.length; i += sn) {
        var tmp = str.substring(i, i + sn)
        newstr += tmp + insertStr
    }
    return newstr
}

const getPrivateKey = function (key: string): string {
    const result = formatKey(key.trim(), '\n', 64)
    return '-----BEGIN PRIVATE KEY-----\n' + result + '-----END PRIVATE KEY-----'
}

const getPublicKey = function (key: string): string {
    const result = formatKey(key.trim(), '\n', 64)
    return '-----BEGIN PUBLIC KEY-----\n' + result + '-----END PUBLIC KEY-----'
}

export class RSA {

    nodeRSA: NodeRSA = new NodeRSA()

    loadKey(publicKey: string): void {
        this.nodeRSA.importKey(Buffer.from(getPublicKey(publicKey)), 'pkcs8-public-pem')
    }

    encryptByPub(text: string): string {
        return this.nodeRSA.encrypt(text, 'base64')
    }

}
