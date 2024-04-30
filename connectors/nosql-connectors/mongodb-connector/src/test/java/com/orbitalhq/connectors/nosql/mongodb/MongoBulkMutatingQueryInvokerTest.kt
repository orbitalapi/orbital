package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.InMemoryMongoConnectionRegistry
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.winterbe.expekt.should
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MongoBulkMutatingQueryInvokerTest: MongoDbTestcontainer() {
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
        val mongo1ConnectionConfig = MongoConnectionConfiguration("flightsMongo", connectionParams)
        connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
        connectionFactory =  MongoConnectionFactory(connectionRegistry)
    }

    @Test
    fun `Can Insert Into Mongo Collection`(): Unit = runBlocking {
        val vyne = testVyne(flightInfoSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema))) }

        // Insert a Brand New Flight Into flightInfo, note we pass 'null' objectId so that Mongo will perform 'insert'
        val insertResult = vyne.query("""
                given { movie : FlightInfoWithObjectId = { objectId : "1" , code : "TK 1989", departure: "IST", arrival: "LHR", airline: { code: "TK", name: "Turkish Airlines", starAlliance: true} } }
               call FlightsDb::upsertFlightWithObjectId
               """.trimIndent())
            .typedObjects()
        insertResult.should.have.size(1)
        insertResult.single()["objectId"].shouldNotBeNull()
        val objectId = insertResult.first()["objectId"].value!!


        // Now Update the previous flight Info with a new flight code.
        val updateResult = vyne.query("""
                given { movie : FlightInfoWithObjectId = { objectId : "$objectId" , code : "TK 1990", departure: "IST", arrival: "LHR", airline: { code: "TK", name: "Turkish Airlines", starAlliance: true} } }
               call FlightsDb::upsertFlightWithObjectId
               """.trimIndent())
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
        result.first().toRawObject()
            .should.equal(mapOf(
                "objectId" to "1",
                "code" to "TK 1990",
                "departure" to "IST",
                "arrival" to "LHR",
                "airline" to mapOf<String, Any>(
                    "code" to "TK",
                    "name" to "Turkish Airlines",
                    "starAlliance" to true
                )
            ))

    }
}
