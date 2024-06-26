import { expect, Page, test } from '@playwright/test';
import CatalogPage from './pages/catalog.page';

test.describe('Catalog', () => {
   // First foray here, need to add the full suite as listed in ORB-450
   test('Services diagram', async ({ page, request }) => {
      const catalogPage = await CatalogPage.createAndGoto(page);
      await catalogPage.selectTab('Services diagram')
      await page.waitForTimeout(3000);
   });
});
