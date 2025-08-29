package com.orbitalhq.models.functions.stdlib

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.functions.stdlib.collections.createFailureWithTypedNull
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.log
import lang.taxi.functions.FunctionAccessor
import kotlin.reflect.typeOf

inline fun <reified T> TypedInstance.valueAs(): T {
   return this.value as? T ?: error("Cannot cast ${this.value} to type ${T::class.simpleName}")
}

inline fun <reified T : TypedInstance?> List<TypedInstance>.nullableArgumentAsTypedInstanceOrError(
   index: Int,
   returnType: Type,
   function: FunctionAccessor
): Either<TypedNull, TypedInstance> {
   if (index >= this.size) {
      return createFailureWithTypedNull("No argument passed at index $index", returnType, function, this)
         .left()
   }
   return when (val argument = this[index]) {
      is T -> argument.right()
      is TypedNull -> return argument.right()
      else -> createFailureWithTypedNull(
         "Expected a ${T::class.simpleName} at index $index but got ${argument::class.simpleName}",
         returnType,
         function,
         this
      )
         .left()
   }

}

inline fun <reified T : TypedInstance> List<TypedInstance>.argumentAsTypedInstanceOrError(
   index: Int,
   returnType: Type,
   function: FunctionAccessor
): Either<TypedNull, T> {
   if (index >= this.size) {
      return createFailureWithTypedNull("No argument passed at index $index", returnType, function, this)
         .left()
   }
   val argument = this[index]

   return if (argument is T) {
      argument.right()
   } else {
      createFailureWithTypedNull(
         "Expected a ${T::class.simpleName} at index $index but got ${argument::class.simpleName}",
         returnType,
         function,
         this
      )
         .left()
   }
}

inline fun <reified T> TypedInstance.valueAsOrError(
   returnType: Type,
   inputValues: List<TypedInstance>,
   function: FunctionAccessor
): Either<TypedNull, T> {
   val value = this.value
   return if (this.value is T) {
      (this.value!! as T).right()
   } else {
      val actualTypeName = if (value != null) value::class.simpleName!! else "null"
      createFailureWithTypedNull(
         "Expected instance of ${T::class.simpleName} but found $actualTypeName",
         returnType,
         function,
         inputValues
      )
         .left()
   }
}
