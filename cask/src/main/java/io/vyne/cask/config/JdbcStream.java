package io.vyne.cask.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class JdbcStream extends JdbcTemplate {

   @Autowired
   public JdbcStream(DataSource dataSource) {
      super(dataSource);
   }

   public <T> T streamQuery(String sql, Function<Stream<SqlRow>, ? extends T> streamer, Object... args) {
      return query(sql, resultSet -> {
         final SqlRowSet rowSet = new ResultSetWrappingSqlRowSet(resultSet);
         final SqlRow sqlRow = new SqlRowAdapter(rowSet);

         Supplier<Spliterator<SqlRow>> supplier = () -> Spliterators.spliteratorUnknownSize(new Iterator<SqlRow>() {
            @Override
            public boolean hasNext() {
               return !rowSet.isLast();
            }

            @Override
            public SqlRow next() {
               if (!rowSet.next()) {
                  throw new NoSuchElementException();
               }
               return sqlRow;
            }
         }, Spliterator.IMMUTABLE);
         return streamer.apply(StreamSupport.stream(supplier, Spliterator.IMMUTABLE, false));

      }, args);
   }

   public StreamableQuery streamableQuery(String sql, Object... args) throws SQLException {
      Connection connection = DataSourceUtils.getConnection(getDataSource());
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      newArgPreparedStatementSetter(args).setValues(preparedStatement);
      return new StreamableQuery(connection, preparedStatement);
   }

   public class StreamableQuery implements Closeable {
      private final Connection connection;
      private final PreparedStatement preparedStatement;

      private StreamableQuery(Connection connection, PreparedStatement preparedStatement) {
         this.connection = connection;
         this.preparedStatement = preparedStatement;
      }

      public Stream<SqlRow> stream() throws SQLException {
         final SqlRowSet rowSet = new ResultSetWrappingSqlRowSet(preparedStatement.executeQuery());
         final SqlRow sqlRow = new SqlRowAdapter(rowSet);

         Supplier<Spliterator<SqlRow>> supplier = () -> Spliterators.spliteratorUnknownSize(new Iterator<SqlRow>() {
            @Override
            public boolean hasNext() {
               return !rowSet.isLast();
            }

            @Override
            public SqlRow next() {
               if (!rowSet.next()) {
                  throw new NoSuchElementException();
               }
               return sqlRow;
            }
         }, Spliterator.IMMUTABLE);
         return StreamSupport.stream(supplier, Spliterator.IMMUTABLE, false);
      }
      

      @Override
      public void close() throws IOException {
         DataSourceUtils.releaseConnection(connection, getDataSource());
      }
   }

   /**
    * Facade to hide the cursor movement methods of an SqlRowSet
    */
   public interface SqlRow {

      Long getLong(String columnLabel);

      String getString(String columnLabel);

      Timestamp getTimestamp(String columnLabel);
   }

   public class SqlRowAdapter implements SqlRow {
      private final SqlRowSet sqlRowSet;

      public SqlRowAdapter(SqlRowSet sqlRowSet) {
         this.sqlRowSet = sqlRowSet;
      }

      @Override
      public Long getLong(String columnLabel) {
         return sqlRowSet.getLong(columnLabel);
      }

      @Override
      public String getString(String columnLabel) {
         return sqlRowSet.getString(columnLabel);
      }

      @Override
      public Timestamp getTimestamp(String columnLabel) {
         return sqlRowSet.getTimestamp(columnLabel);
      }
   }
}

