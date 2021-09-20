package io.vyne.connectors.jdbc.schema

import io.vyne.connectors.jdbc.DatabaseMetadataService
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

      val taxi = metadataService.generateTaxi(tables = listOf(actorTable), namespace = "io.vyne.test")
      taxi.shouldCompileTheSameAs(
         """
         namespace io.vyne.test.actor {
            type ActorId inherits Int

            type FirstName inherits String

            type LastName inherits String

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
   fun `uses same type when foriegnKey is present`() {
      val metadataService = DatabaseMetadataService(jdbcTemplate)
      val tablesToGenerate = metadataService.listTables()
      val taxi = metadataService.generateTaxi(tables = tablesToGenerate, namespace = "io.vyne.test")
      taxi.shouldCompileTheSameAs("""
         namespace io.vyne.test.actor {
            type ActorId inherits lang.taxi.Int
            type FirstName inherits lang.taxi.String
            type LastName inherits lang.taxi.String
            model Actor {
               @Id ACTOR_ID : ActorId
               FIRST_NAME : FirstName?
               LAST_NAME : LastName?
            }
         }
         namespace io.vyne.test.movie {
            type MovieId inherits lang.taxi.Int
            type Title inherits lang.taxi.String
            model Movie {
               @Id MOVIE_ID : MovieId
               TITLE : Title?
            }
         }
         namespace io.vyne.test.movieActors {
            model MovieActors {
               MOVIE_MOVIE_ID : io.vyne.test.movie.MovieId
               ACTORS_ACTOR_ID : io.vyne.test.actor.ActorId
            }
         }
      """)
   }



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

