package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.InMemoryMongoConnectionRegistry
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.winterbe.expekt.should
import io.kotest.matchers.maps.shouldContainKey
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MongoReadOnlyQueryInvokerTest: MongoDbTestcontainer() {

   private lateinit var connectionRegistry: InMemoryMongoConnectionRegistry
   private lateinit var connectionFactory: MongoConnectionFactory

   private val harryPotterSchema = listOf(
      MongoConnector.schema,
      VyneQlGrammar.QUERY_TYPE_TAXI,
      """
         ${MongoConnector.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type FirstName inherits String
         type LastName inherits String
         type NickName inherits String
         type Email inherits String
         type Age inherits Int
         type MongoObjectId inherits String

         @Collection(connection = "usersMongo", collection = "users")
         model User {
            firstName : FirstName
            lastName : LastName
            nickname: NickName
            email: Email
            age: Age
         }

         @Collection(connection = "usersMongo", collection = "users")
         model UserWithObjectId {
            @Id
            objectId: MongoObjectId
            firstName : FirstName
            lastName : LastName
            nickname: NickName
            email: Email
            age: Age
         }

         @MongoService( connection = "usersMongo" )
         service UsersDb {
            table user : User[]
            table mongoUsers: UserWithObjectId[]
         }

         @Collection(connection = "usersMongo", collection = "films")
         model FilmsWithIntObjectId {
            @Id
            id: FilmId inherits Int
            name: FilmName inherits String
         }

         @MongoService( connection = "usersMongo" )
         service FilmsDb {
            table films : FilmsWithIntObjectId[]
         }

         @Collection(connection = "usersMongo", collection = "compositeIds")
         model ModelWithMongoCompositeId {
            @Id
            id: MongoKey
            name: CompositeModelName inherits String
         }

         model MongoKey {
            key1: FirstKey inherits Int
            key2: SecondKey inherits String
         }

         @MongoService( connection = "usersMongo" )
         service CompositeDb {
            table composites : ModelWithMongoCompositeId[]
         }

         @Collection(connection = "usersMongo", collection = "ratings")
         model RatingsWithStringMongoId {
            @Id
            id: RatingId inherits String
            name: RatingName inherits String
         }

         @MongoService( connection = "usersMongo" )
         service RatingsDocument {
            table ratings : RatingsWithStringMongoId[]
         }
      """
   )

   /***
    * _id: {
    *       key1: 1,
    *       key2: "foo"
    *    },
    *    name: "Composite Id 1"
    */

   @BeforeEach
   fun setup() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("usersMongo", connectionParams)
      connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
      connectionFactory =  MongoConnectionFactory(connectionRegistry, SimpleMeterRegistry())
   }

   @Test
   fun `can fetch data from mongodb with equals criteria`(): Unit = runBlocking  {
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }
      val result = vyne.query("""find { User[]( FirstName == "Harry" ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf(
            "firstName" to "Harry",
            "lastName" to "Potter",
            "nickname" to "The Slayer",
            "email" to "harry.potter@gmail.com",
            "age" to 11))
   }

   @Test
   fun `can fetch data from mongodb with less than criteria`(): Unit = runBlocking  {
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }
      val result = vyne.query("""find { User[]( Age < 14 ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf(
            "firstName" to "Harry",
            "lastName" to "Potter",
            "nickname" to "The Slayer",
            "email" to "harry.potter@gmail.com",
            "age" to 11))
   }

   @Test
   fun `can fetch data from mongodb with larger than criteria`(): Unit = runBlocking  {
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }
      val result = vyne.query("""find { User[]( Age > 65 ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf(
            "firstName" to "Albus",
            "lastName" to "Dumbledore",
            "nickname" to "",
            "email" to "albus.dumbledore@gmail.com",
            "age" to 110))
   }

   @Test
   fun `can fetch data from mongodb with logical and criteria`(): Unit = runBlocking  {
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }

      val query = "find { User[]( Age >= 60 && Age < 70 ) }"
      val result = vyne.query(query)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf(
            "firstName" to "Tom",
            "lastName" to "Riddle",
            "nickname" to "Lord Voldemort",
            "email" to "tom.riddle@gmail.com",
            "age" to 65))
   }

   @Test
   fun `can fetch data from mongodb with logical or criteria`(): Unit = runBlocking  {
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }

      val query = "find { User[]( Age == 14 || Age == 15 ) }"
      val result = vyne.query(query)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf(
            "firstName" to "George",
            "lastName" to "Weasley",
            "nickname" to "The holy",
            "email" to "george.weasley@gmail.com",
            "age" to 14))
   }

   @Test
   fun `can fetch data from mongodb with ObjectId with equals criteria`(): Unit = runBlocking  {
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }
      val result = vyne.query("""find { UserWithObjectId[]( FirstName == "Harry" ) } """)
         .typedObjects()
      result.should.have.size(1)
      val resultMap = result.first().toRawObject()
      (resultMap as Map<String, Any>).shouldContainKey("objectId")
      resultMap
         .filter { it.key != "objectId" }
         .should.equal(mapOf(
            "firstName" to "Harry",
            "lastName" to "Potter",
            "nickname" to "The Slayer",
            "email" to "harry.potter@gmail.com",
            "age" to 11))
   }

   @Test
   fun `can fetch data from mongodb with Integer _id fields`(): Unit = runBlocking  {
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }
      val result = vyne.query("""find { FilmsWithIntObjectId[]( FilmId == 1 ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf(
            "id" to 1,
            "name" to "Star Wars"))
   }

   @Test
   fun `can fetch data from mongodb with String _id field`(): Unit = runBlocking  {
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }
      val result = vyne.query("""find { RatingsWithStringMongoId[]( RatingId == "goodRating" ) }""")
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf(
            "id" to "goodRating",
            "name" to "Good"))
   }

































}
