import {DewSDK} from "@idealworld/sdk";

export type ItemDTO = {
    id: number,
    content: string
    createUserName: string
    createUserId: string
}

export const db = DewSDK.reldb.subject("todoDB")

export async function init() {
    await db.exec(`create table if not exists todo
(
    id bigint auto_increment primary key,
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    create_user bigint not null comment '创建者Id',
    content varchar(255) not null comment '内容'
)
comment '任务表'`, [])
    await db.exec('insert into todo(content,create_user) values (?,?)', ['这是个示例', ''])
}

export async function fetchItems(): Promise<ItemDTO[]> {
    return doFetchItems()
}

async function doFetchItems(): Promise<ItemDTO[]> {
    if (DewSDK.iam.auth.fetch() == null) {
        return []
    }
    if (DewSDK.iam.auth.fetch()?.roleInfo.some(r => r.defCode === 'APP_ADMIN')) {
        return db.exec('select * from todo', [])
    }
    return db.exec('select * from todo where create_user = ?', [DewSDK.iam.auth.fetch()?.accountCode])
}

export async function addItem(content: string): Promise<null> {
    if (DewSDK.iam.auth.fetch() == null) {
        throw '请先登录'
    }
    await DewSDK.cache.set("xxx", content)
    return db.exec('insert into todo(content,create_user) values (?, ?)', [content, DewSDK.iam.auth.fetch()?.accountCode])
        .then(() => null)
}
