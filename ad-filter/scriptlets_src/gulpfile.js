const gulp = require('gulp');
var del = require('del');
const fs = require('fs');

const splitFile = require('split-file');

// uglify-es
const uglifyjs = require('uglify-es');
const composer = require('gulp-uglify/composer');
const pump = require('pump');
const uglifyES = composer(uglifyjs, console);

const file = 'scriptlets.js';
const dist = '../src/main/js/scriptlets/';

gulp.task('uglify', function (cb) {
    var options = {
        toplevel: true,
        compress: {
            inline: false,
            keep_fargs: true,
            keep_classnames: true,
            keep_fnames: true
        },
        mangle: false
    };

    pump([
        gulp.src(file),
        uglifyES(options),
        gulp.dest(dist)
    ],
        cb
    );
});

// split into 3 files
gulp.task('split', function (done) {
    const distFile = dist + file;
    splitFile.splitFile(distFile, 3)
        .then((names) => {
            for (let i = 0; i < names.length; i++) {
                let file = names[i];
                let newName = file.replace(/.sf-part\d$/g, '-' + (i + 1));
                fs.renameSync(file, newName);
            }
            del.sync([distFile], { force: true });
        }).then(done);
});

gulp.task('default', gulp.series('uglify', 'split'));