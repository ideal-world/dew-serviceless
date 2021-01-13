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
import * as fs from "fs";

const PluginError = gutil.PluginError;
const PLUGIN_NAME = 'Dew-Build';

const path = require('path')

export function jvmPrepare(relativeBasePath: string) {
    let basePath = path.join(process.cwd(), relativeBasePath)
    return through.obj(function (file, enc, cb) {
        if (!file.path.startsWith(basePath)) {
            cb()
            return
        }
        if (file.isBuffer()) {
            DewPlugin.generateJVMFile(basePath, file.path)
            fs.writeFileSync(file.path, DewPlugin.replaceImport(file.contents.toString(enc), true))
            cb()
        }
        if (file.isStream()) {
            this.emit('error', new PluginError(PLUGIN_NAME, 'Streams are not supported!'))
            cb()
        }
    })
}

export function jvmBuild(relativeBasePath: string) {
    return through.obj(function (file, enc, cb) {
        if (file.isBuffer()) {
            DewPlugin.sendTask(file.contents.toString(enc))
                .then(() => {
                    DewPlugin.deleteJVMFile(path.join(process.cwd(), relativeBasePath))
                    cb()
                })
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
            let fileName = file.path.substring(file.path.lastIndexOf(path.sep) + 1, file.path.lastIndexOf('.'))
            let content = file.contents.toString(enc)
            content = DewPlugin.replaceImport(content, false)
            content = DewPlugin.rewriteAction(content, fileName)
            content = DewPlugin.initDewSDK(content)
            fs.writeFileSync(file.path, content)
            cb()
        }
        if (file.isStream()) {
            this.emit('error', new PluginError(PLUGIN_NAME, 'Streams are not supported!'))
            cb()
        }
    })
}


