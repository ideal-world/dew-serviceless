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
import * as DewPlugin from "@idealworld/plugin-kernel";

const PluginError = gutil.PluginError;
const PLUGIN_NAME = 'Dew-Build';

const fs = require('fs')
const config = JSON.parse(fs.readFileSync('./package.json'))['dew']
const serverUrl: string = config.serverUrl
const appId: number = config.appId
const ak: string = config.ak
const sk: string = config.sk


export function jvmPrepare() {
    return through.obj(function (file, enc, cb) {
        if (file.isBuffer()) {
            file.contents = Buffer.from(DewPlugin.replaceImport(file.contents.toString('utf8'),true))
            this.push(file)
            cb()
        }
        if (file.isStream()) {
            this.emit('error', new PluginError(PLUGIN_NAME, 'Streams are not supported!'))
            cb()
        }
    })
}

export function jvmBuild() {
    return through.obj(function (file, enc, cb) {
        if (file.isBuffer()) {
            DewPlugin.sendTask(file.contents.toString('utf8'), serverUrl, appId, ak, sk)
                .then(() => {
                    cb()
                })
        }
        if (file.isStream()) {
            this.emit('error', new PluginError(PLUGIN_NAME, 'Streams are not supported!'))
            cb()
        }
    })
}

export function jsPrepare() {
    return through.obj(function (file, enc, cb) {
        if (file.isBuffer()) {
            file.contents = Buffer.from(DewPlugin.replaceImport(file.contents.toString('utf8'),false))
            this.push(file)
            cb()
        }
        if (file.isStream()) {
            this.emit('error', new PluginError(PLUGIN_NAME, 'Streams are not supported!'))
            cb()
        }
    })
}

export function jsBuild() {
    return through.obj(function (file, enc, cb) {
        if (file.isBuffer()) {
            DewPlugin.rewriteAction(file.contents.toString('utf8'), 'xxx', appId)
            cb()
        }
        if (file.isStream()) {
            this.emit('error', new PluginError(PLUGIN_NAME, 'Streams are not supported!'))
            cb()
        }
    })
}


