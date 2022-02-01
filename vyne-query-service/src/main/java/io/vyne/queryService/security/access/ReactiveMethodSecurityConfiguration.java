package io.vyne.queryService.security.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.ExpressionBasedAnnotationAttributeFactory;
import org.springframework.security.access.expression.method.ExpressionBasedPostInvocationAdvice;
import org.springframework.security.access.expression.method.ExpressionBasedPreInvocationAdvice;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityMetadataSourceAdvisor;
import org.springframework.security.access.method.AbstractMethodSecurityMetadataSource;
import org.springframework.security.access.method.DelegatingMethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PrePostAnnotationSecurityMetadataSource;
import org.springframework.security.config.core.GrantedAuthorityDefaults;

import java.util.Arrays;

/**
 * This is the direct copy from Spring.
 *
 * When reactive method security is enabled through @EnableReactiveMethodSecurity, corresponding implementation in our current spring version
 * can not deal with functions returning kotlin coroutine types (e.g. Flow) as it can only deal with functions returning reactor types (i.e. Flux and Mono)
 *
 * This version which is taken from spring security so that we can inject @see io.vyne.queryService.security.access.PrePostAdviceReactiveMethodInterceptor
 * @PreAuthorize("hasAuthority('foo')")
 * annotations to control the access on suspendable functions.
 */
@ConditionalOnProperty(name = {"vyne.security.openIdp.enabled"}, havingValue = "true")
@Configuration
class ReactiveMethodSecurityConfiguration implements ImportAware {
   private int advisorOrder;
   private GrantedAuthorityDefaults grantedAuthorityDefaults;

   ReactiveMethodSecurityConfiguration() {
   }

   @Bean
   @Role(2)
   public MethodSecurityMetadataSourceAdvisor methodSecurityInterceptor(AbstractMethodSecurityMetadataSource source) {
      MethodSecurityMetadataSourceAdvisor advisor = new MethodSecurityMetadataSourceAdvisor("securityMethodInterceptor", source, "methodMetadataSource");
      advisor.setOrder(this.advisorOrder);
      return advisor;
   }

   @Bean
   @Role(2)
   public DelegatingMethodSecurityMetadataSource methodMetadataSource(MethodSecurityExpressionHandler methodSecurityExpressionHandler) {
      ExpressionBasedAnnotationAttributeFactory attributeFactory = new ExpressionBasedAnnotationAttributeFactory(methodSecurityExpressionHandler);
      PrePostAnnotationSecurityMetadataSource prePostSource = new PrePostAnnotationSecurityMetadataSource(attributeFactory);
      return new DelegatingMethodSecurityMetadataSource(Arrays.asList(prePostSource));
   }

   @Bean
   public PrePostAdviceReactiveMethodInterceptor securityMethodInterceptor(AbstractMethodSecurityMetadataSource source, MethodSecurityExpressionHandler handler) {
      ExpressionBasedPostInvocationAdvice postAdvice = new ExpressionBasedPostInvocationAdvice(handler);
      ExpressionBasedPreInvocationAdvice preAdvice = new ExpressionBasedPreInvocationAdvice();
      preAdvice.setExpressionHandler(handler);
      return new PrePostAdviceReactiveMethodInterceptor(source, preAdvice, postAdvice);
   }

   @Bean
   @Role(2)
   public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler() {
      DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
      if (this.grantedAuthorityDefaults != null) {
         handler.setDefaultRolePrefix(this.grantedAuthorityDefaults.getRolePrefix());
      }

      return handler;
   }

   public void setImportMetadata(AnnotationMetadata importMetadata) {
      this.advisorOrder = Integer.MAX_VALUE;
   }

   @Autowired(
      required = false
   )
   void setGrantedAuthorityDefaults(GrantedAuthorityDefaults grantedAuthorityDefaults) {
      this.grantedAuthorityDefaults = grantedAuthorityDefaults;
   }
}
