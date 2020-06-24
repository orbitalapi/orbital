# Data Transformation

## Requirement
Read multiple files, in multiple different formats which can be
projected to a standard taxonomy.

Support querying across the models, including aggregations.

## Challenges

### Ingest files in different formats
How do we handle ingestion of different formats?

### Date formats
Can we encode date formats in the data type / schema. Producers of flat files
will not use common formats for this, so we need to be able to encode this at the schema level

Do we create 'special' formats in Taxi for ingesting this?  Should we go as far as having a FormattedType?
We could do something like:

```
format TemporalFormat {
    'yyyy' -> LongYear : Int
    'yy' -> ShortYear : Int
    'mon' -> ignorecase enum MonthName { JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC } // Adds concept 'ignorecase enum'
    'm' -> MonthNumber : Int  // Do we need to support Padded / unpadded here?
    'mm' -> PaddedMonthNumber : Int(1..12) // Note - ranged types
    'd' -> DayOfMonth : Int(1..31)
    'dd' -> PaddedDayOfMonth : Int(1..31)
    'h' -> HourOfDay12Hr : Int(1..12)
    'hh' -> HourOfDay12Hr : Int(1..12)
    'HH' -> HourOfDay24Hr : Int(0..23)
    'p' -> ignorecase enum AmPm { AM, PM }
    'MM' -> Minute : Int(0..59)
    'SS' -> Second : Int(0..59)
    'L' -> Millisecond : Int(0..999)
    'Z' -> ZoneOffset : Int
    'EM' -> MillisecondSinceEpoch : Int
    'ES' -> SecondSinceEpoch : Int
    '-' | 'T' | ':' | '.' | '/' | ' ' -> Seperator : String

    // Support mapping functions
    // How do handle the scripts in here?  Drop out to another lang (kotlin?)
    // or support our own limited language?
    // Or, offer some native mappers?
    fun(h:HourOfDay12Hr, p:AmPm):HourOfDay24Hr  {
        return when(p) {
            AM : h
            PM : h + 12 
        }
    } 
}

formatted type Instant( format : TemporalFormat ) {
    year : TemporalFormat.LongYear
    month : TemporalFormat.MonthNumber
    day : TemporalFormat.DayOfMonth
    hour : TemporalFormat.HourOfDay24Hr
    minute : TemporalFormat.Minute
    second : TemporalFormat.Second
    milli -> TemporalFormat.Millisecond
}
type IsoDateTime : Instant( format : 'yyyy-mm-ddTHH:MM:SS.LLLLZ' ) // Should match 2020-04-20T23:40:23.0Z
type OddDateTime : Instant( format : 'mon/dd/yy hh:MM:ss p' )  // Should match Apr/23/20 11:40:23 PM
```


## Versioning Taxi schemas
Ingestion and transformation is going to evolve over time, but we should be able to remain
backwards compatable, and ensure repeatability.

Therefore, we likely need to flesh out import syntax to support resolution

```
// Current syntax, assumes all imports are resolvable locally, because Vyne is doing the import:

import test.FirstName

// Proposed syntax
import test.FirstName from @folder/orders // Takes latest, or if a dependency is defined in taxi.conf, uses that
import test.FirstName from @folder/orders/2.3.0 // Takes a specific version, regardless of what is defined in taxi.conf
```

In the above, when trying to resolve, we'll need to apply a resolution algo, that uses the heirachy of taxi.conf
files to find both a local repo somewhere, and remote repos to call out to.

## Flat file ingestion

We can get significant mileage by supporting columnlar data.
This should support *sv files (eg., psv, csv, tsv), an excel/google spreadsheet, or even a db.

```
type Person {
    firstName : FirstName as String
    lastName : LastName as String
}

// File access should use ant wildcard style matching to identify either a single file, or a 
// list of files.

source csv(`/some/localfile/location') provides rowsOf Person {
    firstName by column(0) // column as a new accessor
    lastName by column(1)
}

// Or, as an inline type:
source csv(`/some/localfile/location') provides rowsOf Person {
    firstName : FirstName as String by column(0)
    lastName : LastName as String by column(1)
}

// support accessing an excel spreadsheet
source excel('/some/file/location') provides rowsOf Person {
}

database('jdbc://some/db/access') {
    source table('someTableName') provides rowOf Person {
        firstName : FirstName by dbColumn('someColumnName')
        lasttName : LastName by dbColumn('someColumnName')
    }
}
```


### Additional providers
We should rethink the HTTP operatons we currently in the context of "providers", and - at some point -
refactor towards them.

We're going to need to import data from file based resources - including raw formats like CSV / PSV / TSV, or
richer formats like Excel. 

### Defining external providers
