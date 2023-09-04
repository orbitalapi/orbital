package com.orbitalhq.auth.authentication

import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.*
import org.hibernate.annotations.Type

/**
 * A workspace is the equivalent of a single Schema Server
 * config.  It combines multiple schema sources (taxi projects, etc)
 * to provide a single Schema.
 *
 * Each workspace emits a single composed schema.
 *
 * A workspace belongs to an organisation,
 * and has several users who have access.
 */
@Entity
data class Workspace(
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   val id: Long,

   @Column(nullable = false)
   val name: String,

   @ManyToOne
   @JoinColumn(name = "organisation_id")
   val organisation: Organisation
)

@Entity
data class WorkspaceMember(
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   val id: Long,

//   @Column(name = "user_id")
//   val userId: UserOrbitalId,

   @ManyToOne
   @JoinColumn(name = "user_id")
   val user: VyneUser,

   @ManyToOne
   @JoinColumn(name = "workspace_id")
   val workspace: Workspace,

   @Enumerated(EnumType.STRING)
   @Type(ListArrayType::class)
   @Column(name = "workspace_roles", columnDefinition = "text[]")
   val roles: Set<WorkspaceRole>
) {
   fun hasRole(role: WorkspaceRole) = roles.contains(role)
}

typealias WorkspaceRole = String

object WorkspaceRoles {
   const val ADMIN: WorkspaceRole = "ADMIN"
   const val MEMBER: WorkspaceRole = "MEMBER"
}
