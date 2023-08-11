package io.vyne.auth.authentication

import io.hypersistence.utils.hibernate.type.array.ListArrayType
import org.hibernate.annotations.Type;
import jakarta.persistence.*

/**
 * A team is a top-level entity, like an Organisation.
 * (Choosing the name Team, to feel less enterprise heavy)
 *
 * A user may belong to multiple teams.
 *
 * A workspace belongs to at most one Organisation.
 *
 * Not sure if this makes sense in a self-hosted environment.
 * We get organisation management from PropelAuth (invitations, etc), and don't want to
 * have to rebuild all those features ourselves.
 *
 * Therefore, we should allow a user to belong to zero organisations,
 * to defer having to build Org management on-prem.
 */
@Entity
data class Organisation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(nullable = false)
    val name: String,

    /**
     * The ID of this organisation as defined in an external
     * IDP.
     *
     * Design choice: We're either using PropelAuth for IDP (cloud)
     * or Keycloak on-prem, where an organisation is the equivalent of a realm.
     * Therefore, there should always be an id, which we should receive when the user signs in.
     */
    @Column(nullable = false)
    val idpId: String
)

@Entity
data class OrganisationMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: VyneUser,

    @Column(name = "org_id")
    val organisationId: Long,


    @Type(ListArrayType::class)
    @Column(name = "org_roles", columnDefinition = "text[]")
    val roles: Set<OrganisationRole>
)

typealias OrganisationRole = String

object OrganisationRoles {
    const val ADMIN: OrganisationRole = "ADMIN"
    const val MEMBER: OrganisationRole = "MEMBER"
}

