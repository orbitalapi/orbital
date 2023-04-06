package io.vyne.cockpit.core.security.authorisation.rest

import io.vyne.auth.authorisation.VyneUserAuthorisationRole
import io.vyne.auth.authorisation.VyneUserRoleDefinitionRepository
import io.vyne.auth.authorisation.VyneUserRoleMappingRepository
import io.vyne.auth.authorisation.VyneUserRoles
import io.vyne.security.VynePrivileges
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class UserRoleMappingController(
   private val vyneUserRoleMappingRepository: VyneUserRoleMappingRepository,
   private val vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository
) {

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewUsers}')")
   // Note re produces:  Without this, for some reason (in tests at least), we're sending cbor. Adding the media type sends JSON.
   @GetMapping("/api/user/roles", produces = [MediaType.APPLICATION_JSON_VALUE])
   fun getUserRoleDefinitions(): Mono<MutableList<VyneUserAuthorisationRole>> {
      /**
       * From Spring Docs:
       * By default both Jackson2Encoder and Jackson2Decoder do not support elements of type String.
       * Instead the default assumption is that a string or a sequence of strings represent serialized JSON content.
       * If what you need is to render a JSON array from Flux<String>, use Flux#collectToList() and encode a Mono<List<String>>.
       */
      val roles = vyneUserRoleDefinitionRepository.findAll().keys.sorted()
      return Flux.fromIterable(roles).collectList()

   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewUsers}')")
   // Note re produces:  Without this, for some reason (in tests at least), we're sending cbor. Adding the media type sends JSON.
   @GetMapping("/api/user/roles/{username}",  produces = [MediaType.APPLICATION_JSON_VALUE])
   fun getRolesForUser(@PathVariable username: String): Mono<MutableList<VyneUserAuthorisationRole>> {
      val resultFlux = vyneUserRoleMappingRepository.findByUserName(username)?.let {
         Flux.fromIterable(it.roles.sorted())
      } ?: Flux.empty()
      return resultFlux.collectList()
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditUsers}')")
   // Note re produces:  Without this, for some reason (in tests at least), we're sending cbor. Adding the media type sends JSON.
   @PostMapping("/api/user/roles/{username}",  produces = [MediaType.APPLICATION_JSON_VALUE])
   fun updateRolesForUser(@PathVariable username: String, @RequestBody userRoles: Set<VyneUserAuthorisationRole>):
      Mono<MutableList<VyneUserAuthorisationRole>> {
      val updatedRoles = vyneUserRoleMappingRepository.save(username, VyneUserRoles(userRoles))
      return Flux.fromIterable(updatedRoles.roles.sorted()).collectList()
   }
}

