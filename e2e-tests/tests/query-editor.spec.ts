import { expect, Page, test } from '@playwright/test';
import { QueryEditorPage } from './pages/query-editor.page';

test.describe('Copilot', () => {
   test('Copilot panel should display/hide when toggle button clicked', async ({page, request}) => {
      const queryEditorPage = await QueryEditorPage.createAndGoto(page);
      await queryEditorPage.toggleCopilot();
      await queryEditorPage.expectCopilotVisible(true)
      await queryEditorPage.toggleCopilot();
      await queryEditorPage.expectCopilotVisible(false)
      await queryEditorPage.toggleCopilot();
      await queryEditorPage.expectCopilotVisible(true)
   })

   test.skip('Submit query button should be disabled until text is entered', async({page, request}) => {
   })

   test.skip('Query should return results from server', async({page, request}) => {
   })

   test.skip('Results should be able to be set in query editor', async({page, request}) => {
   })

   test.skip('Results should be able to be copied to clipboard', async({page, request}) => {
   })

   test.skip('Results should be able to be run', async({page, request}) => {
   })

   test.skip('Query should be persisted in localStorage', async({page, request}) => {
   })
})


