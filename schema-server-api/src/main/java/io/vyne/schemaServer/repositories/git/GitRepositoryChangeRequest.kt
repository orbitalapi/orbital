package io.vyne.schemaServer.repositories.git

import io.vyne.schemaServer.packages.PackageLoaderSpec
import io.vyne.schemaServer.packages.TaxiPackageLoaderSpec

data class GitRepositoryChangeRequest(
   val name: String,
   val uri: String,
   val branch: String,

   val projectRootPath: String = "/",
   val pullRequestConfig: GitUpdateFlowConfig? = null,
   val isEditable: Boolean = pullRequestConfig != null,
   val loader: PackageLoaderSpec = TaxiPackageLoaderSpec
)
