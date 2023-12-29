package com.orbitalhq.query.chat

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.types.AnnotationType
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

data class TaxiQlGenerationResult(
   val taxi: String,
)

@OptIn(ExperimentalTime::class)
class ChatQueryParser(
   private val apiKey: String,
   private val client: OkHttpClient = OkHttpClient(),
   private val mapper: ObjectMapper = ChatGptMapper,
   private val queryRefiner: GeneratedQueryRefiner = GeneratedQueryRefiner()
) {

   companion object {
      private val logger = KotlinLogging.logger {}

      val ChatGptMapper: ObjectMapper = jacksonObjectMapper()
         .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
   }

   private fun generateInitialConversation(schema: Schema, queryText: String): List<OpenAiChatMessage> {
      val scalarsAndDescriptions = buildScalars(schema)
      val modelsAndDescriptions = buildModelDescriptions(schema)
      val streamTypesAndDescriptions = buildStreamTypes(schema)
      val scalars = scalarsAndDescriptions.joinToString(separator = "\n")
      val models = modelsAndDescriptions.joinToString("\n")
      val streams = streamTypesAndDescriptions.joinToString("\n")
      val terminatedQuery = if (!queryText.endsWith(".")) {
         "${queryText.trim()}."
      } else {
         queryText
      }
      val userPrompt = "Write a query that answers this question:\n$terminatedQuery"

      val systemPrompt = buildSystemPromptReturningTaxi(scalars, models, streams)
      val prompts = listOf(
         OpenAiChatMessage(OpenAiChatMessage.Role.system, systemPrompt),
         OpenAiChatMessage(OpenAiChatMessage.Role.user, userPrompt)
      )
      return prompts
   }

   private fun submitConversation(messages: List<OpenAiChatMessage>): TaxiQlGenerationResult {
      val request = OpenAiChatRequest(messages)
      val httpRequest = Request.Builder()
         .url("https://api.openai.com/v1/chat/completions")
         .addHeader("Authorization", "Bearer $apiKey")
         .post(mapper.writeValueAsString(request).toRequestBody("application/json".toMediaType()))
         .build()
      val response = client.newCall(httpRequest)
         .execute()

      if (response.isSuccessful) {
         val responseBody =
            mapper.readValue<OpenAiChatResponse>(response.body!!.bytes())
         val content = responseBody.choices.first().message.content.trim()
         if (content.startsWith("FAILURE")) {
            // OpenAI almost always prefixes with FAILURE:, but sometimes only FAILURE
            throw QueryGenerationFailedException(content.removePrefix("FAILURE").removePrefix(":"), messages)
         }
         val startIndex = content.indexOfAny(listOf("find {", "stream {"))
         if (startIndex == -1) {
            throw QueryGenerationFailedException(content, messages)
         }
         val trimmedQuery = content.substring(startIndex)
         return TaxiQlGenerationResult(taxi = trimmedQuery)
      } else {
         val message = "OpenAI request failed: Code ${response.code} : ${response.body!!.string()}"
         logger.warn { message }
         throw RuntimeException(message)
      }
   }

   fun generateQueryFromText(schema: Schema, queryText: String): TaxiQlGenerationResult {
      val messages = generateInitialConversation(schema, queryText)
      return submitConversation(messages)
   }

   fun generateAndRefineQueryFromText(
      schema: Schema,
      queryText: String,
      maxIterations: Int = 3
   ): TaxiQlGenerationResult {
      var iterationCount = 0
      val conversation = mutableListOf<OpenAiChatMessage>()
      conversation.addAll(generateInitialConversation(schema, queryText))

      while (iterationCount < maxIterations) {
         val response = submitConversation(conversation)
         // attempt to compile the response
         try {
            val (parsedQuery, _, querySchema) = schema.parseQuery(response.taxi, useCache = false)
            val (refinedQuery, duration) = measureTimedValue { queryRefiner.apply(parsedQuery,querySchema) }
            logger.info { "Refining query took $duration" }
            return response.copy(taxi = refinedQuery.first.source)
         } catch (e: CompilationException) {
            val errorMessage = "Compilation failed with exception: ${e.message}"
            if (iterationCount < maxIterations - 1) {
               logger.info { "$errorMessage - (Iteration $iterationCount of ${maxIterations - 1}. Will try again" }
               conversation.add(OpenAiChatMessage(OpenAiChatMessage.Role.assistant, response.taxi))
               conversation.add(
                  OpenAiChatMessage(
                     OpenAiChatMessage.Role.user,
                     "That failed, with the following compilation error: \n> ${e.message}\n Provide an mended query."
                  )
               )
            } else {
               logger.info { "$errorMessage - giving up." }
            }
            iterationCount++
         }
      }
      throw QueryGenerationFailedException(
         "Failed to generate a working query after ${iterationCount + 1} attempts",
         conversation
      )

   }

   private fun buildStreamTypes(schema: Schema): List<TypeAndDescription> {
      return schema.streamOperations
         .map { operation ->
            val returnType = operation.returnType.typeParameters[0]
            val description = listOfNotNull(
               returnType.typeDoc,
               operation.typeDoc?.let { "(returned from an operation with the following description: $it)" },
            ).joinToString("\n")
            TypeAndDescription(
               returnType.fullyQualifiedName,
               returnType.basePrimitiveTypeName?.shortDisplayName,
               description
            )
         }
   }

   private fun buildSystemPromptReturningTaxi(scalars: String, models: String, streams: String): String {
      return """
You are an assistant who converts requirements into data queries, using a language called Taxi.
If someone asks for data that we don't have types defined for, then inform them.  Avoid the term "semantic type", and just say "data"

# Today's date is ${LocalDate.now()}.

# Taxi uses Types to define data and criteria.
# Following are some sample queries In Taxi.  They use a different set of types from the ones just shown, for illustrative purposes.  IN YOUR RESPONSE, ONLY USE TYPES YOU'RE TOLD EXIST.

# This is an example query, with a comment describing it:

// The base type to find.  In this example, it's an array, indicating "Find all Orders"
find { Order[] }

# Queries can ask for a single entity or a collection of entities to be returned.  To ask for a collection,
# use array notation after the type name.  For example:

// Find exactly one Order:
find { Order }

// Find all matching Orders:
find { Order[] }

# Criteria are specified in parenthesis after the target type:

// finds all Orders after October 1st 2021 with a notional value greater than 1 million,
find { Order[]( SettlementDate  >= '2021-10-01' && demo.orderFeeds.trading.Notional >= 1000000 }

// Find a single Movie entitled Gladiator
find { Movie( Title == 'Gladiator' ) }

# After specifying the criteria, you can define the fields to return in a "projection" using an "as" clause.
# A projection is defined as:


find { Something[]  } as {
   fieldName : TypeName // A field named "fieldName", with type "TypeName"
}[]


# Field names are similar to a database column name - they may not contain spaces or periods.
# You may only use types that you are told exist. I'll list the types shortly. IT IS AN ERROR TO USE A SEMANTIC TYPE OTHER THAN THE ONES YOU'RE TOLD EXIST.
# If the type in the find clause was an array, then the projection must also close with an array token ([]).

# Here's an example:



// finds all Orders after October 1st 2021 with a notional value greater than 1 million, returning order Id and order type
find { Order[]( SettlementDate  >= '2021-10-01' && Notional >= 1000000 }
as {
orderId:  OrderId // a field named "orderId" with type OrderId
endDate: OrderEndDate // a field named "endDate" with type OrderEndDate
}[]

# When specifying criteria, you must consider the base type. For example, things that inherit from String should be quoted. Dates should be formatted according to their base type (whose name and expected format broadly align with types from java DateTime packages)
# For example:
```taxi
type ReleaseDate inherits Date // should be queried as 'YYYY-MM-DD'
type PerformanceStartDate inherits Instant // should be queried as 'YYYY-MM-DDTHH:NN:SSZ'
```

# In a projection, there are no commas after a field / type pair:


// correct:
find { ... } as {
  orderId : OrderId
  type: OrderType
}


# Queries can indicate that missing data should be discovered by adding a @FirstNotEmpty annotation preceeding the field name.
# Annotations MUST appear  BEFORE the field name,  and do not have parenthesis.

Query: Find me information about orders.  Fill in the gaps of any missing order status and end dates.
Expected result:


find { Order } as {
  orderId : OrderId

 // Annotations MUST appear BEFORE the field name.
  @FirstNotEmpty orderStatus: OrderStatus
  @FirstNotEmpty endDate : OrderSettlementDate

}


# Concatenation of strings is performed using the + operator, like this:

find { ... } as {
  fullName : FirstName + ' ' + LastName // FirstName and LastName are types
}

## Asking for data
Queries can ask for data using one of two verbs:

find { ... } // Use `find` to fetch a request / response type data - similar to a SELECT query in SQL.

stream { ... } // Use `stream` to request a continuous stream of data - similar to subscribing to a Kafka topic

stream queries that contain a projection (eg: an `as` clause) must always end in an array token.
# Here's an example:

// build a stream of last trade events, including the traders name

```
stream { LastTradeEvent } as {
   tradersFullName : FullName
}[] // Note the stream query ended in an array token
```

You can only create stream requests for types that are exposed as a stream operation.  You will be told which types are candidates for streaming.
When requesting a stream, do not request an array.

# The following models can be used as the starting point for your query (before a projection), as well as fields within the query:
$models

# The following field types can be used in your query. (The corresponding base type is shown in parenthesis):
$scalars


# The following types can be used for streams:
$streams

If you're unable to satisfy the request, prefix your response with FAILURE:

Only provide code. Any explanation can be included as // comments.

      """.trimIndent()
   }


   fun parseToTaxiQl(schema: Schema, queryText: String): TaxiQLQueryString {
      val query = generateQueryFromText(schema, queryText)
      return query.taxi!!
//      return TaxiQlGenerator.convertToTaxi(query, schema)
   }

   private val excludedNamespaces = listOf(
      PrimitiveType.NAMESPACE,
      "com.orbitalhq",
      "taxi.stdlib",
      "vyne.vyneQl",
      "vyne.cask"
   )

   private fun buildScalars(schema: Schema): List<TypeAndDescription> {

      return schema.types
         .asSequence()
         .filter { it.isScalar }
         .filter { it.taxiType !is AnnotationType }
         .filter { type ->
            excludedNamespaces.none { excludedNamespace ->
               type.qualifiedName.namespace.startsWith(
                  excludedNamespace
               )
            }
         }
         .map { type ->
            TypeAndDescription(type.name.name, type.basePrimitiveTypeName?.shortDisplayName, type.typeDoc)
         }
         .toList()
   }

   private fun buildModelDescriptions(schema: Schema): List<TypeAndDescription> {
      return schema.remoteOperations
         .asSequence()
         .map { operation ->
            operation.returnType.collectionType ?: operation.returnType
         }
         .distinctBy { it.name }
         .filter { !it.isScalar }
//         .filter { type ->
//              excludedNamespaces.none { excludedNamespace ->
//                 type.qualifiedName.namespace.startsWith(
//                    excludedNamespace
//                 )
//              }
//         }
         .map { TypeAndDescription(it.name.name, it.basePrimitiveTypeName?.shortDisplayName, it.typeDoc) }
         .toList()
   }
}

data class TypeAndDescription(val typeName: String, val baseType: String?, val description: String?) {
   override fun toString(): String {
      val typeWithBaseType = if (baseType.isNullOrEmpty()) {
         typeName
      } else {
         "$typeName ($baseType)"
      }
      val descriptionAsComment = if (description.isNullOrEmpty()) {
         ""
      } else {
         "// $description"
      }
      return "Name: $typeWithBaseType $descriptionAsComment"
   }
}


data class OpenAiChatRequest(
   val messages: List<OpenAiChatMessage>,
   val model: String = OpenAiModel.GPT_4,
)

object OpenAiModel {
   const val GPT_4 = "gpt-4"
   const val GPT_3_5_TURBO_1106 = "gpt-3.5-turbo-1106"
}

data class OpenAiChatMessage(
   val role: Role,
   val content: String,
) {
   enum class Role {
      system, user, assistant
   }
}

data class OpenAiChatResponse(
   val id: String,
   val `object`: String,
   val created: Long,
   val model: String,
   val system_fingerprint: String?,
   val choices: List<ChatCompletionChoice>,
   val usage: ChatGptUsage
)

data class OpenAiCompletionsResponse(
   val id: String,
   val warning: String? = null,
   val `object`: String,
   val created: Long,
   val model: String,
   val choices: List<CompletionChoice>,
   val usage: ChatGptUsage

)

data class CompletionChoice(
   val text: String,
   val index: Int,
   val logprobs: Map<String, Any>? = null,
   val finish_reason: String
)

data class ChatCompletionChoice(
   val index: String,
   val message: OpenAiChatMessage,
   val finish_reason: String,
   val logprobs: Map<String, Any>? = null
)

data class ChatGptUsage(
   val prompt_tokens: Int,
   val completion_tokens: Int,
   val total_tokens: Int
)

class QueryGenerationFailedException(message: String, val conversation: List<OpenAiChatMessage>) :
   RuntimeException(message)
