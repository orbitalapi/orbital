package com.orbitalhq.schemaServer.core.adaptors.functions

import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.writeText

class CustomFunctionKotlinScriptLoadingTest {
   @field:TempDir
   lateinit var projectTestDir: File

   @Test
   fun `can load a custom function from a kotlin script`() {
      projectTestDir.deployProject("custom-functions/function-in-script-file")
      // project should include functions/greeting.kts
      val sourcePackage = loadSourcePackage(projectTestDir.toPath())
      val schema = TaxiSchema.from(sourcePackage)

      schema.functionRegistry.containsFunction("com.foo.bar.greet".fqn()).shouldBeTrue()
      schema.taxi.function("com.foo.bar.greet").shouldNotBeNull()
   }

   @Test
   fun `script with syntax error fails to load`() {
      projectTestDir.deployProject("custom-functions/function-in-script-file")
      val badScript = projectTestDir.resolve("functions/broken.taxi.kts").toPath()
      badScript.writeText(
         """
         import com.orbitalhq.functions.*

         class Broken : TaxiFunctionProvider {
            @TaxiFunction fun oops(@TaxiParam name: String): String = "Hello"
         // missing closing brace and no return
         """
      )
      val sourcePackage = loadSourcePackage(projectTestDir.toPath())
      val schema = TaxiSchema.from(sourcePackage)

      // We don't have error detection at the moment -- but really just want to make sure that we didn't crash

   }

}
