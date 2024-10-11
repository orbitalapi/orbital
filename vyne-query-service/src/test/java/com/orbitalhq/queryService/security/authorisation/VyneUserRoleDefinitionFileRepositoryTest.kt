package com.orbitalhq.queryService.security.authorisation

import com.winterbe.expekt.should
import com.orbitalhq.auth.authorisation.VyneUserRoleDefinitionFileRepository
import com.orbitalhq.security.VyneGrantedAuthority
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.core.io.ClassPathResource

class VyneUserRoleDefinitionFileRepositoryTest {
   @Test
   fun `can read vyne role definitions from file`() {
      val configFile = ClassPathResource("authorisation/vyne-authorisation-role-definitions.conf").file
      val repo = VyneUserRoleDefinitionFileRepository(configFile.toPath())
      val adminRoleDefinition = repo.findByRoleName("Admin")
      val queryRunnerRoleDefinition = repo.findByRoleName("QueryRunner")
      val viewerRoleDefinition = repo.findByRoleName("Viewer")
      val platformManager = repo.findByRoleName("PlatformManager")
      val expected = setOf(
         VyneGrantedAuthority.RunQuery,
         VyneGrantedAuthority.CancelQuery,
         VyneGrantedAuthority.ViewQueryHistory,
         VyneGrantedAuthority.ViewHistoricQueryResults,
         VyneGrantedAuthority.BrowseSchema,
         VyneGrantedAuthority.EditSchema,
         VyneGrantedAuthority.ViewCaskDefinitions,
         VyneGrantedAuthority.EditCaskDefinitions,
         VyneGrantedAuthority.ViewPipelines,
         VyneGrantedAuthority.EditPipelines,
         VyneGrantedAuthority.ViewAuthenticationTokens,
         VyneGrantedAuthority.EditAuthenticationTokens,
         VyneGrantedAuthority.ViewConnections,
         VyneGrantedAuthority.EditConnections,
         VyneGrantedAuthority.ViewUsers,
         VyneGrantedAuthority.EditUsers,
         VyneGrantedAuthority.CreateWorkspace,
         VyneGrantedAuthority.ViewWorkspaces,
         VyneGrantedAuthority.ModifyWorkspaceMembership,
         VyneGrantedAuthority.ViewMetrics,
         VyneGrantedAuthority.ModifyLicense,
         VyneGrantedAuthority.TestConnections,
         VyneGrantedAuthority.ViewActiveQueries,
         VyneGrantedAuthority.ViewChangelog,
         VyneGrantedAuthority.ViewLoaderStatus
      )
      adminRoleDefinition!!.grantedAuthorities.shouldBe(expected)

      queryRunnerRoleDefinition!!.grantedAuthorities.shouldNotBeEmpty()

      viewerRoleDefinition!!.grantedAuthorities.shouldNotBeEmpty()
      platformManager!!.grantedAuthorities.shouldNotBeEmpty()
   }
}
