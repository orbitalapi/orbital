package com.orbitalhq.query.history

import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.fqn
import jakarta.persistence.AttributeConverter

object QualifiedNameJpaConverter : AttributeConverter<QualifiedName, String> {
   override fun convertToDatabaseColumn(attribute: QualifiedName): String {
      return attribute.parameterizedName
   }

   override fun convertToEntityAttribute(dbData: String): QualifiedName {
      return dbData.fqn()
   }
}
