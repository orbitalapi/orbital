package com.orbitalhq.functions.kotlin

import com.orbitalhq.functions.ReturnType
import com.orbitalhq.functions.TaxiFunction
import com.orbitalhq.functions.TaxiFunctionProvider
import com.orbitalhq.functions.TaxiParam
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
   fileExtension = "taxi.kts",
   compilationConfiguration = TaxiFunctionCompilationConfiguration::class,
   displayName = "Taxi Custom Functions",

)
abstract class KotlinTaxiFunctionScript : TaxiFunctionProvider

val scriptDefaultImports = setOf(
   TaxiFunction::class.java,
   TaxiParam::class.java,
   ReturnType::class.java
).map { it.simpleName }

object TaxiFunctionCompilationConfiguration : ScriptCompilationConfiguration({
   baseClass(KotlinTaxiFunctionScript::class)
   jvm {
      dependenciesFromCurrentContext(wholeClasspath = true)
   }
   defaultImports(scriptDefaultImports)
})

