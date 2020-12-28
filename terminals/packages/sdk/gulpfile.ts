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

const gulp = require("gulp")
const browserify = require("browserify")
const source = require('vinyl-source-stream')
const buffer = require('vinyl-buffer')
const babel = require('gulp-babel')
const uglify = require("gulp-uglify")
const tsify = require("tsify")

function build() {
    return browserify({
        basedir: '.',
        debug: false,
        entries: ['src/DewSDK.ts'],
        cache: {},
        packageCache: {},
    }).require('./src/DewSDK.ts', {expose: 'DewSDK'})
        .plugin(tsify)
        .bundle()
        .pipe(source('DewSDK_browserify.js'))
        .pipe(buffer())
        .pipe(babel({
            presets: ['@babel/preset-env']
        }))
        .pipe(uglify())
        .pipe(gulp.dest('dist/'))
}

exports.default = build

