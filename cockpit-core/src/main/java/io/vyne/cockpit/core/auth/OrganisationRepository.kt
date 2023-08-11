package io.vyne.cockpit.core.auth

import io.vyne.auth.authentication.Organisation
import io.vyne.auth.authentication.OrganisationMember
import io.vyne.auth.authentication.UserOrbitalId
import org.springframework.data.jpa.repository.JpaRepository

interface OrganisationRepository : JpaRepository<Organisation, Long> {
}

interface OrganisationMemberRepository : JpaRepository<OrganisationMember, Long> {
    fun findAllByOrganisationId(organisationId: Long):List<OrganisationMember>

    fun findByOrganisationIdAndUserId(organisationId: Long, userOrbitalId: UserOrbitalId):OrganisationMember?
}
