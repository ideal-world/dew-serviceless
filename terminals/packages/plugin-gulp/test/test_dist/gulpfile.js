"use strict";
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
const gulp = require("gulp");
const { series, parallel } = require('gulp');
const browserify = require("browserify");
const babelify = require('babelify');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const uglify = require("gulp-uglify");
const rm = require('rimraf');
const tsify = require("tsify");
let ts = require("gulp-typescript");
let tsProject = ts.createProject('tsconfig.test.json');
const _clean = (done) => {
    rm('./test_dist', error => {
        if (error)
            throw error;
        done();
    });
};
const _ts = (done) => {
    tsProject.src().pipe(tsProject()).js.pipe(gulp.dest('./test_dist'));
    done();
};
function _jvmBuild() {
    return browserify({
        basedir: '.',
        debug: false,
        entries: ['test_dist/actions/TodoAction1.test.ts', 'test_dist/actions/TodoAction2.test.ts'],
        extensions: ['.js', '.jsx', 'tsx', '.json'],
        standalone: "JVM"
    })
        .plugin(tsify)
        .transform(babelify, {
        presets: ['@babel/preset-env', '@babel/preset-react'],
        plugins: [
            '@babel/plugin-transform-runtime',
            ['@babel/plugin-proposal-decorators', { 'legacy': true }],
            ['@babel/plugin-proposal-class-properties', { 'loose': true }]
        ]
    })
        .bundle()
        .pipe(source('JVM.js'))
        .pipe(buffer())
        .pipe(uglify())
        .pipe(gulp.dest('./test_dist'));
}
module.exports = {
    test: series(_clean, _ts),
};
