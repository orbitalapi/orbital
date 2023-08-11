package io.vyne.cockpit.core.auth

import io.vyne.auth.authentication.*
import io.vyne.cockpit.core.NotAuthorizedException
import io.vyne.cockpit.core.security.VyneUserJpaRepository
import io.vyne.security.VynePrivileges
import io.vyne.spring.http.NotFoundException
import jakarta.validation.constraints.NotEmpty
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
    private val workspaceMembershipRepository: WorkspaceMembershipRepository
) {

    data class CreateWorkspaceRequest(
        @NotEmpty
        val workspaceName: String
    )

    @PostMapping("/api/{organisationId}/workspaces")
    @PreAuthorize("hasAuthority('${VynePrivileges.CreateWorkspace}')")
    fun createWorkspace(
        @AuthenticationPrincipal auth: Mono<Authentication>,
        @RequestBody @Valid request: CreateWorkspaceRequest
    ): Mono<Workspace> {
        return auth.ifAuthenticated { auth ->
            createWorkspace(
                request.workspaceName,
                auth.name
            )
        }
    }

    fun createWorkspace(
        name: String,
        requestingUser: UserOrbitalId,
    ): Workspace {
        val workspace = workspaceRepository.save(
            Workspace(0, name)
        )
        val user = userRepository.findByIdOrNull(requestingUser)
            ?: throw NotFoundException("No user $requestingUser found")
        val adminMember = workspaceMembershipRepository.save(
            WorkspaceMember(
                0,
                user,
                workspace,
                setOf(WorkspaceRoles.ADMIN)
            )
        )
        return workspace
    }

    data class MembershipRequest(
        val memberToAdd: UserOrbitalId,
        val roles: Set<WorkspaceRole>
    )

    @PreAuthorize("hasAuthority('${VynePrivileges.ModifyWorkspaceMembership}')")
    @PostMapping("/api/{organisationId}/workspaces/{workspaceId}/members")
    fun addMemberToWorkspace(
        @AuthenticationPrincipal requestingUser: UserDetails,
        @PathVariable("workspaceId") workspaceId: Long,
        @RequestBody request: MembershipRequest
    ): WorkspaceMember {
        ensureUserIdWorkspaceAdmin(workspaceId, requestingUser)
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
        requestingUser: UserDetails
    ) {
        val member =
            workspaceMembershipRepository.findWorkspaceMemberByWorkspaceIdAndUserId(
                workspaceId,
                requestingUser.username
            )
                ?: throw NotAuthorizedException("You must be a member of the workspace to perform this action")
        if (!member.hasRole(WorkspaceRoles.ADMIN)) {
            throw NotAuthorizedException("You must be an admin of the workspace to perform this action")
        }
    }

    @GetMapping("/api/{organisationId}/workspaces/{workspaceId}/members")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewWorkspaces}')")
    fun getWorkspaceMembers(
        @AuthenticationPrincipal requestingUser: UserDetails,
        @PathVariable("workspaceId") workspaceId: Long
    ): List<WorkspaceMember> {
        ensureUserIdWorkspaceAdmin(workspaceId, requestingUser)
        return workspaceMembershipRepository.findAllByWorkspaceId(workspaceId)
    }

    @GetMapping("/api/{organisationId}/workspaces")
    fun getWorkspacesForUser(
        @AuthenticationPrincipal auth: Mono<Authentication>,
    ): Mono<List<WorkspaceMembershipDto>> {
        return auth
            .ifAuthenticated { auth -> getWorkspacesForUser(auth.name) }
    }

    fun getWorkspacesForUser(
        userId: UserOrbitalId
    ): List<WorkspaceMembershipDto> {
        return workspaceMembershipRepository.findAllByUserId(userId)
    }
}


fun <R> Mono<Authentication>.ifAuthenticated(mapper: (Authentication) -> R): Mono<R> {
    return this
        .switchIfEmpty { throw NotAuthorizedException() }
        .map(mapper)
}
