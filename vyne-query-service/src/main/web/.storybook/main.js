const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin');

module.exports = {
  webpackFinal: async (config, { configType }) => {
    // `configType` has a value of 'DEVELOPMENT' or 'PRODUCTION'
    // You can change the configuration based on that.
    // 'PRODUCTION' is used when building the static version of storybook.

    return {
      ...config,
      resolve: {
        ...config.resolve,
        alias: {
          ...config.resolve.alias,
          'vscode': require.resolve('@codingame/monaco-languageclient/lib/vscode-compatibility'),
          'src/environments/environment': require.resolve('../src/environments/environment.dev.ts')
        }
      }
    }
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
