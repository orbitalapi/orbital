package com.orbitalhq.query.chat

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.types.FormulaOperator
import lang.taxi.types.PrimitiveType
import lang.taxi.utils.quoted
import lang.taxi.utils.quotedIfNecessary
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.math.BigDecimal
import java.time.LocalDate

data class ChatGptQuery(
   val taxi: String? = null,
   val structuredQuery: StructuredQuery? = null
)

data class StructuredQuery(
   val fields: List<String>,
   val conditions: List<Condition>
)

data class Condition(
   val operator: Operator,
   val left: ConditionPart,
   val right: ConditionPart
) {

   fun asTaxi(): String {
      return """${left.asTaxi()} ${operator.operator.symbol} ${right.asTaxi()}"""
   }

   fun getTypeNames(): List<QualifiedName> {
      return listOfNotNull(left.referencedType, right.referencedType)
   }

   /**
    * FormulaOperator is named a little less clearly, which
    * could confuse ChatGPT.
    * These are the names that ChatGPT suggested
    */
   enum class Operator(val operator: FormulaOperator) {
      Equals(FormulaOperator.Equal),
      NotEquals(FormulaOperator.NotEqual),
      Or(FormulaOperator.LogicalOr),
      And(FormulaOperator.LogicalAnd),
      LessThan(FormulaOperator.LessThan),
      LessThanOrEqual(FormulaOperator.LessThanOrEqual),
      GreaterThan(FormulaOperator.GreaterThan),
      GreaterThanOrEqual(FormulaOperator.GreaterThanOrEqual);

      companion object {
         val typescriptDeclaration = values()
            .joinToString(" | ") { it.name.quoted("'") }
      }
   }

}

@JsonTypeInfo(
   use = JsonTypeInfo.Id.NAME,
   include = JsonTypeInfo.As.EXISTING_PROPERTY,
   property = "type"
)
@JsonSubTypes(
   JsonSubTypes.Type(FieldCondition::class, name = "Field"),
   JsonSubTypes.Type(LiteralCondition::class, name = "Literal"),
   JsonSubTypes.Type(NestedCondition::class, name = "Condition")
)
sealed class ConditionPart(val type: ConditionType) {
   open val referencedType: QualifiedName? = null

   abstract fun asTaxi(): String
}

data class FieldCondition(val value: String) : ConditionPart(ConditionType.Field) {
   override val referencedType: QualifiedName?
      get() = value.fqn()

   override fun asTaxi(): String = value
}

data class LiteralCondition(val value: Any) : ConditionPart(ConditionType.Literal) {
   override fun asTaxi(): String = value.quotedIfNecessary()
}

data class NestedCondition(val value: Condition) : ConditionPart(ConditionType.Condition) {
   override fun asTaxi(): String = value.asTaxi()
}


enum class ConditionType {
   Field,
   Literal,
   Condition
}

val typescriptResponseApi = """
   interface Query {
     queriedType: string; // the name of the main type to return
     fields: string[]; // the list of fields that should be returned
     conditions: Condition[]; // array of conditions (can be empty)
   }

   interface Condition {
     operator: ${Condition.Operator.typescriptDeclaration}; // The type of operator to use
     left?: ConditionPart; // left side of the condition (optional for unary operators like NOT)
     right?: ConditionPart; // right side of the condition (optional for unary operators like NOT, or for some binary operators like IN)
   }

   interface ConditionPart {
     type: 'Field' | 'Literal' | 'Condition';
     value: Condition | any;
   }
""".trimIndent()

class ChatQueryParser(
   private val apiKey: String,
   private val client: OkHttpClient = OkHttpClient(),
   private val mapper: ObjectMapper = ChatGptMapper
) {

   companion object {
      private val logger = KotlinLogging.logger {}

      val ChatGptMapper: ObjectMapper = jacksonObjectMapper()
         .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
   }

   fun parseToChatQuery(schema: Schema, queryText: String): ChatGptQuery {
      val scalarsAndDescriptions = buildScalars(schema)
      val modelsAndDescriptions = buildModelDescriptions(schema)
      val scalars = scalarsAndDescriptions.joinToString(separator = "\n")
      val models = modelsAndDescriptions.joinToString("\n")
      val prompts = listOf(
         OpenAiChatMessage(OpenAiChatMessage.Role.system, buildSystemPromptReturningTaxi(scalars, models)),
         OpenAiChatMessage(OpenAiChatMessage.Role.user, queryText)
      )

      val terminatedQuery = if (!queryText.endsWith(".")) {
         "${queryText.trim()}."
      } else {
         queryText
      }
      val promptResponseType =
         "\n\nProvide responses to the following questions as JSON objects that conform to this typescript API\n$typescriptResponseApi\n\n.  Do not include any other text, only JSON.  THE RESPONSE MUST BE VALID JSON."
      val questionPrompt = "Generate a query that answers this question:\n$terminatedQuery"
      val chatGptQuestion = listOf(
         buildSystemPromptReturningTaxi(scalars, models),
         "Write a query that answers this question:",
         terminatedQuery
      ).joinToString("\n\n")
      logger.debug { "ChatGPT request: \n$chatGptQuestion" }
      val request = OpenAiCompletionRequest(chatGptQuestion)
//      val request = OpenAiChatRequest(
//         prompts,
//         model = OpenAiModel.GPT_3_5_TURBO
//      )


      val httpRequest = Request.Builder()
         .url("https://api.openai.com/v1/completions")
//         .url("https://api.openai.com/v1/chat/completions")
         .addHeader("Authorization", "Bearer $apiKey")
//         .addHeader("Content-Type", "application/json")
         .post(mapper.writeValueAsString(request).toRequestBody("application/json".toMediaType()))
         .build()
      val response = client.newCall(httpRequest)
         .execute()

      if (response.isSuccessful) {
         val responseBody =
            mapper.readValue<OpenAiCompletionsResponse>(response.body!!.bytes())
         val content = responseBody.choices.first().text.trim()
//         val content = responseBody.choices.first().message.content.trim()
         logger.info { "OpenAI response: \n${content}" }
         val trimmedQuery = content.substring(content.indexOf("find {"))
         return ChatGptQuery(taxi = trimmedQuery, null)
      } else {
         val message = "OpenAI request failed: Code ${response.code} : ${response.body!!.string()}"
         logger.warn { message }
         throw RuntimeException(message)
      }
   }


   private fun buildSystemPromptReturningJson(scalars: String) = """
Today's date is ${LocalDate.now()}.
Provide responses to the following questions as JSON objects that conform to this typescript API

$typescriptResponseApi

Do not include any other text, only JSON.  THE RESPONSE MUST BE VALID JSON.

You must express the query using the following fields.  If the query cannot be expressed using these fields, then tell me.

$scalars

Generate a query that answers this question:
   """.trimIndent()

   private fun buildSystemPromptReturningTaxi(scalars: String, models: String): String {
      return """
# You are an assistant who converts requirements into data queries, using a language called Taxi.
# If someone asks for data that we don't have types defined for, then inform them.  Avoid the term "semantic type", and just say "data"

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

# The following field types can be used in your query:

$scalars
$models

Only respond to the user using code, do not include any explanations in your response.

      """.trimIndent()
   }


   fun parseToTaxiQl(schema: Schema, queryText: String): TaxiQLQueryString {
      val query = parseToChatQuery(schema, queryText)
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
         .filter { type ->
            excludedNamespaces.none { excludedNamespace ->
               type.qualifiedName.namespace.startsWith(
                  excludedNamespace
               )
            }
         }
         .map { type ->
            TypeAndDescription(type.name.name, type.typeDoc)
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
         .map { TypeAndDescription(it.name.name, it.typeDoc) }
         .toList()
   }
}

data class TypeAndDescription(val typeName: String, val description: String?) {
   override fun toString(): String {
      val descriptionAsComment = if (description.isNullOrEmpty()) {
         ""
      } else {
         "// $description"
      }
      return "Name: $typeName $descriptionAsComment"
   }
}


data class OpenAiCompletionRequest(
   val prompt: String,
   val model: String = OpenAiModel.TEXT_DAVINCI_003,
   val temperature: BigDecimal = BigDecimal(0.3),
   val max_tokens: Int = 1000,
//   val top_p: Int = 1,
//   val frequency_penalty: BigDecimal = BigDecimal(0),
//   val presence_penalty: BigDecimal = BigDecimal.ZERO
)

data class OpenAiChatRequest(
   val messages: List<OpenAiChatMessage>,
   val model: String = OpenAiModel.GPT_4,
//   val temperature: BigDecimal = BigDecimal(0.3),
//   val max_tokens: Int = 1000,
//   val top_p: Int = 1,
//   val frequency_penalty: BigDecimal = BigDecimal(0),
//   val presence_penalty: BigDecimal = BigDecimal.ZERO
)

object OpenAiModel {
   const val GPT_4 = "gpt-4"
   const val GPT_4_0314 = "gpt-4-0314"
   const val GPT_4_32K = "gpt-4-32k"
   const val GPT_4_32K_0314 = "gpt-4-32k-0314"
   const val GPT_3_5_TURBO = "gpt-3.5-turbo"
   const val GPT_3_5_TURBO_0301 = "gpt-3.5-turbo-0301"
   const val TEXT_DAVINCI_003 = "text-davinci-003"
   const val TEXT_DAVINCI_002 = "text-davinci-002"
   const val TEXT_CURIE_001 = "text-curie-001"
   const val TEXT_BABBAGE_001 = "text-babbage-001"
   const val TEXT_ADA_001 = "text-ada-001"
   const val TEXT_DAVINCI_EDIT_001 = "text-davinci-edit-001"
   const val CODE_DAVINCI_EDIT_001 = "code-davinci-edit-001"
   const val WHISPER_1 = "whisper-1"
   const val DAVINCI = "davinci"
   const val CURIE = "curie"
   const val BABBAGE = "babbage"
   const val ADA = "ada"
   const val TEXT_EMBEDDING_ADA_002 = "text-embedding-ada-002"
   const val TEXT_SEARCH_ADA_DOC_001 = "text-search-ada-doc-001"
   const val TEXT_MODERATION_STABLE = "text-moderation-stable"
   const val TEXT_MODERATION_LATEST = "text-moderation-latest"

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
   val logprobs: Int? = null,
   val finish_reason: String
)

data class ChatCompletionChoice(
   val index: String,
   val message: OpenAiChatMessage,
   val finish_reason: String
)

data class ChatGptUsage(
   val prompt_tokens: Int,
   val completion_tokens: Int,
   val total_tokens: Int
)
