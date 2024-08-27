package com.orbitalhq.connectors.config.jdbc


/**
 * This class provides a way to capture the subtle
 * differences between each JDBC driver implementation
 * when fetching metadata.
 *
 * We try to provide reasonable defaults, and then let
 * each driver override as necessary.
 */
@Suppress("ArrayInDataClass")
data class JdbcMetadataParams(
   /*
    * This is the value to pass when doing a jdbc call
    * to list all tables.
    * It varies between drivers, in Postgres it's "TABLE",
    * whereas for H2 it's null.
    * Null is a valid option, and indicates not to perform a filter.
    */
   val tableTypesToListTables: Array<String>? =  arrayOf("TABLE"),

   /**
    * When iterating the results of metaData.getTables(...)
    * which column contains the name of the table?
    */
   val tableListTableNameColumn: String = "TABLE_NAME",

   /**
    * When iterating the results of metaData.getTables(...)
    * which column conains the name of the schema?
    */
   val tableListSchemaNameColumn: String = "TABLE_SCHEM", // Not a typo, _SCHEM tested against H2 and Postgres

   /**
    * When listing tables in metadata.getTables(...) use this schema pattern.
    * Null is a reasonable default here
    */
   val tableListSchemaPattern: String? = null,


   /**
    * A query to use to test the connection is valid.
    * Should be fast, and require minimal db permissions
    */
   val testQuery: String = "SELECT 1"
)


