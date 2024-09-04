package com.orbitalhq.connectors.nosql.mongodb

import lang.taxi.query.DiscoveryType
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.Type

fun Type.hasMongoCollectionAnnotation(): Boolean {
   return annotations.any { it.qualifiedName == MongoConnector.Annotations.Collection.NAME }
}

fun Type.collectionNameOrTypeName(): String {
   return if (hasMongoCollectionAnnotation()) {
      getCollectionName()
   } else {
      toQualifiedName().typeName
   }
}

fun Type.getCollectionName(): String {
   val collectionAnnotation =
      annotations.firstOrNull { it.qualifiedName == MongoConnector.Annotations.Collection.NAME }
         ?: error("Type $qualifiedName is missing a ${MongoConnector.Annotations.Collection.NAME} annotation")
   val collectionAnnotationWrapper = MongoConnector.Annotations.Collection.from(collectionAnnotation)
   return collectionAnnotationWrapper.collectionName
}

fun ObjectType.IdFields(): List<Field> {
   return this.allFields.filter { it.annotations.any { annotation -> annotation.name == "Id" } }
}

fun Field.IdField(): Boolean{
   return this.annotations.any { annotation -> annotation.name == "Id" }
}


object MongoQueryHelpers {
    fun getCollectionNames(
      typesToFind: List<Pair<ObjectType, DiscoveryType>>,
   ): Map<Type, String> {
      val tableNames: Map<Type, String> = typesToFind.mapIndexed { index, (type, _) ->
         val collectionName = type.collectionNameOrTypeName()
         type to collectionName
      }.toMap()
      return tableNames
   }
}

typealias OrbitalMongoEntity = Map<String, Any?>
