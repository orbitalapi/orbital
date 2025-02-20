import { APIRequestContext, expect, Page, test } from '@playwright/test';
import {
   avroGitProjectImportPostPayload, avroLocalDiskProjectImportPostPayload,
   openApiGitProjectImportPostPayload, openApiLocalDiskProjectImportPostPayload,
   taxiGitProjectImportPostPayload,
   taxiLocalDiskProjectImportPostPayload
} from './helpers/mock.payloads';
import ProjectImportPage from './pages/project-import.page';
import path from 'path'
import ProjectListPage from './pages/project-list.page';
import { readFileSync } from 'fs';

const gitTaxiProjectPackageId = 'com.acme:test-repo:0.1.0'
const generalProjectPackageId = 'test:petstore:1.0.0'
// /openApi-test.yaml
const gitProjectUrl = 'https://gitlab.com/vyne/test-project.git'
const localDiskTaxiProjectPackageId = 'demo.vyne:films-demo:0.1.0'
// TODO: this is going to need to be abstracted to a file that lives with the e2e tests I guess...?
const localDiskTaxiPath = 'C:\\Projects\\notional\\demos-old\\films\\taxi\\'
// TODO: not sure what this is gaining us really..., at least it's not attached to my local file system and lives with the code I guess
//const localDiskOpenAPIPath = path.resolve('./resources/openApi-test.yaml')
const localDiskOpenAPIPath = path.resolve('resources/openApi-test.yaml')
const localDiskAvroPath = path.resolve('resources/addressBook.avsc')

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
      })

      test('Add Open Api project', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeGitGeneralProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await addingGenericGitProject(projectImportPage)
         await projectImportPage.setGitProjectName('test-open-api-git-project')
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
         await projectImportPage.setGitProjectName('test-avro-git-project')
         await projectImportPage.selectProjectType('Avro')
         await projectImportPage.fillPathToSpecFile('/addressBook.avsc')
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
      test('create new taxi project via disk projects menu', async ({ page, request }) => {
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await projectImportPage.clickAddLocalDiskButton()
         await projectImportPage.selectProjectType('Taxi')
         await projectImportPage.selectUploadWorkflow('filePath')
         await projectImportPage.fillPathToSpecFile('test3/taxi.conf')
         await projectImportPage.expectProjectPathNotFound();
         await projectImportPage.expectTaxiPathToFailValidation()
         await projectImportPage.fillPathToSpecFile('test3')
         await projectImportPage.expectTaxiPathToBeValid();
         await projectImportPage.expectCreateNewProjectButton();
         await projectImportPage.clickCreateNewProjectButton()
         await projectImportPage.fillPackageId('com.foo', 'create-new-taxi-project-test', '0.1.0')

         let interceptedRequest: any = null;
         await setupPostInterceptor(page, '/api/repositories/file', request => {
            interceptedRequest = request;
         })
         await projectImportPage.clickCreateButton()
         await projectImportPage.applicationShell.expectConfigurationErrorsBannerNotVisible()
         await projectImportPage.expectSchemaUpdateNotification()

         const expectedPayload = {
            'loader': { 'packageType': 'Taxi' },
            'isEditable': true,
            'newProjectIdentifier': {
               'name': 'create-new-taxi-project-test',
               'organisation': 'com.foo',
               'version': '0.1.0',
               'id': null,
               'unversionedId': null
            },
            'path': 'test3'
         }
         verifyPostPayload(interceptedRequest, expectedPayload)

         // Verify the path appears correctly
         const projectListPage = await ProjectListPage.create(page)
         await projectListPage.navigate()
         await projectListPage.selectProjectByProjectId('create-new-taxi-project-test')
         await projectListPage.clickSettingsTab();

         // can't test the full path, as we don't know the root directory.
         // But, the full path should be what's shown here.
         // expect(projectListPage.readPathField()).toContain('workspace/test3')
      })
      test('Import taxi project already on server', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeLocalDiskTaxiProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await projectImportPage.clickAddLocalDiskButton()
         await projectImportPage.selectProjectType('Taxi')
         await projectImportPage.selectUploadWorkflow('filePath')
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

      test('Upload OpenAPI spec', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeLocalDiskGeneralProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await projectImportPage.clickAddLocalDiskButton()
         await projectImportPage.selectProjectType('OpenAPI')

         await projectImportPage.setFileToUpload(localDiskOpenAPIPath)
         const [organisation, name, version] = generalProjectPackageId.split(':');
         await projectImportPage.fillPackageId(organisation, name, version)
         let interceptedRequest: any = null;
         await setupPostInterceptor(page, '/api/workspace/projects/test:petstore:1.0.0?format=OpenApi&defaultNamespace=test.petstore', request => {
            interceptedRequest = request;
         })
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectSchemaUpdateNotification()
         const openApiSpec = readFileSync(localDiskOpenAPIPath).toString('utf8')
         verifyPostedString(interceptedRequest, openApiSpec)
      })

      test('Upload Avro spec', async ({ page, request }) => {
         // TODO: only need to delete project for initial local testing, shouldn't be required if we're running in CI
         await removeLocalDiskGeneralProjectIfNeeded(request)
         const projectImportPage = await ProjectImportPage.createAndGoto(page)
         await projectImportPage.clickAddLocalDiskButton()
         await projectImportPage.selectProjectType('Avro')
         await projectImportPage.setFileToUpload(localDiskAvroPath)
         // await projectImportPage.fillPathToSpecFile(localDiskAvroPath)
         const [organisation, name, version] = generalProjectPackageId.split(':');
         await projectImportPage.fillPackageId(organisation, name, version)
         let interceptedRequest: any = null;
         await setupPostInterceptor(page, '/api/workspace/projects/test:petstore:1.0.0?format=Avro', request => {
            interceptedRequest = request;
         })
         await projectImportPage.clickCreateButton()
         await projectImportPage.expectSchemaUpdateNotification()
         const avroSpec = readFileSync(localDiskAvroPath).toString('utf8')
         verifyPostedString(interceptedRequest, avroSpec)
      })
   })
})

const removeGitTaxiProjectIfNeeded = async (request: APIRequestContext) => {
   // delete the taxi project
   const taxiProject = await request.get(`/api/packages/${gitTaxiProjectPackageId}`)
   if (taxiProject.status() === 200) {
      const response = await request.delete(`/api/packages/${gitTaxiProjectPackageId}`)
      expect(response.status()).toBe(200);
   }
}

const removeGitGeneralProjectIfNeeded = async (request: APIRequestContext) => {
   // delete the openapi/avro project
   const openApiProject = await request.get(`/api/packages/${generalProjectPackageId}`)
   if (openApiProject.status() === 200) {
      const response = await request.delete(`/api/packages/${generalProjectPackageId}`)
      expect(response.status()).toBe(200);
   }
}

const removeLocalDiskTaxiProjectIfNeeded = async (request: APIRequestContext) => {
   // delete the taxi project
   const taxiProject = await request.get(`/api/packages/${localDiskTaxiProjectPackageId}`)
   if (taxiProject.status() === 200) {
      const response = await request.delete(`/api/packages/${localDiskTaxiProjectPackageId}`)
      expect(response.status()).toBe(200);
   }
}

const removeLocalDiskGeneralProjectIfNeeded = async (request: APIRequestContext) => {
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

const verifyPostedString = (interceptedRequest, payloadString) => {
   expect(interceptedRequest).not.toBeNull();
   if (interceptedRequest) {
      const postData = interceptedRequest.postData();
      console.log(postData)
      expect(postData).toBe(payloadString);
   }
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

