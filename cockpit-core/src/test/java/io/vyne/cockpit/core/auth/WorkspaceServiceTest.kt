package io.vyne.cockpit.core.auth

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.context.ContextConfiguration


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

   @BeforeEach
   fun setup() {
      workspaceService = WorkspaceService(
         userRepo,
         workspaceRepo,
         workspaceMembershipRepo
      )
      organisationService = OrganisationService(
         organisationRepo,
         membershipRepo,
         userRepo
      )
      testOrganisation = organisationService.createOrganisation("test", user("super-admin"))
   }

   @Test
   fun `can add a user to a workspace using db`() {
      val user = user("marty")

      val workspace = workspaceService.createWorkspace(
         "Test",
         user.id
      )
      workspace.id.shouldNotBe(0)
      workspaceRepo.findAll().shouldHaveSize(1)

      val members = workspaceMembershipRepo.findAllByWorkspaceId(workspace.id)
      members.shouldHaveSize(1)
      val member = members.single()
      member.hasRole(WorkspaceRoles.ADMIN).shouldBeTrue()
      member.user.id.shouldBe(user.id)
   }

   @Test
   fun `a workspace admin can add a user to an existing workspace`() {
      val workspaceOwner = user("marty")
      val member = user("jimmy")
      val workspace = workspaceService.createWorkspace(
         "Test",
         workspaceOwner.id
      )
      val membership = workspaceService.addMemberToWorkspace(
         userDetails(workspaceOwner.id),
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
   fun `cannot add a workspace if the name is not unique`() {

   }

   @Test
   fun `an admin can list members`() {
      val workspaceOwner = user("marty")
      val workspace = workspaceService.createWorkspace(
         "Test",
         workspaceOwner.id
      )
      val members = workspaceService.getWorkspaceMembers(
         userDetails(workspaceOwner.id),
         workspace.id
      )
      members.shouldHaveSize(1)
   }

   @Test
   fun `a member cannot list members`() {
      val workspaceOwner = user("marty")
      val member = user("jimmy")
      val workspace = workspaceService.createWorkspace(
         "Test",
         requestingUser = workspaceOwner.id
      )
      workspaceService.addMemberToWorkspace(
         userDetails(workspaceOwner.id),
         workspace.id,
         WorkspaceService.MembershipRequest(
            member.id,
            setOf(WorkspaceRoles.MEMBER)
         )

      )
      assertThrows<NotAuthorizedException> {
         workspaceService.getWorkspaceMembers(
            userDetails(member.id),
            workspace.id
         )
      }
   }

   @Test
   fun `a user can list the workspaces they belong to`() {
      val workspaceOwner = user("marty")
      val workspace = workspaceService.createWorkspace(
         "Test",
         requestingUser = workspaceOwner.id
      )
      val members = workspaceService.getWorkspacesForUser(workspaceOwner.id)
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

   @Test
   fun `a user who is not a workspace admin cannot add a member`() {
      val workspaceOwner = user("marty")
      val member = user("jimmy")
      val workspace = workspaceService.createWorkspace(
         "Test",
         workspaceOwner.id
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
         workspaceService.addMemberToWorkspace(
            // Member is a member of the workspace, but not
            // an owner, so can't add members
            userDetails(member.id),
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
   fun `a user who is not a workspace member cannot add a member`() {
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
            0, "test"
         )
      )
      assertThrows<NotAuthorizedException> {
         workspaceService.addMemberToWorkspace(
            userDetails(user.id),
            workspace.id,
            WorkspaceService.MembershipRequest(
               "newUser",
               setOf(WorkspaceRoles.MEMBER)
            )
         )

      }

   }


}

fun userDetails(userId: UserOrbitalId): UserDetails {
   return mock<UserDetails> {
      on { username } doReturn userId
   }
}
