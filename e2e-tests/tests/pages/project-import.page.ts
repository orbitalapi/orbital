import { expect, Page } from '@playwright/test';

export default class ProjectImportPage {
   // Locators
   private addGitRepoButton = () => this.page.locator('tui-island').filter({ hasText: 'Git Repository Connect' }).getByRole('button');
   private addLocalDiskButton = () => this.page.locator('tui-island').filter({ hasText: 'Local diskRead projects' }).getByRole('button');
   private repositoryUrl = () => this.page.getByLabel('Repository URL');
   private testConnectionButton = () => this.page.getByRole('button', { name: 'Test connection' });
   private connectionTestedSuccessfullyNotification = () => this.page.getByText('Connection tested successfully');
   private connectionAuthenticationFailureNotification = () => this.page.getByText('Authentication failed');
   private projectPathFoundNotification = () => this.page.locator('tui-notification').filter({ hasText: 'Great - we\'ve found project' });
   private branchDropdown = () => this.page.getByLabel('Branch');
   private branchDropdownOptions = () => this.page.getByRole('listbox').locator('button');
   private branchDropdownOption = (index: number) => this.page.getByRole('listbox').locator('button').locator(`nth=${index}`);
   private projectTypeDropdown = () => this.page.getByLabel('Project type');
   private projectTypeDropdownOption = (type: string) => this.page.getByRole('listbox').locator('button').getByText(type);
   private createButton = () => this.page.getByRole('button', { name: 'Create' });
   private schemaUpdateNotification = () => this.page.getByText('Schema has been updated');
   private projectAlreadyImportedErrorNotification = () => this.page.getByText('A git repository with the name test-project already exists');
   private specPath = () => this.page.getByLabel('Path', { exact: true });
   private packageIdOrganisation = () => this.page.getByLabel('Organisation');
   private packageIdName = () => this.page.getByLabel('Name', { exact: true });
   private packageIdVersion = () => this.page.getByLabel('Version');

   private constructor(private page: Page) {
   }

   public static async createAndGoto(page: Page): Promise<ProjectImportPage> {
      const projectImportPage = new ProjectImportPage(page);
      await page.goto('/')
      await page.waitForResponse('https://signin.orbitalhq.dev/oauth2/token')
      await projectImportPage.goto();
      return projectImportPage;
   }

   async goto(): Promise<void> {
      await this.page.goto('/projects/project-import');
   }

   // Actions
   async clickAddGitRepoButton(): Promise<void> {
      await this.addGitRepoButton().click();
   }

   async clickAddLocalDiskButton(): Promise<void> {
      await this.addLocalDiskButton().click();
   }

   async fillRepositoryUrl(text): Promise<void> {
      await this.repositoryUrl().fill(text)
   }

   async clickTestConnectionButton(): Promise<void> {
      await this.testConnectionButton().click()
   }

   async selectBranch(index: number, numberOfBranchesExpected: number): Promise<void> {
      await this.branchDropdown().click()
      await this.expectNumberOfBranches(numberOfBranchesExpected)
      await this.branchDropdownOption(index).click()
   }

   async selectProjectType(type: string): Promise<void> {
      await this.projectTypeDropdown().click()
      await this.projectTypeDropdownOption(type).click()
   }

   async fillPackageId(organisation: string, name: string, version: string): Promise<void> {
      await this.packageIdOrganisation().fill(organisation)
      await this.packageIdName().fill(name)
      await this.packageIdVersion().fill(version)
   }

   async fillPathToSpecFile(path: string): Promise<void> {
      await this.specPath().fill(path)
   }

   async clickCreateButton(): Promise<void> {
      await this.createButton().click()
   }

   // Assertions
   async expectConnectionSuccess(): Promise<void> {
      await expect(this.connectionTestedSuccessfullyNotification()).toBeVisible()
   }

   async expectConnectionFailure(): Promise<void> {
      await expect(this.connectionAuthenticationFailureNotification()).toBeVisible()
   }

   async expectNumberOfBranches(val: number): Promise<void> {
      await expect(this.branchDropdownOptions()).toHaveCount(val)
   }

   async expectSchemaUpdateNotification(): Promise<void> {
      await expect(this.schemaUpdateNotification()).toBeVisible()
   }

   async expectProjectAlreadyImportedError(): Promise<void> {
      await expect(this.projectAlreadyImportedErrorNotification()).toBeVisible()
   }

   async expectProjectPathFound(): Promise<void> {
      await expect(this.projectPathFoundNotification()).toBeVisible()
   }

}
