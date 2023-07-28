package io.vyne.query.runtime.executor

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints
import java.util.function.Consumer

// Required for Jooq to work with GraalVM
// from this comment:
// https://github.com/spring-projects/spring-boot/issues/33552#issuecomment-1381425721
// Also worth tracking this:
// https://github.com/jOOQ/jOOQ/issues/8779 in the Jooq repo, where it's being worked on
@Configuration
@ImportRuntimeHints(NativeImageRuntimeHintsConfiguration.ThirdPartyHintsRegistrar::class)
class NativeImageRuntimeHintsConfiguration {
   internal class ThirdPartyHintsRegistrar : RuntimeHintsRegistrar {
      override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
         // Temporary hints which should be included into the libraries, or https://github.com/oracle/graalvm-reachability-metadata
         listOf(
            "java.time.LocalDate[]",
            "java.time.LocalDateTime[]",
            "java.time.LocalTime[]",
            "java.time.ZonedDateTime[]",
            "java.time.OffsetDateTime[]",
            "java.time.OffsetTime[]",
            "java.time.Instant[]",
            "java.sql.Timestamp[]",
            "java.sql.Date[]",
            "java.sql.Time[]",
            "java.math.BigInteger[]",
            "java.math.BigDecimal[]",
            "org.jooq.types.UNumber[]",
            "org.jooq.types.UByte[]",
            "org.jooq.types.UInteger[]",
            "org.jooq.types.ULong[]",
            "org.jooq.types.Unsigned[]",
            "org.jooq.types.UShort[]",
            "java.lang.Byte[]",
            "java.lang.Integer[]",
            "java.lang.Long[]",
            "java.lang.Float[]",
            "java.lang.Double[]",
            "java.lang.String[]",
            "org.jooq.types.YearToMonth[]",
            "org.jooq.types.YearToSecond[]",
            "org.jooq.types.DayToSecond[]",
            "org.jooq.RowId[]",
            "org.jooq.Result[]",
            "org.jooq.Record[]",
            "org.jooq.JSON[]",
            "org.jooq.JSONB[]",
            "org.jooq.XML[]",
            "org.jooq.Geography[]",
            "org.jooq.Geometry[]",
            "java.util.UUID[]",
            "byte[]",
            "org.jooq.impl.SQLDataType",
            "org.jooq.util.cubrid.CUBRIDDataType",
            "org.jooq.util.derby.DerbyDataType",
            "org.jooq.util.firebird.FirebirdDataType",
            "org.jooq.util.h2.H2DataType",
            "org.jooq.util.hsqldb.HSQLDBDataType",
            "org.jooq.util.ignite.IgniteDataType",
            "org.jooq.util.mariadb.MariaDBDataType",
            "org.jooq.util.mysql.MySQLDataType",
            "org.jooq.util.postgres.PostgresDataType",
            "org.jooq.util.sqlite.SQLiteDataType",
            "org.jooq.util.oracle.OracleDataType",
            "org.jooq.util.sqlserver.SQLServerDataType",
            "org.jooq.impl.DefaultBinding\$Mdsys",
            "org.jooq.impl.DefaultBinding\$SdoElemInfoArray",
            "org.jooq.impl.DefaultBinding\$SdoOrdinateArray",
            "org.jooq.impl.DefaultBinding\$SdoGeometry",
            "org.jooq.impl.DefaultBinding\$SdoGeometryRecord",
            "org.jooq.impl.DefaultBinding\$SdoPointType",
            "org.jooq.impl.DefaultBinding\$SdoPointTypeRecord"
         ).forEach(Consumer { it: String ->
            hints.reflection().registerType(TypeReference.of(it), MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
         })
      }
   }
}
