package com.orbitalhq.cockpit.core.auth

import com.orbitalhq.auth.authentication.Organisation
import com.orbitalhq.auth.authentication.OrganisationRoles
import com.orbitalhq.auth.authentication.UserOrbitalId
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.cockpit.core.DatabaseTest
import com.orbitalhq.cockpit.core.NotAuthorizedException
import com.orbitalhq.cockpit.core.security.VyneUserJpaRepository
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional

@ContextConfiguration(
    classes = [OrganisationServiceTest.Companion.Config::class],

    )
class OrganisationServiceTest : DatabaseTest() {


    companion object {

        @EnableAutoConfiguration(exclude = [JpaRepositoriesAutoConfiguration::class])
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

    @Autowired
    lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setup() {
        service = OrganisationService(
            organisationRepo,
            membershipRepo,
            userRepo
        )
    }


    @Test
    fun `can create a new organisation`():Unit = runBlocking{

        val user = user("marty")

        val organisation = service.doCreateOrganisation(
            authForUserId(user.id),
            "Test"
        )

        organisation.id.shouldNotBe(0L)
        organisationRepo.findById(organisation.id).shouldNotBeNull()
        membershipRepo.findAllByOrganisationId(organisation.id).shouldHaveSize(1)
    }

    @Test
    fun `an organisation admin can add a user to an existing org`(): Unit = runBlocking {
        val orgOwner = user("marty")
        val member = user("jimmy")
        val organisation = service.doCreateOrganisation(authForUserId(orgOwner.id), "test")

        val membership = service.doModifyOrganisationMembership(
            authForUserId(orgOwner.id),
            organisation.id,
            OrganisationService.MembershipRequest(
                member.id,
                setOf(OrganisationRoles.MEMBER)
            )
        )
        membershipRepo.findByOrganisationIdAndUserId(organisation.id, member.id).shouldNotBeNull()
    }

    @Test
    fun `a member cannot add other members`(): Unit = runBlocking {
        val orgOwner = user("marty")
        val member = user("jimmy")
        val anotherMember = user("jack")
        val organisation = service.doCreateOrganisation(authForUserId(orgOwner.id), "test")

        service.doModifyOrganisationMembership(
            authForUserId(orgOwner.id),
            organisation.id,
            OrganisationService.MembershipRequest(
                member.id,
                setOf(OrganisationRoles.MEMBER)
            )
        )

        assertThrows<NotAuthorizedException> {
            service.doModifyOrganisationMembership(
                authForUserId(member.id),
                organisation.id,
                OrganisationService.MembershipRequest(
                    anotherMember.id,
                    setOf(OrganisationRoles.MEMBER)
                )
            )
        }

    }


    private fun user(userId: UserOrbitalId): VyneUser {
        val user = userRepo.save(
            VyneUser(
                id = userId,
                issuer = "google",
                username = userId,
                email = "$userId@foo.com"
            )
        )
        userRepo.flush()
        return user
    }
}
