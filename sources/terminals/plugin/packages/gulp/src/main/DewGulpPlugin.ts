import * as through from 'through2'
import * as gutil from 'gulp-util'
import {checkAndReplace} from "@dew/plugin-kernel/dist/main/DewPlugin";

const PluginError = gutil.PluginError;
const PLUGIN_NAME = 'Dew-Build';

module.exports = function () {
    return through.obj(function (file, enc, cb) {
        if (file.isBuffer()) {
            file.contents =Buffer.from(checkAndReplace(file.contents.toString('utf8')))
            this.push(file)
            cb()
        }
        if (file.isStream()) {
            this.emit('error', new PluginError(PLUGIN_NAME, 'Streams are not supported!'))
            cb()
        }
    })
}
