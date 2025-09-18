package com.orbitalhq.functions

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TaxiFunction(
   /**
    * Optional explicit name of the function in Taxi.
    * Defaults to the Kotlin/Java method name if not provided.
    */
   val name: String = "",

   /**
    * Optional description for schema/docs.
    */
   val description: String = "",

   /**
    * Allows defining a custom return type.
    * If not provided, the corresponding Taxi primitive of the return
    * type of the function will be used.
    */
   val returnType: String = ""
)


@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class TaxiParam(
   /**
    * Name of the parameter in the Taxi function signature.
    * Defaults to the method parameter name if available.
    */
   val name: String = "",

   /**
    * The Taxi type name, if you want to override or be explicit.
    * Example: "com.acme.types.PersonName"
    * If blank, inferred from the Kotlin/Java type.
    */
   val type: String = "",

   val nullable: Boolean = false
)

/**
 * Annotation that indicates an argument into a custom function
 * is a Type reference to the return type of the function.
 * (Needed if the function is doing graph searches or constructing typed instances)
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReturnType
