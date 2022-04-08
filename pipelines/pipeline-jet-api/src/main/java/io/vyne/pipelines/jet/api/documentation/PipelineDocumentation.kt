package io.vyne.pipelines.jet.api.documentation

import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass

typealias Markdown = String

enum class Maturity(val description: Markdown /* = kotlin.String */) {
   EXPERIMENTAL("New or evolving. This spec has been tested, but we still expect to uncover new issues"),
   BETA("Has been tested and used in production runs for some time, and the API is approaching maturity, but we still expect issues around edge cases."),
   STABLE("Considered stable.  Has received extensive testing, and we expect most edge cases have been handled")
}

@Target(AnnotationTarget.CLASS)
annotation class PipelineDocs(
   val name: String,
   @Language("Markdown")
   val docs: Markdown,
   val sample: KClass<out PipelineDocumentationSample<*>>,
   val maturity: Maturity
)

@Target(AnnotationTarget.PROPERTY)
annotation class PipelineParam(
   @Language("Markdown")
   val description: Markdown,
   val supressFromDocs: Boolean = false
)

interface PipelineDocumentationSample<T : PipelineTransportSpec> {
   val sample: T
}
