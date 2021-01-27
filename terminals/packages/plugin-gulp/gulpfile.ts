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

import {dewBuild} from "./src";

const gulp = require("gulp")
const {series} = require('gulp')
const glob = require('glob');
const browserify = require("browserify")
const source = require('vinyl-source-stream')
const buffer = require('vinyl-buffer')
const uglify = require("gulp-uglify-es").default
const rm = require('rimraf')
let ts = require("gulp-typescript")
let tsProject = ts.createProject('tsconfig.test.json')

const _path = {
    dist: './test_dist',
    action: './test_dist/test/actions',
}

function _clean(done) {
    rm(_path.dist, error => {
        if (error) throw error
        done()
    })
}

function _ts() {
    return tsProject.src()
        .pipe(tsProject()).js
        .pipe(gulp.dest(_path.dist))
}

function _dewBuild() {
    return dewBuild(_path.action,true)
}

function _dewTestBuild() {
    return dewBuild(_path.action, true,'../../../module/task/src/test/resources/test.js')
}

function _jsBuild() {
    return browserify({
        entries: glob.sync(_path.dist + "/**/*.js")
    })
        .bundle()
        .pipe(source('bundle.js'))
        .pipe(buffer())
        .pipe(uglify())
        .pipe(gulp.dest(_path.dist))
}

module.exports = {
    testToTaskModule: series(_clean, _ts, _dewTestBuild),
    testIT: series(_clean, _ts, _dewBuild, _jsBuild),
}
