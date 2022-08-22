package io.vyne

import io.vyne.schemas.taxi.TaxiSchema
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


fun SourcePackage.toParsedPackages(): List<ParsedPackage> = listOf(this.toParsedPackage())
fun SourcePackage.toParsedPackage(): ParsedPackage {
   return ParsedPackage(
      this.packageMetadata,
      this.sources.map { ParsedSource(it) }
   )
}

fun List<ParsedSource>.asParsedPackage(
   organisation: String = "io.cask",
   name: String = "test",
   version: String = "1.0.0"
): ParsedPackage {
   return ParsedPackage(PackageMetadata.from(organisation, name, version), this)
}

fun List<ParsedSource>.asParsedPackages(
   organisation: String = "io.cask",
   name: String = "test",
   version: String = "1.0.0"
): List<ParsedPackage> = listOf(this.asParsedPackage(organisation, name, version))

fun TaxiSchema.Companion.from(sources: List<VersionedSource>): TaxiSchema {
   return TaxiSchema.from(sources.asPackage())
}
fun TaxiSchema.Companion.from(source: VersionedSource): TaxiSchema {
   return TaxiSchema.from(listOf(source).asPackage())
}

