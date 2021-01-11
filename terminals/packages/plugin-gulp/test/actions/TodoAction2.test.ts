import {DewSDK} from "@idealworld/sdk";
import {db} from "./TodoAction1.test";

export async function removeItem(itemId: number): Promise<null> {
    if (DewSDK.iam.auth.fetch().roleInfo.some(r => r.defCode === 'APP_ADMIN')) {
        return db.exec('delete from todo where id = ? ', [itemId])
            .then(() => null)
    }
    return db.exec('delete from todo where id = ? and create_user = ?', [itemId, DewSDK.iam.auth.fetch().accountCode])
        .then(delRowNumber => {
            // TODO
            if (delRowNumber[0] === 1) {
                return null
            }
            throw '权限错误'
        })
}
