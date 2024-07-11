package com.orbitalhq.connectors.aws.s3

import com.orbitalhq.connectors.aws.s3.S3ConnectorTaxi.FilenamePatternFqn
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Parameter

abstract class BaseS3Invoker {
   protected fun getFilenamePattern(parameters: List<Pair<Parameter, TypedInstance>>):String {
      val (_, filePatternInstance) = parameters.singleOrNull { it.first.type.name == FilenamePatternFqn }
         ?: error("No filename pattern was found. Consider declaring a parameter to your operation of type ${FilenamePatternFqn.fullyQualifiedName}, with a default value of the filename. ")
      val filePattern = filePatternInstance.value
      require(filePattern is String) { "Expected a String for filename, but found ${filePatternInstance.value}" }
      return filePattern
   }
}
