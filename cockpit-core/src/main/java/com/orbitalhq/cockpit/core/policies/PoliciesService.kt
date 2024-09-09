package com.orbitalhq.cockpit.core.policies

import com.orbitalhq.VersionedSource
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.schemas.taxi.toVyneSources
import com.orbitalhq.schemas.toVyneQualifiedName
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.NotFoundException
import lang.taxi.policies.Policy
import lang.taxi.services.OperationScope
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PoliciesService(
   private val schemaProvider: SchemaProvider,
) {
   @GetMapping("/api/policies")
   @PreAuthorize("hasAuthority('${VynePrivileges.BrowseSchema}')")
   suspend fun getPolicies(): List<PolicyDto> {
      return schemaProvider.schema.policies.map { policy ->
         PolicyDto.fromPolicy(policy)
      }
   }
   @GetMapping("/api/policies/{policyName}")
   @PreAuthorize("hasAuthority('${VynePrivileges.BrowseSchema}')")
   suspend fun getPolicy(@PathVariable("policyName") policyName: String): PolicyDto    {
      val policy = schemaProvider.schema.policies.firstOrNull { it.qualifiedName == policyName } ?: throw NotFoundException("No policy $policyName was found")
      return PolicyDto.fromPolicy(policy)
   }

}

data class PolicyDto(
   val name: QualifiedName,
   val targetType: QualifiedName,
   val definesReadPolicy: Boolean,
   val definesWritePolicy: Boolean,
   val sourceFile: VersionedSource
) {
   companion object {
      fun fromPolicy(policy: Policy): PolicyDto {
         return PolicyDto(
            policy.toQualifiedName().toVyneQualifiedName(),
            policy.targetType.toVyneQualifiedName(),
            policy.rules.any { it.operationScope == OperationScope.READ_ONLY },
            policy.rules.any { it.operationScope == OperationScope.MUTATION },
            policy.compilationUnits.toVyneSources().singleOrNull()
               ?: error("Expected a single source for a policy, but found multiple")
         )
      }
   }
}
