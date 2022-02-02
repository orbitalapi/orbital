package io.vyne.queryService.security.authorisation

import com.winterbe.expekt.should
import io.vyne.security.VyneGrantedAuthorities
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
         VyneGrantedAuthorities.values().toSet()
      )

      queryRunnerRoleDefinition!!.grantedAuthorities.should.equal(
         setOf(VyneGrantedAuthorities.RunQuery)
      )

      viewerRoleDefinition!!.grantedAuthorities.should.equal(
         setOf(VyneGrantedAuthorities.BrowseCatalog)
      )

      platformManager!!.grantedAuthorities.should.equal(
         setOf(
            VyneGrantedAuthorities.BrowseSchema,
            VyneGrantedAuthorities.EditSchema,
            VyneGrantedAuthorities.CancelQuery,
            VyneGrantedAuthorities.ViewHistoricQueryResults,
            VyneGrantedAuthorities.ViewQueryHistory,
            VyneGrantedAuthorities.ViewCaskDefinitions,
            VyneGrantedAuthorities.EditCaskDefinitions,
            VyneGrantedAuthorities.ViewPipelines,
            VyneGrantedAuthorities.EditPipelines,
            VyneGrantedAuthorities.ViewAuthenticationTokens,
            VyneGrantedAuthorities.EditAuthenticationTokens,
            VyneGrantedAuthorities.ViewConnections,
            VyneGrantedAuthorities.EditConnections
         )
      )
   }
}
