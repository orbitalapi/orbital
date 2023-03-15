import { expect, Page } from '@playwright/test';
import { clickButton, selectTab, waitFor } from '../helpers/ui';

export class QueryEditorPage {
   constructor(private page: Page) {
   }

   async goto(): Promise<void> {
      await this.page.goto('/query/editor');
   }

   async runQuery(query: string): Promise<void> {
      const monacoEditor = this.page.locator('.monaco-editor').nth(0);
      await monacoEditor.click();
      await this.page.keyboard.press('Meta+KeyA');
      await this.page.keyboard.insertText(query);
      await this.page.waitForTimeout(1000); // No idea why it doesn't work without this..
      await clickButton(this.page, 'Run');
   }

   async selectResultTab(tab: string): Promise<void> {
      await selectTab(this.page, tab);
   }

   async expectTableHeaders(labels: string[], timeout?: number): Promise<void> {
      for (const label of labels) {
         await expect(this.page.locator(`app-results-table .ag-header-cell-text:has-text("${label}")`)).toBeVisible({
            timeout
         });
      }
   }

   async expectTableRow(index: number, values: string[]): Promise<void> {
      const mappedItems: Map<string, string[]> = new Map<string, string[]>();
      values.forEach(item => {
         if (!mappedItems[item]) {
            mappedItems[item] = [];
         }
         mappedItems[item].push(item);
      });

      for (const value of Array.from(mappedItems.keys())) {
         const values = mappedItems[value];
         if (values.length > 1) {
            await expect(this.page.locator(`app-results-table .ag-row[row-id="${index}"] .ag-cell:has-text("${value}")`).count()).toBe(values.length);
         } else {
            await expect(this.page.locator(`app-results-table .ag-row[row-id="${index}"] .ag-cell:has-text("${value}")`)).toBeVisible();
         }
      }
   }

   async expectHaveText(values: string[]): Promise<void> {
      const fieldValues = await this.page.locator('.field-value').allInnerTexts();
      console.log(fieldValues);
      for(const value of values) {
         console.log(value);
         expect(fieldValues.findIndex(item => item === value)).toBeGreaterThan(-1);
      }
   }

   async expectStreamingQueryIsRunning(): Promise<void> {
      await expect(this.page.locator('button:has-text("Cancel")')).toBeVisible({
         timeout: 2000
      });
   }

   async waitFor(waitForDuration: number = 1000): Promise<void> {
      await waitFor(this.page, waitForDuration);
   }

}
