package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.InMemoryMongoConnectionRegistry
import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedInstace
import com.orbitalhq.firstTypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.sun.source.tree.UnionTypeTree
import com.winterbe.expekt.should
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.bson.types.Decimal128
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.CriteriaDefinition
import java.math.BigDecimal
import java.time.Instant
import java.util.Date

class MongoMutatingQueryInvokerTest : MongoDbTestcontainer() {
   private lateinit var connectionRegistry: InMemoryMongoConnectionRegistry
   private lateinit var connectionFactory: MongoConnectionFactory

   private val flightInfoSchema = listOf(
      MongoConnector.schema,
      VyneQlGrammar.QUERY_TYPE_TAXI,
      """
         ${MongoConnector.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type FlightCode inherits String
         type DepartureTime inherits Instant
         type DepartureAirport inherits String
         type ArrivalAirport inherits String
         type MongoObjectId inherits String

         type AirlineCode inherits String
         type AirlineName inherits String
         type StarAllianceMember inherits Boolean

         model Airline {
            code: AirlineCode
            name: AirlineName
            starAlliance: StarAllianceMember
         }

         @Collection(connection = "flightsMongo", collection = "flightInfo")
         model FlightInfo {
            code: FlightCode
            depTime : DepartureTime
            arrival: ArrivalAirport
            airline: Airline
         }

         @Collection(connection = "flightsMongo", collection = "flightInfo")
         model FlightInfoWithObjectId {
            @Id
            objectId: MongoObjectId?
            code: FlightCode
            departure: DepartureAirport
            arrival: ArrivalAirport
            airline: Airline
         }

         @MongoService( connection = "flightsMongo" )
         service FlightsDb {
            table FlightInfo : FlightInfo[]
            table mongoFlights: FlightInfoWithObjectId[]

            // This is effectively Insert as the FlightInfo does not have @Id annotation.
            @UpsertOperation
            write operation insertFlight(FlightInfo):FlightInfo

            @UpsertOperation
            write operation upsertFlightWithObjectId(FlightInfoWithObjectId):FlightInfoWithObjectId
         }
      """
   )

   @BeforeEach
   fun setup() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("flightsMongo", connectionParams)
      connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
      connectionFactory = MongoConnectionFactory(connectionRegistry, SimpleMeterRegistry())
   }

   @Test
   fun `Can Insert Into Mongo Collection`(): Unit = runBlocking {
      val vyne = testVyne(flightInfoSchema) { schema ->
         listOf(
            MongoDbInvoker(
               connectionFactory,
               SimpleSchemaProvider(schema), SimpleMeterRegistry()
            )
         )
      }

      // Insert a Brand New Flight Into flightInfo, note we pass 'null' objectId so that Mongo will perform 'insert'
      val insertResult = vyne.query(
         """
                given { movie : FlightInfoWithObjectId = { objectId : null , code : "TK 1989", departure: "IST", arrival: "LHR", airline: { code: "TK", name: "Turkish Airlines", starAlliance: true} } }
               call FlightsDb::upsertFlightWithObjectId
               """.trimIndent()
      )
         .typedObjects()
      insertResult.should.have.size(1)
      insertResult.single()["objectId"].shouldNotBeNull()
      val objectId = insertResult.first()["objectId"].value!!


      // Now Update the previous flight Info with a new flight code.
      val updateResult = vyne.query(
         """
                given { movie : FlightInfoWithObjectId = { objectId : "$objectId" , code : "TK 1990", departure: "IST", arrival: "LHR", airline: { code: "TK", name: "Turkish Airlines", starAlliance: true} } }
               call FlightsDb::upsertFlightWithObjectId
               """.trimIndent()
      )
         .typedObjects()
      updateResult.should.have.size(1)
      updateResult.single()["objectId"].shouldNotBeNull()
      val updateObjectId = updateResult.first()["objectId"].value!!

      // objectIds should match.
      updateObjectId.should.equal(objectId)

      // verify the updated field.
      updateResult.first()["code"].value.should.equal("TK 1990")

      //re query
      val result = vyne.query("""find { FlightInfoWithObjectId[]( MongoObjectId == "$objectId" ) } """)
         .typedObjects()

      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(
            mapOf(
               "objectId" to objectId,
               "code" to "TK 1990",
               "departure" to "IST",
               "arrival" to "LHR",
               "airline" to mapOf<String, Any>(
                  "code" to "TK",
                  "name" to "Turkish Airlines",
                  "starAlliance" to true
               )
            )
         )

   }

   @Test
   fun `can insert a mongo collection with nested array`(): Unit = runBlocking {
      val schema = """
          ${MongoConnector.Annotations.imports}
          type MongoObjectId inherits String

         @Collection(connection = "flightsMongo", collection = "people")
         parameter model Person {
            @Id
            objectId: MongoObjectId?
            name : Name inherits String
            contacts : Contact[]
         }
         model Contact {
            name : String
            email : String
         }

         @MongoService( connection = "flightsMongo" )
         service PeopleDb {
            table people : Person[]

            @UpsertOperation
            write operation insertPerson(Person):Person
         }
      """
      val vyne = testVyne(listOf(schema, MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }
      val result = vyne.query(
         """
         given { Person = { objectId: null, name : 'Jimmy', contacts: [ { name : "Jimmy's Mum", email : "mum@jimmy.com" }, { name : "Jimmy's Dad", email : "dad@jimmy.com" } ] } }
         call PeopleDb::insertPerson
      """
      )
         .firstRawObject()
      result["objectId"].shouldNotBeNull()
   }

   @Test
   fun `dates are persisted and read as dates`(): Unit = runBlocking {
      val schema = """
          ${MongoConnector.Annotations.imports}
          type MongoObjectId inherits String


         @Collection(connection = "flightsMongo", collection = "flightInfo")
         model Flight {
            @Id
            objectId: MongoObjectId?
            code: FlightCode inherits String
            depTime : DepartureTime inherits Instant
         }


         @MongoService( connection = "flightsMongo" )
         service FlightsDb {
            table flights : Flight[]

            @UpsertOperation
            write operation insertFlight(Flight):Flight
         }
      """
      val vyne = testVyne(listOf(schema, MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }
      val result = vyne.query(
         """
         given { Flight = {
               objectId: null,
               code : 'LHR-AKL',
               depTime : parseDate('2024-11-20T00:30:00Z')
             }
          }
         call FlightsDb::insertFlight
      """
      )
         .firstRawObject()

      val mongoTemplate = connectionFactory.reactiveMongoTemplate(connectionFactory.config("flightsMongo"))
      val fromMongo = mongoTemplate.findById<Map<String,Any>>(result["objectId"]!!, "flightInfo")
         .block()!!
      fromMongo["depTime"].shouldNotBeNull()

      // Mongo reads / writes as a Date.
      fromMongo["depTime"].shouldBeInstanceOf<Date>()

      // But when we get the value back into a TypedInstance,
      // it should be an Instant
      val readResult = vyne.query("""find { Flight(MongoObjectId == '${result["objectId"]}') } """)
         .firstTypedObject()["depTime"] as TypedValue
      readResult.value.shouldBeInstanceOf<Instant>()
   }

   @Test
   fun `decimals are persisted and read as decimals`(): Unit = runBlocking {
      val schema = """
          ${MongoConnector.Annotations.imports}
          type MongoObjectId inherits String


         @Collection(connection = "flightsMongo", collection = "flightInfo")
         model Flight {
            @Id
            objectId: MongoObjectId?
            code: FlightCode inherits String
            cost: Price inherits Decimal
         }


         @MongoService( connection = "flightsMongo" )
         service FlightsDb {
            table flights : Flight[]

            @UpsertOperation
            write operation insertFlight(Flight):Flight
         }
      """
      val vyne = testVyne(listOf(schema, MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }
      val result = vyne.query(
         """
         given { Flight = {
               objectId: null,
               code : 'LHR-AKL',
               cost: 200.15
             }
          }
         call FlightsDb::insertFlight
      """
      )
         .firstRawObject()

      val mongoTemplate = connectionFactory.reactiveMongoTemplate(connectionFactory.config("flightsMongo"))
      val fromMongo = mongoTemplate.findById<Map<String,Any>>(result["objectId"]!!, "flightInfo")
         .block()!!
      fromMongo["cost"].shouldNotBeNull()
      // Mongo reads / writes as a Decimal.
      fromMongo["cost"].shouldBeInstanceOf<Decimal128>()

      // But when we get the value back into a TypedInstance,
      // it should be a BigDecimal
      val readResult = vyne.query("""find { Flight(MongoObjectId == '${result["objectId"]}') } """)
         .firstTypedObject()["cost"] as TypedValue
      readResult.value.shouldBeInstanceOf<BigDecimal>()
         .shouldBe(BigDecimal("200.15"))
   }
}
