package com.orbitalhq.plugins.jdbc

import java.sql.Driver

/**
 * This is a very simple proxy around a JDBC driver implementation, so that the
 * DriverManager will accept registered drivers.
 *
 * There is an issue with the DriverManager that it refuses to load drivers that aren't
 * present in the system classloader.
 *
 * Therefore, because this DriverProxy is present in the system class-loader, this works around the
 * issue.
 *
 * https://www.kfu.com/~nsayer/Java/dyn-jdbc.html
 * https://stackoverflow.com/a/10781394/59015
 * https://stackoverflow.com/a/14479658/59015
 */
class DriverProxy(private val driver:Driver) : Driver by driver {
}
