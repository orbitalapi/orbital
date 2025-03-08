package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.InMemoryMongoConnectionRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.testVyneWithStub
import com.orbitalhq.typedObjects
import com.orbitalhq.utils.formatAsFileSize
import com.winterbe.expekt.should
import io.kotest.assertions.timing.eventually
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class MongoBulkMutatingQueryInvokerTest : MongoDbTestcontainer() {
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

         @Collection(connection = "testMongo", collection = "flightInfo")
         model FlightInfo {
            code: FlightCode
            depTime : DepartureTime
            arrival: ArrivalAirport
            airline: Airline
         }

         @Collection(connection = "testMongo", collection = "flightInfo")
         model FlightInfoWithObjectId {
            @Id
            objectId: MongoObjectId?
            code: FlightCode
            departure: DepartureAirport
            arrival: ArrivalAirport
            airline: Airline
            @SetOnInsert
            _inserted: InsertedTimeStamp inherits Instant = now()
            _updated: UpdatedTimeStamp inherits Instant = now()
         }

         @MongoService( connection = "testMongo" )
         service FlightsDb {
            table FlightInfo : FlightInfo[]
            table mongoFlights: FlightInfoWithObjectId[]

            // This is effectively Insert as the FlightInfo does not have @Id annotation.
            @UpsertOperation(batchSize = 1, batchDuration = 10000)
            write operation insertFlight(FlightInfo):FlightInfo


            @UpsertOperation(batchSize = 1, batchDuration = 10000)
            write operation upsertFlightWithObjectId(FlightInfoWithObjectId):FlightInfoWithObjectId
         }
      """
   )

   @BeforeEach
   fun setup() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("testMongo", connectionParams)
      connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
      connectionFactory = MongoConnectionFactory(connectionRegistry, SimpleMeterRegistry())
   }

   @Test
   fun `can stream batches into Mongo`(): Unit = runBlocking {
      val schemaSrc = listOf(
         MongoConnector.schema,
         VyneQlGrammar.QUERY_TYPE_TAXI,
         """
         ${MongoConnector.Annotations.imports}
          @Collection(connection = "testMongo", collection = "prices")
          parameter model StockPrice {
            symbol : Symbol inherits String
            price : Price inherits Decimal
         }
         service PriceStream {
            stream prices : Stream<StockPrice>
         }
         @MongoService( connection = "testMongo" )
         service MongoService {
            // Small batch size, long duration - should write when the batch is filled
            @UpsertOperation(batchSize = 3, batchDuration = 100000)
            write operation insertStockPrice(StockPrice):StockPrice
         }
         """
      )
      val (vyne, stub) = testVyneWithStub(schemaSrc) { schema ->
         listOf(
            MongoDbInvoker(
               connectionFactory,
               SimpleSchemaProvider(schema), SimpleMeterRegistry()
            )
         )
      }

      val pricesFlow = MutableSharedFlow<TypedInstance>(replay = 10)
      stub.addResponseFlow("prices") { _, _ -> pricesFlow }

      val resultFlow = vyne.query(
         """stream { StockPrice }
         call MongoService::insertStockPrice
      """.trimMargin()
      )
         .results
      val collectedResults = mutableListOf<Any>()

      val job = launch {
         resultFlow.onEach {
            collectedResults.add(it)
         }
            .collect()
      }

      listOf(
         mapOf("symbol" to "AAPL", "price" to 1.112.toBigDecimal()),
         mapOf("symbol" to "IBM", "price" to 2.112.toBigDecimal()),
         mapOf("symbol" to "HZC", "price" to 3.112.toBigDecimal()),
      ).forEach {
         val typedInstance = TypedInstance.from(vyne.schema.type("StockPrice"), it, vyne.schema)
         pricesFlow.emit(typedInstance)
         println("Emitted item")
      }

      eventually(5.seconds) {
         collectedResults shouldHaveSize 3
      }
      job.cancelAndJoin()
   }

   @Test
   fun `can stream batches with projection into Mongo`(): Unit = runBlocking {
      val schemaSrc = listOf(
         MongoConnector.schema,
         VyneQlGrammar.QUERY_TYPE_TAXI,
         """
         ${MongoConnector.Annotations.imports}
          model StockPrice {
            symbol : Symbol inherits String
            price : Price inherits Decimal
         }
         @Collection(connection = "testMongo", collection = "prices")
         parameter model MongoStockPrice {
            ticker : Symbol
            realPrice : Price
            retailPrice : RetailPrice inherits Decimal
         }
         service PriceStream {
            stream prices : Stream<StockPrice>
         }
         @MongoService( connection = "testMongo" )
         service MongoService {
            // Small batch size, long duration - should write when the batch is filled
            @UpsertOperation(batchSize = 25, batchDuration = 100000)
            write operation insertStockPrice(MongoStockPrice):MongoStockPrice
         }
         """
      )
      val (vyne, stub) = testVyneWithStub(schemaSrc) { schema ->
         listOf(
            MongoDbInvoker(
               connectionFactory,
               SimpleSchemaProvider(schema), SimpleMeterRegistry()
            )
         )
      }

      val recordsToEmit = 50_000
      val pricesFlow = MutableSharedFlow<TypedInstance>(replay = recordsToEmit)
      stub.addResponseFlow("prices") { _, _ -> pricesFlow }

      System.gc() // Request garbage collection
      Thread.sleep(100) // Give GC time to run

      val memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()


      val resultFlow = vyne.query(
         """stream { StockPrice } as {
               ticker: Symbol
               cost : Price
               retailPrice : RetailPrice = Price + 1
            }[]
         call MongoService::insertStockPrice
      """.trimMargin()
      )
         .results
      val collectedResults = AtomicInteger(0)

      val thread = launch {
         resultFlow.onEach {
            collectedResults.incrementAndGet()
         }
            .collect()
      }

      (0..recordsToEmit).mapIndexed { index, i ->
         val item = mapOf("symbol" to "AAPL", "price" to Random.nextDouble(1.000005, 5.500000).toBigDecimal())
         val typedInstance = TypedInstance.from(vyne.schema.type("StockPrice"), item, vyne.schema)
         pricesFlow.emit(typedInstance)
      }

      // Make sure this is less than the batch write timeout, to assert that
      // writes are triggered by batch size, not timeout
      eventually(60.seconds) {
         collectedResults.get() shouldBe recordsToEmit
      }
      thread.cancelAndJoin()
      val mongo = connectionFactory.reactiveMongoTemplate(connectionFactory.config("testMongo"))
      val count = mongo.getCollection("prices")
         .flatMap { it ->
            it.countDocuments().toMono()
         }.block()!!
      count.shouldBe(recordsToEmit)

      System.gc() // Request garbage collection
      Thread.sleep(100) // Give GC time to run

      val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
      println("Memory increase : ${(memoryAfter - memoryBefore).formatAsFileSize}")


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
                given { movie : FlightInfoWithObjectId = { objectId : "1" , code : "TK 1989", departure: "IST", arrival: "LHR", airline: { code: "TK", name: "Turkish Airlines", starAlliance: true} } }
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
      val result = vyne.query("""find { FlightInfoWithObjectId[]( MongoObjectId == "1" ) } """)
         .typedObjects()

      result.should.have.size(1)

      val resultFiltered = (result.first().toRawObject() as Map<String, Any>)
         .filter { it.key != "_inserted" && it.key != "_updated" }
         .toMap()

      resultFiltered.should.equal(
         mapOf(
            "objectId" to "1",
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
}
