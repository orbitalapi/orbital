import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const DateFormatting: StubQueryMessageWithSlug = {
  "title": "Date formats",
  "slug": "date-formats",
  "query": {
    "schema": ``,
    "query": `// Add a format on a defined type
@Format("yyyy-MM-dd'T'HH:mm:ssXXX")
type FormattedInstant inherits Instant

given { timestamp: Instant = parseDate('2023-12-25T14:30:00Z')}
find {
    @Format("yyyy-MM-dd'T'HH:mm:ssXXX")
    isoTimestamp : Instant,

    @Format("dd/MM/yyyy hh:mm a z")
    readableTimestamp : Instant

    // Here, the format is defined on a named type,
    // and we're casting a raw Instant to the named type
    withAFormattedType: (FormattedInstant) Instant

    // formats defined inline override what's defined on the type
    @Format("dd/MM/yyyy hh:mm a z")
    overridingFormattedType: (FormattedInstant) Instant
}

`,
    "parameters": {},
    "stubs": [],
    "readme": "# Working with Dates in Taxi\n\nTaxi provides four date/time types, each with configurable formats using the `@Format` annotation.\n\n## Default Formats\n\nWhen no `@Format` annotation is provided, these formats are used:\n\n| Type | Default Format | Example |\n|------|----------------|---------|\n| Date | `yyyy-MM-dd` | 2023-12-25 |\n| Time | `HH:mm:ss` | 13:45:30 |\n| DateTime | `yyyy-MM-dd'T'HH:mm:ss.SSS` | 2023-12-25T13:45:30.123 |\n| Instant | `yyyy-MM-dd'T'HH:mm:ss[.SSS]X` | 2023-12-25T13:45:30.123Z |\n\n## Adding a format\nFormats are added using a `@Format()` annotation, using the following symbols:\n\n| Symbol | Meaning | Example |\n|--------|---------|---------|\n| y | Year | 2023, 23 |\n| M | Month | 12, Dec |\n| d | Day | 25 |\n| H | Hour (24h) | 13 |\n| h | Hour (12h) | 01 |\n| m | Minute | 30 |\n| s | Second | 45 |\n| S | Millisecond | 123 |\n| a | AM/PM | PM |\n| Z | Timezone | +0000, -0800 |\n| z | Timezone name | PST |\n| X | ISO timezone | Z, +00:00 |\n\n## Examples\n\n### Basic Date Formatting\nConvert a date to a different format:\n```taxiql\ngiven { date: Date = parseDate('2023-12-25')}\nfind { \n    @Format(\"dd/MM/yyyy\")\n    formattedDate : Date\n}\n```\n\n### Multiple Format Conversions\nShow the same date in different formats:\n```taxiql\ngiven { date: Date = parseDate('2023-12-25')}\nfind { \n    @Format(\"dd/MM/yyyy\")\n    shortDate : Date,\n    \n    @Format(\"EEEE, MMMM d, yyyy\")\n    longDate : Date,\n    \n    @Format(\"dd-MMM-yy\")\n    mediumDate : Date\n}\n```\n\n### Working with timestamps\nFormat a timestamp in different ways:\n```taxiql\n// Add a format on a defined type\n@Format(\"yyyy-MM-dd'T'HH:mm:ssXXX\")\ntype FormattedInstant inherits Instant\n\ngiven { timestamp: Instant = parseDate('2023-12-25T14:30:00Z')}\nfind {\n    @Format(\"yyyy-MM-dd'T'HH:mm:ssXXX\")\n    isoTimestamp : Instant,\n    \n    @Format(\"dd/MM/yyyy hh:mm a z\")\n    readableTimestamp : Instant\n\n    // Here, the format is defined on a named type,\n    // and we're casting a raw Instant to the named type\n    withAFormattedType: (FormattedInstant) Instant\n\n    // formats defined inline override what's defined on the type\n    @Format(\"dd/MM/yyyy hh:mm a z\")\n    overridingFormattedType: (FormattedInstant) Instant\n}\n\n\n```\n\n## Type Definitions\n\n### Date Only\n```taxi\n// Just the date, no time component\n@Format(\"dd/MM/yyyy\")\ntype PublishDate inherits Date\n```\n\n### Time Only\n```taxi\n// Just the time, no date component\n@Format(\"HH:mm\")\ntype OpeningTime inherits Time\n```\n\n### Date and Time\n```taxi\n// Date and time, but no timezone\n@Format(\"yyyy-MM-dd HH:mm\")\ntype AppointmentTime inherits DateTime\n```\n\n### Full Timestamp\n```taxi\n// Complete timestamp with timezone\n@Format(\"yyyy-MM-dd'T'HH:mm:ss.SSSZ\")\ntype EventTime inherits Instant\n```\n\n## Common Format Patterns\n\n### Dates\n```taxi\n@Format(\"dd/MM/yy\")\ntype ShortDate inherits Date        // 25/12/23\n\n@Format(\"dd-MMM-yyyy\")\ntype MediumDate inherits Date       // 25-Dec-2023\n\n@Format(\"EEEE, MMMM d\")\ntype LongDate inherits Date         // Monday, December 25\n```\n\n### Times\n```taxi\n@Format(\"HH:mm\")\ntype BasicTime inherits Time        // 13:30\n\n@Format(\"HH:mm:ss\")\ntype TimeWithSeconds inherits Time  // 13:30:45\n\n@Format(\"hh:mm a\")\ntype TwelveHourTime inherits Time  // 01:30 PM\n```\n\n### Timestamps\n```taxi\n@Format(\"yyyy-MM-dd'T'HH:mm:ss'Z'\")\ntype UTCTime inherits Instant      // 2023-12-25T13:30:45Z\n\n@Format(\"yyyy-MM-dd HH:mm:ss Z\")\ntype ZonedTime inherits Instant    // 2023-12-25 13:30:45 +0000\n```\n\n"
  }
}
