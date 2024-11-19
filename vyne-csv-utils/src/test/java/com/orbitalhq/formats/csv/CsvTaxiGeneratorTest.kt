package com.orbitalhq.formats.csv

import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.toTaxiQualifiedName
import lang.taxi.testing.shouldCompileTheSameAs
import org.junit.jupiter.api.Test

class CsvTaxiGeneratorTest {

   @Test
   fun `can generate taxi from csv`() {
      val csv = """id|first_name|last_name|date_of_birth|salary|active|temperature_celsius|favorite_color|joined_at|scores|notes
1|John|Smith|1990-01-15|75000.50|true|37.2|blue|2023-01-01T10:00:00Z|[85,92,78]|Regular customer
2|Mary|Johnson|1988-12-03|82500.75|YES|36.9|"red, blue"|2023-01-02T15:30:00+01:00|[95,88,91]|"Prefers email, not calls"
3|Bob|Williams|1995-03-21|NULL|1|38.1|green|2023-02-15T08:45:00Z|[75,80,82]|NULL
4|Alice|Brown|1992-07-30|69999|false|36.8|yellow|2023-03-20 14:30:00|NULL|First time visitor
5|Charlie|Davis|1985-11-08|92750.25|n|37.5|"black"|2023-04-01T16:15:00Z|[90,87,93]|Needs follow-up
6|Emma|Wilson|1993-04-17|78250.00|0|36.7|NULL|2023-05-10 09:00:00|[82,85,88]|"Special handling, fragile items"
7|James|Miller|1987-09-22|88750.50|TRUE|37.0|white|2023-06-01T11:20:00Z|[88,85,91]|Priority shipping
8|Sarah|Anderson|1991-06-14|71500.25|No|36.9|"purple, pink"|2023-07-15 13:45:00|[79,83,80]|"Allergic to nuts, check packages"
9|Michael|Taylor|1989-02-28|95000.00|YES|37.3|orange|2023-08-20T17:30:00+02:00|[93,91,94]|International customer
10|Emily|Thomas|1994-08-11|68250.75|F|36.8|"red"|2023-09-01 10:15:00|[87,84,86]|"Contact between 9-5 only""""

      val generator = CsvTaxiGenerator(csv, "com.test.SampleData".fqn().toTaxiQualifiedName())
      val generated = generator.generateSchema()
      generated.concatenatedSource.shouldCompileTheSameAs("""
namespace com.test.sampledata {
   type SampledataId inherits Int
   type FirstName inherits String
   type LastName inherits String
   type DateOfBirth inherits Date
   type Salary inherits Double
   type Active inherits String
   type TemperatureCelsius inherits Double
   type FavoriteColor inherits String
   type JoinedAt inherits DateTime
   type Scores inherits String
   type Notes inherits String
}
namespace com.test {
   @com.orbitalhq.formats.Csv(delimiter = "|" , nullValue = "NULL")
   closed model SampleData {
      id : com.test.sampledata.SampledataId
      first_name : com.test.sampledata.FirstName
      last_name : com.test.sampledata.LastName
      date_of_birth : com.test.sampledata.DateOfBirth
      salary : com.test.sampledata.Salary?
      active : com.test.sampledata.Active
      temperature_celsius : com.test.sampledata.TemperatureCelsius
      favorite_color : com.test.sampledata.FavoriteColor?
      joined_at : com.test.sampledata.JoinedAt
      scores : com.test.sampledata.Scores?
      notes : com.test.sampledata.Notes?
   }
}
      """)
   }
}
