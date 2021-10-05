package io.vyne.schemaServer.editor

import io.vyne.schemas.QualifiedName

object FileNames {
   // We have to work out a Type-to-file strategy.
   // As a first pass, I'm using a seperate file for each type.
   // It's a little verbose on the file system, but it's a reasonable start, as it makes managing edits easier, since
   // we don't have to worry about insertions / modification within the middle of a file.
   /**
    * contentType allows tooling to use a different file per type of content.
    * eg: com.foo.bar.MyType.annotations.taxi
    *
    */
   fun fromQualifiedName(qualifiedName: String, contentType: FileContentType? = null): String {
      val suffix = contentType?.extension.orEmpty() + ".taxi"
      return qualifiedName.replace(".", "/") + suffix
   }
}

fun lang.taxi.types.QualifiedName.toFilename(contentType: FileContentType? = null): String {
   return FileNames.fromQualifiedName(this.fullyQualifiedName, contentType)
}

fun QualifiedName.toFilename(contentType: FileContentType? = null): String {
   return FileNames.fromQualifiedName(this.fullyQualifiedName, contentType)
}

enum class FileContentType(val extension: String) {
   Annotations(".annotations")
}
