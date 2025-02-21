package io.orbital.station

import com.orbitalhq.cockpit.core.ConfigService
import com.winterbe.expekt.should
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [OrbitalStationApp::class])
@SpringBootTest(

    useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "vyne.db.enabled=false",
        "vyne.search.directory=./search/\${random.int}"
    ]
)
class DblessStartupTest {
    @Autowired
    private lateinit var configService: ConfigService

    @Test
    fun `Orbital starts up in dbless mode`() {
        configService.getConfig().orbitalDbEnabled.should.equals(false)

    }
}