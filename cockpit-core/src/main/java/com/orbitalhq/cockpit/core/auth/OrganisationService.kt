package com.orbitalhq.cockpit.core.auth

import com.orbitalhq.auth.authentication.*
import com.orbitalhq.cockpit.core.NotAuthorizedException
import com.orbitalhq.cockpit.core.security.VyneUserJpaRepository
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.http.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactor.core.publisher.Mono

class OrganisationService(
    private val organisationRepo: OrganisationRepository,
    private val memberRepository: OrganisationMemberRepository,
    private val userRepository: VyneUserJpaRepository
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @PostMapping("/api/organizations/{name}")
    suspend fun createOrganisation(
        @AuthenticationPrincipal auth: Mono<Authentication>,
        @PathVariable("name") name: String
    ): Organisation = withContext(Dispatchers.IO) {
        val principal = auth.requireIsAuthenticated()
        doCreateOrganisation(principal, name)
    }

    internal fun doCreateOrganisation(principal: Authentication, name: String): Organisation {
        logger.info { "User ${principal.name} is attempting to create a new organisation named $name" }
        val user =
            userRepository.findByIdOrNull(principal.name) ?: throw NotFoundException("No user ${principal.name} found")
        if (organisationRepo.findByName(name) != null) {
            logger.info { "Organisation $name already exists - rejecting request" }
            throw BadRequestException("Organisation $name already exists")
        }
        val organisation = organisationRepo.save(
            Organisation(
                0, name, "?"
            )
        )
        logger.info { "Created organisation ${organisation.id} with name ${organisation.name} successfully" }
        val membership = memberRepository.save(
            OrganisationMember(
                0,
                user,
                organisation.id,
                setOf(OrganisationRoles.ADMIN)
            )
        )
        logger.info { "Successfully made user ${user.id} an admin of organisation ${organisation.id}" }
        return organisation
    }

    @PostMapping("/api/{organisationId}/members")
    suspend fun modifyOrganisationMembership(
        @AuthenticationPrincipal auth: Mono<Authentication>,
        @PathVariable("organisationId") organisationId: Long,
        @RequestBody request: MembershipRequest
    ): OrganisationMember = withContext(Dispatchers.IO) {

        val requestingUser = auth.requireIsAuthenticated()
        doModifyOrganisationMembership(requestingUser, organisationId, request)
    }

    internal fun doModifyOrganisationMembership(
        requestingUser: Authentication,
        organisationId: Long,
        request: MembershipRequest
    ): OrganisationMember {
        ensureUserIdIsOrganisationAdmin(organisationId, requestingUser.name)
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
        requestingUser: UserOrbitalId
    ) {
        val member = memberRepository.findByOrganisationIdAndUserId(organisationId, requestingUser)
            ?: throw NotAuthorizedException("You must be a member of the organisation to perform this action")
        if (!member.roles.contains(OrganisationRoles.ADMIN)) {
            throw NotAuthorizedException("You must be an admin of the organisation to perform this action")
        }
    }

}
