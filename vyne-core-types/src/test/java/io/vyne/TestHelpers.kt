package io.vyne

import java.time.Instant

fun List<VersionedSource>.asPackage(
   organisation: String = "com.foo",
   name: String = "test",
   version: String = "1.0.0"
): SourcePackage = SourcePackage(PackageMetadata.from(organisation, name, version), this)

fun VersionedSource.asPackage(
   organisation: String = "com.foo",
   name: String = "test",
   version: String = "1.0.0",
   submissionDate: Instant = Instant.now()
): SourcePackage {
   return SourcePackage(
      PackageMetadata.from(PackageIdentifier(organisation, name, version), submissionDate),
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
