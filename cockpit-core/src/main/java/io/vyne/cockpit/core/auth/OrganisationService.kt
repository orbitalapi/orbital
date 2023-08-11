package io.vyne.cockpit.core.auth

import io.vyne.auth.authentication.*
import io.vyne.cockpit.core.NotAuthorizedException
import io.vyne.cockpit.core.security.VyneUserJpaRepository
import io.vyne.spring.http.NotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

class OrganisationService(
    private val organisationRepo: OrganisationRepository,
    private val memberRepository: OrganisationMemberRepository,
    private val userRepository: VyneUserJpaRepository
) {

    fun createOrganisation(
        name: String,
        @AuthenticationPrincipal requestingUser: UserDetails
    ): Organisation {
        val organisation = organisationRepo.save(
            Organisation(
                0, name, "?"
            )
        )
        val user = userRepository.findByIdOrNull(requestingUser.username)
            ?: throw NotFoundException("No user ${requestingUser.username} found")

        val adminUser = memberRepository.save(
            OrganisationMember(
                0,
                user,
                organisation.id,
                setOf(OrganisationRoles.ADMIN)
            )
        )
        return organisation
    }

    @PostMapping("/api/{organisationId}/members")
    fun modifyOrganisationMembership(
        @AuthenticationPrincipal requestingUser: UserDetails,
        @PathVariable("organisationId") organisationId: Long,
        @RequestBody request: MembershipRequest
    ): OrganisationMember {
        ensureUserIdIsOrganisationAdmin(organisationId, requestingUser)
        val newUser =
            userRepository.findByIdOrNull(request.memberToAdd)
                ?: throw NotFoundException("User $request.memberToAdd was not found")
        return memberRepository.save(
            OrganisationMember(
                0,
                newUser,
                organisationId,
                request.roles
            )
        )
    }


    data class MembershipRequest(
        val memberToAdd: UserOrbitalId,
        val roles: Set<OrganisationRole>
    )

    private fun ensureUserIdIsOrganisationAdmin(
        organisationId: Long,
        requestingUser: UserDetails
    ) {
        val member = memberRepository.findByOrganisationIdAndUserId(organisationId, requestingUser.username)
            ?: throw NotAuthorizedException("You must be a member of the organisation to perform this action")
        if (!member.roles.contains(OrganisationRoles.ADMIN)) {
            throw NotAuthorizedException("You must be an admin of the organisation to perform this action")
        }
    }

}
