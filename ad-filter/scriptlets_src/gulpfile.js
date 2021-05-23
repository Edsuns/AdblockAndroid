const gulp = require('gulp');

// uglify-es
const uglifyjs = require('uglify-es');
const composer = require('gulp-uglify/composer');
const pump = require('pump');
const uglifyES = composer(uglifyjs, console);

const file = 'scriptlets.js';
const dist = '../src/main/js/';

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

gulp.task('default', gulp.series('uglify'));