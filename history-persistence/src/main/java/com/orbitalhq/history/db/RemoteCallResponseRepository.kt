package com.orbitalhq.history.db

import com.orbitalhq.query.history.PartialRemoteCallResponse
import com.orbitalhq.query.history.RemoteCallResponse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

/**
 *
 *    @Id

val dataSourceId: String,
val queryId: String,
val dataSourceType: String,
val dataSourceJson: String
 */

interface RemoteCallResponseRepository : JpaRepository<RemoteCallResponse, String> {
   @Transactional
   fun findByQueryId(queryId: String): List<PartialRemoteCallResponse>

   fun findAllByRemoteCallId(remoteCallId: String): List<RemoteCallResponse>
   fun findAllByQueryId(queryId: String): List<RemoteCallResponse>

   @Modifying
   @Query(
      value = "INSERT INTO REMOTE_CALL_RESPONSE VALUES(:responseId, :remoteCallId, :queryId, :response) ON CONFLICT DO NOTHING",
      nativeQuery = true
   )
   @Transactional
   fun upsertRemoteCallResponse(
      @Param("responseId") responseId: String,
      @Param("remoteCallId") remoteCallId: String,
      @Param("queryId") queryId: String,
      @Param("response") response: String?
   )
}
