import { expect, Page } from '@playwright/test';
import { clickButton, fillValue, openAccordion, selectTreeItem } from '../helpers/ui';

export class SchemaImporterPage {
   constructor(private page: Page) {
   }

   async clickButton(text: string): Promise<void> {
      await clickButton(this.page, text);
   }

   async openMenuSection(label: string): Promise<void> {
      await openAccordion(this.page, label);
   }

   async chooseMenuItem(label: string): Promise<void> {
      await selectTreeItem(this.page, label);
   }

   async selectTypeForParameter(name: string, type: string): Promise<void> {
      await this.page.click(`.parameter-list tr:has-text("${name}") td:nth-child(2) button`);
      await fillValue(this.page, 'Search for a type', 'FilmId');
      await this.page.click(`.search-result:has-text("${type}")`);
   }

   async expectNotification(text: string): Promise<void> {
      await expect(this.page.locator(`tui-notification:has-text("${text}")`)).toBeVisible();
   }
}
