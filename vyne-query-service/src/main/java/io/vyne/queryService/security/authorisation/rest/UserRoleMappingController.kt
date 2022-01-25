package io.vyne.queryService.security.authorisation.rest

import io.vyne.queryService.security.authorisation.VyneUserAuthorisationRole
import io.vyne.queryService.security.authorisation.VyneUserRoleMappingRepository
import io.vyne.queryService.security.authorisation.VyneUserRoles
import io.vyne.security.VynePrivileges
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class UserRoleMappingController(private val vyneUserRoleMappingRepository: VyneUserRoleMappingRepository) {

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewUsers}')")
   @GetMapping("/api/user/roles")
   fun getUserRoleDefinitions(): Flux<VyneUserRoleDto> {
      return Flux.fromIterable(
         VyneUserAuthorisationRole.values().map { role ->
            VyneUserRoleDto(role.name, role.displayValue)
         }
      )
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewUsers}')")
   @GetMapping("/api/user/roles/{username}")
   fun getRolesForUser(@PathVariable username: String): Flux<VyneUserAuthorisationRole> {
      return vyneUserRoleMappingRepository.findByUserName(username)?.let {
         Flux.fromIterable(it.roles)
      } ?: Flux.empty()
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditUsers}')")
   @PostMapping("/api/user/roles/{username}")
   fun updateRolesForUser(@PathVariable username: String, @RequestBody userRoles: Set<VyneUserAuthorisationRole>):
      Flux<VyneUserAuthorisationRole> {
      val updatedRoles = vyneUserRoleMappingRepository.save(username, VyneUserRoles(userRoles))
      return  Flux.fromIterable(updatedRoles.roles)
   }
}

data class VyneUserRoleDto(val name: String, val displayName: String)
