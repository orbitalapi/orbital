// In order to make these tests easier, they depend on classes in
// schema-server.  However, that project is wired as a spring-boot runnable
// jar, so cannot be used as a dependency in other projects.
// The solution is to move some classes around.
// We ultimately plan on splitting the schema-server-runtime and schema-server-library,
// so that the server can be embedded.
// That will give these tests a library they can depend on, which will make
// them work.
// But, right now there's a very large refactor going on in the schema-server.
// So, Imma leave these tests commented out until that's merged, and
// then move stuff around and re-enable these.

//
//package io.vyne.queryService.schemas.importing.swagger
//
//import com.google.common.io.Resources
//import com.winterbe.expekt.should
//import io.vyne.http.MockWebServerRule
//import io.vyne.queryService.schemas.importing.BaseSchemaConverterServiceTest
//import io.vyne.queryService.schemas.importing.SchemaConversionRequest
//import lang.taxi.errors
//import org.junit.Rule
//import org.junit.Test
//import java.time.Duration
//
//class SwaggerSchemaConverterTest : BaseSchemaConverterServiceTest() {
//   private val converter = SwaggerSchemaConverter()
//
//   @Rule
//   @JvmField
//   final val server = MockWebServerRule()
//
//
//   @Test
//   fun `loads swagger from http`() {
//      val converterService = createConverterService(converter)
//
//      val swagger = Resources.getResource("schemas/swagger/petstore.yaml").readText()
//      server.prepareResponse { response -> response.setBody(swagger) }
//
//      val conversionResponse = converterService.import(
//         SchemaConversionRequest(
//            SwaggerSchemaConverter.SWAGGER_FORMAT,
//            SwaggerConverterOptions(defaultNamespace = "com.vyne.petstore", serviceBasePath = "http://petstore.com", url = server.url("/").toString())
//         )
//      ).block(Duration.ofSeconds(1))!!
//
//      conversionResponse.types.should.have.size(3)
//      conversionResponse.services.should.have.size(2)
//   }
//
//   @Test
//   fun `generate and persist simple schema`() {
//      val converterService = createConverterService(converter)
//
//      val swagger = Resources.getResource("schemas/swagger/petstore.yaml").readText()
//
//      val conversionResponse = converterService.import(
//         SchemaConversionRequest(
//            SwaggerSchemaConverter.SWAGGER_FORMAT,
//            SwaggerConverterOptions(defaultNamespace = "com.vyne.petstore", serviceBasePath = "http://petstore.com", swagger = swagger)
//         )
//      ).block(Duration.ofSeconds(1))!!
//      conversionResponse.dryRun.should.be.`false`
//      tempFolder.root.exists().should.be.`true`
//      val sourceDir = tempFolder.root.resolve("src/com/vyne/petstore")
//      sourceDir.exists().should.be.`true`
//      sourceDir.listFiles().toList().should.have.size(5)
//
//      conversionResponse.messages.errors().should.be.empty
//      conversionResponse.types.should.have.size(3)
//      conversionResponse.services.should.have.size(2)
//   }
//
//   @Test
//   fun canImportComplexSwagger() {
//      val swagger = Resources.getResource("schemas/swagger/jira-swagger-v3.json").readText()
//      val generatedTaxiCode = converter.convert(
//         SchemaConversionRequest(
//            SwaggerSchemaConverter.SWAGGER_FORMAT,
//         ),
//         SwaggerConverterOptions(defaultNamespace = "com.vyne.petstore", serviceBasePath = "http://myjira.com", swagger = swagger)
//
//      )
//      generatedTaxiCode.hasErrors.should.be.`false`
//   }
//
//   @Test
//   fun submitsAndPersistsImportedSchema() {
//      val converterService = createConverterService(converter)
//
//      val swagger = Resources.getResource("schemas/swagger/jira-swagger-v3.json").readText()
//
//      val conversionResponse = converterService.preview(
//         SchemaConversionRequest(
//            SwaggerSchemaConverter.SWAGGER_FORMAT,
//            SwaggerConverterOptions(defaultNamespace = "com.vyne.petstore", serviceBasePath = "http://myjira.com", swagger = swagger)
//         )
//      ).block(Duration.ofSeconds(1))!!
//      conversionResponse.dryRun.should.be.`true`
//      tempFolder.root.exists().should.be.`true`
//      tempFolder.root.resolve("src").listFiles()
//         .toList()
//         .filter { it.name != ".gitkeep" }
//         .should.be.empty
//
//      conversionResponse.messages.errors().should.be.empty
//      conversionResponse.types.should.have.size(434)
//      conversionResponse.services.should.have.size(219)
//   }
//
//   // submit a swagger json, generate the schema
//   // submit a url to a swagger, generate the schema
//
//
//}
//
