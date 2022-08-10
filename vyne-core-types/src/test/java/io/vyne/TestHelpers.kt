package io.vyne

fun VersionedSource.asPackage(
   organisation: String = "com.foo",
   name: String = "test",
   version: String = "1.0.0"
): SourcePackage {
   return SourcePackage(
      PackageMetadata.from(organisation, name, version),
      listOf(this)
   )
}


fun ParsedSource.asParsedPackage(
   organisation: String = "com.foo",
   name: String = "test",
   version: String = "1.0.0"
): ParsedPackage {
   return ParsedPackage(
      PackageMetadata.from(organisation, name, version),
      listOf(this)
   )
}
