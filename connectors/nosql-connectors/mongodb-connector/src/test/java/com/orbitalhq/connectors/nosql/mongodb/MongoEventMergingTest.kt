package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.InMemoryMongoConnectionRegistry
import com.orbitalhq.firstRawObject
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyneWithStub
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class MongoEventMergingTest : MongoDbTestcontainer() {
   val TRIPLE_QUOTE = "\"\"\""

   private lateinit var connectionRegistry: InMemoryMongoConnectionRegistry
   private lateinit var connectionFactory: MongoConnectionFactory

   companion object {
      private val logger = KotlinLogging.logger {}
   }


   @BeforeEach
   fun setup() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("testMongo", connectionParams)
      connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
      connectionFactory = MongoConnectionFactory(connectionRegistry, SimpleMeterRegistry())
      logger.info { "Mongo available at $connectionString" }
   }


   /**
    * Tests event streaming enrichment pattern with conditional merging to avoid race conditions.
    *
    * Simulates a common streaming architecture where:
    * - Primary stream: User registrations (stored in 'users' collection)
    * - Enriching stream: Profile scores (stored in 'user_enrichments' holding table)
    *
    * The test demonstrates conditional merge logic that:
    * 1. Only enriches primary records where corresponding enrichment data exists
    * 2. Only processes enrichment data where corresponding primary records exist
    * 3. Marks processed enrichments as merged to prevent reprocessing
    * 4. Leaves orphaned enrichment data unprocessed (isMerged: false)
    *
    * Verifies that out of 3 users and 3 enrichments, only the 2 with matching pairs
    * get successfully merged, while orphaned records remain untouched.
    */
   @Test
   fun `event stream enrichment with conditional merging and transaction handling`(): Unit = runBlocking {
      val schema = """
      ${MongoConnector.Annotations.imports}
      import ${VyneQlGrammar.QUERY_TYPE_NAME}

      type UserId inherits String
      type UserName inherits String
      type Email inherits String
      type ProfileScore inherits Int
      type IsEnriched inherits Boolean
      type IsMerged inherits Boolean

      // Primary stream model - user registrations
      @Collection(connection = "testMongo", collection = "users_primary")
      closed parameter model User {
         @Id
         id: UserId
         name: UserName
         email: Email
         profileScore: ProfileScore?  // Will be enriched
         isEnriched: IsEnriched = false
      }

      // Secondary stream model - enrichment data holding table
      @Collection(connection = "testMongo", collection = "user_enrichments")
      closed parameter model UserEnrichment {
         @Id
         userId: UserId
         profileScore: ProfileScore
         isMerged: IsMerged = false
      }

      model MergeResult {
         mergedCount: MergeCount inherits Int
      }

      @MongoService(connection = "testMongo")
      service UserService {
         // Primary table operations
         @UpsertOperation
         write operation saveUser(User): User

         table users: User[]

         // Secondary table operations
         @UpsertOperation
         write operation saveEnrichment(UserEnrichment): UserEnrichment

         table enrichments: UserEnrichment[]

        /**
    * Aggregate transaction to merge pending user enrichments into users
    * and mark enrichments as merged. Runs both pipelines in one Mongo
    * transaction to avoid race conditions.
    */
   @MultiAggregation(
      transactional = false,
      pipelines = [
         {
            collection: "user_enrichments",
            stages: [
              '{ "${'$'}match": { "isMerged": false } }',
              $TRIPLE_QUOTE{ "${'$'}lookup": {
                  "from": "users_primary",
                  "localField": "_id",
                  "foreignField": "_id",
                  "as": "user"
              }}$TRIPLE_QUOTE,
              '{ "${'$'}match": { "user": { "${'$'}ne": [] } } }',
              '{ "${'$'}unwind": "${'$'}user" }',
              $TRIPLE_QUOTE{ "${'$'}replaceRoot": {
                  "newRoot": {
                    "${'$'}mergeObjects": [
                      "${'$'}user",
                      { "profileScore": "${'$'}profileScore", "isEnriched": true }
                    ]
                  }
              }}$TRIPLE_QUOTE,
              $TRIPLE_QUOTE{ "${'$'}merge": {
                  "into": "users_primary",
                  "on": "_id",
                  "whenMatched": "merge",
                  "whenNotMatched": "discard"
              }}$TRIPLE_QUOTE
            ]
         },
       {
            collection: "user_enrichments",
            stages: [
               '{ ${'$'}match: { isMerged: false } }',
               $TRIPLE_QUOTE{ ${'$'}lookup: {
                  from: "users_primary",
                  localField: "_id",
                  foreignField: "_id",
                  as: "user"
               } }$TRIPLE_QUOTE,
               '{ ${'$'}match: { user: { ${'$'}ne: [] } } }',

               // Mark enrichment as merged
               '{ ${'$'}set: { isMerged: true } }',

               // Merge back into user_enrichments
               $TRIPLE_QUOTE{ ${'$'}merge: {
                  into: "user_enrichments",
                  on: "_id",
                  whenMatched: "merge",
                  whenNotMatched: "discard"
               } }$TRIPLE_QUOTE
            ]
         }
      ]
   )
   write operation mergePendingUserUpdates(): MergeResult[]
   }
   """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      // === SETUP: Add primary stream data (user registrations) ===

      // User 1: Will have enrichment data to merge
      vyne.query("""
      given { User = {
         id: "user-1",
         name: "Alice Smith",
         email: "alice@example.com",
         profileScore: null,
         isEnriched: false
      } }
      call UserService::saveUser
   """.trimIndent()).rawObjects()

      // User 2: Will have enrichment data to merge
      vyne.query("""
      given { User = {
         id: "user-2",
         name: "Bob Jones",
         email: "bob@example.com",
         profileScore: null,
         isEnriched: false
      } }
      call UserService::saveUser
   """.trimIndent()).rawObjects()

      // User 3: Primary record exists, but NO enrichment data
      vyne.query("""
      given { User = {
         id: "user-3",
         name: "Charlie Brown",
         email: "charlie@example.com",
         profileScore: null,
         isEnriched: false
      } }
      call UserService::saveUser
   """.trimIndent()).rawObjects()

      // === SETUP: Add secondary stream data (enrichment data) ===

      // Enrichment for user-1 (primary record exists)
      vyne.query("""
      given { UserEnrichment = {
         userId: "user-1",
         profileScore: 85,
         isMerged: false
      } }
      call UserService::saveEnrichment
   """.trimIndent()).rawObjects()

      // Enrichment for user-2 (primary record exists)
      vyne.query("""
      given { UserEnrichment = {
         userId: "user-2",
         profileScore: 92,
         isMerged: false
      } }
      call UserService::saveEnrichment
   """.trimIndent()).rawObjects()

      // Enrichment for user-999 (NO primary record exists)
      vyne.query("""
      given { UserEnrichment = {
         userId: "user-999",
         profileScore: 78,
         isMerged: false
      } }
      call UserService::saveEnrichment
   """.trimIndent())
         .rawObjects()

      // === VERIFICATION: Check initial state ===

      val initialUsers = vyne.query("""find { User[] }""").rawObjects()
      logger.info { "Initial user state: $initialUsers" }
      initialUsers.shouldHaveSize(3)
      initialUsers.forEach { user ->
         user["profileScore"].shouldBe(null)
         user["isEnriched"].shouldBe(false)
      }

      val initialEnrichments = vyne.query("""find { UserEnrichment[] }""").rawObjects()
      logger.info { "Initial enrichment state: $initialEnrichments" }
      initialEnrichments.shouldHaveSize(3)
      initialEnrichments.forEach { enrichment ->
         enrichment["isMerged"].shouldBe(false)
      }

      // === TRANSACTION: Execute the merge operation ===

      // Step 1: Find enrichments that can be merged (have corresponding primary records)
      val mergeResult = vyne.query("""  call UserService::mergePendingUserUpdates""").firstRawObject()
      // Should only return 2 enrichments (user-1 and user-2), not user-999
      // TODO:
//      mergeResult["mergedCount"].shouldBe(2)

      val currentPendingEnrichments = vyne.query("""find { UserEnrichment[] }""")
         .rawObjects()
      val finalUsers = vyne.query("""find { User[] }""").rawObjects()
      logger.info { "Enrichment state after call: $currentPendingEnrichments" }
      logger.info { "User state after call: $finalUsers" }
      currentPendingEnrichments.count { it["isMerged"] == true }.shouldBe(2)
      currentPendingEnrichments.count { it["isMerged"] == false }.shouldBe(1)

      // === VERIFICATION: Check final state ===
      finalUsers.shouldHaveSize(3)

      // User-1: Should be enriched
      val user1 = finalUsers.find { it["id"] == "user-1" }!!
      user1["profileScore"].shouldBe(85)
      user1["isEnriched"].shouldBe(true)

      // User-2: Should be enriched
      val user2 = finalUsers.find { it["id"] == "user-2" }!!
      user2["profileScore"].shouldBe(92)
      user2["isEnriched"].shouldBe(true)

      // User-3: Should NOT be enriched (no enrichment data existed)
      val user3 = finalUsers.find { it["id"] == "user-3" }!!
      user3["profileScore"].shouldBe(null)
      user3["isEnriched"].shouldBe(false)

      val finalEnrichments = vyne.query("""find { UserEnrichment[] }""").rawObjects()
      finalEnrichments.shouldHaveSize(3)

      // Enrichments for user-1 and user-2: Should be marked as merged
      val user1Enrichment = finalEnrichments.find { it["userId"] == "user-1" }!!
      user1Enrichment["isMerged"].shouldBe(true)

      val user2Enrichment = finalEnrichments.find { it["userId"] == "user-2" }!!
      user2Enrichment["isMerged"].shouldBe(true)

      // Enrichment for user-999: Should NOT be marked as merged (no primary record)
      val user999Enrichment = finalEnrichments.find { it["userId"] == "user-999" }!!
      user999Enrichment["isMerged"].shouldBe(false)

      logger.info { "Successfully completed event stream enrichment with conditional merging" }
      logger.info { "Merged ${mergeResult.size} enrichments out of ${initialEnrichments.size} total" }
   }


}
