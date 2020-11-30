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

const { src, dest } = require('gulp')
const {series} = require("gulp")
const ts = require("gulp-typescript")
const browserify = require("browserify")
const source = require('vinyl-source-stream')
const uglify = require("gulp-uglify-es").default
const rename = require('gulp-rename')
const buffer = require('vinyl-buffer')
const tsify = require("tsify")
const tsProject = ts.createProject('tsconfig.json')

function buildTS() {
    return tsProject
        .src()
        .pipe(tsProject())
        .js.pipe(dest('dist'))
}

function buildDew() {
    return browserify({
        basedir: '.',
        debug: true,
        entries: ['src/main/DewSDK.ts'],
        cache: {},
        packageCache: {},
    }).require('./src/main/DewSDK.ts', {expose: 'Dew'})
        .plugin(tsify)
        .bundle()
        .pipe(source('Dew.js'))
        .pipe(dest('dist/'))
        .pipe(rename({ extname: '.min.js' }))
        .pipe(buffer())
        .pipe(uglify())
        .pipe(dest('dist/'))
}

function buildExample() {
    return src('src/test/example/**')
        .pipe(dest('dist/example'))
}

function buildExampleDew() {
    return src('dist/Dew.js')
        .pipe(dest('dist/example/libs/'))
}


exports.build = series(buildDew, buildExample, buildExampleDew)
