package io.vyne.cockpit.core.auth

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import io.vyne.auth.authentication.*
import io.vyne.cockpit.core.DatabaseTest
import io.vyne.cockpit.core.NotAuthorizedException
import io.vyne.cockpit.core.security.VyneUserJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [OrganisationServiceTest.Companion.Config::class])
class OrganisationServiceTest : DatabaseTest() {


    companion object {
        @SpringBootConfiguration
        @EntityScan(basePackageClasses = [VyneUser::class, Organisation::class])
        @EnableJpaRepositories(basePackageClasses = [VyneUserJpaRepository::class, OrganisationRepository::class])
        class Config
    }

    @Autowired
    lateinit var userRepo: VyneUserJpaRepository

    @Autowired
    lateinit var organisationRepo: OrganisationRepository

    @Autowired
    lateinit var membershipRepo: OrganisationMemberRepository

    lateinit var service: OrganisationService

    @BeforeEach
    fun setup() {
        service = OrganisationService(
            organisationRepo,
            membershipRepo,
            userRepo
        )
    }


    @Test
    fun `can create a new organisation`() {
        val user = user("marty")

        val organisation = service.createOrganisation(
            "Test",
            userDetails(user.id)
        )

        organisation.id.shouldNotBe(0L)
        organisationRepo.findByIdOrNull(organisation.id).shouldNotBeNull()
        membershipRepo.findAllByOrganisationId(organisation.id).shouldHaveSize(1)
    }

    @Test
    fun `an organisation admin can add a user to an existing org`() {
        val orgOwner = user("marty")
        val member = user("jimmy")
        val organisation = service.createOrganisation("test", userDetails(orgOwner.id))

        val membership = service.modifyOrganisationMembership(
            userDetails(orgOwner.id),
            organisation.id,
            OrganisationService.MembershipRequest(
                member.id,
                setOf(OrganisationRoles.MEMBER)
            )
        )
        membershipRepo.findByOrganisationIdAndUserId(organisation.id, member.id).shouldNotBeNull()
    }

    @Test
    fun `a member cannot add other members`() {
        val orgOwner = user("marty")
        val member = user("jimmy")
        val anotherMember = user("jack")
        val organisation = service.createOrganisation("test", userDetails(orgOwner.id))

        service.modifyOrganisationMembership(
            userDetails(orgOwner.id),
            organisation.id,
            OrganisationService.MembershipRequest(
                member.id,
                setOf(OrganisationRoles.MEMBER)
            )
        )

        assertThrows<NotAuthorizedException> {
            service.modifyOrganisationMembership(
                userDetails(member.id),
                organisation.id,
                OrganisationService.MembershipRequest(
                    anotherMember.id,
                    setOf(OrganisationRoles.MEMBER)
                )
            )
        }

    }


    private fun user(userId: UserOrbitalId): VyneUser {
        return userRepo.save(
            VyneUser(
                id = userId,
                issuer = "google",
                username = userId,
                email = "$userId@foo.com"
            )
        )
    }
}
