module.exports = {
  "stories": [
    "../src/**/*.stories.mdx",
    "../src/app/landing-page/*.stories.@(js|jsx|ts|tsx)",
    "../src/app/expanding-panelset/*.stories.@(js|jsx|ts|tsx)",
    "../src/app/catalog-explorer-panel/*.stories.@(js|jsx|ts|tsx)",
    "../src/app/schema-diagram/*.stories.@(js|jsx|ts|tsx)",
    "../src/app/auth-manager/*.stories.@(js|jsx|ts|tsx)",
    "../src/app/query-panel/*.stories.@(js|jsx|ts|tsx)",
    "../src/app/query-snippet-panel/*.stories.@(js|jsx|ts|tsx)",
  ],
  "addons": [
    "@storybook/addon-links",
    "@storybook/addon-essentials"
  ],
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
          // 'src/environments/environment': require.resolve('../src/environments/environment.dev.ts')
        }
      }
    }
  },
  "framework": "@storybook/angular",
  "core": {
    "builder": "webpack5"
  }
}
