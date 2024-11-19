package com.orbitalhq.formats.csv

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class CsvFormatDetectorTest : DescribeSpec({
    val detector = CsvFormatDetector

    describe("prelude detection") {
        it("detects comment-style prelude") {
            val csv = """
                # Generated on: 2024-03-13
                # Version: 1.0
                id,name,value
                1,test,123
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.preludeText shouldBe """
                # Generated on: 2024-03-13
                # Version: 1.0""".trimIndent()
        }

        it("detects key-value style prelude") {
            val csv = """
                Generated: 2024-03-13
                Version: 1.0
                Export-Type: Full

                id,name,value
                1,test,123
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.preludeText shouldBe """Generated: 2024-03-13
Version: 1.0
Export-Type: Full
"""
        }

        it("handles no prelude") {
            val csv = """
                id,name,value
                1,test,123
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.preludeText.shouldBeNull()
        }
    }

    describe("delimiter detection") {
        it("detects comma delimiter") {
            val csv = """
                id,name,value
                1,test,123
                2,another,456
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.delimiter shouldBe ','
        }

        it("detects pipe delimiter") {
            val csv = """
                id|name|value
                1|test|123
                2|another|456
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.delimiter shouldBe '|'
        }

        it("detects tab delimiter") {
            val csv = """
                id	name	value
                1	test	123
                2	another	456
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.delimiter shouldBe '\t'
        }
    }

    describe("quote character detection") {
        it("detects double quotes") {
            val csv = """
                id,name,description
                1,"John Smith","Hello, World"
                2,"Jane Doe","Another, value"
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.quote shouldBe '"'
        }

        it("detects single quotes") {
            val csv = """
                id,name,description
                1,'John Smith','Hello, World'
                2,'Jane Doe','Another, value'
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.quote shouldBe '\''
        }

        it("handles no quotes") {
            val csv = """
                id,name,value
                1,test,123
                2,another,456
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.quote shouldBe null
        }
    }

    describe("null value detection") {
        it("detects NULL keyword") {
            val csv = """
                id,name,value
                1,test,NULL
                2,another,NULL
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.nullValue shouldBe setOf("NULL")
        }

        it("detects NA keyword") {
            val csv = """
                id,name,value
                1,test,NA
                2,another,NA
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.nullValue shouldBe setOf("NA")
        }

        it("detects empty string as null") {
            val csv = """
                id,name,value
                1,test,
                2,another,
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.nullValue shouldBe setOf("")
        }

        it("detects slash N as null") {
            val csv = """
                id,name,value
                1,test,\N
                2,another,\N
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.nullValue shouldBe setOf("\\N")
        }
    }

    describe("header detection") {
        it("detects typical headers") {
            val csv = """
                id,first_name,last_name,email
                1,John,Smith,john@example.com
                2,Jane,Doe,jane@example.com
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.firstRecordAsHeader shouldBe true
        }

        it("detects headers with different types than data") {
            val csv = """
                id,age,active,score
                1,25,true,98.5
                2,30,false,95.2
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.firstRecordAsHeader shouldBe true
        }

        it("identifies no headers when all rows look like data") {
            val csv = """
                1,25,true,98.5
                2,30,false,95.2
                3,28,true,97.8
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.firstRecordAsHeader shouldBe false
        }
    }

    describe("record separator detection") {
        it("detects Windows-style line endings") {
            val csv = "id,name\r\n1,test\r\n2,another"

            val format = detector.detectFormat(csv)
            format.recordSeparator shouldBe "\r\n"
        }

        it("detects Unix-style line endings") {
            val csv = "id,name\n1,test\n2,another"

            val format = detector.detectFormat(csv)
            format.recordSeparator shouldBe "\n"
        }

        it("detects old Mac-style line endings") {
            val csv = "id,name\r1,test\r2,another"

            val format = detector.detectFormat(csv)
            format.recordSeparator shouldBe "\r"
        }
    }

    describe("complex scenarios") {
        it("handles mixed concerns") {
            val csv = """
                # Export date: 2024-03-13
                # Version: 1.0

                id|first_name|last_name|salary|notes
                1|John|Smith|75000.50|"Senior, experienced"
                2|Mary|Johnson|82500.75|NULL
                3|Bob|Williams|NULL|"New hire, needs training"
            """.trimIndent()

            val format = detector.detectFormat(csv)
            format.delimiter shouldBe '|'
            format.quote shouldBe '"'
            format.nullValue shouldBe setOf("NULL")
            format.firstRecordAsHeader shouldBe true
        }
    }
})
