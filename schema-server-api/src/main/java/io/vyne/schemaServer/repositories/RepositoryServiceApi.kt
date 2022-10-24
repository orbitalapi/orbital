package io.vyne.schemaServer.repositories

import org.springframework.web.bind.annotation.PostMapping
import reactivefeign.spring.config.ReactiveFeignClient

@ReactiveFeignClient("\${vyne.schema-server.name:schema-server}", qualifier = "schemaEditorFeignClient")
interface RepositoryServiceApi {

   @PostMapping("/api/repositories/file")
   fun createFileRepository(request: FileRepositoryChangeRequest)
}


data class FileRepositoryChangeRequest(
   val path: String,
   val editable: Boolean
)

data class GitRepositoryChangeRequest(

)

