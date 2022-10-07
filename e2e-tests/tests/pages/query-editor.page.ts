import { expect, Page } from '@playwright/test';
import { clickButton, selectTab } from '../helpers/ui';

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
      await this.page.keyboard.type(query, { delay: 10 });
      await this.page.waitForTimeout(1000); // No idea why it doesn't work without this..
      await clickButton(this.page, 'Run');
   }

   async selectResultTab(tab: string): Promise<void> {
      await selectTab(this.page, tab);
   }

   async expectTableHeaders(labels: string[]): Promise<void> {
      for (const label of labels) {
         await expect(this.page.locator(`app-results-table .ag-header-cell-text:has-text("${label}")`)).toBeVisible();
      }
   }

   async expectTableRow(index: number, values: string[]): Promise<void> {
      for (const value of values) {
         await expect(this.page.locator(`app-results-table .ag-row[row-id="${index}"] .ag-cell:has-text("${value}")`)).toBeVisible();
      }
   }
}
