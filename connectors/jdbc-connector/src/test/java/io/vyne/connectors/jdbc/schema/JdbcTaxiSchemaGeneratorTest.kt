package io.vyne.connectors.jdbc.schema

import io.vyne.VersionedSource
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.jdbc.TableTaxiGenerationRequest
import io.vyne.query.VyneQlGrammar
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.Compiler
import lang.taxi.testing.TestHelpers
import lang.taxi.testing.TestHelpers.expectToCompileTheSame
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany

internal const val TEST_NAMESPACE = "io.vyne.test"

val builtInSources = arrayOf(
   JdbcConnectorTaxi.schema,
   VyneQlGrammar.QUERY_TYPE_TAXI
)
val builtInSchema = TaxiSchema.from(
   builtInSources.map { VersionedSource.sourceOnly(it) }
)


@SpringBootTest(classes = [JdbcTaxiSchemaGeneratorTestConfig::class])
@RunWith(SpringRunner::class)
class JdbcTaxiSchemaGeneratorTest {

   @Autowired
   lateinit var jdbcTemplate: JdbcTemplate


   @Test
   fun `can generate taxi definition of single table`() {
      val metadataService = DatabaseMetadataService(jdbcTemplate)
      val tables = metadataService.listTables()
      val actorTable = tables.single { it.tableName == "ACTOR" }

      val taxi = metadataService.generateTaxi(
         tables = listOf(TableTaxiGenerationRequest(actorTable)),
         schema = builtInSchema,
         connectionName = "testConnection"
      )
      taxi.shouldCompileWithJdbcSchemasTheSameAs(
         """
         import io.vyne.jdbc.Table

         namespace io.vyne.test.actor {
            type ActorId inherits Int

            type FirstName inherits String

            type LastName inherits String

            @Table(name = "ACTOR" , schema = "PUBLIC", connection = "testConnection)
            model Actor {
               @Id ACTOR_ID : ActorId
               FIRST_NAME : FirstName?
               LAST_NAME : LastName?
            }
         }
      """
      )
   }

   @Test
   fun `generate taxi for query service of table`() {
      val metadataService = DatabaseMetadataService(jdbcTemplate)
      val tables = metadataService.listTables()
      val actorTable = tables.single { it.tableName == "ACTOR" }

      val taxi = metadataService.generateTaxi(
         tables = listOf(TableTaxiGenerationRequest(actorTable)),
         schema = builtInSchema,
         "testDb"
      )
      val generated = Compiler.forStrings(taxi.single(), *builtInSources).compile()
      val expected = Compiler.forStrings(
         """
            import io.vyne.jdbc.DatabaseService
            import io.vyne.jdbc.Table
         namespace io.vyne.test.actor {
            type ActorId inherits Int

            type FirstName inherits String

            type LastName inherits String

              @Table(name = "ACTOR" , schema = "PUBLIC", connection = "testDb")
            model Actor {
               @Id ACTOR_ID : ActorId
               FIRST_NAME : FirstName?
               LAST_NAME : LastName?
            }

            @DatabaseService(connectionName = "testDb")
            service ActorService {
               vyneQl query actorQuery(querySpec: vyne.vyneQl.VyneQlQuery):Actor[] with capabilities {
                  sum,
                  count,
                  avg,
                  min,
                  max,
                  filter(==,!=,in,like,>,<,>=,<=)
               }
            }
         }
      """,
         *builtInSources
      ).compile()
      TestHelpers.assertAreTheSame(generated, expected, taxi)
   }

   @Test
   fun `uses same type when foriegnKey is present`() {
      val metadataService = DatabaseMetadataService(jdbcTemplate)
      val tablesToGenerate = metadataService.listTables().map { TableTaxiGenerationRequest(it) }
      val taxi = metadataService.generateTaxi(
         tables = tablesToGenerate,
         schema = builtInSchema,
         connectionName = "testDb"
      )
      taxi.shouldCompileWithJdbcSchemasTheSameAs(
         """
            import io.vyne.jdbc.Table
         namespace io.vyne.test.actor {
            type ActorId inherits lang.taxi.Int
            type FirstName inherits lang.taxi.String
            type LastName inherits lang.taxi.String
               @Table(name = "ACTOR" , schema = "PUBLIC")
            model Actor {
               @Id ACTOR_ID : ActorId
               FIRST_NAME : FirstName?
               LAST_NAME : LastName?
            }
         }
         namespace io.vyne.test.movie {
            type MovieId inherits lang.taxi.Int
            type Title inherits lang.taxi.String

               @Table(name = "MOVIE" , schema = "PUBLIC", connection = "testDb")
            model Movie {
               @Id MOVIE_ID : MovieId
               TITLE : Title?
            }
         }
         namespace io.vyne.test.movieActors {
          @Table(name = "MOVIE_ACTORS" , schema = "PUBLIC")
            model MovieActors {
               MOVIE_MOVIE_ID : io.vyne.test.movie.MovieId
               ACTORS_ACTOR_ID : io.vyne.test.actor.ActorId
            }
         }
      """
      )
   }


}

private fun List<String>.shouldCompileWithJdbcSchemasTheSameAs(other: String) {
   val source = this.joinToString("\n")
   val generated = Compiler.forStrings(source, *builtInSources).compile()
   val expected = Compiler.forStrings(
      other,
      *builtInSources
   ).compile()
   TestHelpers.assertAreTheSame(generated, expected, source)
}


@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories
class JdbcTaxiSchemaGeneratorTestConfig

@Entity(name = "actor")
data class Actor(
   @Id val actorId: Int,
   val firstName: String,
   val lastName: String
)

@Entity(name = "movie")
data class Movie(
   @Id
   val movieId: Int,
   val title: String,
   @OneToMany(targetEntity = Actor::class)
   val actors: List<Actor>
)

interface MovieRepository : JpaRepository<Movie, Int>

interface ActorRepository : JpaRepository<Actor, Int>

private fun List<String>.shouldCompileTheSameAs(expected: String) {
   expectToCompileTheSame(
      this,
      expected
   )
}

