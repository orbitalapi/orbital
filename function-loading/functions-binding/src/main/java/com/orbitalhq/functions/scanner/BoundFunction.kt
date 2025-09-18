package com.orbitalhq.functions.scanner

import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.FailedEvaluatedExpression
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.functions.FunctionInvoker
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.stdlib.collections.createFailureWithTypedNull
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import io.vavr.control.Either
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName
import mu.KotlinLogging
import java.lang.invoke.MethodHandle

/**
 * A runtime-ready function: has a MethodHandle for invocation,
 * and a precomputed list of ParameterBindings for argument resolution.
 */
class BoundFunction(
   val name: String,
   val namespace: String,
   val description: String,
   val bindings: List<ParameterBinding>,
   /**
    * The return type that the user has explicitly supplied.
    * Not what the actual method is declared as.
    * This is a taxi type name
    */
   val suppliedReturnType: String?,
   private val methodHandle: MethodHandle,
   val jvmReturnType: JvmReturnType
) : FunctionInvoker, NamedFunctionInvoker {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override val functionName: QualifiedName = QualifiedName(namespace, name)

   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      rawMessageBeingParsed: Any?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {

      // Resolve all parameter values from bindings
      val args = bindings.map { binding ->
         binding.resolve(inputValues, schema, returnType, function, objectFactory, rawMessageBeingParsed)
      }.toTypedArray()

      // Invoke the target method directly
      val source = EvaluatedExpression(function.asTaxi(), inputValues)
      val res = kotlin.runCatching { methodHandle.invokeWithArguments(*args) }
         .fold(
            onSuccess = { convertResultToTypedInstance(it, returnType, function, inputValues, source, schema)},
            onFailure = { throwableToTypedNull(it, returnType, function, inputValues) }
         )
      return res
   }

   private fun throwableToTypedNull(
      e: Throwable,
      returnType: Type,
      function: FunctionAccessor,
      inputValues: List<TypedInstance>
   ): TypedNull {
      val message =
         "Invoking function ${functionName.parameterizedName} failed with exception ${e::class.simpleName} - ${e.message}"
      logger.warn { message }
      return createFailureWithTypedNull(message, returnType, function, inputValues)
   }

   private fun convertResultToTypedInstance(
      result: Any?,
      returnType: Type,
      function: FunctionAccessor,
      inputValues: List<TypedInstance>,
      source: EvaluatedExpression,
      schema: Schema
   ): TypedInstance {
      return when (result) {
          null -> TypedNull.create(returnType, source)
          is TypedInstance -> {
              if (!result.type.taxiType.isAssignableTo(returnType.taxiType)) {
                 val errorMessage =
                    "Custom function ${functionName.parameterizedName} returned a TypedInstance of type ${result.type.paramaterizedName} which is not assignable to the declared type ${returnType.paramaterizedName} - returning null"
                 logger.warn { errorMessage }
                 TypedNull.create(returnType, FailedEvaluatedExpression(function.asTaxi(), inputValues, errorMessage))
              } else {
                 DataSourceUpdater.update(result, source)
              }
          }

         is Either<*, *> -> {
              result
                 .map { convertResultToTypedInstance(it, returnType, function, inputValues, source, schema) }
                 .mapLeft { error ->
                    if (error !is Throwable) {
                       createFailureWithTypedNull(error.toString(), returnType, function, inputValues)
                    } else {
                       throwableToTypedNull(error, returnType, function, inputValues)
                    }
                 }
                 .getOrElseGet { it }
         }

         else -> {
              try {
                 TypedInstance.from(returnType, result, schema, source = source)
              } catch (e: Exception) {
                 val errorMessage =
                    "Failed to construct a TypedInstance from the result of function ${function.qualifiedName} - a ${e::class.simpleName} exception was thrown: ${e.message}. Will return null"
                 logger.warn { errorMessage }
                 TypedNull.create(returnType, FailedEvaluatedExpression(function.asTaxi(), inputValues, errorMessage))
              }
         }
      }
   }
}

