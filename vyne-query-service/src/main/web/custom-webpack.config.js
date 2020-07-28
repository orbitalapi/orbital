const webpack = require('webpack');

// Need to 'mock' some server side node librarie due to this problem...
// vscode-jsonrpc is re-exporting net, crypto and some libs not available in the browser....
// See https://github.com/TypeFox/monaco-languageclient/issues/2

module.exports = {
    resolve: {
        alias: {
            'vscode': require.resolve('monaco-languageclient/lib/vscode-compatibility')
        }
    },
    node: {
        "fs": "empty",
        "global": true,
        "crypto": "empty",
        "tls": "empty",
        "net": "empty",
        "process": true,
        "module": false,
        "clearImmediate": false,
        "setImmediate": true
      },
};
