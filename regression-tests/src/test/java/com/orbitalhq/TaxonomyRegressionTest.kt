package com.orbitalhq

import com.orbitalhq.support.TestHelper
import com.orbitalhq.support.TestHelper.bristolCBOrderReportType
import com.orbitalhq.support.TestHelper.niceaType
import com.orbitalhq.support.TestHelper.troyType
import com.orbitalhq.support.TestHelper.bankIrdReport
import com.orbitalhq.support.TestHelper.bankOrdersType
import com.orbitalhq.support.TestHelper.bankRfqType
import com.orbitalhq.support.TestHelper.lesbosWebOrderFilled
import com.orbitalhq.support.TestHelper.lesbosWebOrderReportView
import com.orbitalhq.support.TestHelper.lesbosWebOrderSent
import com.orbitalhq.support.TestHelper.smyrnaType
import com.orbitalhq.support.TestHelper.londonOrderType
import com.orbitalhq.support.TestHelper.tenedosOrdersType
import com.orbitalhq.support.TestHelper.tenedosTradesType
import com.orbitalhq.support.TestHelper.rfqConvertibleBondsReport
import com.orbitalhq.support.TestHelper.rfqConvertibleBondsType
import com.orbitalhq.support.TestHelper.rfqIrdType
import com.orbitalhq.support.TestHelper.philadelphiaType
import com.orbitalhq.support.TestHelper.magnesiaType
import com.orbitalhq.support.TestHelper.knidosType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate

/**
 * If you want to run this test against a particular version of Vyne, then use:
 * -Drun.mode=docker
 * This will run the system in docker, to specify the Vyne Version, use:
 * -Dvyne.tag=1.8.9
 */
class TaxonomyRegressionTest {
   @Test
   fun `find All nicean Orders `() {
      val response = TestHelper.submitVyneQl("""
            find {
              $niceaType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(niceaType))
   }

   @Test
   fun `find All troy Orders `() {
      val response = TestHelper.submitVyneQl("""
            find {
              $troyType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(troyType))
   }

   @Test
   fun `find All smyrna orders`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $smyrnaType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(smyrnaType))
   }

   @Test
   fun `find All philadelphia orders`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $philadelphiaType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(philadelphiaType))
   }

   @Test
   fun `find All magnesia orders`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $magnesiaType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(magnesiaType))
   }

   @Test
   fun `find All knidos orders`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $knidosType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(knidosType))
   }

   @Test
   fun `find All tenedos orders`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $tenedosOrdersType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(tenedosOrdersType))
   }

   @Test
   fun `find All tenedos trades`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $tenedosTradesType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(tenedosTradesType))
   }

   @Test
   fun `find All Convertible Bond Rfqs`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $rfqConvertibleBondsType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(rfqConvertibleBondsType))
   }

   @Test
   fun `find All Ird Rfqs`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $rfqIrdType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(rfqIrdType))
   }

   @Test
   fun `bristol CBOrder Report for today`() {
      val response = TestHelper.submitVyneQl("""
            ${betweenCaskInsertedAtVyneQlQuery(bankOrdersType, bristolCBOrderReportType)}
        """.trimIndent())
      TestHelper.compareCsvContents(response, findCaskBetweenExpectedFile(bristolCBOrderReportType))
   }

   @Test
   fun `Rfq ConvertibleBonds Report for Today`() {
      val response = TestHelper.submitVyneQl("""
            ${betweenCaskInsertedAtVyneQlQuery(bankRfqType, rfqConvertibleBondsReport)}
        """.trimIndent())
      TestHelper.compareCsvContents(response, findCaskBetweenExpectedFile(rfqConvertibleBondsReport))
   }

   @Test
   fun `london Report for today`() {
      val response = TestHelper.submitVyneQl("""
            ${betweenCaskInsertedAtVyneQlQuery(bankOrdersType, londonOrderType)}
        """.trimIndent())
      TestHelper.compareCsvContents(response, findCaskBetweenExpectedFile(londonOrderType))
   }

   @Test
   fun `Ird Rfqs Report for today`() {
      val response = TestHelper.submitVyneQl("""
            ${betweenCaskInsertedAtVyneQlQuery(rfqIrdType, bankIrdReport)}
        """.trimIndent())
      TestHelper.compareCsvContents(response, findCaskBetweenExpectedFile(bankIrdReport))
   }

   @Test
   fun `findAll lesbos OrderSent`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $lesbosWebOrderSent[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(lesbosWebOrderSent))

   }

   @Test
   fun `findAll lesbos OrderFilled`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $lesbosWebOrderFilled[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(lesbosWebOrderFilled))

   }

   @Test
   fun `findAll LesbosOrderReportView`() {
      val response = TestHelper.submitVyneQl("""
            find {
              $lesbosWebOrderReportView[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(lesbosWebOrderReportView))

   }

   // TODO Add tests for other queries....

   companion object {
      val logger = LoggerFactory.getLogger(TaxonomyRegressionTest::class.java)
      @JvmStatic
      @BeforeAll
      fun init() {
         TestHelper.init()
      }

      @JvmStatic
      @AfterAll
      fun destroy() {
         TestHelper.destroy()
      }

      private fun betweenCaskInsertedAtVyneQlQuery(
         fetchType: String,
         projectedToType: String): String {
         val now = LocalDate.now()
         return """
                find { $fetchType[] ( CaskInsertedAt >= "${now}T00:00:00", CaskInsertedAt < "${now.plusDays(1)}T00:00:00" ) } as $projectedToType[]
            """.trimIndent()
      }

      private fun findAllExpectedFile(type: String) = File("src/test/resources/expected_responses/findAll/$type")
      private fun findCaskBetweenExpectedFile(projectedToType: String) = File("src/test/resources/expected_responses/betweenCaskInsertedAt/$projectedToType")
   }
}
