package com.orbitalhq.queryService.security.authorisation

import com.winterbe.expekt.should
import com.orbitalhq.auth.authorisation.VyneUserRoleDefinitionFileRepository
import com.orbitalhq.security.VyneGrantedAuthority
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
      adminRoleDefinition!!.grantedAuthorities.should.equal(
         VyneGrantedAuthority.values().toSet()
      )

      queryRunnerRoleDefinition!!.grantedAuthorities.should.equal(
         setOf(VyneGrantedAuthority.RunQuery)
      )

      viewerRoleDefinition!!.grantedAuthorities.should.equal(
         setOf(VyneGrantedAuthority.BrowseCatalog)
      )

      platformManager!!.grantedAuthorities.should.equal(
         setOf(
            VyneGrantedAuthority.BrowseSchema,
            VyneGrantedAuthority.EditSchema,
            VyneGrantedAuthority.CancelQuery,
            VyneGrantedAuthority.ViewHistoricQueryResults,
            VyneGrantedAuthority.ViewQueryHistory,
            VyneGrantedAuthority.ViewCaskDefinitions,
            VyneGrantedAuthority.EditCaskDefinitions,
            VyneGrantedAuthority.ViewPipelines,
            VyneGrantedAuthority.EditPipelines,
            VyneGrantedAuthority.ViewAuthenticationTokens,
            VyneGrantedAuthority.EditAuthenticationTokens,
            VyneGrantedAuthority.ViewConnections,
            VyneGrantedAuthority.EditConnections
         )
      )

      setOf("Viewer").should.equal(repo.defaultUserRoles().roles)
      setOf("QueryRunner").should.equal(repo.defaultApiClientUserRoles().roles)
   }
}
