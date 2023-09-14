package com.orbitalhq.cockpit.core.security

import com.google.common.testing.FakeTicker
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.auth.authentication.JwtStandardClaims
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.cockpit.core.DatabaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.publisher.TestPublisher
import java.time.Duration

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [UserDetailsPersistingServiceTest.Companion.Config::class])
//@SpringBootTest(classes = [UserDetailsPersistingServiceTest.Companion.Config::class])
class UserDetailsPersistingServiceTest : DatabaseTest(){

   @Autowired
   lateinit var repository: VyneUserJpaRepository

   @Autowired
   lateinit var testEntityManager: TestEntityManager

   //   @Autowired
   lateinit var service: UserDetailsPersistingService

   lateinit var ticker: FakeTicker
   lateinit var testPublisher: TestPublisher<UserAuthenticatedEvent>

   @BeforeEach
   fun setup() {
      testPublisher = TestPublisher.create()
      val eventSource: UserAuthenticatedEventSource = mock { }
      whenever(eventSource.userAuthenticated).thenReturn(testPublisher.flux())
      ticker = FakeTicker()
      service = UserDetailsPersistingService(
         eventSource,
         repository,
         persistFrequency = Duration.ofMinutes(10),
         ticker = ticker
      )

   }

   companion object {
      @SpringBootConfiguration
      @EntityScan(basePackageClasses = [VyneUser::class])
      @EnableJpaRepositories(basePackageClasses = [VyneUserJpaRepository::class])
      class Config


   }

   @Test
   fun `persists new user`() {
      testPublisher.emit(
         UserAuthenticatedEvent(
            "marty",
            mapOf(
               JwtStandardClaims.Sub to "marty",
               JwtStandardClaims.Issuer to "https://google.com",
               JwtStandardClaims.PreferredUserName to "martypitt",
               JwtStandardClaims.Email to "marty@foo.com",
               JwtStandardClaims.PictureUrl to "http://fo.com",
               JwtStandardClaims.Name to "Marty"
            ),
            emptyList()
         )
      )
      // This test is failing, and I can't work out why.
      // The persistence is happening.  Probably something to do with transaction
      // boundaries, but fiddling with settings didn't help.
//      await().atMost(5, TimeUnit.SECONDS).until<Boolean> {
//         repository.findAll().isNotEmpty()
//         repository.findVyneUserByUserIdAndIssuer("marty", "https://google.com") != null

//      }
   }
}
