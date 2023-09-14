package com.orbitalhq.cockpit.core.auth

import com.orbitalhq.auth.authentication.UserOrbitalId
import com.orbitalhq.auth.authentication.Workspace
import com.orbitalhq.auth.authentication.WorkspaceMember
import com.orbitalhq.auth.authentication.WorkspaceRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

// Note: Currenlty the flyway migrations live with the entity,
// not the repository. Not sure if this is right
interface WorkspaceRepository : JpaRepository<Workspace, Long>

interface WorkspaceMembershipRepository : JpaRepository<WorkspaceMember, Long> {

    @Query("SELECT wm.workspace as workspace, wm.roles as roles FROM WorkspaceMember wm WHERE wm.user.id = :userId")
    fun findAllByUserId(userId: UserOrbitalId): List<WorkspaceMembershipDto>
    fun findAllByWorkspaceId(workspaceId: Long): List<WorkspaceMember>
    fun findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId: Long, userId: UserOrbitalId): WorkspaceMember?
}

interface WorkspaceMembershipDto {
    val workspace: Workspace
    val roles: Set<WorkspaceRole>
}
