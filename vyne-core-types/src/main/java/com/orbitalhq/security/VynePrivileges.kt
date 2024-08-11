@file:Suppress("ConstPropertyName")

package com.orbitalhq.security

import mu.KotlinLogging


/**
 * Resources:
 *  - Query
 *    - save
 *    - load
 *    - run
 *    - cancel
 *  - Schema
 *  - Catalog
 *  - Workspace
 *    - create
 *    - view
 *    - modifyMembership
 *  - Connections
 */

typealias GrantedAuthorityName = String

object VynePrivileges {
   @Deprecated("Use BROWSE_SCHEMA instead.", ReplaceWith("BROWSE_SCHEMA"))
   const val BrowseCatalog = "BROWSE_CATALOG"
   const val BrowseSchema = "BROWSE_SCHEMA"
   const val CancelQuery = "CANCEL_QUERY"
   const val CreateWorkspace = "CREATE_WORKSPACE"
   const val EditAuthenticationTokens = "EDIT_AUTHENTICATION_TOKENS"
   const val EditCaskDefinitions = "EDIT_CASK_DEFINITIONS"
   const val EditConnections = "EDIT_CONNECTIONS"
   const val EditPipelines = "EDIT_PIPELINES"
   const val EditSchema = "EDIT_SCHEMA"
   const val EditUsers = "EDIT_USERS"
   const val ModifyWorkspaceMembership = "MODIFY_WORKSPACE_MEMBERSHIP"
   const val ModifyLicense = "MODIFY_LICENSE"
   const val RunQuery = "RUN_QUERY"
   const val TestConnections = "TEST_CONNECTIONS"
   const val ViewActiveQueries = "VIEW_ACTIVE_QUERIES"
   const val ViewAuthenticationTokens = "VIEW_AUTHENTICATION_TOKENS"
   @Deprecated("Casks are no longer a thing")
   const val ViewCaskDefinitions = "VIEW_CASK_DEFINITIONS"
   const val ViewChangelog = "VIEW_CHANGELOG"
   const val ViewConnections = "VIEW_CONNECTIONS"
   const val ViewHistoricQueryResults = "VIEW_HISTORIC_QUERY_RESULT"
   const val ViewLoaderStatus = "VIEW_LOADER_STATUS"
   const val ViewMetrics = "VIEW_METRICS"
   const val ViewPipelines = "VIEW_PIPELINES"
   const val ViewQueryHistory = "VIEW_HISTORIC_QUERY_LIST"
   const val ViewUsers = "VIEW_USERS"
   const val ViewWorkspaces = "VIEW_WORKSPACES"

}


enum class VyneGrantedAuthority(val constantValue: GrantedAuthorityName) {
   @Deprecated("Use BrowseSchema instead.", ReplaceWith("BrowseSchema"))
   BrowseCatalog(VynePrivileges.BrowseCatalog),
   BrowseSchema(VynePrivileges.BrowseSchema),
   CancelQuery(VynePrivileges.CancelQuery),
   CreateWorkspace(VynePrivileges.CreateWorkspace),
   EditAuthenticationTokens(VynePrivileges.EditAuthenticationTokens),
   EditCaskDefinitions(VynePrivileges.EditCaskDefinitions),
   EditConnections(VynePrivileges.EditConnections),
   EditPipelines(VynePrivileges.EditPipelines),
   EditSchema(VynePrivileges.EditSchema),
   EditUsers(VynePrivileges.EditUsers),
   ModifyWorkspaceMembership(VynePrivileges.ModifyWorkspaceMembership),
   ModifyLicense(VynePrivileges.ModifyLicense),
   RunQuery(VynePrivileges.RunQuery),
   TestConnections(VynePrivileges.TestConnections),
   ViewActiveQueries(VynePrivileges.ViewActiveQueries),
   ViewAuthenticationTokens(VynePrivileges.ViewAuthenticationTokens),
   ViewCaskDefinitions(VynePrivileges.ViewCaskDefinitions),
   ViewChangelog(VynePrivileges.ViewChangelog),
   ViewConnections(VynePrivileges.ViewConnections),
   ViewHistoricQueryResults(VynePrivileges.ViewHistoricQueryResults),
   ViewLoaderStatus(VynePrivileges.ViewLoaderStatus),
   ViewMetrics(VynePrivileges.ViewMetrics),
   ViewPipelines(VynePrivileges.ViewPipelines),
   ViewQueryHistory(VynePrivileges.ViewQueryHistory),
   ViewUsers(VynePrivileges.ViewUsers),
   ViewWorkspaces(VynePrivileges.ViewWorkspaces);

   companion object {
      private val byConstant = values().associateBy { it.constantValue }
      private val logger = KotlinLogging.logger {}
      fun from(authorities: Collection<String>): List<VyneGrantedAuthority> = authorities.mapNotNull { authority ->
         if (!byConstant.containsKey(authority)) {
            logger.warn { "There is no authority defined named $authority" }
         }
         byConstant[authority]
      }
   }
}
