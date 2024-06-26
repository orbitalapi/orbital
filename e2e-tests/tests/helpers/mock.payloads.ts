// type GitProjectStoreChangeRequest, use this when the e2e-tests folder is rehouses at orbital-ui/e2e
export const taxiGitProjectImportPostPayload = {
   "pullRequestConfig": {
      "branchPrefix": "schema-updates/",
      "hostingProvider": "Github"
   },
   "isEditable": false,
   "path": "/",
   "loader": {
      "packageType": "Taxi"
   },
   "uri": "https://gitlab.com/vyne/test-project.git",
   "name": "test-project",
   "branch": "main"
}

// type GitProjectStoreChangeRequest, use this when the e2e-tests folder is rehouses at orbital-ui/e2e
export const openApiGitProjectImportPostPayload = {
   'pullRequestConfig': {
      'branchPrefix': 'schema-updates/',
      'hostingProvider': 'Github'
   },
   'isEditable': false,
   'path': '/openApi-test.yaml',
   'loader': {
      'packageType': 'OpenApi',
      'identifier': {
         'name': 'petstore',
         'organisation': 'test',
         'version': '1.0.0',
         'id': null,
         'unversionedId': null
      },
      'defaultNamespace': 'test.petstore'
   },
   'uri': 'https://gitlab.com/vyne/test-project.git',
   'name': 'test-project',
   'branch': 'main'
}

// type GitProjectStoreChangeRequest, use this when the e2e-tests folder is rehouses at orbital-ui/e2e
export const avroGitProjectImportPostPayload = {
   "pullRequestConfig": {
      "branchPrefix": "schema-updates/",
      "hostingProvider": "Github"
   },
   "isEditable": false,
   "path": "/addressBook.avs",
   "loader": {
      "packageType": "Avro",
      "identifier": {
         "name": "petstore",
         "organisation": "test",
         "version": "1.0.0",
         "id": null,
         "unversionedId": null
      }
   },
   "uri": "https://gitlab.com/vyne/test-project.git",
   "name": "test-project",
   "branch": "main"
}


// type CreateFileProjectStoreRequest, use this when the e2e-tests folder is rehouses at orbital-ui/e2e
export const taxiLocalDiskProjectImportPostPayload = {
   "loader": {
      "packageType": "Taxi"
   },
   "isEditable": true,
   "newProjectIdentifier": null,
   "path": "C:\\Projects\\notional\\demos-old\\films\\taxi\\"
}

// type CreateFileProjectStoreRequest, use this when the e2e-tests folder is rehouses at orbital-ui/e2e
export const openApiLocalDiskProjectImportPostPayload = {
   'loader': {
      'packageType': 'OpenApi',
      'identifier': {
         'name': 'petstore',
         'organisation': 'test',
         'version': '1.0.0',
         'id': null,
         'unversionedId': null
      },
      'defaultNamespace': 'test.petstore'
   },
   'isEditable': true,
   'newProjectIdentifier': null,
   'path': 'C:\\Users\\Jason\\Downloads\\openApi-test.yaml'
}

// type CreateFileProjectStoreRequest, use this when the e2e-tests folder is rehouses at orbital-ui/e2e
export const avroLocalDiskProjectImportPostPayload = {
   'loader': {
      'packageType': 'Avro',
      'identifier': {
         'name': 'petstore',
         'organisation': 'test',
         'version': '1.0.0',
         'id': null,
         'unversionedId': null
      }
   },
   'isEditable': true,
   'newProjectIdentifier': null,
   'path': 'C:\\Users\\Jason\\Downloads\\addressBook.avsc'
}
