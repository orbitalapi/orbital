package com.orbitalhq.cockpit.core.auth

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.auth.authentication.*
import com.orbitalhq.cockpit.core.DatabaseTest
import com.orbitalhq.cockpit.core.NotAuthorizedException
import com.orbitalhq.cockpit.core.security.VyneUserJpaRepository
import io.kotest.common.runBlocking
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono


@ContextConfiguration(classes = [WorkspaceServiceTest.Companion.Config::class])
class WorkspaceServiceTest : DatabaseTest() {

    companion object {
        @SpringBootConfiguration
        @EntityScan(basePackageClasses = [VyneUser::class, Workspace::class])
        @EnableJpaRepositories(basePackageClasses = [VyneUserJpaRepository::class, WorkspaceRepository::class])
        class Config
    }

    @Autowired
    lateinit var userRepo: VyneUserJpaRepository

    @Autowired
    lateinit var workspaceRepo: WorkspaceRepository

    @Autowired
    lateinit var workspaceMembershipRepo: WorkspaceMembershipRepository

    @Autowired
    lateinit var organisationRepo: OrganisationRepository

    @Autowired
    lateinit var membershipRepo: OrganisationMemberRepository

    lateinit var organisationService: OrganisationService


    lateinit var workspaceService: WorkspaceService

    lateinit var testOrganisation: Organisation
    lateinit var testOrgAdmin: VyneUser

    @BeforeEach
    fun setup() {
        workspaceService = WorkspaceService(
            userRepo,
            workspaceRepo,
            workspaceMembershipRepo,
            membershipRepo,
            organisationRepo
        )
        organisationService = OrganisationService(
            organisationRepo,
            membershipRepo,
            userRepo
        )
        createTestOrganisation()
    }

    private fun createTestOrganisation() {
        testOrgAdmin = user("super-admin")
        testOrganisation = organisationService.doCreateOrganisation(authForUserId(testOrgAdmin.id), "test")
    }


    @Test
    fun `can add a user to a workspace using db`() = runBlocking {
        val user = user("marty")

        val workspace = workspaceService.doCreateWorkspace(
            authForUserId(user.id),
            WorkspaceService.CreateWorkspaceRequest(
                "Test",
                testOrganisation.id
            )
        )
        workspace.id.shouldNotBe(0)
        workspaceRepo.findAll().toList().shouldHaveSize(1)

        val members = workspaceMembershipRepo.findAllByWorkspaceId(workspace.id)
        members.shouldHaveSize(1)
        val member = members.single()
        member.hasRole(WorkspaceRoles.ADMIN).shouldBeTrue()
        member.user.id.shouldBe(user.id)
    }

    @Test
    fun `a workspace admin can add a user to an existing workspace`() = runBlocking {
        val workspaceOwner = user("marty")
        val member = user("jimmy")
        val workspace = workspaceService.doCreateWorkspace(
            authForUserId(workspaceOwner.id),
            WorkspaceService.CreateWorkspaceRequest(
                "Test",
                testOrganisation.id
            )
        )
        val membership = workspaceService.doAddMemberToWorkspace(
            authForUserId(workspaceOwner.id),
            workspace.id,
            WorkspaceService.MembershipRequest(
                member.id,
                setOf(WorkspaceRoles.ADMIN)
            )
        )
        membership.id.shouldNotBe(0L)
        membership.hasRole(WorkspaceRoles.ADMIN).shouldBeTrue()
    }

    @Test
    fun `cannot add a workspace if the name is not unique within the organisation`() {

    }

    @Test
    fun `an admin can list members`() = runBlocking {
        val workspaceOwner = user("marty")
        val workspace = workspaceService.doCreateWorkspace(
            authForUserId(workspaceOwner.id),
            WorkspaceService.CreateWorkspaceRequest(
                "Test",
                testOrganisation.id
            )
        )
        val members = workspaceService.doGetWorkspaceMembers(
            authForUserId(workspaceOwner.id),
            workspace.id
        )
        members.shouldHaveSize(1)
    }

    @Test
    fun `a member cannot list members`(): Unit = runBlocking {
        val workspaceOwner = user("marty")
        val member = user("jimmy")
        val workspace = workspaceService.doCreateWorkspace(
            authForUserId(workspaceOwner.id),
            WorkspaceService.CreateWorkspaceRequest(
                "Test",
                testOrganisation.id
            )
        )
        workspaceService.doAddMemberToWorkspace(
            authForUserId(workspaceOwner.id),
            workspace.id,
            WorkspaceService.MembershipRequest(
                member.id,
                setOf(WorkspaceRoles.MEMBER)
            )

        )
        assertThrows<NotAuthorizedException> {
            workspaceService.doGetWorkspaceMembers(
                authForUserId(member.id),
                workspace.id
            )
        }
    }

    @Test
    fun `a user can list the workspaces they belong to`(): Unit = runBlocking {
        val workspaceOwner = user("marty")
        val workspace = workspaceService.doCreateWorkspace(
            authForUserId(workspaceOwner.id),
            WorkspaceService.CreateWorkspaceRequest(
                "Test",
                testOrganisation.id
            )
        )
        val members = workspaceService.doGetWorkspacesForUser(workspaceOwner.id)
        members.shouldHaveSize(1)
        members.single().workspace.name.shouldBe("Test")
        members.single().roles.shouldBe(setOf(WorkspaceRoles.ADMIN))

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

    private fun userInOrg(
        userId: UserOrbitalId,
        organisation: Organisation = testOrganisation,
        roles:Set<OrganisationRole> = setOf(OrganisationRoles.ADMIN)
    ): VyneUser {
        val user = user(userId)
        organisationService.doModifyOrganisationMembership(
            authForUserId(testOrgAdmin.id),
            organisation.id,
            OrganisationService.MembershipRequest(
                user.id,
                roles
            )
        )
        return user
    }

    @Test
    fun `a user who is not a workspace admin cannot add a member`(): Unit = runBlocking {
        val workspaceOwner = userInOrg("marty")
        val member = userInOrg("jimmy")
        val workspace = workspaceService.doCreateWorkspace(
            authForUserId(workspaceOwner.id),
            WorkspaceService.CreateWorkspaceRequest(
                "Test",
                testOrganisation.id
            )
        )
        workspaceMembershipRepo.save(
            WorkspaceMember(
                0,
                member,
                workspace,
                setOf(WorkspaceRoles.MEMBER)
            )
        )
        val exception = assertThrows<NotAuthorizedException> {
            workspaceService.doAddMemberToWorkspace(
                // Member is a member of the workspace, but not
                // an owner, so can't add members
                authForUserId(member.id),
                workspace.id,
                WorkspaceService.MembershipRequest(
                    "newUser",
                    setOf(WorkspaceRoles.MEMBER)
                )
            )

        }
        exception.message.shouldBe("You must be an admin of the workspace to perform this action")
    }

    @Test
    fun `a user who is not a workspace member cannot add a member`(): Unit = runBlocking {
        val user = userRepo.save(
            VyneUser(
                id = "marty",
                issuer = "google",
                username = "marty",
                email = "marty@foo.com"
            )
        )
        val workspace = workspaceRepo.save(
            Workspace(
                0, "test", testOrganisation
            )
        )
        assertThrows<NotAuthorizedException> {
            workspaceService.doAddMemberToWorkspace(
                authForUserId(user.id),
                workspace.id,
                WorkspaceService.MembershipRequest(
                    "newUser",
                    setOf(WorkspaceRoles.MEMBER)
                )
            )

        }

    }


}

fun authForUserId(userId: String): Authentication {
    val auth = mock<Authentication> {
        on { name } doReturn userId
    }
    return auth
}
