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


import {DewSDK} from "../src/DewSDK";

const GATEWAY_SERVER_URL = "http://127.0.0.1:9000"
const APP_ID = 1
const IAM_SERVER_NAME = "127.0.0.1:8081"
const USERNAME = "dew"
const PASSWORD = "iVCn7UnP1A"

beforeAll(() => {
    DewSDK.init(GATEWAY_SERVER_URL, APP_ID, IAM_SERVER_NAME)
})

test('Test iam sdk', async () => {
    let identOptInfo = await DewSDK.iam.login(USERNAME, PASSWORD)
    expect(identOptInfo.roleInfo[0].name).toContain('管理员')
    let menus = await DewSDK.iam.menu.fetch()
    expect(menus.length).toBe(1)
    let eles = await DewSDK.iam.ele.fetch()
    expect(eles.length).toBe(0)
    await DewSDK.iam.logout()
})

test('Test cache sdk', async () => {
    await DewSDK.cache.del('test-Key')
    await DewSDK.cache.del('test-field')
    await DewSDK.cache.del('test-inrc')
    await DewSDK.cache.del('test-field-inrc')
    await DewSDK.cache.del('test-ex')

    expect(await DewSDK.cache.exists('test-Key')).toBe(false)
    expect(await DewSDK.cache.hexists('test-field', 'f1')).toBe(false)
    expect(await DewSDK.cache.get('test-Key')).toBe(null)
    expect(await DewSDK.cache.hget('test-field', 'f1')).toBe(null)
    expect(await DewSDK.cache.hgetall('test-field')).toStrictEqual({})

    await DewSDK.cache.set('test-Key', 'v1')
    await DewSDK.cache.hset('test-field', 'f1', 'v1')
    await DewSDK.cache.hset('test-field', 'f2', 'v2')

    expect(await DewSDK.cache.exists('test-Key')).toBe(true)
    expect(await DewSDK.cache.hexists('test-field', 'f1')).toBe(true)
    expect(await DewSDK.cache.get('test-Key')).toBe('v1')
    expect(await DewSDK.cache.hget('test-field', 'f1')).toBe('v1')
    expect(await DewSDK.cache.hgetall('test-field')).toStrictEqual({'f1': 'v1', 'f2': 'v2'})

    await DewSDK.cache.incrby('test-inrc', 1)
    await DewSDK.cache.incrby('test-inrc', 1)
    await DewSDK.cache.hincrby('test-field-inrc', 'f1', 1)
    await DewSDK.cache.hincrby('test-field-inrc', 'f1', 1)

    expect(await DewSDK.cache.exists('test-inrc')).toBe(true)
    expect(await DewSDK.cache.hexists('test-field-inrc', 'f1')).toBe(true)
    expect(await DewSDK.cache.get('test-inrc')).toBe('2')
    expect(await DewSDK.cache.hget('test-field-inrc', 'f1')).toBe('2')
    expect(await DewSDK.cache.hgetall('test-field-inrc')).toStrictEqual({'f1': '2'})

    await DewSDK.cache.del('test-inrc')
    await DewSDK.cache.hdel('test-field-inrc', 'f1')
    expect(await DewSDK.cache.exists('test-inrc')).toBe(false)
    expect(await DewSDK.cache.hexists('test-field-inrc', 'f1')).toBe(false)

    await DewSDK.cache.setex('test-ex', 'v1', 1)
    expect(await DewSDK.cache.exists('test-key')).toBe(true)
    await DewSDK.cache.expire('test-key', 1)
    expect(await DewSDK.cache.exists('test-ex')).toBe(true)
    expect(await DewSDK.cache.exists('test-key')).toBe(true)
    // subject
    expect(await DewSDK.cache.subject(APP_ID + '.cache.default').exists('test-key')).toBe(true)

    setTimeout(async () => {
        expect(await DewSDK.cache.exists('test-ex')).toBe(false)
        expect(await DewSDK.cache.exists('test-key')).toBe(false)
        // subject
        expect(await DewSDK.cache.subject(APP_ID + '.cache.default').exists('test-key')).toBe(false)
    }, 1000)
}, 100000)

test('Test reldb sdk', async () => {
    let accounts = await DewSDK.reldb.exec('select name from iam_account', [])
    expect(accounts.length).toBe(1)
    // @ts-ignore
    expect(accounts[0].name).toBe('dew')
    accounts = await DewSDK.reldb.exec('select name from iam_account where name = ?', ['dew'])
    expect(accounts.length).toBe(1)
    // @ts-ignore
    expect(accounts[0].name).toBe('dew')
    accounts = await DewSDK.reldb.exec('select name from iam_account where name = ?', ['dew2'])
    expect(accounts.length).toBe(0)
    // subject
    accounts = await DewSDK.reldb.subject(APP_ID + '.reldb.default').exec('select name from iam_account', [])
    expect(accounts.length).toBe(1)
    // @ts-ignore
    expect(accounts[0].name).toBe('dew')
})

test('Test http sdk', async () => {
    // POST
    let postR = await DewSDK.http.post<any>('/common/login', {
        ak: USERNAME,
        sk: PASSWORD,
        relAppId: APP_ID
    })
    expect(postR.roleInfo[0].name).toContain('管理员')
    /* // GET
     let getR = await DewSDK.http.get<any>('/')
     expect(getR.result.headers.Host).toBe('httpbin.org')
     let menus = await DewSDK.iam.menu.fetch()
     expect(menus.length).toBe(1)
     let eles = await DewSDK.iam.ele.fetch()
     expect(eles.length).toBe(0)
     await DewSDK.iam.logout()

     // get
     let getR = await DewSDK.http.subject("httpbin").get<any>('/get')
     expect(getR.result.headers.Host).toBe('httpbin.org')
     getR = await DewSDK.http.get<any>('https://httpbin.org/get', {
         'Customer-A': 'AAA'
     })
     expect(getR.result.headers['Customer-A']).toBe('AAA')
     // delete
     await DewSDK.http.delete('https://httpbin.org/delete')

     // put
     let putR = await DewSDK.http.put<any>('https://httpbin.org/put', 'some data')
     expect(putR.result.data).toBe('some data')
     // patch
     let patchR = await DewSDK.http.patch<any>('https://httpbin.org/patch', 'some data')
     expect(patchR.result.data).toBe('some data')*/
}, 200000)
