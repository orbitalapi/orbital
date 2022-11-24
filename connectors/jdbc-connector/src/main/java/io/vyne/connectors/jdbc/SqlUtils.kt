package io.vyne.connectors.jdbc

import lang.taxi.types.Annotatable
import lang.taxi.types.Type
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.JDBCUtils
import java.util.*

object SqlUtils {

   /**
    * Returns the annotated TableName if present, otherwise
    * falls back to the type name (without namespace)
    */
   fun tableNameOrTypeName(type: Type, tableNameSuffix: String? = null): String {
      val tableName = if (hasTableAnnotation(type)) {
         getTableName(type)
      } else {
         // Putting .toLowerCase() here, as Postgres (which I'm testing with)
         // fails with uppercase chars for tablenames if not quoted (and JOOQ doesn't
         // appear to be handling the quoting)
         type.toQualifiedName().typeName.lowercase(Locale.getDefault())
      }
      return tableName + tableNameSuffix.orEmpty()
   }

   fun getTableName(type: Type): String {
      require(type is Annotatable) { "Type ${type.qualifiedName} does not support annotations" }
      val tableAnnotation =
         type.annotations.firstOrNull { it.qualifiedName == JdbcConnectorTaxi.Annotations.Table.NAME }
            ?: error("Type ${type.qualifiedName} is missing a ${JdbcConnectorTaxi.Annotations.Table.NAME} annotation")
      val tableWrapper = JdbcConnectorTaxi.Annotations.Table.from(tableAnnotation)
      return tableWrapper.tableName
   }

   fun hasTableAnnotation(type: Type): Boolean {
      require(type is Annotatable) { "Type ${type.qualifiedName} does not support annotations" }
      return type.annotations.any { it.qualifiedName == JdbcConnectorTaxi.Annotations.Table.NAME }
   }
}

internal fun JdbcConnectionConfiguration.sqlBuilder(): DSLContext {
   val credentials = this.buildUrlAndCredentials()
   return DSL.using(
      JDBCUtils.dialect(credentials.url)
   )
}
