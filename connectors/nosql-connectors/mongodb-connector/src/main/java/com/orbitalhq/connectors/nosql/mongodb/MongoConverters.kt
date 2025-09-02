package com.orbitalhq.connectors.nosql.mongodb

import com.mongodb.MongoClientSettings
import org.bson.codecs.DocumentCodec
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.jsr310.InstantCodec
import org.bson.codecs.jsr310.LocalDateCodec
import org.bson.codecs.jsr310.LocalDateTimeCodec
import org.bson.codecs.jsr310.LocalTimeCodec

/**
 * Exposes converters for going to/from BSON and Java types
 */
object MongoConverters {
   private val codecRegistry = CodecRegistries.fromRegistries(
      MongoClientSettings.getDefaultCodecRegistry(),
      CodecRegistries.fromCodecs(
         InstantCodec(),
         LocalDateCodec(),
         LocalDateTimeCodec(),
         LocalTimeCodec()
      )
   )

   val encoder = DocumentCodec(codecRegistry)
}
