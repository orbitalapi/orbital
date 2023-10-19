package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
import java.nio.file.Path

class PathGlobTest : DescribeSpec({
   describe("Resolving files against path globs") {
      it("should resolve a file against a path glob") {
         val glob = PathGlob(Path.of("/a/b/c"), "orbital/config/*.conf")
         val resolved = glob.resolveFileName("env.conf")
         resolved.toString().shouldBe("/a/b/c/orbital/config/env.conf")
      }

      it("should fail if the requested file does not match the provided glob") {
         val glob = PathGlob(Path.of("/a/b/c"), "orbital/config/*.conf")
         val exception = assertThrows<Exception> {
            val resolved = glob.resolveFileName("env.exe")
         }
         exception.message.shouldBe("Requested filename (env.exe) does not match the glob (orbital/config/*.conf)")
      }
      it("should fail if the provided glob is ambiguous") {
         val glob = PathGlob(Path.of("/a/b/c"), "orbital/**/config/*.conf")
         val execption = assertThrows<Exception> {
            val resolved = glob.resolveFileName("env.conf")
         }
         execption.message.shouldBe("Path is ambiguous.")
      }
   }

})
