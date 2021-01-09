let db = DewSDK.reldb.subject("todoDB");
 async function init() {
    await db.exec(`create table if not exists todo
(
    id bigint auto_increment primary key,
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    create_user varchar(255) not null comment '创建者OpenId',
    content varchar(255) not null comment '内容'
)
comment '任务表'`, []);
    await db.exec('insert into todo(content,create_user) values (?,?)', ['这是个示例', '']);
}
 async function fetchItems() {
    if (DewSDK.iam.auth.fetch() == null) {
        return [];
    }
    if (DewSDK.iam.auth.fetch().roleInfo.some(r => r.defCode === 'APP_ADMIN')) {
        return db.exec('select * from todo', []);
    }
    return db.exec('select * from todo where create_user = ?', [DewSDK.iam.auth.fetch().accountCode]);
}
 async function addItem(content) {
    if (DewSDK.iam.auth.fetch() == null) {
        throw '请先登录';
    }
    await DewSDK.cache.set("xxx", content);
    return db.exec('insert into todo(content,create_user) values (?, ?)', [content, DewSDK.iam.auth.fetch().accountCode])
        .then(() => null);
}
 async function removeItem(itemId) {
    if (DewSDK.iam.auth.fetch().roleInfo.some(r => r.defCode === 'APP_ADMIN')) {
        return db.exec('delete from todo where id = ? ', [itemId])
            .then(() => null);
    }
    return db.exec('delete from todo where id = ? and create_user = ?', [itemId, DewSDK.iam.auth.fetch().accountCode])
        .then(delRowNumber => {
            // TODO
            if (delRowNumber[0] === 1) {
                return null;
            }
            throw '权限错误';
        });
}
