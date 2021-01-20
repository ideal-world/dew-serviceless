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
exports.removeItem = void 0;
const jvm_1 = require("@idealworld/sdk");
const TodoAction1_1 = require("./TodoAction1");
async function removeItem(itemId) {
    if (jvm_1.DewSDK.iam.auth.fetch().roleInfo.some(r => r.defCode === 'APP_ADMIN')) {
        return TodoAction1_1.db.exec('delete from todo where id = ? ', [itemId])
            .then(() => null);
    }
    return TodoAction1_1.db.exec('delete from todo where id = ? and create_user = ?', [itemId, jvm_1.DewSDK.iam.auth.fetch().accountCode])
        .then(delRowNumber => {
        // TODO
        if (delRowNumber[0] === 1) {
            return null;
        }
        throw '权限错误';
    });
}
exports.removeItem = removeItem;
