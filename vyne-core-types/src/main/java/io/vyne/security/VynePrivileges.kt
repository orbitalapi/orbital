package io.vyne.security


object VynePrivileges {
   const val RunQuery = "RUN_QUERY"
   const val CancelQuery = "CANCEL_QUERY"
   const val ViewQueryHistory = "VIEW_HISTORIC_QUERY_LIST"
   const val ViewHistoricQueryResults = "VIEW_HISTORIC_QUERY_RESULT"
   const val BrowseCatalog = "BROWSE_CATALOG"
   const val BrowseSchema = "BROWSE_SCHEMA"
   const val EditSchema = "EDIT_SCHEMA"
   const val ViewCaskDefinitions = "VIEW_CASK_DEFINITIONS"
   const val EditCaskDefinitions = "EDIT_CASK_DEFINITIONS"
   const val ViewPipelines = "VIEW_PIPELINES"
   const val EditPipelines = "EDIT_PIPELINES"
   const val ViewAuthenticationTokens = "VIEW_AUTHENTICATION_TOKENS"
   const val EditAuthenticationTokens = "EDIT_AUTHENTICATION_TOKENS"
   const val ViewConnections = "VIEW_CONNECTIONS"
   const val EditConnections = "EDIT_CONNECTIONS"
   const val ViewUsers = "VIEW_USERS"
   const val EditUsers = "EDIT_USERS"
}

enum class VyneGrantedAuthorities(val constantValue: String) {
   RunQuery(VynePrivileges.RunQuery),
   CancelQuery(VynePrivileges.CancelQuery),
   ViewQueryHistory(VynePrivileges.ViewQueryHistory),
   ViewHistoricQueryResults(VynePrivileges.ViewHistoricQueryResults),
   BrowseCatalog(VynePrivileges.BrowseCatalog),
   BrowseSchema(VynePrivileges.BrowseSchema),
   EditSchema(VynePrivileges.EditSchema),
   ViewCaskDefinitions(VynePrivileges.ViewCaskDefinitions),
   EditCaskDefinitions(VynePrivileges.EditCaskDefinitions),
   ViewPipelines(VynePrivileges.ViewPipelines),
   EditPipelines(VynePrivileges.EditPipelines),
   ViewAuthenticationTokens(VynePrivileges.ViewAuthenticationTokens),
   EditAuthenticationTokens(VynePrivileges.EditAuthenticationTokens),
   ViewConnections(VynePrivileges.ViewConnections),
   EditConnections(VynePrivileges.EditConnections),
   ViewUsers(VynePrivileges.ViewUsers),
   EditUsers(VynePrivileges.EditUsers)
}
