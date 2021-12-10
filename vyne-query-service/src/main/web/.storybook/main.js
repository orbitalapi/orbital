const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin');

module.exports = {
  webpackFinal: async (config, { configType }) => {
    // `configType` has a value of 'DEVELOPMENT' or 'PRODUCTION'
    // You can change the configuration based on that.
    // 'PRODUCTION' is used when building the static version of storybook.

    const newConfig =  {
      ...config,
      resolve: {
        ...config.resolve,
        alias: {
          ...config.resolve.alias,
          'vscode': require.resolve('@codingame/monaco-languageclient/lib/vscode-compatibility'),
          'src/environments/environment':  require.resolve('../src/environments/environment.dev.ts')
        }
      }
    }
    console.log('================================================================================')
    console.log('================================================================================')
    console.log('================================================================================')
    console.log('================================================================================')
    console.log('Original Config resolve: \n ' + JSON.stringify(config, null, 2));
    console.log('================================================================================')
    console.log('================================================================================')
    console.log('================================================================================')
    console.log('================================================================================')
    console.log('New Config resolve: \n ' + JSON.stringify(newConfig, null, 2));
    console.log('================================================================================')
    console.log('================================================================================')
    console.log('================================================================================')
    console.log('================================================================================')

    return newConfig
  },
  "stories": [
    "../src/**/*.stories.mdx",
    "../src/**/*.stories.@(js|jsx|ts|tsx)"
  ],
  "addons": [
    "@storybook/addon-links",
    "@storybook/addon-essentials"
  ],
  "framework": "@storybook/angular"
}
