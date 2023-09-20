package com.orbitalhq.cockpit.core.auth

import com.orbitalhq.auth.authentication.Organisation
import com.orbitalhq.auth.authentication.OrganisationMember
import com.orbitalhq.auth.authentication.UserOrbitalId
import org.springframework.data.jpa.repository.JpaRepository

interface OrganisationRepository : JpaRepository<Organisation, Long> {
   fun findByName(name: String): Organisation?
}

interface OrganisationMemberRepository : JpaRepository<OrganisationMember, Long> {
   fun findAllByOrganisationId(organisationId: Long): List<OrganisationMember>

   fun findByOrganisationIdAndUserId(organisationId: Long, userOrbitalId: UserOrbitalId): OrganisationMember?

}
