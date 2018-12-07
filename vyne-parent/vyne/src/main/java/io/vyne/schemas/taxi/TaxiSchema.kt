package io.vyne.schemas.taxi

import com.google.common.collect.ArrayListMultimap
import io.vyne.SchemaAggregator
import io.vyne.schemas.*
import io.vyne.schemas.Modifier
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.services.Constraint
import lang.taxi.types.*
import lang.taxi.types.Annotation
import org.antlr.v4.runtime.CharStreams

class TaxiSchema(private val document: TaxiDocument) : Schema {
   override val types: Set<Type>
   override val services: Set<Service>
   override val policies: Set<Policy>
   // TODO : Are these still required / meaningful?
   override val links: Set<Link> = emptySet()
   override val attributes: Set<QualifiedName> = emptySet()
   override val typeCache: TypeCache
   private val constraintConverter = TaxiConstraintConverter(this)

   init {
      this.types = parseTypes(document)
      this.typeCache = DefaultTypeCache(this.types)
      this.services = parseServices(document)
      this.policies = parsePolicies(document)
   }

   private fun parsePolicies(document: TaxiDocument): Set<Policy> {
      return document.policies.map { taxiPolicy ->
         Policy(QualifiedName(taxiPolicy.qualifiedName),
            this.type(taxiPolicy.targetType.toVyneQualifiedName()),
            taxiPolicy.ruleSets
         )
      }.toSet()
   }

   private fun parseServices(document: TaxiDocument): Set<Service> {
      return document.services.map { taxiService ->
         // hahahaha
         Service(QualifiedName(taxiService.qualifiedName),
            operations = taxiService.operations.map { taxiOperation ->
               val returnType = this.type(taxiOperation.returnType.toVyneQualifiedName())
               Operation(OperationNames.qualifiedName(taxiService.qualifiedName, taxiOperation.name),
                  taxiOperation.parameters.map { taxiParam ->
                     val type = this.type(taxiParam.type.qualifiedName)
                     Parameter(
                        type = type,
                        name = taxiParam.name,
                        metadata = parseAnnotationsToMetadata(taxiParam.annotations),
                        constraints = constraintConverter.buildConstraints(type, taxiParam.constraints)
                     )
                  },
                  operationType = taxiOperation.scope,
                  returnType = returnType,
                  metadata = parseAnnotationsToMetadata(taxiOperation.annotations),
                  contract = constraintConverter.buildContract(returnType, taxiOperation.contract?.returnTypeConstraints
                     ?: emptyList()),
                  sources = taxiOperation.compilationUnits.toVyneSources()
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
            is EnumType -> {
               val enumValues = taxiType.values.map { it.name }
               rawTypes.add(Type(QualifiedName(taxiType.qualifiedName), modifiers = parseModifiers(taxiType), enumValues = enumValues, sources = taxiType.compilationUnits.toVyneSources()))
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
            emptyList()
         }

         if (rawType.isTypeAlias) {
            val aliasedType = originalTypes[rawType.aliasForType!!.fullyQualifiedName]
               ?: error("Type ${rawType.fullyQualifiedName} is declared as a type alias of type ${rawType.aliasForType!!.fullyQualifiedName}, but that type doesn't exist")
            aliasedType.copy(name = rawType.name, aliasForType = aliasedType.name, inherits = inheritedTypes, sources = rawType.sources)
         } else {
            rawType.copy(inherits = inheritedTypes)
         }
      }.toSet()
      return typesWithAliases
   }

   private fun getTaxiPrimitiveTypes(): Collection<Type> {
      return PrimitiveType.values().map { taxiPrimitive ->
         Type(taxiPrimitive.qualifiedName.fqn(), modifiers = parseModifiers(taxiPrimitive), sources = listOf(SourceCode.native(TaxiSchema.LANGUAGE)))
      }
   }

   private fun buildDeferredConstraintProvider(fqn: QualifiedName, constraints: List<Constraint>): DeferredConstraintProvider {
      return FunctionConstraintProvider {
         val type = this.type(fqn)
         constraintConverter.buildConstraints(type, constraints)
      }
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

private fun lang.taxi.QualifiedName.toVyneQualifiedName(): QualifiedName {
   return QualifiedName(this.toString(), this.parameters.map { it.toVyneQualifiedName() })
}

private fun lang.taxi.Type.toVyneQualifiedName(): QualifiedName {
   return this.toQualifiedName().toVyneQualifiedName()
}

private fun lang.taxi.SourceCode.toVyneSource(): SourceCode {
   return io.vyne.schemas.SourceCode(this.origin, TaxiSchema.LANGUAGE, this.content)
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
