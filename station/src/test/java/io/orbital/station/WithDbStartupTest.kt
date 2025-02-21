package io.orbital.station

import com.orbitalhq.cockpit.core.ConfigService
import com.winterbe.expekt.should
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [OrbitalStationApp::class])
@SpringBootTest(

    useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "vyne.search.directory=./search/\${random.int}"
    ]
)
class WithDbStartupTest {
    @Autowired
    private lateinit var configService: ConfigService

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer<Nothing>("postgres:11.1")
    }

    @Test
    fun `Orbital starts up in DB enabled mode`() {
        configService.getConfig().orbitalDbEnabled.should.equals(true)

    }
}