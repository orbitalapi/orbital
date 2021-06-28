package io.vyne.cask.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Back port of JdbcTemplate from spring 5.3 to include streaming functionality
 */
@Component
public class JdbcStreamingTemplate extends JdbcTemplate implements JdbcOperations {


   @Autowired
   public JdbcStreamingTemplate(@Qualifier("dataSource") DataSource streamingDataSource) {
      setDataSource(streamingDataSource);
      afterPropertiesSet();
      this.setFetchSize(200);
   }

   @Nullable
   private <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action, boolean closeResources)
      throws DataAccessException {

      Assert.notNull(psc, "PreparedStatementCreator must not be null");
      Assert.notNull(action, "Callback object must not be null");
      if (logger.isDebugEnabled()) {
         String sql = getSql(psc);
         logger.debug("Executing prepared SQL statement" + (sql != null ? " [" + sql + "]" : ""));
      }

      Connection con = DataSourceUtils.getConnection(obtainDataSource());

      PreparedStatement ps = null;
      try {
         con.setAutoCommit(false);
         ps = psc.createPreparedStatement(con);
         applyStatementSettings(ps);
         T result = action.doInPreparedStatement(ps);
         handleWarnings(ps);
         return result;
      } catch (SQLException ex) {
         // Release Connection early, to avoid potential connection pool deadlock
         // in the case when the exception translator hasn't been initialized yet.
         if (psc instanceof ParameterDisposer) {
            ((ParameterDisposer) psc).cleanupParameters();
         }
         String sql = getSql(psc);
         psc = null;
         JdbcUtils.closeStatement(ps);
         ps = null;

         DataSourceUtils.releaseConnection(con, getDataSource());

         try {
            con.commit();
         } catch (SQLException e) {}

         con = null;
         throw translateException("PreparedStatementCallback", sql, ex);
      } finally {


         try {
            con.commit();
         } catch (SQLException e) {}

         if (closeResources) {
            if (psc instanceof ParameterDisposer) {
               ((ParameterDisposer) psc).cleanupParameters();
            }
            JdbcUtils.closeStatement(ps);

            DataSourceUtils.releaseConnection(con, getDataSource());
         }
      }
   }



   /**
    * Query using a prepared statement, allowing for a PreparedStatementCreator
    * and a PreparedStatementSetter. Most other query methods use this method,
    * but application code will always work with either a creator or a setter.
    *
    * @param psc       a callback that creates a PreparedStatement given a Connection
    * @param pss       a callback that knows how to set values on the prepared statement.
    *                  If this is {@code null}, the SQL will be assumed to contain no bind parameters.
    * @param rowMapper a callback that will map one object per row
    * @return the result Stream, containing mapped objects, needing to be
    * closed once fully processed (e.g. through a try-with-resources clause)
    * @throws DataAccessException if the query fails
    * @since 5.3
    */
   public <T> Stream<T> queryForStream(PreparedStatementCreator psc, @Nullable PreparedStatementSetter pss,
                                       RowMapper<T> rowMapper) throws DataAccessException {

      return result(execute(psc, ps -> {
         if (pss != null) {
            pss.setValues(ps);
         }
         ResultSet rs = ps.executeQuery();
         Connection con = ps.getConnection();
         return new ResultSetSpliterator<>(rs, rowMapper).stream().onClose(() -> {
            JdbcUtils.closeResultSet(rs);
            if (pss instanceof ParameterDisposer) {
               ((ParameterDisposer) pss).cleanupParameters();
            }
            JdbcUtils.closeStatement(ps);
            try {
               con.setAutoCommit(true);
            } catch (SQLException sqlException) {}
            DataSourceUtils.releaseConnection(con, getDataSource());
         });
      }, false));
   }

   public <T> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
      return queryForStream(psc, null, rowMapper);
   }

   public <T> Stream<T> queryForStream(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
      return queryForStream(new SimplePreparedStatementCreator(sql), pss, rowMapper);
   }

   public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
      return queryForStream(new SimplePreparedStatementCreator(sql), newArgPreparedStatementSetter(args), rowMapper);
   }

   public Stream<Map<String, Object>> queryForStream(String sql) throws DataAccessException {
      return this.queryForStream(sql, this.getColumnMapRowMapper());
   }

   public Stream<Map<String, Object>> queryForStream(String sql, @Nullable Object... args) throws DataAccessException {
      return this.queryForStream(sql, this.getColumnMapRowMapper(), args);
   }


   @Override
   @Nullable
   public <T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action)
      throws DataAccessException {

      Assert.notNull(csc, "CallableStatementCreator must not be null");
      Assert.notNull(action, "Callback object must not be null");
      if (logger.isDebugEnabled()) {
         String sql = getSql(csc);
         logger.debug("Calling stored procedure" + (sql != null ? " [" + sql + "]" : ""));
      }

      Connection con = DataSourceUtils.getConnection(obtainDataSource());
      CallableStatement cs = null;
      try {
         cs = csc.createCallableStatement(con);
         applyStatementSettings(cs);
         T result = action.doInCallableStatement(cs);
         handleWarnings(cs);
         return result;
      } catch (SQLException ex) {
         // Release Connection early, to avoid potential connection pool deadlock
         // in the case when the exception translator hasn't been initialized yet.
         if (csc instanceof ParameterDisposer) {
            ((ParameterDisposer) csc).cleanupParameters();
         }
         String sql = getSql(csc);
         csc = null;
         JdbcUtils.closeStatement(cs);
         cs = null;
         DataSourceUtils.releaseConnection(con, getDataSource());
         con = null;
         throw translateException("CallableStatementCallback", sql, ex);
      } finally {
         if (csc instanceof ParameterDisposer) {
            ((ParameterDisposer) csc).cleanupParameters();
         }
         JdbcUtils.closeStatement(cs);
         DataSourceUtils.releaseConnection(con, getDataSource());
      }
   }

   @Override
   @Nullable
   public <T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException {
      return execute(new SimpleCallableStatementCreator(callString), action);
   }


   /**
    * Determine SQL from potential provider object.
    *
    * @param sqlProvider object which is potentially an SqlProvider
    * @return the SQL string, or {@code null} if not known
    * @see SqlProvider
    */
   @Nullable
   private static String getSql(Object sqlProvider) {
      if (sqlProvider instanceof SqlProvider) {
         return ((SqlProvider) sqlProvider).getSql();
      } else {
         return null;
      }
   }

   private static <T> T result(@Nullable T result) {
      Assert.state(result != null, "No result");
      return result;
   }


   private static class SimplePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

      private final String sql;

      public SimplePreparedStatementCreator(String sql) {
         Assert.notNull(sql, "SQL must not be null");
         this.sql = sql;
      }

      @Override
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
         return con.prepareStatement(this.sql);
      }

      @Override
      public String getSql() {
         return this.sql;
      }
   }



   private static class SimpleCallableStatementCreator implements CallableStatementCreator, SqlProvider {

      private final String callString;

      public SimpleCallableStatementCreator(String callString) {
         Assert.notNull(callString, "Call string must not be null");
         this.callString = callString;
      }

      @Override
      public CallableStatement createCallableStatement(Connection con) throws SQLException {
         return con.prepareCall(this.callString);
      }

      @Override
      public String getSql() {
         return this.callString;
      }
   }



   /**
    * Spliterator for queryForStream adaptation of a ResultSet to a Stream.
    *
    * @since 5.3
    */
   private static class ResultSetSpliterator<T> implements Spliterator<T> {

      private final ResultSet rs;

      private final RowMapper<T> rowMapper;

      private int rowNum = 0;

      public ResultSetSpliterator(ResultSet rs, RowMapper<T> rowMapper) {
         this.rs = rs;
         this.rowMapper = rowMapper;
      }

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
         try {
            if (this.rs.next()) {
               action.accept(this.rowMapper.mapRow(this.rs, this.rowNum++));
               return true;
            }
            return false;
         } catch (SQLException ex) {
            throw new InvalidResultSetAccessException(ex);
         }
      }

      @Override
      @Nullable
      public Spliterator<T> trySplit() {
         return null;
      }

      @Override
      public long estimateSize() {
         return Long.MAX_VALUE;
      }

      @Override
      public int characteristics() {
         return Spliterator.ORDERED;
      }

      public Stream<T> stream() {
         return StreamSupport.stream(this, false);
      }
   }

}
