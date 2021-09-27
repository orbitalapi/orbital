package io.vyne.spring

import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry

fun BeanDefinitionRegistry.registerBeanDefinitionOfType(clazz: Class<*>): String {
   val beanName = clazz.simpleName
   this.registerBeanDefinition(
      beanName,
      BeanDefinitionBuilder.genericBeanDefinition(clazz).beanDefinition
   )
   return beanName
}
