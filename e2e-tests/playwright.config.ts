import type { PlaywrightTestConfig } from '@playwright/test';
import { devices } from '@playwright/test';

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */

import * as dotenv from 'dotenv';
dotenv.config()

/**
 * See https://playwright.dev/docs/test-configuration.
 */
const config: PlaywrightTestConfig = {
   testDir: './tests',
   /* Maximum time one test can run for. */
   timeout: 60 * 1000,
   expect: {
      /**
       * Maximum time expect() should wait for the condition to be met.
       * For example in `await expect(locator).toHaveText();`
       */
      timeout: 5000
   },
   /* Run tests in files in parallel */
   fullyParallel: true,
   /* Fail the build on CI if you accidentally left test.only in the source code. */
   forbidOnly: !!process.env.CI,
   /* Retry on CI only */
   retries: 0,
   /* Opt out of parallel tests on CI. */
   workers: process.env.CI ? 1 : undefined,
   /* Reporter to use. See https://playwright.dev/docs/test-reporters */
   reporter: 'html',
   /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
   use: {
      /* Maximum time each action such as `click()` can take. Defaults to 0 (no limit). */
      actionTimeout: 0,
      /* Base URL to use in actions like `await page.goto('/')`. */
      baseURL: !!process.env.BASE_URL ? process.env.BASE_URL : 'http://localhost:9022',

      /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
      trace: 'on-first-retry',
      screenshot: 'only-on-failure'
   },

   /* Configure projects for major browsers */
   projects: [
      // Setup project
      { name: 'setup', testMatch: /.*\.setup\.ts/ },
      {
         name: 'chromium',
         dependencies: ['setup'],
         use: {
            ...devices['Desktop Chrome'],
            storageState: 'playwright/.auth/user.json',
            launchOptions: {
               args: ["--start-fullscreen"], // starting the browser in full screen
               slowMo: 500, // a 500 milliseconds pause before each operation. Useful for slow systems.
            },
         }
      }

      // {
      //   name: 'firefox',
      //   dependencies: ['setup'],
      //   use: {
      //     ...devices['Desktop Firefox'],
      //     storageState: 'playwright/.auth/user.json',
      //   },
      // },
      //
      // {
      //   name: 'webkit',
      //   dependencies: ['setup'],
      //   use: {
      //     ...devices['Desktop Safari'],
      //     storageState: 'playwright/.auth/user.json',
      //   },
      // },

      /* Test against mobile viewports. */
      // {
      //   name: 'Mobile Chrome',
      //   dependencies: ['setup'],
      //   use: {
      //     ...devices['Pixel 5'],
      //     storageState: 'playwright/.auth/user.json',
      //   },
      // },
      // {
      //   name: 'Mobile Safari',
      //   dependencies: ['setup'],
      //   use: {
      //     ...devices['iPhone 12'],
      //     storageState: 'playwright/.auth/user.json',
      //   },
      // },

      /* Test against branded browsers. */
      // {
      //   name: 'Microsoft Edge',
      //   dependencies: ['setup'],
      //   use: {
      //     channel: 'msedge',
      //     storageState: 'playwright/.auth/user.json',
      //   },
      // },
      // {
      //   name: 'Google Chrome',
      //   dependencies: ['setup'],
      //   use: {
      //     channel: 'chrome',
      //     storageState: 'playwright/.auth/user.json',
      //   },
      // },
   ],

   /* Folder for test artifacts such as screenshots, videos, traces, etc. */
   outputDir: 'test-results/'

   /* Run your local dev server before starting the tests */
   // webServer: {
   //   command: 'npm run start',
   //   port: 3000,
   // },
};

export default config;
