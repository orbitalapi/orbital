const path = require('path');

// Export a function. Accept the base config as the only param.
module.exports = {
    webpackFinal: async (config, { configType }) => {
        // `configType` has a value of 'DEVELOPMENT' or 'PRODUCTION'
        // You can change the configuration based on that.
        // 'PRODUCTION' is used when building the static version of storybook.

        // Make whatever fine-grained changes you need

        // Return the altered config
        return {
            ...config, resolve: { ...config.resolve, 
                alias: {
                    ...config.resolve.alias,
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
        }
    },
};