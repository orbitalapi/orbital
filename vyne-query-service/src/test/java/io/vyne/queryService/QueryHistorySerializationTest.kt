package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import com.winterbe.expekt.should
import io.vyne.Vyne
import io.vyne.models.OperationResult
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.json.parseJsonModel
import io.vyne.query.Query
import io.vyne.query.RemoteCall
import io.vyne.query.ResultMode
import io.vyne.queryService.persistency.QueryHistoryRecordReadingConverter
import io.vyne.queryService.persistency.QueryHistoryRecordWritingConverter
import io.vyne.queryService.persistency.ReactiveDatabaseSupport
import io.vyne.schemas.fqn
//import io.vyne.testVyne
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
/*
class QueryHistorySerializationTest {

   private lateinit var objectMapper: ObjectMapper

   @Before
   fun before() {
      val reactiveDatabaseSupport = ReactiveDatabaseSupport()
      reactiveDatabaseSupport.r2dbcCustomConversions()
      objectMapper = reactiveDatabaseSupport.objectMapper
   }

   private fun mockVyne(): Vyne {
      val schema = """
            enum PersonType {
               Child,
               Adult
            }

            type alias EmailAddress as String
            type Person {
               emails: EmailAddress[]
               personType: PersonType
            }

            enum CustomerType {
               Under18 synonym of PersonType.Child,
               Adult synonym of PersonType.Adult
            }
            type Customer {
               emails : EmailAddress[]
               customerType: CustomerType
            }
            service CustomerService {
               operation customers(): Customer[]
            }
         """.trimIndent()
      val (vyne, stubService) = testVyne(schema)
      val remoteCallDataSource = mockRemoteCallDataSource(vyne)
      val collectionOfCustomers = vyne.parseJsonModel("Customer[]", """
            [
               { "emails" : ["adult1@gmail.com"], "customerType": "Adult"},
               { "emails" : ["child1@gmail.com"], "customerType": "Under18"}
            ]
            """.trimIndent(), remoteCallDataSource)
      stubService.addResponse("customers", collectionOfCustomers)

      return vyne
   }

   private fun mockRemoteCallDataSource(vyne: Vyne): OperationResult {
      val remoteCall = RemoteCall(
         "CustomerService".fqn(),
         "http://localhost/CustomerService",
         "getEmails()",
         "EmailAddress[]".fqn(),
         "GET",
         "{}",
         200,
         14,
         "{...}"
      )
      val paramValue = TypedInstance.from(vyne.schema.type("EmailAddress"), "sigma@foo.com", vyne.schema, source = Provided)
      return OperationResult(remoteCall, listOf(OperationResult.OperationParam("emailFilter", paramValue)))
   }
//
//   @Test
//   fun `can serialize and deserialize VyneqlQueryHistoryRecord - ResultMode=VERBOSE`() {
//      // prepare
//      val vyneQlQuery = """findAll { Customer[] } as Person[]""".trimIndent()
//      val result = mockVyne().query(vyneQlQuery, resultMode = ResultMode.VERBOSE)
//      val actualValue: QueryHistoryRecord<out Any> = VyneQlQueryHistoryRecord(vyneQlQuery, HistoryQueryResponse.from(result))
//      val serializedValue = QueryHistoryRecordWritingConverter(objectMapper).convert(actualValue)
//
//      // act
//      val deserializedValue = QueryHistoryRecordReadingConverter(objectMapper).convert(serializedValue)
//
//      // assert
//      actualValue.response.sources.should.be.equal(deserializedValue?.response?.sources)
//      actualValue.response.resultsVerbose.should.be.equal(deserializedValue?.response?.resultsVerbose)
//      actualValue.response.should.not.be.`null`
//   }
//
//   @Test
//   fun `can serialize and deserialize VyneqlQueryHistoryRecord - ResultMode=SIMPLE`() {
//      // prepare
//      val vyneQlQuery = """findAll { Customer[] } as Person[]""".trimIndent()
//      val result = mockVyne().query(vyneQlQuery, resultMode = ResultMode.SIMPLE)
//      val actualValue: QueryHistoryRecord<out Any> = VyneQlQueryHistoryRecord(vyneQlQuery, HistoryQueryResponse.from(result))
//      val serializedValue = QueryHistoryRecordWritingConverter(objectMapper).convert(actualValue)
//
//      // act
//      val deserializedValue = QueryHistoryRecordReadingConverter(objectMapper).convert(serializedValue)
//
//      // assert
//      actualValue.should.be.equal(deserializedValue)
//      actualValue.response.sources.should.be.equal(emptyList())
//      actualValue.response.resultsVerbose.should.be.equal(emptyMap())
//      actualValue.response.results.should.be.equal(deserializedValue?.response?.results)
//   }
//
//   @Test
//   fun `serialize VyneQL results to json, ResultMode=VERBOSE`() {
//      // prepare
//      val vyneQlQuery = """findAll { Customer[] } as Person[]""".trimIndent()
//      val result = mockVyne().query(vyneQlQuery, resultMode = ResultMode.VERBOSE)
//      val actualValue: QueryHistoryRecord<out Any> = VyneQlQueryHistoryRecord(vyneQlQuery, HistoryQueryResponse.from(result))
//      val actualJson = QueryHistoryRecordWritingConverter(objectMapper).convert(actualValue).asString()
//
//      // assert
//      val expectedJson = """
//      {
//        "className": "io.vyne.queryService.VyneQlQueryHistoryRecord",
//        "query": "findAll { Customer[] } as Person[]",
//        "response": {
//            "results": {
//               "lang.taxi.Array<Person>": [
//                 {
//                   "typeName": "Person",
//                   "value": {
//                     "emails": [
//                       {
//                         "typeName": "EmailAddress",
//                         "value": "adult1@gmail.com"
//                       }
//                     ],
//                     "personType": {
//                       "typeName": "PersonType",
//                       "value": "Adult"
//                     }
//                   }
//                 },
//                 {
//                   "typeName": "Person",
//                   "value": {
//                     "emails": [
//                       {
//                         "typeName": "EmailAddress",
//                         "value": "child1@gmail.com"
//                       }
//                     ],
//                     "personType": {
//                       "typeName": "PersonType",
//                       "value": "Child"
//                     }
//                   }
//                 }
//               ]
//             },
//          "resultsVerbose": {
//            "lang.taxi.Array<Person>": [
//              {
//                "typeName": "Person",
//                "value": {
//                  "emails": {
//                    "typeName": "lang.taxi.Array",
//                    "value": [
//                      {
//                        "typeName": "EmailAddress",
//                        "value": "adult1@gmail.com",
//                        "sourceReference": 2
//                      }
//                    ],
//                    "sourceReference": 0
//                  },
//                  "personType": {
//                    "typeName": "PersonType",
//                    "value": "Adult",
//                    "sourceReference": 3
//                  }
//                },
//                "sourceReference": 0
//              },
//              {
//                "typeName": "Person",
//                "value": {
//                  "emails": {
//                    "typeName": "lang.taxi.Array",
//                    "value": [
//                      {
//                        "typeName": "EmailAddress",
//                        "value": "child1@gmail.com",
//                        "sourceReference": 2
//                      }
//                    ],
//                    "sourceReference": 0
//                  },
//                  "personType": {
//                    "typeName": "PersonType",
//                    "value": "Child",
//                    "sourceReference": 3
//                  }
//                },
//                "sourceReference": 0
//              }
//            ]
//          },
//          "sources": [
//            {
//              "dataSourceName": "Multiple sources"
//            },
//            {
//              "dataSourceName": "Provided"
//            },
//            {
//              "remoteCall": {
//                "service": "CustomerService",
//                "addresss": "http://localhost/CustomerService",
//                "operation": "getEmails()",
//                "responseTypeName": "lang.taxi.Array<EmailAddress>",
//                "method": "GET",
//                "requestBody": "{}",
//                "resultCode": 200,
//
//                "response": "{...}",
//                "operationQualifiedName": "CustomerService@@getEmails()"
//              },
//              "inputs": [
//                {
//                  "parameterName": "emailFilter",
//                  "value": {
//                    "typeName": "EmailAddress",
//                    "value": "sigma@foo.com",
//                    "sourceReference": 1
//                  }
//                }
//              ],
//              "dataSourceName": "Operation result"
//            },
//            {
//              "mappingType": "SYNONYM",
//              "dataSourceName": "Mapped"
//            }
//          ],
//          "unmatchedNodes": [],
//          "path": null,
//          "queryResponseId": "${actualValue.id}",
//          "resultMode": "VERBOSE",
//          "profilerOperation": {
//            "componentName": "io.vyne.query.QueryProfiler",
//            "operationName": "Root",
//            "children": [],
//            "result": null,
//            "type": "ROOT",
//            "duration": ${actualValue.response.profilerOperation?.duration},
//            "remoteCalls": [],
//            "context": {},
//            "timings": {
//
//            },
//            "description": "io.vyne.query.QueryProfiler.Root"
//          },
//          "remoteCalls": [],
//          "timings": {
//
//          },
//          "fullyResolved": true,
//          "truncated": false
//        },
//        "timestamp": ${objectMapper.writeValueAsString(actualValue.timestamp)},
//        "id": "${actualValue.id}"
//      }
//      """.trimIndent()
//      JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
//   }
//
//   @Test
//   fun `can serialize and deserialize RestfulQueryHistoryRecord - ResultMode=VERBOSE`() {
//      // prepare
//      val result = mockVyne().query(resultMode = ResultMode.VERBOSE).findAll("Customer[]")
//      val actualValue: QueryHistoryRecord<out Any> = RestfulQueryHistoryRecord(Query("Customer[]"), HistoryQueryResponse.from(result))
//      val serializedValue = QueryHistoryRecordWritingConverter(objectMapper).convert(actualValue)
//
//      // act
//      val deserializedValue = QueryHistoryRecordReadingConverter(objectMapper).convert(serializedValue)
//
//      // assert
//      actualValue.response.sources.should.be.equal(deserializedValue?.response?.sources)
//      actualValue.response.resultsVerbose.should.be.equal(deserializedValue?.response?.resultsVerbose)
//      actualValue.response.results.should.not.be.`null`
//   }
//
//   @Test
//   fun `serialize RestfulQueryHistoryRecord to json, ResultMode=SIMPLE`() {
//      // prepare
//      val result = mockVyne().query(resultMode = ResultMode.SIMPLE).findAll("Customer[]")
//      val actualValue: QueryHistoryRecord<out Any> = RestfulQueryHistoryRecord(Query("Customer[]"), HistoryQueryResponse.from(result))
//      val actualJson = QueryHistoryRecordWritingConverter(objectMapper).convert(actualValue).asString()
//
//      // assert
//      val expectedJson = """
//         {
//           "className": "io.vyne.queryService.RestfulQueryHistoryRecord",
//           "query": {
//             "expression": "Customer[]",
//             "facts": [],
//             "queryMode": "DISCOVER",
//             "resultMode": "SIMPLE"
//           },
//           "response": {
//             "results": {
//               "lang.taxi.Array<Customer>": [
//                 {
//                   "emails": [
//                     "adult1@gmail.com"
//                   ],
//                   "customerType": "Adult"
//                 },
//                 {
//                   "emails": [
//                     "child1@gmail.com"
//                   ],
//                   "customerType": "Under18"
//                 }
//               ]
//             },
//             "resultsVerbose": {},
//             "sources": [],
//             "unmatchedNodes": [],
//             "path": null,
//             "queryResponseId": "${actualValue.response.queryResponseId}",
//             "resultMode": "SIMPLE",
//             "profilerOperation": {
//               "componentName": "io.vyne.query.QueryProfiler",
//               "operationName": "Root",
//               "children": [],
//               "result": null,
//               "type": "ROOT",
//               "duration": ${actualValue.response.profilerOperation?.duration},
//               "remoteCalls": [],
//               "context": {},
//               "timings": {
//
//               },
//               "description": "io.vyne.query.QueryProfiler.Root"
//             },
//             "remoteCalls": [],
//             "timings": {
//
//             },
//             "fullyResolved": true,
//             "truncated": false
//           },
//           "timestamp": ${objectMapper.writeValueAsString(actualValue.timestamp)},
//           "id": "${actualValue.id}"
//         }
//      """.trimIndent()
//      JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
//   }
//
//   @Test
//   fun `serialize VyneQL results to json, ResultMode=SIMPLE`() {
//      // prepare
//      val vyneQlQuery = """findAll { Customer[] } as Person[]""".trimIndent()
//      val result = mockVyne().query(vyneQlQuery, resultMode = ResultMode.SIMPLE)
//      val actualValue: QueryHistoryRecord<out Any> = VyneQlQueryHistoryRecord(vyneQlQuery, HistoryQueryResponse.from(result))
//      val actualJson = QueryHistoryRecordWritingConverter(objectMapper).convert(actualValue).asString()
//
//      // assert
//      val expectedJson = """
//         {
//           "className": "io.vyne.queryService.VyneQlQueryHistoryRecord",
//           "query": "findAll { Customer[] } as Person[]",
//           "response": {
//             "results": {
//               "lang.taxi.Array<Person>": [
//                 {
//                   "emails": [
//                     "adult1@gmail.com"
//                   ],
//                   "personType": "Adult"
//                 },
//                 {
//                   "emails": [
//                     "child1@gmail.com"
//                   ],
//                   "personType": "Child"
//                 }
//               ]
//             },
//             "resultsVerbose": {},
//             "sources": [],
//             "unmatchedNodes": [],
//             "path": null,
//             "queryResponseId": "${actualValue.id}",
//             "resultMode": "SIMPLE",
//             "profilerOperation": {
//               "componentName": "io.vyne.query.QueryProfiler",
//               "operationName": "Root",
//               "children": [],
//               "result": null,
//               "type": "ROOT",
//               "duration": ${actualValue.response.profilerOperation?.duration},
//               "remoteCalls": [],
//               "context": {},
//               "timings": {
//
//               },
//               "description": "io.vyne.query.QueryProfiler.Root"
//             },
//             "remoteCalls": [],
//             "timings": {
//
//             },
//             "fullyResolved": true,
//             "truncated": false
//           },
//           "timestamp": ${objectMapper.writeValueAsString(actualValue.timestamp)},
//           "id": "${actualValue.id}"
//         }
//      """.trimIndent()
//      JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
//   }

}

 */
