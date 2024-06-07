package com.orbitalhq.copilot

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.copilot.prompts.V2Prompt
import com.orbitalhq.copilot.refiner.GeneratedQueryRefiner
import com.orbitalhq.schemas.ParsedQuery
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.taxi.toMessage
import lang.taxi.CompilationException
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.AnnotationType
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class TaxiQlGenerationResult(
   val taxi: String,
)

class OpenAiChatService(
   private val apiKey: String,
   private val url: String,
   private val model: Model,
   private val client: OkHttpClient = OkHttpClient(),
   private val mapper: ObjectMapper = ChatGptMapper,
   private val queryRefiner: GeneratedQueryRefiner = GeneratedQueryRefiner(),
   private val messageChunker: MessageChunker = MessageChunker()
) {
   constructor(settings: CopilotSettings) : this(settings.apiKey, settings.endpointUrl, settings.model)

   companion object {
      private val logger = KotlinLogging.logger {}

      val ChatGptMapper: ObjectMapper = jacksonObjectMapper()
         .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
   }

   private fun buildSystemPrompt(schema: Schema): ConversationMessage {
      val scalarsAndDescriptions = buildScalars(schema)
      val modelsAndDescriptions = buildModelDescriptions(schema)
      val streamTypesAndDescriptions = buildStreamTypes(schema)
      val scalars = scalarsAndDescriptions.joinToString(separator = "\n")
      val models = modelsAndDescriptions.joinToString("\n")
      val streams = streamTypesAndDescriptions.joinToString("\n")
      val systemPrompt = V2Prompt.buildSystemPrompt(scalars, models, streams)
      return ConversationMessage.messageOnly(OpenAiChatMessage.Role.system, systemPrompt)
   }

   private fun generateInitialConversation(schema: Schema, queryText: String): Conversation {
      val terminatedQuery = if (!queryText.endsWith(".")) {
         "${queryText.trim()}."
      } else {
         queryText
      }
      val userPrompt = "Write a query that answers this question:\n$terminatedQuery"

      val prompts = listOf(
         buildSystemPrompt(schema),
         ConversationMessage.messageOnly(OpenAiChatMessage.Role.user, userPrompt, queryText)
      )

      logger.debug { "OpenAI Prompts:\n${prompts.joinToString("\n")}" }
      return Conversation(prompts)
   }

   private fun submitConversation(conversation: Conversation): Pair<ConversationMessage, Conversation> {
      val messages = conversation.messages.map { it.toOpenAiChatMessage() }
      val request = OpenAiChatRequest(messages, this.model)
      val httpRequest = Request.Builder()
         .url(url)
         .addHeader("Authorization", "Bearer $apiKey")
         .post(mapper.writeValueAsString(request).toRequestBody("application/json".toMediaType()))
         .build()
      val response = client.newCall(httpRequest)
         .execute()

      if (response.isSuccessful) {
         val responseBody =
            mapper.readValue<OpenAiChatResponse>(response.body!!.bytes())
         val content = responseBody.choices.first().message.content.trim()
         val chunkedMessage = messageChunker.chunkMessage(content, OpenAiChatMessage.Role.assistant)
         return chunkedMessage to conversation.appendMessage(chunkedMessage)
      } else {
         val message = "OpenAI request failed: Code ${response.code} : ${response.body!!.string()}"
         logger.warn { message }
         throw RuntimeException(message)
      }
   }

   fun generateQueryFromText(schema: Schema, queryText: String): ConversationMessage {
      val messages = generateInitialConversation(schema, queryText)
      return submitConversation(messages).first
   }

   fun submitConversationFromMessages(
      schema: Schema,
      messages: List<ConversationMessage>
   ): ConversationMessage {
      val conversation = prependSystemPrompts(messages, schema)
      return submitConversation(conversation).first
   }

   private fun prependSystemPrompts(messages: List<ConversationMessage>, schema: Schema): Conversation {
      val cleansedMessages = messages.filter { it.role != OpenAiChatMessage.Role.system }
         .toMutableList()

      cleansedMessages.add(0, buildSystemPrompt(schema))
      return Conversation(
         cleansedMessages
      )
   }

   fun generateAndRefineQueryFromText(
      schema: Schema,
      queryText: String,
   ): Conversation {
      val conversation = generateInitialConversation(schema, queryText)
      val (response, updatedConversation) = submitConversation(conversation)


      // For each query block that we got back, first validate it (to ensure it compiles).
      // If it doesn't compile, we attempt to repair it by having further conversations with the AI.
      // Once we have a compiled query, we then "refine" it, by making changes
      // based on the current schema
      val replacements = response.chunks
         .filterIsInstance<QueryMessageChunk>()
         .map { queryMessageChunk ->
            val (compiledQuery, querySchema) = validateOrRepairQuery(schema, queryMessageChunk, conversation)
            val (refinedQuery, refinedQuerySchema) = queryRefiner.apply(
               compiledQuery,
               querySchema
            )
            queryMessageChunk to CompiledQueryMessageChunk(ParsedQuery(refinedQuery))
         }
      val validatedResponse = response.replaceChunks(replacements)
      return updatedConversation.replaceLastMessage(validatedResponse)
   }

   private fun validateOrRepairQuery(
      schema: Schema,
      messageChunk: QueryMessageChunk,
      conversation: Conversation,
      attemptCount: Int = 0
   ): Pair<TaxiQlQuery, Schema> {
      val maxIterations = 3
      if (attemptCount >= maxIterations) {
         val message = "Attempting to repair a broken query has failed after $attemptCount attempts. Aborting"
         logger.warn { message }
         throw QueryGenerationFailedException(message, conversation)
      }
      return try {
         val (parsedQuery, _, querySchema) = schema.parseQuery(messageChunk.taxi, useCache = false)
         parsedQuery to querySchema
      } catch (e: CompilationException) {
         // This will recurse back into this function after asking the AI to repair the query
         repairInvalidQuery(messageChunk, schema, e.errors.toMessage(), conversation, attemptCount)
      }
   }

   private fun repairInvalidQuery(
      messageChunk: QueryMessageChunk,
      schema: Schema,
      errorMessage: String,
      conversation: Conversation,
      attemptCount: Int
   ): Pair<TaxiQlQuery, Schema> {
      logger.info { "Query from AI failed to compile: $errorMessage - (Attempt $attemptCount). Will try again" }
      val repairConversation = conversation.appendMessage(
         OpenAiChatMessage.Role.user,
         "The most recent query you provided failed, with the following compilation error: \n> $errorMessage\n Provide an amended query."
      )
      val (repairResponse, updatedConversation) = submitConversation(repairConversation)
      return when (repairResponse.queries.size) {
         0 -> {
            // there were no queries in the response.
            // Just try asking again.
            validateOrRepairQuery(schema, messageChunk, conversation, attemptCount + 1)
         }

         1 -> {
            // We got an amended query from the AI.
            // Try to validate this one.
            // Note that the updated conversation (including this attempt to repair)
            // is passed on,to provide the AI more context on the next attempt
            validateOrRepairQuery(schema, repairResponse.queries.single(), updatedConversation, attemptCount + 1)
         }

         else -> {
            val message =
               "While attempting to repair a broken query, the AI responded with multiple new queries. This is too complex to support, so bailing out"
            logger.warn { message }
            throw QueryGenerationFailedException(message, updatedConversation)
         }

      }
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
   val model: String
) {
   constructor(messages: List<OpenAiChatMessage>, model: Model) : this(messages, model.id)
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

class QueryGenerationFailedException(message: String, val conversation: Conversation) :
   RuntimeException(message)

