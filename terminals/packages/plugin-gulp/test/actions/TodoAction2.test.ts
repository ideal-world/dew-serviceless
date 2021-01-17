import {DewSDK} from "@idealworld/sdk";
import {db, ItemDTO} from "./TodoAction1.test";
import {JsonMap} from "@idealworld/sdk/dist/domain/Basic";

export async function removeItem(itemId: number): Promise<null> {
    if (DewSDK.iam.auth.fetch()?.roleInfo.some(r => r.defCode === 'APP_ADMIN')) {
        return db.exec('delete from todo where id = ? ', [itemId])
            .then(() => null)
    }
    return db.exec('delete from todo where id = ? and create_user = ?', [itemId, DewSDK.iam.auth.fetch()?.accountCode])
        .then(delRowNumber => {
            // TODO
            if (delRowNumber[0] === 1) {
                return null
            }
            throw '权限错误'
        })
}

export async function ioTestStr(str: string, num: number, arr: string[], obj: any): Promise<string> {
    return str
}

export async function ioTestNum(str: string, num: number, arr: string[], obj: any): Promise<number> {
    return num
}

export async function ioTestArr(str: string, num: number, arr: string[], obj: any): Promise<string[]> {
    return arr
}

export async function ioTestArr2(): Promise<string[]> {
    return ['xxxcxccc']
}

export async function ioTestArr3(): Promise<ItemDTO[]> {
    return [{
        id: 1,
        content: 'xxxcxccc',
        createUserName: '',
        createUserId: ''
    }]
}

export async function ioTestObj(str: string, num: number, arr: string[], obj: any): Promise<any> {
    return obj
}

export async function ioTestMap(map: JsonMap<string>): Promise<JsonMap<string>> {
    map['add'] = 'add'
    return map
}

export async function ioTestDto(dto: ItemDTO): Promise<ItemDTO> {
    dto.createUserId = '100'
    return dto
}

export async function ioTestDtos(dtos: ItemDTO[]): Promise<ItemDTO[]> {
    dtos[0].createUserId = '100'
    return dtos
}

