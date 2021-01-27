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


import {DewSDK} from "@idealworld/sdk";
import {ResourceKind} from "../src/domain/Enum";

const GATEWAY_SERVER_URL = "http://127.0.0.1:9000"

// 请先启动 TestServicelessApplication 并修改此变量
let APP_ID = 'apd10636b51ed84fd5986ade4bc8832543'
const USERNAME = "dew"
const PASSWORD = "TestPwd1d"

beforeEach(async () => {
    DewSDK.init(GATEWAY_SERVER_URL, APP_ID)
})

test('Test iam sdk', async () => {
    let identOptInfo = await DewSDK.iam.tenant.register('t1', 'a1', USERNAME, PASSWORD)
    let newAppCode = identOptInfo.appCode
    DewSDK.setting.appId(newAppCode)
    identOptInfo = await DewSDK.iam.account.login(USERNAME, PASSWORD)
    expect(identOptInfo.roleInfo[0].name).toContain('管理员')
    let resourceSubjectId = await DewSDK.iam.resource.subject.create('menu', ResourceKind.MENU, '菜单', 'menu://iam', '', '')
    await DewSDK.iam.resource.create('用户模块', '/user', resourceSubjectId)
    let menus = await DewSDK.iam.resource.menu.fetch()
    expect(menus.length).toBe(1)
    let eles = await DewSDK.iam.resource.ele.fetch()
    expect(eles.length).toBe(0)
    await DewSDK.iam.account.logout()
    // TODO 更多接口
})

test('Test cache sdk', async () => {
    await DewSDK.cache.del('test-key')
    await DewSDK.cache.del('test-field')
    await DewSDK.cache.del('test-inrc')
    await DewSDK.cache.del('test-field-inrc')
    await DewSDK.cache.del('test-ex')

    expect(await DewSDK.cache.exists('test-key')).toBe(false)
    expect(await DewSDK.cache.hexists('test-field', 'f1')).toBe(false)
    expect(await DewSDK.cache.get('test-key')).toBe(null)
    expect(await DewSDK.cache.hget('test-field', 'f1')).toBe(null)
    expect(await DewSDK.cache.hgetall('test-field')).toStrictEqual({})

    await DewSDK.cache.set('test-key', 'v1')
    await DewSDK.cache.hset('test-field', 'f1', 'v1')
    await DewSDK.cache.hset('test-field', 'f2', 'v2')

    expect(await DewSDK.cache.exists('test-key')).toBe(true)
    expect(await DewSDK.cache.hexists('test-field', 'f1')).toBe(true)
    expect(await DewSDK.cache.get('test-key')).toBe('v1')
    expect(await DewSDK.cache.hget('test-field', 'f1')).toBe('v1')
    expect(await DewSDK.cache.hgetall('test-field')).toStrictEqual({'f1': 'v1', 'f2': 'v2'})

    await DewSDK.cache.incrby('test-inrc', 1)
    await DewSDK.cache.incrby('test-inrc', 1)
    await DewSDK.cache.hincrby('test-field-inrc', 'f1', 1)
    await DewSDK.cache.hincrby('test-field-inrc', 'f1', 1)

    expect(await DewSDK.cache.exists('test-inrc')).toBe(true)
    expect(await DewSDK.cache.hexists('test-field-inrc', 'f1')).toBe(true)
    expect(await DewSDK.cache.get('test-inrc')).toBe(2)
    expect(await DewSDK.cache.hget('test-field-inrc', 'f1')).toBe(2)
    expect(await DewSDK.cache.hgetall('test-field-inrc')).toStrictEqual({'f1': '2'})

    await DewSDK.cache.del('test-inrc')
    await DewSDK.cache.hdel('test-field-inrc', 'f1')
    expect(await DewSDK.cache.exists('test-inrc')).toBe(false)
    expect(await DewSDK.cache.hexists('test-field-inrc', 'f1')).toBe(false)

    await DewSDK.cache.setex('test-ex', 'v1', 1)
    await DewSDK.cache.expire('test-key', 1)
    expect(await DewSDK.cache.exists('test-ex')).toBe(true)
    expect(await DewSDK.cache.exists('test-key')).toBe(true)
    // subject
    expect(await DewSDK.cache.subject('default').exists('test-key')).toBe(true)

    setTimeout(async () => {
        expect(await DewSDK.cache.exists('test-ex')).toBe(false)
        expect(await DewSDK.cache.exists('test-key')).toBe(false)
        // subject
        expect(await DewSDK.cache.subject('default').exists('test-key')).toBe(false)
    }, 1000)
})

test('Test reldb sdk', async () => {
    await DewSDK.iam.account.login(USERNAME, PASSWORD)
    let publicKey = await DewSDK.iam.app.key.fetchPublicKey()
    let accounts = await DewSDK.reldb.exec('select name from iam_account', [])
    // @ts-ignore
    expect(accounts[0].name).toBe('dew')
    accounts = await DewSDK.reldb.exec('select name from iam_account where name = ?', ['dew'])
    expect(accounts.length).toBe(1)
    // @ts-ignore
    expect(accounts[0].name).toBe('dew')
    accounts = await DewSDK.reldb.exec('select name from iam_account where name = ?', ['dew2'])
    expect(accounts.length).toBe(0)
    // subject
    accounts = await DewSDK.reldb.subject('default').exec('select name from iam_account', [])
    // @ts-ignore
    expect(accounts[0].name).toBe('dew')
})

test('Test http sdk', async () => {
    // get
    let getR = await DewSDK.http.subject("httpbin").get<any>('/get')
    expect(getR.body.url).toBe('https://127.0.0.1/get')
    getR = await DewSDK.http.subject("httpbin").get<any>('/get', {
        'Customer-A': 'AAA'
    })
    expect(getR.body.headers['Customer-A']).toBe('AAA')
    // delete
    await DewSDK.http.subject("httpbin").delete('/delete')
    // post
    let postR = await DewSDK.http.subject("httpbin").post<any>('/post', 'some data')
    expect(postR.body.data).toBe('some data')
    // put
    let putR = await DewSDK.http.subject("httpbin").put<any>('/put', 'some data')
    expect(putR.body.data).toBe('some data')
    // patch
    let patchR = await DewSDK.http.subject("httpbin").patch<any>('/patch', 'some data')
    expect(patchR.body.data).toBe('some data')
}, 200000)

test('Test task sdk', async done => {
    await DewSDK.iam.account.login(USERNAME, PASSWORD)
    let ident = (await DewSDK.iam.app.ident.list()).objects[0]
    let sk = (await DewSDK.iam.app.ident.fetchSk(ident.id))
    DewSDK.setting.aksk(ident.ak, sk)

    await DewSDK.task.initTasks(`
const JVM = {
  DewSDK: {
    iam: {
      auth: {
        create: function(){
          return ''
        }
      }
    }
  }
}
async function add(x, y){
  return x + y
}
    `)
    await DewSDK.task.create("timer", `
$.info(message)
    `, '/5 * * * * ?')
    await DewSDK.task.modify("timer", `
$.info('>>> timer ')
return 'timer'
    `, '/5 * * * * ?')
    expect(await DewSDK.task.execute("add", [10, 20])).toBe(30)
    expect(await DewSDK.task.execute("timer", [])).toBe('timer')
    setTimeout(async () => {
        await DewSDK.task.delete("timer")
        done()
    }, 10000)
}, 15000)

test("Test config", () => {
    let devConfig = DewSDK.conf(`
{
    "ak":"ak1",
    "sk":"sk1",
    "env":{
        "dev":{
            "ak":"ak_dev",
            "sk":"sk_dev",
            "db":{
                "url":"mysqlxxxx"
            }
        },
        "test":{
        },
        "prod":{
        }
    }
}
`, 'dev')
    expect(devConfig).toStrictEqual({"ak": "ak_dev", "db": {"url": "mysqlxxxx"}, "sk": "sk_dev"})
    let testConfig = DewSDK.conf(`
{
    "ak":"ak1",
    "sk":"sk1",
    "env":{
        "dev":{
            "ak":"ak_dev",
            "sk":"sk_dev",
            "db":{
                "url":"mysqlxxxx"
            }
        },
        "test":{
        },
        "prod":{
        }
    }
}
`, 'test')
    expect(testConfig).toStrictEqual({"ak": "ak1", "sk": "sk1"})
})

