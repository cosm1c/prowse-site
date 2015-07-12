// `main.js` is the file that sbt-web will use as an entry point
(function (requirejs) {
    'use strict';

    // -- RequireJS config --
    requirejs.config({
        // Packages = top-level folders; loads a contained file named 'main.js"
        packages: ['ticker'],
        shim: {
            'jsRoutes': {
                deps: [],
                // it's not a RequireJS module, so we have to tell it what var is returned
                exports: 'jsRoutes'
            },
            'bootstrap': ['jquery']
        },
        paths: {
            'requirejs': ['../lib/requirejs/require'],
            'jquery': ['../lib/jquery/jquery'],
            'bootstrap': ['../lib/bootstrap/js/bootstrap'],
            'lodash': ['../lib/lodash/lodash'],
            'jsRoutes': ['/jsroutes']
        }
    });

    requirejs.onError = function (err) {
        console.error(err);
    };

    // Bootstrap webapp
    require(['./app'],
        function () {
            console.info('App Started');
        }
    );
})(requirejs);
