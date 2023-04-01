package io.vyne.query.chat

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
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

data class ChatGptQuery(
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
      val fieldsPrefix = scalarsAndDescriptions.joinToString(
         prefix = "Using the following fields:\n",
         separator = "\n"
      ) { "Name: ${it.typeName}    Description: ${it.description}" }
      val promptResponseType =
         "\n\nProvide responses to the following questions as JSON objects that conform to this typescript API\n$typescriptResponseApi\n\nEnsure that the JSON is valid"
      val questionPrompt = "Generate a query that answers this question:\n$queryText"
      val chatGptQuestion = listOf(fieldsPrefix, promptResponseType, questionPrompt).joinToString("\n\n")
      logger.debug { "ChatGPT request: \n$chatGptQuestion" }
      val request = ChatGptRequest(chatGptQuestion)

      val httpRequest = Request.Builder()
         .url("https://api.openai.com/v1/completions")
         .addHeader("Authorization", "Bearer $apiKey")
//         .addHeader("Content-Type", "application/json")
         .post(mapper.writeValueAsString(request).toRequestBody("application/json".toMediaType()))
         .build()
      val response = client.newCall(httpRequest)
         .execute()
      if (response.isSuccessful) {
         val responseBody = mapper.readValue<ChatGptResponse>(response.body!!.bytes())
         return mapper.readValue<ChatGptQuery>(responseBody.choices.first().text)
      } else {
         throw RuntimeException("ChatGPT request failed: Code ${response.code} : ${response.body!!.string()}")
      }
   }


   fun parseToTaxiQl(schema: Schema, queryText: String): TaxiQLQueryString {
      val query = parseToChatQuery(schema, queryText)
      return TaxiQlGenerator.convertToTaxi(query, schema)
   }

   private fun buildScalars(schema: Schema): List<ScalarAndDescription> {
      val excludedNamespaces = listOf(
         PrimitiveType.NAMESPACE,
         "io.vyne",
         "taxi.stdlib",
         "vyne.vyneQL"
      )
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
            ScalarAndDescription(type.paramaterizedName, type.typeDoc)
         }
         .toList()
   }
}

data class ScalarAndDescription(val typeName: String, val description: String?)


/**
 * {
 *   "model": "text-davinci-003",
 *   "prompt": "Convert this text to a programmatic command:\n\nExample: Ask Constance if we need some bread\nOutput: send-msg `find constance` Do we need some bread?\n\nReach out to the ski store and figure out if I can get my skis fixed before I leave on Thursday\n\nOutput: send-msg `find ski store` Can I get my skis fixed before I leave on Thursday?",
 *   "temperature": 0,
 *   "max_tokens": 100,
 *   "top_p": 1,
 *   "frequency_penalty": 0.2,
 *   "presence_penalty": 0
 * }
 */

data class ChatGptRequest(
   val prompt: String,
   val model: String = "text-davinci-003",
   val temperature: BigDecimal = BigDecimal(0.3),
   val max_tokens: Int = 1000,
   val top_p: Int = 1,
   val frequency_penalty: BigDecimal = BigDecimal(0),
   val presence_penalty: BigDecimal = BigDecimal.ZERO
)

data class ChatGptResponse(
   val id: String,
   val `object`: String,
   val created: Long,
   val model: String,
   val choices: List<ChatGptChoice>,
   val usage: ChatGptUsage

)

data class ChatGptChoice(
   val text: String,
   val index: Int,
   val logprobs: Int? = null,
   val finish_reason: String

)

data class ChatGptUsage(
   val prompt_tokens: Int,
   val completion_tokens: Int,
   val total_tokens: Int
)
