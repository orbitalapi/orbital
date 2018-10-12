package io.vyne.schemas.taxi

import com.google.common.collect.ArrayListMultimap
import io.osmosis.polymer.SchemaAggregator
import io.osmosis.polymer.schemas.*
import io.osmosis.polymer.schemas.Modifier
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.services.Constraint
import lang.taxi.types.*
import lang.taxi.types.Annotation
import org.antlr.v4.runtime.CharStreams

class TaxiSchema(private val document: TaxiDocument) : Schema {
   override val types: Set<Type>
   override val services: Set<Service>
   // TODO : Are these still required / meaningful?
   override val links: Set<Link> = emptySet()
   override val attributes: Set<QualifiedName> = emptySet()

   private val constraintConverter = TaxiConstraintConverter(this)

   init {
      this.types = parseTypes(document)
      this.services = parseServices(document)
   }

   private fun parseServices(document: TaxiDocument): Set<Service> {
      return document.services.map { taxiService ->
         // hahahaha
         Service(QualifiedName(taxiService.qualifiedName),
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
                  contract = constraintConverter.buildContract(returnType, taxiOperation.contract?.returnTypeConstraints
                     ?: emptyList())
               )
            },
            metadata = parseAnnotationsToMetadata(taxiService.annotations),
            sourceCode = taxiService.compilationUnits.toVyneSources()
         )
      }.toSet()
   }

   private fun parseAnnotationsToMetadata(annotations: List<Annotation>): List<Metadata> {
      return annotations.map { Metadata(it.name.fqn(), it.parameters) }
   }

   private fun parseTypes(document: TaxiDocument): Set<Type> {
      val rawTypes = mutableSetOf<Type>()
      val typesWithInheritence = ArrayListMultimap.create<Type, String>()
      // Register primitives, as they're implicitly defined
      rawTypes.addAll(getTaxiPrimitiveTypes())

      document.types.forEach { taxiType: lang.taxi.Type ->
         when (taxiType) {
            is ObjectType -> {
               val typeName = QualifiedName(taxiType.qualifiedName)
               val fields = taxiType.allFields.map { field ->
                  when (field.type) {
                     is ArrayType -> field.name to TypeReference((field.type as ArrayType).type.qualifiedName.fqn(), isCollection = true)
                     else -> field.name to TypeReference(field.type.qualifiedName.fqn(),
                        constraintProvider = buildDeferredConstraintProvider(field.type.qualifiedName.fqn(), field.constraints)
                     )
                  }
               }.toMap()
               val modifiers = parseModifiers(taxiType)
               val type = Type(typeName, fields, modifiers, sources = taxiType.compilationUnits.toVyneSources())
               rawTypes.add(type)
               if (taxiType.inheritsFrom.isNotEmpty()) {
                  typesWithInheritence.putAll(type, taxiType.inheritsFromNames)
               }
            }
            is TypeAlias -> {
               rawTypes.add(Type(QualifiedName(taxiType.qualifiedName), aliasForType = QualifiedName(taxiType.aliasType!!.qualifiedName), sources = taxiType.compilationUnits.toVyneSources()))
            }
            is ArrayType -> TODO()
            else -> rawTypes.add(Type(QualifiedName(taxiType.qualifiedName), modifiers = parseModifiers(taxiType), sources = taxiType.compilationUnits.toVyneSources()))
         }
      }
      val originalTypes = rawTypes.associateBy { it.fullyQualifiedName }


      // Now we have a full set of types, expand
      // references to other types - ie., typeAliased types & inheritence,
      // so they have the correct fields / modifiers /etc
      val typesWithAliases = rawTypes.map { rawType ->
         val inheritedTypes = if (typesWithInheritence.containsKey(rawType)) {
            typesWithInheritence[rawType].map { inheritedType ->
               originalTypes[inheritedType]
                  ?: error("Type ${rawType.fullyQualifiedName} inherits from type $inheritedType, which doesn't exist")
            }
         } else {
            emptyList<Type>()
         }

         if (rawType.isTypeAlias) {
            val aliasedType = originalTypes[rawType.aliasForType!!.fullyQualifiedName]
               ?: error("Type ${rawType.fullyQualifiedName} is declared as a type alias of type ${rawType.aliasForType!!.fullyQualifiedName}, but that type doesn't exist")
            aliasedType.copy(name = rawType.name, aliasForType = aliasedType.name, inherits = inheritedTypes)
         } else {
            rawType.copy(inherits = inheritedTypes)
         }
      }.toSet()
      return typesWithAliases
   }

   private fun getTaxiPrimitiveTypes(): Collection<Type> {
      return PrimitiveType.values().map { taxiPrimitive ->
         Type(taxiPrimitive.qualifiedName.fqn(), modifiers = parseModifiers(taxiPrimitive), sources = listOf(SourceCode.undefined(TaxiSchema.LANGUAGE)))
      }
   }

   private fun buildDeferredConstraintProvider(fqn: QualifiedName, constraints: List<Constraint>): DeferredConstraintProvider {
      return FunctionConstraintProvider({
         val type = this.type(fqn)
         constraintConverter.buildConstraints(type, constraints)
      })
   }

   private fun parseModifiers(type: lang.taxi.Type): List<Modifier> {
      return when (type) {
         is EnumType -> listOf(Modifier.ENUM)
         is PrimitiveType -> listOf(Modifier.PRIMITIVE)
         is ObjectType -> type.modifiers.map {
            when (it) {
               lang.taxi.types.Modifier.PARAMETER_TYPE -> Modifier.PARAMETER_TYPE
            }
         }
         else -> emptyList()
      }
   }

   fun merge(schema: TaxiSchema): TaxiSchema {
      return TaxiSchema(this.document.merge(schema.document))
   }

   companion object {
      val LANGUAGE = "Taxi"
      fun from(taxi: String, sourceName: String = "<unknown>"): TaxiSchema {
         return TaxiSchema(Compiler(CharStreams.fromString(taxi, sourceName)).compile())
      }
   }
}

private fun lang.taxi.SourceCode.toVyneSource(): SourceCode {
   return io.osmosis.polymer.schemas.SourceCode(this.origin, TaxiSchema.LANGUAGE, this.content)
}

private fun List<lang.taxi.CompilationUnit>.toVyneSources(): List<SourceCode> {
   return this.map { it.source.toVyneSource() }
}


class TaxiSchemaAggregator : SchemaAggregator {
   override fun aggregate(schemas: List<Schema>): Pair<Schema?, List<Schema>> {
      val taxiSchemas = schemas.filterIsInstance(TaxiSchema::class.java)
      val remaining = schemas - taxiSchemas
      val aggregatedSchema = if (taxiSchemas.isNotEmpty()) {
         taxiSchemas.reduce { a, b -> a.merge(b) }
      } else null
      return aggregatedSchema to remaining
   }

}
