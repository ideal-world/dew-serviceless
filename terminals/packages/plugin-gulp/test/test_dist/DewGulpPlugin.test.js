"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
const vinyl_1 = require("vinyl");
const plugin = require('../src');
test('Test gulp plugin', () => {
    let fakeFile = new vinyl_1.File({
        contents: Buffer.from(`
        let sqlJson = {
        s1: 'insert into main(name, age) values (?, ?)'
    }
    sqlJson[s2] = 'update main set name = ? where id = ?'
    sqlJson[s3] = {}
    sqlJson[s3][s33] = 'update main set name = ? where id = ?'
    Dew.sql(sqlJson.s1,10,'20')
    Dew.sql(sqlJson.s2,10,'20')
    `)
    });
    let pStream = plugin();
    pStream.write(fakeFile);
    pStream.once('data', function (file) {
        expect(file.isBuffer()).toBe(true);
        expect(file.contents.toString('utf8')).not.toMatch('insert');
    });
});