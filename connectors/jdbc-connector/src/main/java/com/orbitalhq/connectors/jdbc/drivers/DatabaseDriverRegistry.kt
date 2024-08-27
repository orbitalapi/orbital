package com.orbitalhq.connectors.jdbc.drivers

import com.orbitalhq.connectors.config.jdbc.DatabaseDriverName
import com.orbitalhq.connectors.jdbc.drivers.h2.H2DatabaseSupport
import com.orbitalhq.connectors.jdbc.drivers.mssql.MssqlDbSupport
import com.orbitalhq.connectors.jdbc.drivers.postgres.PostgresDbSupport
import com.orbitalhq.connectors.jdbc.drivers.redshift.RedshiftDatabaseSupport
import com.orbitalhq.connectors.jdbc.drivers.snowflake.SnowflakeDatabaseSupport


class DatabaseDriverRegistry {
   private val _drivers = mutableMapOf<DatabaseDriverName, DatabaseSupport>()

   fun addDriver(driver: DatabaseSupport): DatabaseDriverRegistry {
      _drivers[driver.driverName] = driver
      return this
   }

   val drivers: List<DatabaseSupport>
      get() {
         return _drivers.values.toList()
      }

   fun addDrivers(vararg drivers: DatabaseSupport): DatabaseDriverRegistry {
      drivers.forEach { driver -> addDriver(driver) }
      return this
   }

   fun getDatabaseDriver(driverName: DatabaseDriverName): DatabaseSupport {
      return _drivers[driverName] ?: error("No database driver with name $driverName is registered")
   }

   companion object {
      fun withBuiltInDrivers(): DatabaseDriverRegistry {
         val registry = DatabaseDriverRegistry()
         registry.addDrivers(
            H2DatabaseSupport,
            PostgresDbSupport,
            MssqlDbSupport,
            RedshiftDatabaseSupport,
            SnowflakeDatabaseSupport
         )
         return registry
      }
   }
}
