package com.orbitalhq.cockpit.core.auth

import com.orbitalhq.auth.authentication.*
import com.orbitalhq.cockpit.core.NotAuthorizedException
import com.orbitalhq.cockpit.core.security.VyneUserJpaRepository
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.http.NotFoundException
import com.orbitalhq.spring.http.badRequest
import jakarta.validation.constraints.NotEmpty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import javax.validation.Valid

@RestController
class WorkspaceService(
    private val userRepository: VyneUserJpaRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceMembershipRepository: WorkspaceMembershipRepository,
    private val memberRepository: OrganisationMemberRepository,
    private val organisationRepo: OrganisationRepository,
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }


    data class CreateWorkspaceRequest(
        @NotEmpty
        val workspaceName: String,
        val organisationId: Long
    )

    private fun requireHasRoleInOrg(userId: UserOrbitalId, organisationId: Long, role: OrganisationRole) {
        val membership = memberRepository.findByOrganisationIdAndUserId(organisationId, userId)
            ?: throw NotAuthorizedException("User is not a member of the organisation")
        if (!membership.roles.contains(role)) {
            throw NotAuthorizedException("User does not have sufficient privileges within the organisation to perform this action")
        }
    }

    @PostMapping("/api/{organisationId}/workspaces")
    @PreAuthorize("hasAuthority('${VynePrivileges.CreateWorkspace}')")
    suspend fun createWorkspace(
        @AuthenticationPrincipal auth: Mono<Authentication>,
        @RequestBody @Valid request: CreateWorkspaceRequest
    ): Workspace = withContext(Dispatchers.IO) {
        val authentication = auth.requireIsAuthenticated()
        logger.info { "User ${authentication.name} is creating a workspace '${request.workspaceName}' in org ${request.organisationId}" }
        doCreateWorkspace(authentication, request)
    }

    internal fun doCreateWorkspace(
        authentication: Authentication,
        request: CreateWorkspaceRequest
    ): Workspace {
        logger.info { "User ${authentication.name} is creating a workspace '${request.workspaceName}' in org ${request.organisationId}" }
        requireHasRoleInOrg(authentication.name, request.organisationId, OrganisationRoles.ADMIN)


        val organisation =
            organisationRepo.findByIdOrNull(request.organisationId) ?: badRequest("Organisation not found")
        val workspace = workspaceRepository.save(
            Workspace(0, request.workspaceName, organisation)
        )
        logger.info { "Workspace ${workspace.id} with name ${workspace.name} created" }
        val user = userRepository.findByIdOrNull(authentication.name)
            ?: throw NotFoundException("No user ${authentication.name} found")
        val adminMember = workspaceMembershipRepository.save(
            WorkspaceMember(
                0,
                user,
                workspace,
                setOf(WorkspaceRoles.ADMIN)
            )
        )
        logger.info { "User ${authentication.name} successfully made admin of new workspace ${workspace.name}" }
        return workspace
    }

    data class MembershipRequest(
        val memberToAdd: UserOrbitalId,
        val roles: Set<WorkspaceRole>
    )

    @PreAuthorize("hasAuthority('${VynePrivileges.ModifyWorkspaceMembership}')")
    @PostMapping("/api/{organisationId}/workspaces/{workspaceId}/members")
    suspend fun addMemberToWorkspace(
        @AuthenticationPrincipal auth: Mono<Authentication>,
        @PathVariable("workspaceId") workspaceId: Long,
        @RequestBody request: MembershipRequest
    ): WorkspaceMember = withContext(Dispatchers.IO) {
        val authentication = auth.requireIsAuthenticated()
        doAddMemberToWorkspace(authentication, workspaceId, request)
    }

    internal fun doAddMemberToWorkspace(
        authentication: Authentication,
        workspaceId: Long,
        request: MembershipRequest
    ): WorkspaceMember {
        ensureUserIdWorkspaceAdmin(workspaceId, authentication.name)
        val workspace = workspaceRepository.findByIdOrNull(workspaceId)
            ?: throw NotFoundException("Workspace $workspaceId does not exist")
        val newUser =
            userRepository.findByIdOrNull(request.memberToAdd)
                ?: throw NotFoundException("User $request.memberToAdd was not found")
        return workspaceMembershipRepository.save(
            WorkspaceMember(
                0,
                newUser,
                workspace, request.roles
            )
        )
    }

    private fun ensureUserIdWorkspaceAdmin(
        workspaceId: Long,
        requestingUser: UserOrbitalId
    ) {
        val member =
            workspaceMembershipRepository.findWorkspaceMemberByWorkspaceIdAndUserId(
                workspaceId,
                requestingUser
            )
                ?: throw NotAuthorizedException("You must be a member of the workspace to perform this action")
        if (!member.hasRole(WorkspaceRoles.ADMIN)) {
            throw NotAuthorizedException("You must be an admin of the workspace to perform this action")
        }
    }

    @GetMapping("/api/{organisationId}/workspaces/{workspaceId}/members")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewWorkspaces}')")
    suspend fun getWorkspaceMembers(
        @AuthenticationPrincipal auth: Mono<Authentication>,
        @PathVariable("workspaceId") workspaceId: Long
    ): List<WorkspaceMember> = withContext(Dispatchers.IO) {
        val authentication = auth.requireIsAuthenticated()
        doGetWorkspaceMembers(authentication, workspaceId)
    }

    internal fun doGetWorkspaceMembers(authentication: Authentication, workspaceId: Long): List<WorkspaceMember> {
        ensureUserIdWorkspaceAdmin(workspaceId, authentication.name)
        return workspaceMembershipRepository.findAllByWorkspaceId(workspaceId)
    }

    @GetMapping("/api/{organisationId}/workspaces")
    suspend fun getWorkspacesForUser(
        @AuthenticationPrincipal auth: Mono<Authentication>,
    ): List<WorkspaceMembershipDto> = withContext(Dispatchers.IO) {
        val userId = auth.requireIsAuthenticated().name
        doGetWorkspacesForUser(userId)
    }

    internal fun doGetWorkspacesForUser(userId: UserOrbitalId) = workspaceMembershipRepository.findAllByUserId(userId)

}

suspend fun Mono<Authentication>.requireIsAuthenticated(): Authentication {
    return this.awaitSingleOrNull() ?: throw NotAuthorizedException()
}
