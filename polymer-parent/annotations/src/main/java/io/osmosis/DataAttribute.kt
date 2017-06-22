package io.osmosis

annotation class DataAttribute(
   /**
    * The qualified name of the attribute within a taxonomy.
    */
   val value: String,
   val format: DataFormat = DataFormat("")
)

/**
 * Specifies that an input must be provided in a specific format.
 * Eg: On a currency amount field, may specify the currency
 * On a date field, may specify the format
 * On a weight or height field, may specify the unit of measurement.
 *
 * Should be a pointer to a fully qualified Enum value within the
 * global taxonomy
 */
annotation class DataFormat(val value: String)

