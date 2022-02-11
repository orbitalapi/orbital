/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vyne.protobuf.wire

import com.squareup.wire.ProtoAdapter
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.SchemaLoader
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.source
import java.io.File
import java.io.IOException

// This file is taken from
// https://raw.githubusercontent.com/square/wire/master/wire-library/wire-test-utils/src/main/java/com/squareup/wire/schema/RepoBuilder.kt
// However, as per this discussion: https://github.com/square/wire/discussions/2194, square stopped publishing

/**
 * Builds a repository of `.proto` and `.wire` files to create schemas, profiles, and adapters for
 * testing.
 */
class RepoBuilder {
   private val fs = FakeFileSystem()
   private val root = "/source".toPath()
   private val schemaLoader = SchemaLoader(fs)
   private var schema: Schema? = null

   fun add(name: String, protoFile: String): RepoBuilder {
      require(name.endsWith(".proto") || name.endsWith(".wire")) {
         "unexpected file extension: $name"
      }

      val relativePath = name.toPath()
      try {
         val resolvedPath = root / relativePath
         val parent = resolvedPath.parent
         if (parent != null) {
            fs.createDirectories(parent)
         }
         fs.write(resolvedPath) {
            writeUtf8(protoFile)
         }
      } catch (e: IOException) {
         throw AssertionError(e)
      }

      return this
   }

   @Throws(IOException::class)
   fun addLocal(path: String): RepoBuilder {
      val file = File(path)
      file.source().use { source ->
         val protoFile = source.buffer().readUtf8()
         return add(path, protoFile)
      }
   }

   @Throws(IOException::class)
   fun   schema(): Schema {
      var result = schema
      if (result == null) {
         schemaLoader.initRoots(sourcePath = listOf(Location.get("/source")))
         result = schemaLoader.loadSchema()
         schema = result
      }
      return result
   }


   @Throws(IOException::class)
   fun protoAdapter(messageTypeName: String): ProtoAdapter<Any> {
      return schema().protoAdapter(messageTypeName, true)
   }

}
