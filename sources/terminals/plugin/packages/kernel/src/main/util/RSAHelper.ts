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
