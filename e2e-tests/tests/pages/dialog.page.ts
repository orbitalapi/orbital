import { Page } from '@playwright/test';
import { clickButton, fillValue, openDropdown, selectDropdownOption } from '../helpers/ui';

export class DialogPage {
   constructor(private page: Page, private selector = 'tui-dialog') {
   }

   async openDropdown(text: string): Promise<void> {
      await openDropdown(this.page, text, this.selector);
   }

   async selectDropdownOption(text: string): Promise<void> {
      await selectDropdownOption(this.page, text);
   }

   async clickButton(text: string): Promise<void> {
      await clickButton(this.page, text, this.selector);
   }

   async fillValue(label: string, value: string): Promise<void> {
      await fillValue(this.page, label, value, this.selector);
   }
}
