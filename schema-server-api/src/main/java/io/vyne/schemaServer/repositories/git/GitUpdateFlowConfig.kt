package io.vyne.schemaServer.repositories.git

import io.vyne.schemaServer.repositories.git.GitHostingProvider

data class GitUpdateFlowConfig(
   val branchPrefix: String = "schema-updates/",
   val hostingProvider: GitHostingProvider = GitHostingProvider.Github
)
