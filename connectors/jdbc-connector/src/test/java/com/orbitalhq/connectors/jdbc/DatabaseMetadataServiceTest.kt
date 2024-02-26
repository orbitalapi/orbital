package com.orbitalhq.connectors.jdbc

import org.junit.Test
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Connection
import javax.sql.DataSource


class DatabaseMetadataServiceTest {
   @Test
   fun `ensure connection is closed`() {
      val mockDataSource: DataSource = mock {}
      val mockConnection: Connection = mock {}
      whenever(mockDataSource.connection).thenReturn(mockConnection)
      val databaseMetadataService = DatabaseMetadataService(JdbcTemplate(mockDataSource), mock {
         on { jdbcDriver } doReturn JdbcDriver.H2
      })
      try {
         databaseMetadataService.listTables()
      } catch (e: Exception) {
      }
      verify(mockConnection, times(1)).close()
   }
}
