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

import * as through from 'through2'
import * as gutil from 'gulp-util'
import {checkAndReplace} from "@idealworld/plugin-kernel";

const PluginError = gutil.PluginError;
const PLUGIN_NAME = 'Dew-Build';

module.exports = function () {
    return through.obj(function (file, enc, cb) {
        if (file.isBuffer()) {
            // TODO 过滤非js文件
            file.contents = Buffer.from(checkAndReplace(file.contents.toString('utf8')))
            this.push(file)
            cb()
        }
        if (file.isStream()) {
            this.emit('error', new PluginError(PLUGIN_NAME, 'Streams are not supported!'))
            cb()
        }
    })
}
