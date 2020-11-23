import * as request from "./util/Request";

export function init(serverUrl: string, ak: string, sk: string): void {
    request.setServerUrl(serverUrl)
    request.setAkSk(ak, sk)
}

export function checkAndReplace(fileContent: string): string {
    // TODO
    return ''
}


