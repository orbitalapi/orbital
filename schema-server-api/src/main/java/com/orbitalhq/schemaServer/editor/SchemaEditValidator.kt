package com.orbitalhq.schemaServer.editor

import com.orbitalhq.VersionedSource
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.CompilationError
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import org.antlr.v4.runtime.CharStreams

object SchemaEditValidator {
   fun validate(sources: List<VersionedSource>, currentSchema: TaxiSchema): Pair<List<CompilationError>, TaxiDocument> {
      val charStreams = sources.map { source ->
         CharStreams.fromString(source.content, source.name)
      }
      val (messages, compiled) = Compiler(charStreams, listOf(currentSchema.taxi))
         .compileWithMessages()
      return messages to compiled

   }
}
