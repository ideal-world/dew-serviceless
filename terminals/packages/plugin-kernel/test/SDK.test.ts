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

import {checkAndReplace} from "../src";

test('Test init fun', async () => {
    expect(checkAndReplace(`const GATEWAY_SERVER_URL = "http://127.0.0.1:9000"
const APP_ID = 1
DewSDK.init(GATEWAY_SERVER_URL, APP_ID, async () => {
        // 测试
        request.setAkSk(APP_AK, APP_SK)
        await DewSDK.iam.resource.subject.create("todoDB", ResourceKind.RELDB, "todo应用数据库",
            "mysql://127.0.0.1:" + MYSQL_PORT + "/test",
            MYSQL_ADMIN, MYSQL_PASSWORD)

    })`)).toContain(`const GATEWAY_SERVER_URL = "http://127.0.0.1:9000"
const APP_ID = 1
DewSDK.init(GATEWAY_SERVER_URL, APP_ID)`)
})

/*await DewSDK.reldb.subject(APP_ID + ".reldb.todoDB").exec('create table if not exists todo\n' +
    '(\n' +
    'id bigint auto_increment primary key,\n' +
    'create_time timestamp default CURRENT_TIMESTAMP null comment \'创建时间\',\n' +
    'create_user varchar(255) not null comment \'创建者OpenId\',\n' +
    'content varchar(255) not null comment \'内容\'\n' +
    ')
' +
'comment \'任务表\'', [])
await DewSDK.reldb.subject(APP_ID + ".reldb.todoDB").exec('insert into todo(content,create_user) values (?,?)', ['这是个示例', ''])*/

