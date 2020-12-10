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

test('Test check and replace', async () => {
    expect(checkAndReplace(`
    Hi.sql('xxx','select * from main')
    `)).not.toContain('select')
    expect(checkAndReplace(`
    Hi.sql('xxx',"select * from main")
    `)).not.toContain('select')
    expect(checkAndReplace(`
    Hi.sql('xxx','select * from main left join org on org.id = main.orgId')
    `)).not.toContain('select')
    expect(checkAndReplace(
        "Hi.sql('xxx',`select * from main\n" +
        "  left join org on org.id = main.orgId`)"
    )).not.toContain('select')
    // TODO 支持拼接

    /*    expect(checkAndReplace(`
        Hi.sql('xxx','select * from main'+
            '  left join org on org.id = main.orgId')
        `)).toEqual(`
        Hi.sql('xxx','FETCH|xxxxxx')
        `)*/
    expect(checkAndReplace(`
    let insertSql = 'insert into main(name, age) values (?, ?)'
    Hi.sql(insertSql,10,'20')
    `)).not.toContain('insert into main(name, age) values (?, ?)')
    expect(checkAndReplace(`
    let sqlJson = {
        s1: 'insert into main(name, age) values (?, ?)'
    }
    sqlJson[s2] = 'update main set name = ? where id = ?'
    sqlJson[s3] = {}
    sqlJson[s3][s33] = 'update main set name = ? where id = ?'
    Hi.sql(sqlJson.s1,10,'20')
    Hi.sql(sqlJson.s2,10,'20')
    `)).not.toContain('update')
    /*expect(checkAndReplace(`
    type SqlType = {
        s1: string
    }
    let sqlType:SqlType = {
        s1:'insert into main(name, age) values (?, ?)'
    }
    Hi.sql(sqlType.s1,10,'20')
    `)).toEqual(`
   type SqlType = {
        s1: string
    }
    let sqlType:SqlType = {
        s1:'CREATE|xxxxxx'
    }
    Hi.sql(sqlType.s1,10,'20')
    `)
    expect(checkAndReplace(`
    class SqlClass {
    s1!: string
    }
    let sqlClass:SqlClass = {
        s1:'insert into main(name, age) values (?, ?)'
    }
    Hi.sql(sqlClass.s1,10,'20')
    `)).toEqual(`
    class SqlClass {
    s1!: string
    }
    let sqlClass:SqlClass = {
        s1:'CREATE|xxxxxx'
    }
    Hi.sql(sqlClass.s1,10,'20')
    `)*/

    expect(checkAndReplace("xxfsss")).toContain("xxfsss")
    expect(checkAndReplace("'xxfsss'")).toContain("'xxfsss'")
    expect(checkAndReplace("'select haha  aaa fddd, from 1'")).toContain("'select haha  aaa fddd, from 1'")
})
