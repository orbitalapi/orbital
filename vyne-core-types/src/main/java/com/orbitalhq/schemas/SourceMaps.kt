package com.orbitalhq.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VersionedSource
import lang.taxi.generators.SourceMap
import lang.taxi.sources.SourceCodeLanguages

fun SourceMap.toJson():String {
   return jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this)
}
fun SourceMap.toVersionedSource(packageIdentifier: PackageIdentifier):VersionedSource {
   return VersionedSource(
      "sourceMap.json",
      packageIdentifier.version,
      this.toJson(),
      SourceCodeLanguages.SOURCE_MAP_JSON,
   )
}

fun sourceMapFromVersionedSource(versionedSource: VersionedSource, objectMapper: ObjectMapper = jacksonObjectMapper()):SourceMap {
   return objectMapper.readValue<SourceMap>(versionedSource.content)
}
