package io.vyne.cask.config

import lang.taxi.types.QualifiedName
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToQualifiedNameConverter : Converter<String,QualifiedName> {

   override fun convert(source: String): QualifiedName {
      return QualifiedName.from(source)
   }

}
