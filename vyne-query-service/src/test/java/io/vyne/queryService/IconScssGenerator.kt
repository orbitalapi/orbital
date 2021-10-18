package io.vyne.queryService

import io.vyne.utils.log
import org.junit.Test
import java.io.File

class IconScssGenerator {

   @Test
   fun generateScssForIcons() {
      val iconsHome = File(".").resolve("src/main/web2/src/assets/icons")
      val icons = iconsHome.walkTopDown()
         .filter { it.isFile && it.extension == "svg" }
         .map { file ->
            val originalName = file.name
            val conventionalName = file.name.toLowerCase()
               .replace(" ", "-")
               .replace("--","-")
            if (conventionalName != file.name) {
               val renamed = file.renameTo(file.resolveSibling(conventionalName))
               log().info("renamed $originalName to $conventionalName")
            }
            val path = "assets/icons/$conventionalName"
            val nameInCode = conventionalName.split("-").map { it.capitalize() }
               .joinToString("")
               .removeSuffix(".svg")
            nameInCode to path
         }
         .toList()
      val iconTypeName = icons.map { it.first }
         .joinToString(separator = " | \n", prefix = "export type IconName = ", postfix = ";") { "'$it'"}
      val iconPathMap = icons.joinToString(prefix = "export const icons: Record<IconName, string> = {", postfix = "};", separator = ", \n") { (iconName, path) ->
         "'$iconName': '$path'"
      }
      val typescriptFile = """
// THIS FILE IS GENERATED - DO NOT EDIT.
// REGENERATE BY RUNNING IconScssGenerator.generateScssForIcons()
$iconTypeName

$iconPathMap
      """.trimIndent()
      // TODO ... need to write this out to the right file...
   }
}
