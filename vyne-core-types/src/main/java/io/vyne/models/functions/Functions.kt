package io.vyne.models.functions

import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.FailedEvaluatedExpression
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.utils.log
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.QualifiedName

interface NamedFunctionInvoker : FunctionInvoker {
   val functionName: QualifiedName
}

typealias FunctionHandler = (
   inputValues: List<TypedInstance>,
   schema: Schema,
   returnType: Type,
   function: FunctionAccessor
) -> TypedInstance

interface FunctionInvoker {
   fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      /**
       * The raw value / message being parsed.
       * Not always present, but passed when evaluating from TypedObjectFactory
       */
      rawMessageBeingParsed: Any? = null,
      resultCache: MutableMap<FunctionResultCacheKey, Any> = mutableMapOf()
   ): TypedInstance
}

data class FunctionResultCacheKey(
   val functionName: QualifiedName,
   /**
    * The values to use for cache lookup.
    * Don't pass values that change here, or you'll always get a cache miss
    */
   val inputValuesForCache: List<TypedInstance>
)

interface SelfDescribingFunction : NamedFunctionInvoker {
   val taxiDeclaration: String
}


/**
 * Helper class which will return TypedNull if any of the provided arguments were null.
 */
abstract class NullSafeInvoker : NamedFunctionInvoker {
   protected abstract fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      rawMessageBeingParsed: Any?,
      thisScopeValueSupplier: EvaluationValueSupplier
   ): TypedInstance

   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessageBeingParsed: Any?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      return if (inputValues.any { it is TypedNull }) {
         val typedNullTypes = inputValues
            .mapIndexedNotNull { index, typedInstance ->
               if (typedInstance is TypedNull) {
                  index to typedInstance.type.qualifiedName.shortDisplayName
               } else {
                  null
               }
            }
         val unresolvedInputs = typedNullTypes.map { (index, typeName) ->
            typeName.fqn()
         }
         val message = typedNullTypes.joinToString(
            prefix = "Function ${this.functionName} (in statement `${function.asTaxi()}`) does not permit null arguments, but received null for arguments ",
            separator = ","
         ) { (parameterIndex, typeName) ->
            "$parameterIndex ($typeName)"
         }
         log().warn("$message.  Not invoking this function, and returning null")

         TypedNull.create(
            returnType, FailedEvaluatedExpression(
               function.asTaxi(), inputValues, message, unresolvedInputs
            )
         )
      } else {
         doInvoke(inputValues, schema, returnType, function, rawMessageBeingParsed, objectFactory)
      }
   }
}

fun functionOf(functionName: String, handler: FunctionHandler): InlineFunctionInvoker {
   return InlineFunctionInvoker(functionName, handler)
}

class InlineFunctionInvoker(override val functionName: QualifiedName, val handler: FunctionHandler) :
   NullSafeInvoker() {
   constructor(functionName: String, handler: FunctionHandler) : this(QualifiedName.from(functionName), handler)

   override fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      rawMessageBeingParsed: Any?,
      thisScopeValueSupplier: EvaluationValueSupplier
   ): TypedInstance {
      return handler.invoke(inputValues, schema, returnType, function)
   }
}
