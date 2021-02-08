package io.vyne.cask.ingest

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.Provided
import io.vyne.models.TypedObject
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Benchmark
import io.vyne.utils.Benchmark.benchmark
import org.junit.Test

class JsonDeserializationTest {

   companion object {
      val taxi = """namespace foo {

model Dummy {
   recordId : String
   dummyField1 : String
	dummyField2 : String
	dummyField3 : String
	dummyField4 : String
	dummyField5 : String
	dummyField6 : String
	dummyField7 : String
	dummyField8 : String
	dummyField9 : String
	dummyField10 : String
	dummyField11 : String
	dummyField12 : String
	dummyField13 : String
	dummyField14 : String
	dummyField15 : String
	dummyField16 : String
	dummyField17 : String
	dummyField18 : String
	dummyField19 : String
	dummyField20 : String
	dummyField21 : String
	dummyField22 : String
	dummyField23 : String
	dummyField24 : String
	dummyField25 : String
	dummyField26 : String
	dummyField27 : String
	dummyField28 : String
	dummyField29 : String
	dummyField30 : String
	dummyField31 : String
	dummyField32 : String
	dummyField33 : String
	dummyField34 : String
	dummyField35 : String
	dummyField36 : String
	dummyField37 : String
	dummyField38 : String
	dummyField39 : String
	dummyField40 : String
	dummyField41 : String
	dummyField42 : String
	dummyField43 : String
	dummyField44 : String
	dummyField45 : String
	dummyField46 : String
	dummyField47 : String
	dummyField48 : String
	dummyField49 : String
	dummyField50 : String
	dummyField51 : String
	dummyField52 : String
	dummyField53 : String
	dummyField54 : String
	dummyField55 : String
	dummyField56 : String
	dummyField57 : String
	dummyField58 : String
	dummyField59 : String
	dummyField60 : String
	dummyField61 : String
	dummyField62 : String
	dummyField63 : String
	dummyField64 : String
	dummyField65 : String
	dummyField66 : String
	dummyField67 : String
	dummyField68 : String
	dummyField69 : String
	dummyField70 : String
	dummyField71 : String
	dummyField72 : String
	dummyField73 : String
	dummyField74 : String
	dummyField75 : String
	dummyField76 : String
	dummyField77 : String
	dummyField78 : String
	dummyField79 : String
	dummyField80 : String
	dummyField81 : String
	dummyField82 : String
	dummyField83 : String
	dummyField84 : String
	dummyField85 : String
	dummyField86 : String
	dummyField87 : String
	dummyField88 : String
	dummyField89 : String
	dummyField90 : String
	dummyField91 : String
	dummyField92 : String
	dummyField93 : String
	dummyField94 : String
	dummyField95 : String
	dummyField96 : String
	dummyField97 : String
	dummyField98 : String
	dummyField99 : String
	dummyField100 : String
	dummyField101 : String
	dummyField102 : String
	dummyField103 : String
	dummyField104 : String
	dummyField105 : String
	dummyField106 : String
	dummyField107 : String
	dummyField108 : String
	dummyField109 : String
	dummyField110 : String
	dummyField111 : String
	dummyField112 : String
	dummyField113 : String
	dummyField114 : String
	dummyField115 : String
	dummyField116 : String
	dummyField117 : String
	dummyField118 : String
	dummyField119 : String
	dummyField120 : String
	dummyField121 : String
	dummyField122 : String
	dummyField123 : String
	dummyField124 : String
	dummyField125 : String
	dummyField126 : String
	dummyField127 : String
	dummyField128 : String
	dummyField129 : String
	dummyField130 : String
	dummyField131 : String
	dummyField132 : String
	dummyField133 : String
	dummyField134 : String
	dummyField135 : String
	dummyField136 : String
	dummyField137 : String
	dummyField138 : String
	dummyField139 : String
	dummyField140 : String
	dummyField141 : String
	dummyField142 : String
	dummyField143 : String
	dummyField144 : String
	dummyField145 : String
	dummyField146 : String
	dummyField147 : String
	dummyField148 : String
	dummyField149 : String
	dummyField150 : String
	dummyField151 : String
	dummyField152 : String
	dummyField153 : String
	dummyField154 : String
	dummyField155 : String
	dummyField156 : String
	dummyField157 : String
	dummyField158 : String
	dummyField159 : String
	dummyField160 : String
	dummyField161 : String
	dummyField162 : String
	dummyField163 : String
	dummyField164 : String
	dummyField165 : String
	dummyField166 : String
	dummyField167 : String
	dummyField168 : String
	dummyField169 : String
	dummyField170 : String
	dummyField171 : String
	dummyField172 : String
	dummyField173 : String
	dummyField174 : String
	dummyField175 : String
	dummyField176 : String
	dummyField177 : String
	dummyField178 : String
	dummyField179 : String
	dummyField180 : String
	dummyField181 : String
	dummyField182 : String
	dummyField183 : String
	dummyField184 : String
	dummyField185 : String
	dummyField186 : String
	dummyField187 : String
	dummyField188 : String
	dummyField189 : String
	dummyField190 : String
	dummyField191 : String
	dummyField192 : String
	dummyField193 : String
	dummyField194 : String
	dummyField195 : String
	dummyField196 : String
	dummyField197 : String
	dummyField198 : String
	dummyField199 : String
	dummyField200 : String
}

}"""
     const val source = """{
    "recordId" : "test",
 "dummyField1" : "foo-1" ,
 "dummyField2" : "foo-2" ,
 "dummyField3" : "foo-3" ,
 "dummyField4" : "foo-4" ,
 "dummyField5" : "foo-5" ,
 "dummyField6" : "foo-6" ,
 "dummyField7" : "foo-7" ,
 "dummyField8" : "foo-8" ,
 "dummyField9" : "foo-9" ,
 "dummyField10" : "foo-10" ,
 "dummyField11" : "foo-11" ,
 "dummyField12" : "foo-12" ,
 "dummyField13" : "foo-13" ,
 "dummyField14" : "foo-14" ,
 "dummyField15" : "foo-15" ,
 "dummyField16" : "foo-16" ,
 "dummyField17" : "foo-17" ,
 "dummyField18" : "foo-18" ,
 "dummyField19" : "foo-19" ,
 "dummyField20" : "foo-20" ,
 "dummyField21" : "foo-21" ,
 "dummyField22" : "foo-22" ,
 "dummyField23" : "foo-23" ,
 "dummyField24" : "foo-24" ,
 "dummyField25" : "foo-25" ,
 "dummyField26" : "foo-26" ,
 "dummyField27" : "foo-27" ,
 "dummyField28" : "foo-28" ,
 "dummyField29" : "foo-29" ,
 "dummyField30" : "foo-30" ,
 "dummyField31" : "foo-31" ,
 "dummyField32" : "foo-32" ,
 "dummyField33" : "foo-33" ,
 "dummyField34" : "foo-34" ,
 "dummyField35" : "foo-35" ,
 "dummyField36" : "foo-36" ,
 "dummyField37" : "foo-37" ,
 "dummyField38" : "foo-38" ,
 "dummyField39" : "foo-39" ,
 "dummyField40" : "foo-40" ,
 "dummyField41" : "foo-41" ,
 "dummyField42" : "foo-42" ,
 "dummyField43" : "foo-43" ,
 "dummyField44" : "foo-44" ,
 "dummyField45" : "foo-45" ,
 "dummyField46" : "foo-46" ,
 "dummyField47" : "foo-47" ,
 "dummyField48" : "foo-48" ,
 "dummyField49" : "foo-49" ,
 "dummyField50" : "foo-50" ,
 "dummyField51" : "foo-51" ,
 "dummyField52" : "foo-52" ,
 "dummyField53" : "foo-53" ,
 "dummyField54" : "foo-54" ,
 "dummyField55" : "foo-55" ,
 "dummyField56" : "foo-56" ,
 "dummyField57" : "foo-57" ,
 "dummyField58" : "foo-58" ,
 "dummyField59" : "foo-59" ,
 "dummyField60" : "foo-60" ,
 "dummyField61" : "foo-61" ,
 "dummyField62" : "foo-62" ,
 "dummyField63" : "foo-63" ,
 "dummyField64" : "foo-64" ,
 "dummyField65" : "foo-65" ,
 "dummyField66" : "foo-66" ,
 "dummyField67" : "foo-67" ,
 "dummyField68" : "foo-68" ,
 "dummyField69" : "foo-69" ,
 "dummyField70" : "foo-70" ,
 "dummyField71" : "foo-71" ,
 "dummyField72" : "foo-72" ,
 "dummyField73" : "foo-73" ,
 "dummyField74" : "foo-74" ,
 "dummyField75" : "foo-75" ,
 "dummyField76" : "foo-76" ,
 "dummyField77" : "foo-77" ,
 "dummyField78" : "foo-78" ,
 "dummyField79" : "foo-79" ,
 "dummyField80" : "foo-80" ,
 "dummyField81" : "foo-81" ,
 "dummyField82" : "foo-82" ,
 "dummyField83" : "foo-83" ,
 "dummyField84" : "foo-84" ,
 "dummyField85" : "foo-85" ,
 "dummyField86" : "foo-86" ,
 "dummyField87" : "foo-87" ,
 "dummyField88" : "foo-88" ,
 "dummyField89" : "foo-89" ,
 "dummyField90" : "foo-90" ,
 "dummyField91" : "foo-91" ,
 "dummyField92" : "foo-92" ,
 "dummyField93" : "foo-93" ,
 "dummyField94" : "foo-94" ,
 "dummyField95" : "foo-95" ,
 "dummyField96" : "foo-96" ,
 "dummyField97" : "foo-97" ,
 "dummyField98" : "foo-98" ,
 "dummyField99" : "foo-99" ,
 "dummyField100" : "foo-100" ,
 "dummyField101" : "foo-101" ,
 "dummyField102" : "foo-102" ,
 "dummyField103" : "foo-103" ,
 "dummyField104" : "foo-104" ,
 "dummyField105" : "foo-105" ,
 "dummyField106" : "foo-106" ,
 "dummyField107" : "foo-107" ,
 "dummyField108" : "foo-108" ,
 "dummyField109" : "foo-109" ,
 "dummyField110" : "foo-110" ,
 "dummyField111" : "foo-111" ,
 "dummyField112" : "foo-112" ,
 "dummyField113" : "foo-113" ,
 "dummyField114" : "foo-114" ,
 "dummyField115" : "foo-115" ,
 "dummyField116" : "foo-116" ,
 "dummyField117" : "foo-117" ,
 "dummyField118" : "foo-118" ,
 "dummyField119" : "foo-119" ,
 "dummyField120" : "foo-120" ,
 "dummyField121" : "foo-121" ,
 "dummyField122" : "foo-122" ,
 "dummyField123" : "foo-123" ,
 "dummyField124" : "foo-124" ,
 "dummyField125" : "foo-125" ,
 "dummyField126" : "foo-126" ,
 "dummyField127" : "foo-127" ,
 "dummyField128" : "foo-128" ,
 "dummyField129" : "foo-129" ,
 "dummyField130" : "foo-130" ,
 "dummyField131" : "foo-131" ,
 "dummyField132" : "foo-132" ,
 "dummyField133" : "foo-133" ,
 "dummyField134" : "foo-134" ,
 "dummyField135" : "foo-135" ,
 "dummyField136" : "foo-136" ,
 "dummyField137" : "foo-137" ,
 "dummyField138" : "foo-138" ,
 "dummyField139" : "foo-139" ,
 "dummyField140" : "foo-140" ,
 "dummyField141" : "foo-141" ,
 "dummyField142" : "foo-142" ,
 "dummyField143" : "foo-143" ,
 "dummyField144" : "foo-144" ,
 "dummyField145" : "foo-145" ,
 "dummyField146" : "foo-146" ,
 "dummyField147" : "foo-147" ,
 "dummyField148" : "foo-148" ,
 "dummyField149" : "foo-149" ,
 "dummyField150" : "foo-150" ,
 "dummyField151" : "foo-151" ,
 "dummyField152" : "foo-152" ,
 "dummyField153" : "foo-153" ,
 "dummyField154" : "foo-154" ,
 "dummyField155" : "foo-155" ,
 "dummyField156" : "foo-156" ,
 "dummyField157" : "foo-157" ,
 "dummyField158" : "foo-158" ,
 "dummyField159" : "foo-159" ,
 "dummyField160" : "foo-160" ,
 "dummyField161" : "foo-161" ,
 "dummyField162" : "foo-162" ,
 "dummyField163" : "foo-163" ,
 "dummyField164" : "foo-164" ,
 "dummyField165" : "foo-165" ,
 "dummyField166" : "foo-166" ,
 "dummyField167" : "foo-167" ,
 "dummyField168" : "foo-168" ,
 "dummyField169" : "foo-169" ,
 "dummyField170" : "foo-170" ,
 "dummyField171" : "foo-171" ,
 "dummyField172" : "foo-172" ,
 "dummyField173" : "foo-173" ,
 "dummyField174" : "foo-174" ,
 "dummyField175" : "foo-175" ,
 "dummyField176" : "foo-176" ,
 "dummyField177" : "foo-177" ,
 "dummyField178" : "foo-178" ,
 "dummyField179" : "foo-179" ,
 "dummyField180" : "foo-180" ,
 "dummyField181" : "foo-181" ,
 "dummyField182" : "foo-182" ,
 "dummyField183" : "foo-183" ,
 "dummyField184" : "foo-184" ,
 "dummyField185" : "foo-185" ,
 "dummyField186" : "foo-186" ,
 "dummyField187" : "foo-187" ,
 "dummyField188" : "foo-188" ,
 "dummyField189" : "foo-189" ,
 "dummyField190" : "foo-190" ,
 "dummyField191" : "foo-191" ,
 "dummyField192" : "foo-192" ,
 "dummyField193" : "foo-193" ,
 "dummyField194" : "foo-194" ,
 "dummyField195" : "foo-195" ,
 "dummyField196" : "foo-196" ,
 "dummyField197" : "foo-197" ,
 "dummyField198" : "foo-198" ,
 "dummyField199" : "foo-199" ,
 "dummyField200" : "foo-200"
}"""

   }

   @Test
   fun jsonDeserialiationTest() {
      val objectMapper = ObjectMapper()
      Benchmark.benchmark("Read json", warmup = 200, iterations = 1000) {
         val tree = objectMapper.readTree(source)
         tree
      }
   }


   @Test
   fun typedObjectConversionTest() {
      val schema = TaxiSchema.from(taxi)
      val type = schema.type("foo.Dummy")
      benchmark("TypedObject", 100, 5000) {
         val f = TypedObject.fromValue(type, source, schema, source = Provided)
         f
      }

   }
}
