package com.orbitalhq.connectors.aws.dynamodb

import io.kotest.matchers.maps.shouldHaveKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.taxi.TaxiSchema
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest

class DynamoDbRequestBuilderTest {


    @Test
    fun `creates a scan when no filter is provided`() {
        val schema = TaxiSchema.from(
            """
            ${DynamoConnectorTaxi.schema}

            namespace movies {
                @com.orbitalhq.aws.dynamo.Table( connectionName = "conn" , tableName = "movies" )
                model Movie {
                    @Id
                    id : MovieId inherits Int
                    title : MovieTitle inherits String
                }
            }
        """.trimIndent()
        )
        val query = DynamoDbRequestBuilder().buildQuery(
            schema, """
            find { Movie[] }
        """.trimIndent()
        )
        val scanRequest = query.shouldBeInstanceOf<ScanRequest>()
        scanRequest.tableName().shouldBe("movies")
    }

    @Test
    fun `creates a queryItem query when using a filter against a non-id field`() {
        val schema = TaxiSchema.from(
            """
            ${DynamoConnectorTaxi.schema}

            namespace movies {
                @com.orbitalhq.aws.dynamo.Table( connectionName = "conn" , tableName = "movies" )
                model Movie {
                    @Id
                    id : MovieId inherits Int
                    title : MovieTitle inherits String
                }
            }
        """.trimIndent()
        )
        val query = DynamoDbRequestBuilder().buildQuery(
            schema, """
            find { Movie( MovieTitle == 'Jaws' ) }
        """.trimIndent()
        )
        val queryRequest = query.shouldBeInstanceOf<QueryRequest>()
        queryRequest.tableName().shouldBe("movies")
        queryRequest.filterExpression().shouldBe("title = :param0")
        queryRequest.expressionAttributeValues().shouldHaveKey("param0")
        queryRequest.expressionAttributeValues()["param0"].shouldBe(AttributeValue.builder().s("Jaws").build())
    }

    @Test
    fun `creates a get item query when using a numeric id`() {
        val schema = TaxiSchema.from(
            """
            ${DynamoConnectorTaxi.schema}

            namespace movies {
                @com.orbitalhq.aws.dynamo.Table( connectionName = "conn" , tableName = "movies" )
                model Movie {
                    @Id
                    id : MovieId inherits Int
                    title : MovieTitle inherits String
                }
            }
        """.trimIndent()
        )
        val query = DynamoDbRequestBuilder().buildQuery(
            schema, """
            find { Movie( MovieId == 3 ) }
        """.trimIndent()
        )
        val getItemRequest = query.shouldBeInstanceOf<GetItemRequest>()
        getItemRequest.tableName().shouldBe("movies")
        getItemRequest.key().shouldHaveKey("id")
        getItemRequest.key()["id"].shouldBe(AttributeValue.builder().n("3").build())
    }

    @Test
    fun `creates a get item query when using a string id`() {
        val schema = TaxiSchema.from(
            """
            ${DynamoConnectorTaxi.schema}

            namespace movies {
                @com.orbitalhq.aws.dynamo.Table( connectionName = "conn" , tableName = "movies" )
                model Movie {
                    @Id
                    id : MovieId inherits String
                    title : MovieTitle inherits String
                }
            }
        """.trimIndent()
        )
        val query = DynamoDbRequestBuilder().buildQuery(
            schema, """
            find { Movie( MovieId == "abcd" ) }
        """.trimIndent()
        )
        val getItemRequest = query.shouldBeInstanceOf<GetItemRequest>()
        getItemRequest.tableName().shouldBe("movies")
        getItemRequest.key().shouldHaveKey("id")
        getItemRequest.key()["id"].shouldBe(AttributeValue.builder().s("abcd").build())
    }

    @Test
    fun `creates a put request`() {
        val schema = TaxiSchema.from(
            """
            ${DynamoConnectorTaxi.schema}

            namespace movies {
                @com.orbitalhq.aws.dynamo.Table( connectionName = "conn" , tableName = "movies" )
                model Movie {
                    @Id
                    id : MovieId inherits String
                    title : MovieTitle inherits String
                }
            }
        """.trimIndent()
        )
        val instance = TypedInstance.from(
            schema.type("Movie"),
            """{ "id" : "foo" , "title" : "Star Wars" }""",
            schema
        )
        val query = DynamoDbRequestBuilder().buildPut(schema, instance)
        query.tableName().shouldBe("movies")
        query.item().shouldBe(
            mapOf(
                "id" to AttributeValue.fromS("foo"),
                "title" to AttributeValue.fromS("Star Wars")
            )
        )
    }
}
