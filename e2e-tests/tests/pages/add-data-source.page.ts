import { Page } from '@playwright/test';
import { clickButton, fillValue, openDropdown, selectDropdownOption, selectOnAutoComplete, selectTab } from '../helpers/ui';

export class AddDataSourcePage {
   constructor(private page: Page) {
   }

   async goto(): Promise<void> {
      await this.page.goto('/schema-explorer/import');
   }

   async selectTab(text: string): Promise<void> {
      await selectTab(this.page, text);
   }

   async openDropdown(text: string): Promise<void> {
      await openDropdown(this.page, text);
   }

   async selectDropdownOption(text: string): Promise<void> {
      await selectDropdownOption(this.page, text);
   }

   async clickButton(text: string): Promise<void> {
      await clickButton(this.page, text);
   }

   async fillValue(label: string, value: string): Promise<void> {
      await fillValue(this.page, label, value);
   }

   async selectOnAutoComplete(label: string, value: string): Promise<void> {
      await selectOnAutoComplete(this.page, label, value);
   }

   // This is only needed due to having two texts "Kafka topic" on the same page
   async setKafkaTopic(value: string): Promise<void> {
      await this.page.waitForTimeout(1000); // Don't ask why this removes some flakiness
      await this.page.locator(`.form-row:has-text("Set the topic for Vyne to consume from") tui-wrapper:has-text("Kafka topic") input`).fill(value);
   }
}
