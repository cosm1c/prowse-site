global.requirejs = require("requirejs");

requirejs.config({
    nodeRequire: require,
    baseUrl: __dirname,

    paths: {
        'chai': ['../lib/chai/chai']
    }
});

global.assert = require("assert");
