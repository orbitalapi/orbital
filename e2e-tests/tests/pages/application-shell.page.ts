import { expect, Page } from '@playwright/test';

export class ApplicationShellPage {
   constructor(private page: Page) {
   }

   protected configurationProblemsBanner = () => this.page.getByText('of your projects have configuration problems See details');

   async expectConfigurationErrorsBannerNotVisible() {
      await expect(this.configurationProblemsBanner()).not.toBeAttached()
   }
}
