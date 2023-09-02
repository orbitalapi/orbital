package com.orbitalhq.schemaServer.repositories.git

import com.orbitalhq.schemaServer.packages.PackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec

data class GitRepositoryChangeRequest(
   val name: String,
   val uri: String,
   val branch: String,

   val projectRootPath: String = "/",
   val pullRequestConfig: GitUpdateFlowConfig? = null,
   val isEditable: Boolean = pullRequestConfig != null,
   val loader: PackageLoaderSpec = TaxiPackageLoaderSpec
)
