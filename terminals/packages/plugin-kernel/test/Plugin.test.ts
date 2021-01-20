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

import fs from "fs";
import fsPath from "path";
import {replaceImport, rewriteAction} from "../src";

test('Test replace import', async () => {
    let path = fsPath.resolve(process.cwd(), 'test', 'actions', 'TodoAction1.js')
    let fileContent = fs.readFileSync(path, 'utf8')
    expect(replaceImport(fileContent,true)).toContain(`const jvm_1 = require("@idealworld/sdk/dist/jvm");`)
    expect(replaceImport(fileContent,false)).toContain(`const jvm_1 = require("@idealworld/sdk");`)
})

test('Test re-write action', async () => {
    let path = fsPath.resolve(process.cwd(), 'test', 'actions', 'TodoAction1.js')
    let fileContent = fs.readFileSync(path, 'utf8')
    expect(rewriteAction(fileContent,'TodoAction1')).toEqual(`"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.addItem = exports.fetchItems = void 0;
async function fetchItems() {
  return DewSDK.task.execute("TodoAction1.fetchItems", []);
}
exports.fetchItems = fetchItems;
async function addItem(content) {
  return DewSDK.task.execute("TodoAction1.addItem", [content]);
}
exports.addItem = addItem;
`)
})
