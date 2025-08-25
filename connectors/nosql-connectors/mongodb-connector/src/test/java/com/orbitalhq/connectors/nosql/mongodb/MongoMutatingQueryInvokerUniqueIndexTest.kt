package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.InMemoryMongoConnectionRegistry
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.winterbe.expekt.should
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.random.Random

class MongoMutatingQueryInvokerUniqueIndexTest : MongoDbTestcontainer() {
   private lateinit var connectionRegistry: InMemoryMongoConnectionRegistry
   private lateinit var connectionFactory: MongoConnectionFactory

   @BeforeEach
   fun setup() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("accountsMongo", connectionParams)
      connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
      connectionFactory = MongoConnectionFactory(connectionRegistry, SimpleMeterRegistry())
   }

   private fun accountsSchema() = listOf(
      MongoConnector.schema,
      VyneQlGrammar.QUERY_TYPE_TAXI,
      """
         ${MongoConnector.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}

         @Collection(connection = "accountsMongo", collection = "accounts_${Random.nextInt(100, 9999)}")
         closed model Account {
            @UniqueIndex
            accountId : AccountId inherits String
            currency : Currency inherits String
            @SetOnInsert
            insertedAt: InsertedAt inherits Instant = now()
            updatedAt: UpdateAt inherits Instant = now()
         }

         @MongoService( connection = "accountsMongo" )
         service AccountsDb {
            table accounts : Account[]
            @UpsertOperation
            write operation upsertAccount(Account):Account
         }
      """
   )

   @Test
   fun `upserts without UniqueIndex using Id`(): Unit = runBlocking {
      val schemaSource = listOf(
         MongoConnector.schema,
         VyneQlGrammar.QUERY_TYPE_TAXI,
         """
         ${MongoConnector.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}

         @Collection(connection = "accountsMongo", collection = "accounts_2")
         closed model Account {
            @Id
            _id : AccountId inherits String
            currency : Currency inherits String
         }

         @MongoService( connection = "accountsMongo" )
         service AccountsDb {
            table accounts : Account[]
            @UpsertOperation
            write operation upsertAccount(Account):Account
         }"""
      )
      //val taxiSchema =
      val vyne = testVyne(schemaSource) { schema ->
         listOf(
            MongoDbInvoker(
               connectionFactory,
               SimpleSchemaProvider(schema),
               SimpleMeterRegistry()
            )
         )
      }
      // Insert a Brand Account with id = 1
      val insertResult = vyne.query(
         """
               given { account : Account = { _id : "1" , currency: "TL"  } }
               call AccountsDb::upsertAccount
               """.trimIndent()
      )
         .typedObjects()
      insertResult.should.have.size(1)
      insertResult.single()["currency"].value.should.equal("TL")

      // Update the currency of Account with id = 1
      val updatedResult = vyne.query(
         """
                given { account : Account = { _id : "1" , currency: "USD" } }
               call AccountsDb::upsertAccount
               """.trimIndent()
      )
         .typedObjects()
      updatedResult.should.have.size(1)
      updatedResult.single()["currency"].value.should.equal("USD")

      //Now fetch all the accounts, there should only be one!
      val fetchAllAccounts = vyne.query(
         """
            find { Account[] }
        """.trimIndent()
      ).typedObjects()
      fetchAllAccounts.should.have.size(1)
      fetchAllAccounts.single()["currency"].value.should.equal("USD")
   }

   @Test
   fun `can upsert against a unique index`(): Unit = runBlocking {
      //val taxiSchema =
      val vyne = testVyne(accountsSchema()) { schema ->
         listOf(
            MongoDbInvoker(
               connectionFactory,
               SimpleSchemaProvider(schema),
               SimpleMeterRegistry()
            )
         )
      }
      // Insert a Brand Account with id = 1
      val insertResult = vyne.query(
         """
               given { account : Account = { accountId : "1" , currency: "TL"  } }
               call AccountsDb::upsertAccount
               """.trimIndent()
      )
         .typedObjects()
      insertResult.should.have.size(1)
      insertResult.single()["currency"].value.should.equal("TL")

      val originalInsertedAt = vyne.query(
         """
            find { Account[] }
        """.trimIndent()
      ).typedObjects().single()["insertedAt"].value


      // Update the currency of Account with id = 1
      val updatedResult = vyne.query(
         """
                given { account : Account = { accountId : "1" , currency: "USD" } }
               call AccountsDb::upsertAccount
               """.trimIndent()
      )
         .typedObjects()
      updatedResult.should.have.size(1)
      updatedResult.single()["currency"].value.should.equal("USD")

      val updatedInsertedAt = vyne.query(
         """
            find { Account[] }
        """.trimIndent()
      ).typedObjects().single()["insertedAt"].value

      originalInsertedAt.should.equal(originalInsertedAt)

      //Now fetch all the accounts, there should only be one!
      val fetchAllAccounts = vyne.query(
         """
            find { Account[] }
        """.trimIndent()
      ).typedObjects()
      fetchAllAccounts.should.have.size(1)
      fetchAllAccounts.single()["currency"].value.should.equal("USD")
      val insertedAt = fetchAllAccounts.single()["insertedAt"].value as Instant
      val updatedAt = fetchAllAccounts.single()["updatedAt"].value as Instant

      insertedAt.isBefore(updatedAt).should.be.`true`
   }
}
