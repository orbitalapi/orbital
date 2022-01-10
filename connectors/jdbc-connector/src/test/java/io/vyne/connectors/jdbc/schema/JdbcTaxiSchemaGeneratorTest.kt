package io.vyne.connectors.jdbc.schema

import com.winterbe.expekt.should
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
         tables = listOf(TableTaxiGenerationRequest(actorTable, defaultNamespace = "io.vyne.test")),
         schema = builtInSchema,
         connectionName = "testConnection"
      ).taxi
      taxi.shouldCompileWithJdbcSchemasTheSameAs(
         """
namespace io.vyne.test.actor.types {
   type ActorId inherits Int
   type FirstName inherits String
   type LastName inherits String
}
namespace io.vyne.test {
   @io.vyne.jdbc.Table(table = "ACTOR" , schema = "PUBLIC" , connection = "testConnection")
   model Actor {
      @Id ACTOR_ID : io.vyne.test.actor.types.ActorId
      FIRST_NAME : io.vyne.test.actor.types.FirstName?
      LAST_NAME : io.vyne.test.actor.types.LastName?
   }

   @io.vyne.jdbc.DatabaseService(connection = "testConnection")
   service ActorService {
      vyneQl query actorQuery(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<io.vyne.test.Actor> with capabilities {
         sum,
         count,
         avg,
         min,
         max,
         filter(==,!=,in,like,>,<,>=,<=)
      }
   }
}
      """
      )
   }

   @Test
   fun `when field has same name as a table, the field type is assigned correctly`() {
      val metadataService = DatabaseMetadataService(jdbcTemplate)
      val tablesToGenerate = metadataService.listTables()
         .filter { it.tableName == "CITY" }
         .map { TableTaxiGenerationRequest(it) }
      val taxi = metadataService.generateTaxi(
         tables = tablesToGenerate,
         schema = builtInSchema,
         connectionName = "testDb"
      ).taxi
      taxi.shouldCompileWithJdbcSchemasTheSameAs("""
         namespace city.types {
            type CityId inherits Int

            type City inherits String
         }
         namespace city {
            @io.vyne.jdbc.Table(table = "CITY" , schema = "PUBLIC" , connection = "testDb")
            model City {
               @Id CITY_ID : city.types.CityId
               CITY : city.types.City?
            }

            @io.vyne.jdbc.DatabaseService(connection = "testDb")
            service CityService {
               vyneQl query cityQuery(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<city.City> with capabilities {
                  sum,
                  count,
                  avg,
                  min,
                  max,
                  filter(==,!=,in,like,>,<,>=,<=)
               }
            }
         }
      """.trimIndent())
   }

   @Test
   fun `uses same type when foriegnKey is present`() {
      val metadataService = DatabaseMetadataService(jdbcTemplate)
      val tablesToGenerate = metadataService.listTables()
         .filter { setOf("ACTOR","MOVIE","MOVIE_ACTORS").contains(it.tableName) }
         .map { TableTaxiGenerationRequest(it) }
      tablesToGenerate.should.have.size(3)
      val taxi = metadataService.generateTaxi(
         tables = tablesToGenerate,
         schema = builtInSchema,
         connectionName = "testDb"
      ).taxi
      taxi.shouldCompileWithJdbcSchemasTheSameAs(
         """
namespace actor.types {
   type ActorId inherits Int
   type FirstName inherits String
   type LastName inherits String
}
namespace movie.types {
   type MovieId inherits Int
   type Title inherits String
}
namespace actor {
   @io.vyne.jdbc.Table(table = "ACTOR" , schema = "PUBLIC" , connection = "testDb")
   model Actor {
      @Id ACTOR_ID : actor.types.ActorId
      FIRST_NAME : actor.types.FirstName?
      LAST_NAME : actor.types.LastName?
   }

   @io.vyne.jdbc.DatabaseService(connection = "testDb")
   service ActorService {
      vyneQl query actorQuery(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<actor.Actor> with capabilities {
         sum,
         count,
         avg,
         min,
         max,
         filter(==,!=,in,like,>,<,>=,<=)
      }
   }
}
namespace movie {
   @io.vyne.jdbc.Table(table = "MOVIE" , schema = "PUBLIC" , connection = "testDb")
   model Movie {
      @Id MOVIE_ID : movie.types.MovieId
      TITLE : movie.types.Title?
   }

   @io.vyne.jdbc.DatabaseService(connection = "testDb")
   service MovieService {
      vyneQl query movieQuery(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<movie.Movie> with capabilities {
         sum,
         count,
         avg,
         min,
         max,
         filter(==,!=,in,like,>,<,>=,<=)
      }
   }
}
namespace movieActors {
   @io.vyne.jdbc.Table(table = "MOVIE_ACTORS" , schema = "PUBLIC" , connection = "testDb")
   model MovieActors {
      MOVIE_MOVIE_ID : movie.types.MovieId
      ACTORS_ACTOR_ID : actor.types.ActorId
   }

   @io.vyne.jdbc.DatabaseService(connection = "testDb")
   service MovieActorsService {
      vyneQl query movieactorsQuery(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<movieActors.MovieActors> with capabilities {
         sum,
         count,
         avg,
         min,
         max,
         filter(==,!=,in,like,>,<,>=,<=)
      }
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


// Testing defect: When a table has the same name as a field, the data types should be correct.
@Entity(name = "city")
data class City(
   @Id
   val cityId: Int,
   val city:String
)

interface MovieRepository : JpaRepository<Movie, Int>
interface CityRepository : JpaRepository<City,Int>

interface ActorRepository : JpaRepository<Actor, Int>

private fun List<String>.shouldCompileTheSameAs(expected: String) {
   expectToCompileTheSame(
      this,
      expected
   )
}

