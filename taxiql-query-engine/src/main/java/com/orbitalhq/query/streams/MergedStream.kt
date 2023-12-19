package com.orbitalhq.query.streams

import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.types.StreamType

/**
 * Experimental.
 *
 * A synthetic type that represents the joining of multiple streams.
 *
 * We needed a way of providing a single QuerySpecTypeNode (since everything is converging on
 * single nodes), which can signal to a query strategy that it's a combination of multiple streams.
 *
 * Currently using this.
 */
object MergedStream {
   const val TYPE_NAME = "com.orbitalhq.streaming.MergedStream"
   fun buildMergedStreamType(types:List<Type>) : Type{
      val parameters = types.joinToString(",") { it.typeParameters[0].paramaterizedName }
      val qualifiedName = "$TYPE_NAME<$parameters>".fqn()
      return Type(
         qualifiedName,
         taxiType = StreamType.untyped(),
         typeDoc = null,
         sources = emptyList()
      )
   }

   fun isMergedStream(type: Type):Boolean = type.fullyQualifiedName == TYPE_NAME
}
