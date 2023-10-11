package com.orbitalhq.history.db

import com.orbitalhq.query.history.QuerySankeyChartRow
import com.orbitalhq.query.history.SankeyChartRowId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface QuerySankeyChartRowRepository : JpaRepository<QuerySankeyChartRow, SankeyChartRowId> {
   companion object {
      // Trying desperately to solve conflicts-on-write before a demo.
      // Upsert differs based on which dialect we're using, and can't switch.
      const val POSTGRES_UPSERT_QUERY: String =
         """insert into QUERY_SANKEY_ROW (query_id, source_node_type, source_node, source_operation_data, target_node_type, target_node, target_operation_data, node_count)
           values (:queryId, :sourceNodeType, :sourceNode, :sourceNodeOperationData, :targetNodeType, :targetNode, :targetNodeOperationData, :count)
           on conflict (query_id, source_node_type, source_node, target_node_type, target_node)
           do update set node_count = :count, source_operation_data = :sourceNodeOperationData, target_operation_data = :targetNodeOperationData
    """
      const val H2_MERGE_QUERY: String =
         """merge into QUERY_SANKEY_ROW (query_id, source_node_type, source_node, source_operation_data, target_node_type, target_node, target_operation_data, node_count)
           key(query_id, source_node_type, source_node, target_node_type, target_node)
           values (:queryId, :sourceNodeType, :sourceNode, :sourceNodeOperationData, :targetNodeType, :targetNode, :targetNodeOperationData, :count)
    """
   }

   fun findAllByQueryId(queryId: String): List<QuerySankeyChartRow>


   @Transactional
   @Modifying
   @Query(nativeQuery = true, value = POSTGRES_UPSERT_QUERY)
   fun upsert(
      @Param("queryId") queryId: String,
      @Param("sourceNodeType") sourceNodeType: String,
      @Param("sourceNode") sourceNode: String,
      @Param("sourceNodeOperationData") sourceNodeOperationData: String?,
      @Param("targetNodeType") targetNodeType: String,
      @Param("targetNode") targetNode: String,
      @Param("targetNodeOperationData") targetNodeOperationData: String?,
      @Param("count") count: Int
   )
}
