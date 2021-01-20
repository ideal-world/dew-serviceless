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

"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.addItem = exports.fetchItems = exports.db = void 0;
const sdk_1 = require("@idealworld/sdk");
exports.db = sdk_1.DewSDK.reldb.subject("todoDB");
async function init() {
    await exports.db.exec(`create table if not exists todo
(
    id bigint auto_increment primary key,
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    create_user bigint not null comment '创建者Id',
    content varchar(255) not null comment '内容'
)
comment '任务表'`, []);
    await exports.db.exec('insert into todo(content,create_user) values (?,?)', ['这是个示例', '']);
}
init();
async function fetchItems() {
    return doFetchItems();
}
exports.fetchItems = fetchItems;
async function doFetchItems() {
    if (sdk_1.DewSDK.iam.auth.fetch() == null) {
        return [];
    }
    if (sdk_1.DewSDK.iam.auth.fetch().roleInfo.some(r => r.defCode === 'APP_ADMIN')) {
        return exports.db.exec('select * from todo', []);
    }
    return exports.db.exec('select * from todo where create_user = ?', [sdk_1.DewSDK.iam.auth.fetch().accountCode]);
}
async function addItem(content) {
    if (sdk_1.DewSDK.iam.auth.fetch() == null) {
        throw '请先登录';
    }
    await sdk_1.DewSDK.cache.set("xxx", content);
    return exports.db.exec('insert into todo(content,create_user) values (?, ?)', [content, sdk_1.DewSDK.iam.auth.fetch().accountCode])
        .then(() => null);
}
exports.addItem = addItem;
