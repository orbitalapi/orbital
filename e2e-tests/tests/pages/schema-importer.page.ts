import { expect, Page } from '@playwright/test';
import { clickButton, fillValue, openAccordion, selectTreeItem, waitFor } from '../helpers/ui';

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

   async selectTypeForAttribute(searchPhrase: string, sourceType: string, type: string): Promise<void> {
      await clickButton(this.page, sourceType);
      await fillValue(this.page, 'Search for a type', `${searchPhrase}`);
      await this.page.click(`.search-result:has-text("${type}")`);
   }

   async waitFor(waitForDuration: number = 1000): Promise<void> {
      await waitFor(this.page, waitForDuration);
   }

   async createNewType(
      sourceType: string,
      newTypeName: string,
      newTypeBaseName: string,
      optionString: string) {
      await clickButton(this.page, sourceType);
      await clickButton(this.page, 'Create new');
      await this.page
         .locator('text=Type nameDefine the name of the type. Must not contain spaces. By convention, ty >> input[type="text"]')
         .fill(newTypeName);
      await this.page
         .locator('input[role="combobox"]')
         .fill(newTypeBaseName);
      await waitFor(this.page, 3000);
      await this.page.locator(`mat-option[role="option"]:has-text("${optionString}")`).click();
      await waitFor(this.page, 3000);
      await clickButton(this.page, 'Create type' );
   }
   async expectNotification(text: string): Promise<void> {
      await expect(this.page.locator(`tui-notification:has-text("${text}")`)).toBeVisible();
   }
}
