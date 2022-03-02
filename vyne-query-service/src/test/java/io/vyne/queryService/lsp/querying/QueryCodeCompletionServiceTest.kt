package io.vyne.queryService.lsp.querying

import com.winterbe.expekt.should
import io.vyne.query.VyneQlGrammar
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.lsp.TaxiTextDocumentService
import lang.taxi.lsp.sourceService.inMemoryIdentifier
import lang.taxi.lsp.sourceService.inMemoryVersionedId
import org.eclipse.lsp4j.*
import org.junit.Test

class QueryCodeCompletionServiceTest {

   val taxi = """
      model Film {
         @Id filmId : FilmId inherits String
         filmTitle : FilmTitle inherits String
      }
      model Actor {
         @Id actorId : ActorId inherits String
         firstName : ActorFirstName inherits String
         lastName : ActorLastName inherits String
         agent : AgentId inherits String
      }
      model Agent {
         @Id id : AgentId
         name : AgentName inherits String
      }
      model Studio {
         id : StudioId inherits String
         name : StudioName inherits String
      }
      model Tweet {
         text: TweetBody
      }
      service MyService {
         operation listFilms():Film[]
         operation listActor(actorId : ActorId):Actor
         operation getActors(id:FilmId):Actor[]
         operation getAgent(id:AgentId):Agent

         operation streamTweetMentions():Stream<Tweet>

         vyneQl query findStudios(querySpec: vyne.vyneQl.VyneQlQuery):Studio[] with capabilities {
            sum,
            count,
            avg,
            min,
            max,
            filter(==,!=,in,like,>,<,>=,<=)
         }
         vyneQl query findOneStudio(querySpec: vyne.vyneQl.VyneQlQuery):Studio with capabilities {
            sum,
            count,
            avg,
            min,
            max,
            filter(==,!=,in,like,>,<,>=,<=)
         }
      }
   """.trimIndent()
   val schema = TaxiSchema.fromStrings(VyneQlGrammar.QUERY_TYPE_TAXI, taxi)

   @Test
   fun `when defining filter attributes against a type returned from a query operation then attributes from the type are suggested`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "find { Studio(  )}")
      // Move back one character to within the parentheses.
      // The closing parenthesis is added by the editor
      position.character = position.character - 2
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      completions.should.have.size(2)
      completions.map { it.label }.should.have.elements("StudioId", "StudioName")
   }

   @Test
   fun `when defining filter attributes against an array type returned from a query operation then attributes from the type are suggested`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "find { Studio[](  )}")
      // Move back one character to within the parentheses.
      // The closing parenthesis is added by the editor
      position.character = position.character - 2
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      completions.should.have.size(2)
      completions.map { it.label }.should.have.elements("StudioId", "StudioName")
   }

   @Test
   fun `when defining filter attributes against a type then inputs from operations are offered`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "find { Agent(  )}")
      // Move back one character to within the parentheses.
      // The closing parenthesis is added by the editor
      position.character = position.character - 2
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      completions.should.have.size(1)
      completions.map { it.label }.should.have.elements("AgentId")
   }

   @Test
   fun `when writing a streaming query only streamable types are considered`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "stream { ")
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      completions.should.have.size(1)
      completions.map { it.label }.should.contain.elements("Tweet")
   }

   @Test
   fun `when incomplete entries lead to compiler error then completions still offered`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "find { Fi }")
      // Move back one character to the end of the Fi.  The closing brace has been added by the editor
      position.character = position.character - 1
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      // Just make sure we didn't throw an exception
      completions.should.not.be.empty
   }

   @Test
   fun `when no context is provided then completions include list of return types from operations that take no arguments`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "find { ")
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      completions.should.have.size(3)
      completions.map { it.label }.should.have.elements("Studio[]", "Studio", "Film[]")
      val completionItem = completions.first { it.label == "Film[]" }
      completionItem.additionalTextEdits.single().newText.trim().should.equal("import Film")
   }

   @Test
   fun `when writing a given statement types are provided`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "given { id : ")
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      completions.map { it.label }
         .should.contain.elements("Film", "Actor", "ActorId", "FilmTitle") // and others...
   }

   @Test
   fun `when a given statement provides inputs then hints include discoverable types`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "given { id : ActorId = '123' } find { Ac ")
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      completions.should.have.size(7)
      completions.map { it.label }.should.contain.elements(
         "Actor",
         "ActorId",
         "ActorFirstName",
         "ActorLastName",
         "AgentId",
         "Agent",
         "AgentName"
      )
      val agentCompletion = completions.first { it.label == "Agent" }
      agentCompletion.documentation.right.value.should.equal("Discovered by path ActorId -> MyService / listActor -> Actor -> MyService / getAgent -> Agent")
   }

   @Test
   fun `when writing a projection without given then includes hints of discoverable attributes`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "find { Actor } as { id: ")
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      completions.should.have.size(7)
      completions.map { it.label }.should.have.elements(
         "Actor",
         "ActorId",
         "ActorFirstName",
         "ActorLastName",
         "AgentId",
         "Agent",
         "AgentName"
      )
   }

   @Test
   fun `when writing a projection then hints include attributes of discoverable models from given statement`() {
      val documentService = documentServiceForSchema(taxi, schema = schema)
      val position = documentService.applyEdit("query", "given { id : ActorId = '123' } find { Actor } as { id: ")
      val completions = documentService.completion(
         CompletionParams(
            inMemoryIdentifier("query"),
            position
         )
      ).get().left
      completions.should.have.size(7)
      completions.map { it.label }.should.contain.elements(
         "Actor",
         "Agent",
         "ActorId",
         "ActorFirstName",
         "ActorLastName",
         "AgentId",
         "Agent",
         "AgentName"
      )
   }

}


/**
 * Applies an edit to the model, and returns the position as the cursor was
 * at the end of the edit
 */
fun TaxiTextDocumentService.applyEdit(modelName: String, content: String): Position {
   this.didChange(
      DidChangeTextDocumentParams(
         inMemoryVersionedId(modelName),
         listOf(TextDocumentContentChangeEvent(content))
      )
   )
   val lineIndex = content.lines().size - 1
   val charIndex = content.lines().last().length - 1
   return Position(lineIndex, charIndex)
}
