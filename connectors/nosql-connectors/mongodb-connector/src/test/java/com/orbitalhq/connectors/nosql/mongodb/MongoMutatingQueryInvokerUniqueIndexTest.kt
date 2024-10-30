package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.InMemoryMongoConnectionRegistry
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.winterbe.expekt.should
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MongoMutatingQueryInvokerUniqueIndexTest: MongoDbTestcontainer() {
    private lateinit var connectionRegistry: InMemoryMongoConnectionRegistry
    private lateinit var connectionFactory: MongoConnectionFactory

    @BeforeEach
    fun setup() {
        val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
        val mongo1ConnectionConfig = MongoConnectionConfiguration("accountsMongo", connectionParams)
        connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
        connectionFactory =  MongoConnectionFactory(connectionRegistry)
    }

    private val accountsSchema = listOf(
        MongoConnector.schema,
        VyneQlGrammar.QUERY_TYPE_TAXI,
        """
         ${MongoConnector.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         
         @Collection(connection = "accountsMongo", collection = "accounts")
         model Account {
            @UniqueIndex
            accountId : AccountId inherits String
            currency : Currency inherits String
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
    fun `can upsert against a unique index`(): Unit = runBlocking {
        val vyne = testVyne(accountsSchema) { schema -> listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema))) }

        // Insert a Brand Account with id = 1
        val insertResult = vyne.query("""
                given { account : Account = { accountId : "1" , currency: "TL" } }
               call AccountsDb::upsertAccount
               """.trimIndent())
            .typedObjects()
        insertResult.should.have.size(1)
        insertResult.single()["currency"].value.should.equal("TL")

        // Update the currency of Account with id = 1
        val updatedResult = vyne.query("""
                given { account : Account = { accountId : "1" , currency: "USD" } }
               call AccountsDb::upsertAccount
               """.trimIndent())
            .typedObjects()
        updatedResult.should.have.size(1)
        updatedResult.single()["currency"].value.should.equal("USD")

        //Now fetch all the accounts, there should only be one!
        val fetchAllAccounts = vyne.query("""
            find { Account }
        """.trimIndent()).typedObjects()
        fetchAllAccounts.should.have.size(1)
        fetchAllAccounts.single()["currency"].value.should.equal("USD")
    }
}