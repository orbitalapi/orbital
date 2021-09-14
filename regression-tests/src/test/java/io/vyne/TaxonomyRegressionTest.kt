package io.vyne

import io.vyne.support.TestHelper
import io.vyne.support.TestHelper.bristolCBOrderReportType
import io.vyne.support.TestHelper.niceaType
import io.vyne.support.TestHelper.troyType
import io.vyne.support.TestHelper.bankIrdReport
import io.vyne.support.TestHelper.bankOrdersType
import io.vyne.support.TestHelper.bankRfqType
import io.vyne.support.TestHelper.lesbosWebOrderFilled
import io.vyne.support.TestHelper.lesbosWebOrderReportView
import io.vyne.support.TestHelper.lesbosWebOrderSent
import io.vyne.support.TestHelper.smyrnaType
import io.vyne.support.TestHelper.londonOrderType
import io.vyne.support.TestHelper.tenedosOrdersType
import io.vyne.support.TestHelper.tenedosTradesType
import io.vyne.support.TestHelper.rfqConvertibleBondsReport
import io.vyne.support.TestHelper.rfqConvertibleBondsType
import io.vyne.support.TestHelper.rfqIrdType
import io.vyne.support.TestHelper.philadelphiaType
import io.vyne.support.TestHelper.magnesiaType
import io.vyne.support.TestHelper.knidosType
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
            findAll {
              $niceaType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(niceaType))
   }

   @Test
   fun `find All troy Orders `() {
      val response = TestHelper.submitVyneQl("""
            findAll {
              $troyType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(troyType))
   }

   @Test
   fun `find All smyrna orders`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
              $smyrnaType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(smyrnaType))
   }

   @Test
   fun `find All philadelphia orders`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
              $philadelphiaType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(philadelphiaType))
   }

   @Test
   fun `find All magnesia orders`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
              $magnesiaType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(magnesiaType))
   }

   @Test
   fun `find All knidos orders`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
              $knidosType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(knidosType))
   }

   @Test
   fun `find All tenedos orders`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
              $tenedosOrdersType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(tenedosOrdersType))
   }

   @Test
   fun `find All tenedos trades`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
              $tenedosTradesType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(tenedosTradesType))
   }

   @Test
   fun `find All Convertible Bond Rfqs`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
              $rfqConvertibleBondsType[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(rfqConvertibleBondsType))
   }

   @Test
   fun `find All Ird Rfqs`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
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
            findAll {
              $lesbosWebOrderSent[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(lesbosWebOrderSent))

   }

   @Test
   fun `findAll lesbos OrderFilled`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
              $lesbosWebOrderFilled[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(lesbosWebOrderFilled))

   }

   @Test
   fun `findAll LesbosOrderReportView`() {
      val response = TestHelper.submitVyneQl("""
            findAll {
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
                findAll { $fetchType[] ( CaskInsertedAt >= "${now}T00:00:00", CaskInsertedAt < "${now.plusDays(1)}T00:00:00" ) } as $projectedToType[]
            """.trimIndent()
      }

      private fun findAllExpectedFile(type: String) = File("src/test/resources/expected_responses/findAll/$type")
      private fun findCaskBetweenExpectedFile(projectedToType: String) = File("src/test/resources/expected_responses/betweenCaskInsertedAt/$projectedToType")
   }
}
