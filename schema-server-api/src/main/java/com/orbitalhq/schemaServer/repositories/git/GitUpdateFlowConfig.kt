package com.orbitalhq.schemaServer.repositories.git

import com.orbitalhq.schemaServer.repositories.git.GitHostingProvider

data class GitUpdateFlowConfig(
   val branchPrefix: String = "schema-updates/",
   val hostingProvider: GitHostingProvider = GitHostingProvider.Github
)
