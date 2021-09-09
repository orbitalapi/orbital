package io.vyne

import com.winterbe.expekt.should
import io.vyne.support.TestHelper
import io.vyne.support.TestHelper.findAllExpectedFile
import io.vyne.support.VyneUtil
import org.junit.jupiter.api.RepeatedTest

/**
 * If you want to run this test against a particular version of Vyne, then use:
 * -Drun.mode=docker
 * This will run the system in docker, to specify the Vyne Version, use:
 * -Dvyne.tag=1.8.9
 */
class StressRegressionTest {
   // Change the port number if you run Vyne anything other than 9022
   private val vyneUtil = VyneUtil("9022")
   @RepeatedTest(200)
   fun `hammer Vyne`() {
      //nicea
      testCaskDeletePostAndQuery(TestHelper.niceaType, "src/test/resources/nicea", this::uploadCsvFileToCask)
      //troy
      testCaskDeletePostAndQuery(TestHelper.troyType, "src/test/resources/troy", this::uploadCsvFileToCask)
      //smyrna
      testCaskDeletePostAndQuery(TestHelper.smyrnaType, "src/test/resources/smyrna", this::uploadCsvFileToCask)
      //philadelphia
      testCaskDeletePostAndQuery(TestHelper.philadelphiaType, "src/test/resources/philadelphia", this::uploadJsonFiles)
      //magnesia
      testCaskDeletePostAndQuery(TestHelper.magnesiaType, "src/test/resources/magnesia", this::uploadCsvFileToCask)
      //knidos
      testCaskDeletePostAndQuery(TestHelper.knidosType, "src/test/resources/knidos", this::uploadCsvFileToCask)
      //tenedos trades
      testCaskDeletePostAndQuery(TestHelper.tenedosTradesType, "src/test/resources/tenedos/trades", this::uploadJsonFiles)
      //tenedos orders
      testCaskDeletePostAndQuery(TestHelper.tenedosOrdersType, "src/test/resources/tenedos/orders", this::uploadJsonFiles)
      //Convertible bonds
      testCaskDeletePostAndQuery(TestHelper.rfqConvertibleBondsType, "src/test/resources/rfq/cb", this::uploadJsonFiles)
      //Ird
      testCaskDeletePostAndQuery(TestHelper.rfqIrdType, "src/test/resources/rfq/ird", this::uploadJsonFiles)
      //lesbos OrderSent
      testCaskDeletePostAndQuery(TestHelper.lesbosWebOrderSent, "src/test/resources/lesbos/OrderSent", this::uploadCsvFileToCask)
      //lesbos OrderFill
      testCaskDeletePostAndQuery(TestHelper.lesbosWebOrderFilled, "src/test/resources/lesbos/OrderFilled", this::uploadCsvFileToCask)
   }

   private fun testCaskDeletePostAndQuery(
      fullyQualifiedTypeName: String,
      dataFolder: String,
      uploadFunc: (String, String) -> MutableList<Int>?) {
      deletePostQuery(fullyQualifiedTypeName).should.equal(200)
      uploadFunc(dataFolder, fullyQualifiedTypeName)!!.toSet().should.equal(setOf(200))
      vyneUtil.ensureType("vyne.cask.$fullyQualifiedTypeName", 10, 5000)
      val response = TestHelper.submitVyneQl("""
            findAll {
              $fullyQualifiedTypeName[]
            }
        """.trimIndent())
      TestHelper.compareCsvContents(response, findAllExpectedFile(fullyQualifiedTypeName))
   }

   private fun deletePostQuery(typeName: String): Int {
      return vyneUtil.deleteCask(typeName)
   }


   private fun uploadCsvFileToCask(csvFileFolder: String, typeName: String): MutableList<Int>? {
      return TestHelper.uploadFileOnly(csvFileFolder, typeName) { file, _ ->
         vyneUtil.postCsvDataFile(file, typeName)
      }
   }

   private fun uploadXmlFiles(xmlFileFolder: String, typeName: String): MutableList<Int>? {
      return TestHelper.uploadFileOnly(xmlFileFolder, typeName) { file, _ ->
         vyneUtil.postXmlDataFile(file, typeName)
      }
   }

   private fun uploadJsonFiles(jsonFileFolder: String, typeName: String): MutableList<Int>? {
      return TestHelper.uploadFileOnly(jsonFileFolder, typeName) { file, _ ->
         vyneUtil.postJsonDataFile(file, typeName)
      }
   }

}
