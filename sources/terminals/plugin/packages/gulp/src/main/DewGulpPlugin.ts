import * as through from 'through2'
import * as gutil from 'gulp-util'
import {checkAndReplace2} from "@dew/plugin-kernel/dist/main/DewPlugin";

const PluginError = gutil.PluginError;

function checkAndReplace(): string {
    return through.obj(function (file, enc, cb) {
        if (file === null) {
            cb(null, file);
        }
        // TODO
        checkAndReplace2('')
        cb(null, file);

    });
}
