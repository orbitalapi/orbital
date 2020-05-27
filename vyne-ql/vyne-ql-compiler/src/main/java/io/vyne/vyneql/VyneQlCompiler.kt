package io.vyne.vyneql

import arrow.core.Either
import arrow.core.getOrHandle
import io.vyne.VyneQLLexer
import io.vyne.VyneQLParser
import io.vyne.vyneql.compiler.TokenProcessor
import lang.taxi.*
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class VyneQlCompiler(val query: CharStream, val schema: TaxiDocument) {
   constructor(query: String, schema: TaxiDocument) : this(CharStreams.fromString(query), schema)
   constructor(query: String, schema: String) : this(CharStreams.fromString(query), Compiler(schema).compile())

   /**
    * Returns the query, or throws a CompilationException
    */
   fun query(): VyneQlQuery {
      return compile().getOrHandle { errors -> throw CompilationException(errors) }
   }

   fun compile(): Either<List<CompilationError>, VyneQlQuery> {
      val listener = TokenProcessor(schema)
      val errorListener = CollectingErrorListener(query.sourceName)
      val lexer = VyneQLLexer(query)
      val parser = VyneQLParser(CommonTokenStream(lexer))
      parser.addParseListener(listener)
      parser.addErrorListener(errorListener)

      // Use CompilerExceptions for runtime exceptions thrown by the compiler
      // not compilation errors in the source code being compiled
      // These exceptions represent bugs in the compiler
      val compilerExceptions = mutableListOf<CompilationError>()
      // Calling document triggers the parsing
      try {
         parser.queryDocument()
      } catch (e: Exception) {
         compilerExceptions.add(
            CompilationError(
               parser.currentToken,
               "An exception occurred in the compilation process.  This is likely a bug in the VyneQL Compiler. \n ${e.message}", parser.currentToken?.tokenSource?.sourceName
               ?: "Unknown"
            ))
      }


      val errors = compilerExceptions + errorListener.errors
      return if (errors.isNotEmpty()) {
         Either.left(errors)
      } else {
         listener.result
      }
   }
}
