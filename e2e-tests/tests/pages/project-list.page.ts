import { Page } from '@playwright/test';

export default class ProjectListPage {
   public static async create(page: Page): Promise<ProjectListPage> {
      return new ProjectListPage(page)
   }

   private projectSettingsButton = () => this.page.getByRole('button', { name: 'Settings' })
   private projectButtonTitle = (projectId) => this.page.locator('h3').filter({ hasText: projectId })
   private projectPathField = () => this.page.getByLabel('Path');

   public async navigate(): Promise<void> {
      await this.page.goto('/projects')
   }

   private constructor(private page: Page) {
   }

   async selectProjectByProjectId(projectId: string): Promise<void> {
      await this.projectButtonTitle(projectId).click()
   }

   async clickSettingsTab(): Promise<void> {
      await this.projectSettingsButton().click();
   }

   async readPathField(): Promise<string> {
      const textContent = await this.projectPathField().textContent()
      console.log('Project path field', textContent)
      const element = await this.projectPathField().evaluate(el => el.outerHTML)
      console.log('Project path field element: ', element)
      return await this.projectPathField().textContent()
   }

}
