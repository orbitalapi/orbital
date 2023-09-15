package com.orbitalhq.cockpit.core.workspaces.repositories

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository

interface WorkspaceSchemaSpecRepository : JpaRepository<WorkspaceSchemaSpec, Long> {
}

@Entity
data class WorkspaceSchemaSpec(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val kind: SchemaSpecKind,

    @Column(nullable = false)
    // It's actually JSON.
    val spec: String
) {
    enum class SchemaSpecKind {
        Git,
        File
    }
}

