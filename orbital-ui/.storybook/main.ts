import type { StorybookConfig } from '@storybook/angular';

import custom from '../custom-webpack.config.js'; // 👈 Custom Webpack configuration being imported.

const config: StorybookConfig = {
  framework: '@storybook/angular',
  stories: [
    '../src/**/*.mdx',
    '../src/**/*.stories.@(js|jsx|mjs|ts|tsx)'
  ],
  addons: [
    "@storybook/addon-links",
    "@storybook/addon-essentials"
  ],
  features: {
    // Opt out of on-demand story loading, run this to upgrade across to SB v7 fully:
    // https://github.com/storybookjs/storybook/blob/next/MIGRATION.md#storystorev6-and-storiesof-is-deprecated
    storyStoreV7: false,
  },
  webpackFinal: async (config) => {
    return {
      ...config,
      module: { ...config.module, rules: [...config.module.rules, ...custom.module.rules] },
      resolve: {
        ...config.resolve,
        alias: {
          ...config.resolve.alias,
          // 'src/environments/environment': require.resolve('../src/environments/environment.dev.ts')
        }
      }
    };
  },
  docs: {
    autodocs: true
  }
};

export default config;
