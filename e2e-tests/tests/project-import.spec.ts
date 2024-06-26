import { APIRequestContext, expect, Page, test } from '@playwright/test';
import {
   avroGitProjectImportPostPayload, avroLocalDiskProjectImportPostPayload,
   openApiGitProjectImportPostPayload, openApiLocalDiskProjectImportPostPayload,
   taxiGitProjectImportPostPayload,
   taxiLocalDiskProjectImportPostPayload
} from './helpers/mock.payloads';
import ProjectImportPage from './pages/project-import.page';
import path from 'path'
const gitTaxiProjectPackageId = 'com.acme:test-repo:0.1.0'
const generalProjectPackageId = 'test:petstore:1.0.0'
const gitProjectUrl ='https://gitlab.com/vyne/test-project.git'
const localDiskTaxiProjectPackageId = 'demo.vyne:films-demo:0.1.0'
// TODO: this is going to need to be abstracted to a file that lives with the e2e tests I guess...?
const localDiskTaxiPath = 'C:\\Projects\\notional\\demos-old\\films\\taxi\\'
// TODO: not sure what this is gaining us really..., at least it's not attached to my local file system and lives with the code I guess
//const localDiskOpenAPIPath = path.resolve('./resources/openApi-test.yaml')
const localDiskOpenAPIPath = path.resolve('C:\\Users\\Jason\\Downloads\\openApi-test.yaml')
const localDiskAvroPath = path.resolve('C:\\Users\\Jason\\Downloads\\addressBook.avsc')

test.describe('Project import', () => {
   test.describe('Git repository', () => {
      test('Add Taxi project', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeGitTaxiProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await addingGenericGitProject(projectImportPage, true)
         await projectImportPage.selectProjectType('Taxi')
         let interceptedRequest: any = null;
         await setupPostInterceptor(page, '/api/repositories/git', request => {
            interceptedRequest = request;
         })
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectSchemaUpdateNotification()
         verifyPostPayload(interceptedRequest, taxiGitProjectImportPostPayload)

         // Test for project already created
         await addingGenericGitProject(projectImportPage)
         await projectImportPage.selectProjectType('Taxi')
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectProjectAlreadyImportedError()
      })

      test('Add Open Api project', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeGitGeneralProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await addingGenericGitProject(projectImportPage)
         await projectImportPage.selectProjectType('OpenAPI')
         await projectImportPage.fillPathToSpecFile('/openApi-test.yaml')
         const [organisation, name, version] = generalProjectPackageId.split(':');
         await projectImportPage.fillPackageId(organisation, name, version)
         // TODO: there's an issue where git repo's without a valid OpenAPI spec are still imported, this should be a stop the world event right?
         let interceptedRequest: any = null;
         await setupPostInterceptor(page, '/api/repositories/git', request => {
            interceptedRequest = request;
         })
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectSchemaUpdateNotification()
         verifyPostPayload(interceptedRequest, openApiGitProjectImportPostPayload)
      })

      test('Add Avro project', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeGitGeneralProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await addingGenericGitProject(projectImportPage)
         await projectImportPage.selectProjectType('Avro')
         await projectImportPage.fillPathToSpecFile('/addressBook.avs')
         const [organisation, name, version] = generalProjectPackageId.split(':');
         await projectImportPage.fillPackageId(organisation, name, version)
         let interceptedRequest: any = null;
         await setupPostInterceptor(page, '/api/repositories/git', request => {
            interceptedRequest = request;
         })
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectSchemaUpdateNotification()
         verifyPostPayload(interceptedRequest, avroGitProjectImportPostPayload)
      })
   })

   test.describe('Local disk', () => {
      test('Add Taxi project', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeLocalDiskTaxiProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await projectImportPage.clickAddLocalDiskButton()
         await projectImportPage.selectProjectType('Taxi')
         await projectImportPage.fillPathToSpecFile(localDiskTaxiPath)
         await projectImportPage.expectProjectPathFound();
         let interceptedRequest: any = null;
         await setupPostInterceptor(page, '/api/repositories/file', request => {
            interceptedRequest = request;
         })
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectSchemaUpdateNotification()
         verifyPostPayload(interceptedRequest, taxiLocalDiskProjectImportPostPayload)

         // Test for project already created
         await projectImportPage.clickAddLocalDiskButton()
         await projectImportPage.selectProjectType('Taxi')
         await projectImportPage.fillPathToSpecFile(localDiskTaxiPath)
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectProjectAlreadyImportedError()
      })

      test('Add OpenAPI project', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeLocalDiskGeneralProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await projectImportPage.clickAddLocalDiskButton()
         await projectImportPage.selectProjectType('OpenAPI')
         await projectImportPage.fillPathToSpecFile(localDiskOpenAPIPath)
         const [organisation, name, version] = generalProjectPackageId.split(':');
         await projectImportPage.fillPackageId(organisation, name, version)
         let interceptedRequest: any = null;
         await setupPostInterceptor(page, '/api/repositories/file', request => {
            interceptedRequest = request;
         })
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectSchemaUpdateNotification()
         verifyPostPayload(interceptedRequest, openApiLocalDiskProjectImportPostPayload)
      })

      test('Add Avro project', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeLocalDiskGeneralProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await projectImportPage.clickAddLocalDiskButton()
         await projectImportPage.selectProjectType('Avro')
         await projectImportPage.fillPathToSpecFile(localDiskAvroPath)
         const [organisation, name, version] = generalProjectPackageId.split(':');
         await projectImportPage.fillPackageId(organisation, name, version)
         let interceptedRequest: any = null;
         await setupPostInterceptor(page, '/api/repositories/file', request => {
            interceptedRequest = request;
         })
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectSchemaUpdateNotification()
         verifyPostPayload(interceptedRequest, avroLocalDiskProjectImportPostPayload)
      })
   })
})

const removeGitTaxiProjectIfNeeded = async (request: APIRequestContext)=> {
   // delete the taxi project
   const taxiProject = await request.get(`/api/packages/${gitTaxiProjectPackageId}`)
   if (taxiProject.status() === 200) {
      const response = await request.delete(`/api/packages/${gitTaxiProjectPackageId}`)
      expect(response.status()).toBe(200);
   }
}

const removeGitGeneralProjectIfNeeded = async (request: APIRequestContext)=> {
   // delete the openapi/avro project
   const openApiProject = await request.get(`/api/packages/${generalProjectPackageId}`)
   if (openApiProject.status() === 200) {
      const response = await request.delete(`/api/packages/${generalProjectPackageId}`)
      expect(response.status()).toBe(200);
   }
}

const removeLocalDiskTaxiProjectIfNeeded = async (request: APIRequestContext)=> {
   // delete the taxi project
   const taxiProject = await request.get(`/api/packages/${localDiskTaxiProjectPackageId}`)
   if (taxiProject.status() === 200) {
      const response = await request.delete(`/api/packages/${localDiskTaxiProjectPackageId}`)
      expect(response.status()).toBe(200);
   }
}

const removeLocalDiskGeneralProjectIfNeeded = async (request: APIRequestContext)=> {
   // delete the openapi/avro project
   const openApiProject = await request.get(`/api/packages/${generalProjectPackageId}`)
   if (openApiProject.status() === 200) {
      const response = await request.delete(`/api/packages/${generalProjectPackageId}`)
      expect(response.status()).toBe(200);
   }
}

const addingGenericGitProject = async (page: ProjectImportPage, testConnectionFailure?: boolean) => {
   await page.clickAddGitRepoButton()
   if (testConnectionFailure) {
      await page.fillRepositoryUrl('https://gitlab.com/vyne/dud-project.git')
      await page.clickTestConnectionButton()
      await page.expectConnectionFailure()
   }
   await page.fillRepositoryUrl(gitProjectUrl)
   await page.clickTestConnectionButton()
   await page.expectConnectionSuccess()
   await page.selectBranch(0, 3)
   // TODO: what's the go with the "Path to taxi project" input @Marty?
   //       Should we use the same treatment as we do when saving an endpoint, and put the "/" as a prefix?
}

const setupPostInterceptor = async (page: Page, apiPath: string, callback: (request: any) => void) => {
   // Intercept POST requests to the specific apiPath so we can check the payload is correct
   await page.route(apiPath, route => {
      const request = route.request();
      if (request.method() === 'POST') {
         callback(request);
      }
      route.continue();
   });
}

const verifyPostPayload = (interceptedRequest, payload) => {
   expect(interceptedRequest).not.toBeNull();
   if (interceptedRequest) {
      const postData = interceptedRequest.postData();
      console.log(postData)
      const expectedPayload = JSON.stringify(payload);
      expect(postData).toBe(expectedPayload);
   }
}

