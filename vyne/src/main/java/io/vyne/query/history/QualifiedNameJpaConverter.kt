package io.vyne.query.history

import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import javax.persistence.AttributeConverter

object QualifiedNameJpaConverter : AttributeConverter<QualifiedName, String> {
   override fun convertToDatabaseColumn(attribute: QualifiedName): String {
      return attribute.parameterizedName
   }

   override fun convertToEntityAttribute(dbData: String): QualifiedName {
      return dbData.fqn()
   }
}
