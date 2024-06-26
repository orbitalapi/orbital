import { expect, Page } from '@playwright/test';
import { clickButton, selectTab, waitFor } from '../helpers/ui';

export class QueryEditorPage {
   // Locators
   private copilotPanel = () => this.page.locator('app-copilot-panel');
   private copilotToggleButton = () => this.page.getByRole('button', { name: 'Copilot' });
   private monacoEditor = () => this.page.locator('.monaco-editor').nth(0);
   private tableHeader = (label) => this.page.locator(`app-results-table .ag-header-cell-text:has-text("${label}")`);
   private tableCell = (index, value) => this.page.locator(`app-results-table .ag-row[row-id="${index}"] .ag-cell:has-text("${value}")`);
   private fieldValue = () => this.page.locator('.field-value');
   private streamingQueryCancelButton = () => this.page.getByRole('button',{name: 'Cancel'});

   private constructor(private page: Page) {
   }

   public static async createAndGoto(page: Page): Promise<QueryEditorPage> {
      const queryEditorPage = new QueryEditorPage(page);
      await page.goto('/')
      await page.waitForResponse('https://signin.orbitalhq.dev/oauth2/token')
      await queryEditorPage.goto();
      return queryEditorPage;
   }

   async goto(): Promise<void> {
      await this.page.goto('/')
   }

   // Actions
   async toggleCopilot(): Promise<void> {
      await this.copilotToggleButton().click()
   }

   async expectCopilotVisible(isVisible: boolean): Promise<void> {
      if (isVisible) {
         await expect(this.copilotPanel()).toBeVisible()
      } else {
         await expect(this.copilotPanel()).not.toBeVisible()
      }
   }

   async runQuery(query: string): Promise<void> {
      await this.monacoEditor().click();
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
         await expect(this.tableHeader(label)).toBeVisible({
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
            expect(await this.tableCell(index, value).count()).toBe(values.length);
         } else {
            await expect(this.tableCell(index, value)).toBeVisible();
         }
      }
   }

   async expectHaveText(values: string[]): Promise<void> {
      const fieldValues = await this.fieldValue().allInnerTexts();
      console.log(fieldValues);
      for(const value of values) {
         console.log(value);
         expect(fieldValues.findIndex(item => item === value)).toBeGreaterThan(-1);
      }
   }

   async expectStreamingQueryIsRunning(): Promise<void> {
      await expect(this.streamingQueryCancelButton()).toBeVisible({
         timeout: 2000
      });
   }

   async waitFor(waitForDuration: number = 1000): Promise<void> {
      await waitFor(this.page, waitForDuration);
   }

}
