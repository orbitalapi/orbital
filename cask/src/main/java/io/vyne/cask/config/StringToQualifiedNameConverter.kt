package io.vyne.cask.config

import io.vyne.utils.log
import lang.taxi.types.QualifiedName
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.ConfigurableConversionService
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@ConfigurationPropertiesBinding
class StringToQualifiedNameConverter : Converter<String,QualifiedName> {
   @PostConstruct
   fun postConstruct() {
      // This is a hack to patch Spring's inabilitiy to deal with Kotlin Configuration properly.
      (ApplicationConversionService.getSharedInstance() as ApplicationConversionService).addConverter(this)
   }
   override fun convert(source: String): QualifiedName {
      return QualifiedName.from(source)
   }

}
