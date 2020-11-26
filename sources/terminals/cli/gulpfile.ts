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
