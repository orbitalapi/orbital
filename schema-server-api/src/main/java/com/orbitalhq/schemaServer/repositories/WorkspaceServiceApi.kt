package com.orbitalhq.schemaServer.repositories

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.packages.PackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec

data class FileProjectStoreTestRequest(
   val path: String
)

data class FileProjectTestResponse(
   val path: String,
   val exists: Boolean,
   val identifier: PackageIdentifier?
)

data class GitConnectionTestRequest(
   val uri: String,
)

data class GitConnectionTestResult(
   val successful: Boolean,
   val errorMessage: String?,
   val branchNames: List<String>?,
   val defaultBranch: String?

)

data class AddFileProjectRequest(
   val path: String,
   val isEditable: Boolean,
   val loader: PackageLoaderSpec = TaxiPackageLoaderSpec,

   /**
    * If populated, indicates that the path is not
    * expected to contain a project, and a new project will be created
    * using the provided package identifier.
    *
    * If empty, it is expected that a project already
    * exists at the provided path, and the packageIdentifier should
    * be read from the provided location.
    *
    * Only relevant for Taxi projects
    */
   val newProjectIdentifier: PackageIdentifier? = null
)

