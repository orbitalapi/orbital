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
      """
   )


   @BeforeEach
   fun setup() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("usersMongo", connectionParams)
      connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
      connectionFactory =  MongoConnectionFactory(connectionRegistry)
   }

   @Test
   fun `can fetch data from mongodb with equals criteria`(): Unit = runBlocking  {
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema))) }
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
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema))) }
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
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema))) }
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
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema))) }

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
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema))) }

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
      val vyne = testVyne(harryPotterSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema))) }
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

































}
