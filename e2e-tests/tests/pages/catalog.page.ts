import { Page } from '@playwright/test';
import { selectTab } from '../helpers/ui';

export default class CatalogPage {
   // Locators

   private constructor(private page: Page) {
   }

   public static async createAndGoto(page: Page): Promise<CatalogPage> {
      const catalogPage = new CatalogPage(page);
      await page.goto('/')
      await page.waitForResponse('https://signin.orbitalhq.dev/oauth2/token')
      await catalogPage.goto();
      return catalogPage;
   }

   async goto(): Promise<void> {
      await this.page.goto('/')
   }

   // Actions
   async selectTab(text: string): Promise<void> {
      await selectTab(this.page, text);
   }

}
