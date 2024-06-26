import { test as setup, expect } from '@playwright/test';

const authFile = 'playwright/.auth/user.json';

setup('authenticate', async ({ page }) => {
   await page.goto('/');
   await page.waitForURL('https://signin.orbitalhq.dev/**');
   await page.getByRole('textbox', { name: 'name@host.com' }).fill(process.env.USER_NAME)
   await page.getByRole('textbox', { name: 'Password' }).fill(process.env.PASSWORD)
   await page.getByRole('button', { name: 'submit' }).click()
   await page.waitForURL('http://localhost:9022/')
   await expect(page.getByTestId('user-info-dropdown' )).toContainText(process.env.USER_NAME)

   await page.context().storageState({ path: authFile });
});
