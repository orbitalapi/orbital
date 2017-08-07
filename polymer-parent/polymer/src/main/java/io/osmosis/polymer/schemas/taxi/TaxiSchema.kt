package io.osmosis.polymer.schemas.taxi

import io.osmosis.polymer.schemas.*
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.ObjectType

class TaxiSchema(document: TaxiDocument) : Schema {
   override val types: Set<Type>
   override val services: Set<Service>
   // TODO : Are these still required / meaningful?
   override val links: Set<Link> = emptySet()
   override val attributes: Set<QualifiedName> = emptySet()
   init {
      this.types = parseTypes(document)
      this.services = parseServices(document)
      val links = mutableSetOf<Link>()
      val attributes = mutableSetOf<QualifiedName>()

//      this.links = links
//      this.types = types
//      this.attributes = attributes
   }

   private fun parseServices(document: TaxiDocument): Set<Service> {
      val constraintConverter = TaxiConstraintConverter(this)
      return document.services.map { taxiService ->
         // hahahaha
         Service(taxiService.qualifiedName,
            operations = taxiService.operations.map { taxiOperation ->
               val returnType = this.type(taxiOperation.returnType.qualifiedName)
               Operation(taxiOperation.name,
                  taxiOperation.parameters.map { taxiParam ->
                     val type = this.type(taxiParam.type.qualifiedName)
                     Parameter(
                        type = type,
                        name = taxiParam.name,
                        metadata = parseAnnotationsToMetadata(taxiParam.annotations),
                        constraints = constraintConverter.buildConstraints(type, taxiParam.constraints)
                     )
                  }, returnType = returnType,
                  metadata = parseAnnotationsToMetadata(taxiOperation.annotations),
                  contract = constraintConverter.buildContract(returnType,taxiOperation.contract?.returnTypeConstraints ?: emptyList())
               )
            },
            metadata = parseAnnotationsToMetadata(taxiService.annotations)
         )
      }.toSet()
   }

   private fun parseAnnotationsToMetadata(annotations: List<Annotation>): List<Metadata> {
      return annotations.map { Metadata(it.name.fqn(), it.parameters) }
   }

   private fun parseTypes(document: TaxiDocument): Set<Type> {
      val result = mutableSetOf<Type>()
      document.types.forEach { taxiType: lang.taxi.Type ->

         when (taxiType) {
            is ObjectType -> {
               val typeName = QualifiedName(taxiType.qualifiedName)
               val fields = taxiType.fields.map { field ->
                  when (field.type) {
                     is ArrayType -> field.name to TypeReference((field.type as ArrayType).type.qualifiedName.fqn(), isCollection = true)
                     else -> field.name to TypeReference(field.type.qualifiedName.fqn())
                  }
               }.toMap()
               result.add(Type(typeName, fields))
            }
            is ArrayType -> TODO()
            else -> result.add(Type(QualifiedName(taxiType.qualifiedName)))
         }
      }
      return result
   }

   companion object {
      fun from(taxi: String): TaxiSchema {
         return TaxiSchema(Compiler(taxi).compile())
      }
   }
}
