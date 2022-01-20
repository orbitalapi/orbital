package io.vyne.queryService.schemas.importing.database

//import io.vyne.connectors.jdbc.JdbcDriver
//import io.vyne.connectors.jdbc.JdbcTable
//import io.vyne.connectors.jdbc.NamedTemplateConnection
//import io.vyne.connectors.jdbc.TableTaxiGenerationRequest
//import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
//import io.vyne.queryService.schemas.importing.BaseSchemaConverterServiceTest
//import io.vyne.queryService.schemas.importing.SchemaConversionRequest
//import io.vyne.schemaStore.SimpleSchemaProvider
//import io.vyne.schemas.taxi.TaxiSchema
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.jpa.repository.JpaRepository
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories
//import org.springframework.jdbc.core.JdbcTemplate
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
//import org.springframework.test.context.ContextConfiguration
//import org.springframework.test.context.TestPropertySource
//import org.springframework.test.context.junit4.SpringRunner
//import java.time.Duration
//import javax.persistence.Entity
//import javax.persistence.Id
//import javax.persistence.OneToMany

// I couldn't get this to work, try as I might.
// This test is picking up the config in application-test.yml,
// meaning that we can't inject our own db test config with our own db's.
// Without that, I can't test schema creation here.
// Note that we do test this in JdbcTaxiSchemaGeneratorTest which doesn't have the same issue
// because it's in a seperate jar.


//@SpringBootTest
//@ContextConfiguration(classes = [DbTableSchemaConverterTest.DbTableSchemaConverterTestConfig::class])
//@RunWith(SpringRunner::class)
//// See Also JdbcTaxiSchemaGeneratorTest
//class DbTableSchemaConverterTest  : BaseSchemaConverterServiceTest() {
//
//   @Autowired
//   lateinit var jdbcTemplate: JdbcTemplate
//   lateinit var connectionRegistry: InMemoryJdbcConnectionRegistry
//
//   lateinit var converter: DbTableSchemaConverter
//
//
//   @Before
//   fun setup() {
//      val namedParamTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
//      connectionRegistry =
//         InMemoryJdbcConnectionRegistry(listOf(NamedTemplateConnection("movies", namedParamTemplate, JdbcDriver.H2)))
//
//      converter = DbTableSchemaConverter(connectionRegistry, SimpleSchemaProvider(TaxiSchema.empty()))
//   }
//
//   @Test
//   fun `generates schema for table`() {
//      val converterService = createConverterService(converter)
//      val generated = converterService.import(
//         SchemaConversionRequest(
//            DbTableSchemaConverter.FORMAT,
//            DbTableSchemaConverterOptions(
//               "movies",
//               listOf(TableTaxiGenerationRequest(
//                  JdbcTable("public", "movie")
//               ))
//            )
//         )
//      ).block(Duration.ofSeconds(1))!!
//      TODO()
//   }
//
//
//   @Configuration
//   // Don't read application-test.yaml as it declares other db connections
//   @TestPropertySource(inheritLocations = false)
//   @EnableAutoConfiguration
//   @EnableJpaRepositories(basePackageClasses = [MovieRepository::class])
//   class DbTableSchemaConverterTestConfig
//
//
//   @Entity(name = "actor")
//   data class Actor(
//      @Id val actorId: Int,
//      val firstName: String,
//      val lastName: String
//   )
//
//   @Entity(name = "movie")
//   data class Movie(
//      @Id
//      val movieId: Int,
//      val title: String,
//      @OneToMany(targetEntity = Actor::class)
//      val actors: List<Actor>
//   )
//
//   interface MovieRepository : JpaRepository<Movie, Int>
//
//   interface ActorRepository : JpaRepository<Actor, Int>
//
//
//}
