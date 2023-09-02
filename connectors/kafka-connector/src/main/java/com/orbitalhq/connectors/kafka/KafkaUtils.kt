package com.orbitalhq.connectors.kafka

import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.generators.protobuf.ProtobufMessageAnnotation

enum class MessageEncodingType {
   BYTE_ARRAY,
   STRING;

   companion object {
      fun forType(messageType: Type): MessageEncodingType {
         return when {
            messageType.hasMetadata(ProtobufMessageAnnotation.NAME.fqn()) -> MessageEncodingType.BYTE_ARRAY
            // TODO : Other binary types (eg, avro) go here
            else -> MessageEncodingType.STRING
         }
      }
   }
}

