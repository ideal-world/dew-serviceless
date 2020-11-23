import * as through from 'through2'
import * as gutil from 'gulp-util'
import * as dewKernel from '@dew/plugin-kernel'

var PluginError = gutil.PluginError;

function checkAndReplace(): string {
    return through.obj(function (file, enc, cb) {
        if (file === null) {
            cb(null, file);
        }
        // TODO
        dewKernel.checkAndReplace('')
        cb(null, file);

    });
}
