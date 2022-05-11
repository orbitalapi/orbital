package io.vyne.queryService.security.access;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.reactive.AwaitKt;
import kotlinx.coroutines.reactive.ReactiveFlowKt;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.reactivestreams.Publisher;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PostInvocationAttribute;
import org.springframework.security.access.prepost.PostInvocationAuthorizationAdvice;
import org.springframework.security.access.prepost.PreInvocationAttribute;
import org.springframework.security.access.prepost.PreInvocationAuthorizationAdvice;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.Assert;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.springframework.core.CoroutinesUtils.invokeSuspendingFunction;

/**
 * This is the direct copy from Spring, source url:
 * https://github.com/spring-projects/spring-security/blob/e03fe7f0898ce311e194f42ff4d3774fbb925ff1/core/src/main/java/org/springframework/security/access/prepost/PrePostAdviceReactiveMethodInterceptor.java#L56
 * TODO Remove Once upgrade to spring security >= 5.5
 *
 * When reactive method security is enabled through @EnableReactiveMethodSecurity, corresponding implementation in our current spring version
 * can not deal with functions returning kotlin coroutine types (e.g. Flow) as it can only deal with functions returning reactor types (i.e. Flux and Mono)
 *
 * This version which is taken from spring security 5.6, can deal with suspending functions (see line 77) and hence enables us to use
 * @PreAuthorize("hasAuthority('foo')")
 * annotations to control the access on suspendable functions.
 */
public class PrePostAdviceReactiveMethodInterceptor implements MethodInterceptor {

   private Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymous",
      AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

   private final MethodSecurityMetadataSource attributeSource;

   private final PreInvocationAuthorizationAdvice preInvocationAdvice;

   private final PostInvocationAuthorizationAdvice postAdvice;

   private static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";

   private static final int RETURN_TYPE_METHOD_PARAMETER_INDEX = -1;


   public PrePostAdviceReactiveMethodInterceptor(MethodSecurityMetadataSource attributeSource,
                                                 PreInvocationAuthorizationAdvice preInvocationAdvice,
                                                 PostInvocationAuthorizationAdvice postInvocationAdvice) {
      Assert.notNull(attributeSource, "attributeSource cannot be null");
      Assert.notNull(preInvocationAdvice, "preInvocationAdvice cannot be null");
      Assert.notNull(postInvocationAdvice, "postInvocationAdvice cannot be null");
      this.attributeSource = attributeSource;
      this.preInvocationAdvice = preInvocationAdvice;
      this.postAdvice = postInvocationAdvice;
   }

   @Override
   public Object invoke(final MethodInvocation invocation) {
      Method method = invocation.getMethod();
      Class<?> returnType = method.getReturnType();

      boolean isSuspendingFunction = isSuspendingFunction(method);
      boolean hasFlowReturnType = COROUTINES_FLOW_CLASS_NAME
         .equals(new MethodParameter(method, RETURN_TYPE_METHOD_PARAMETER_INDEX).getParameterType().getName());
      boolean hasReactiveReturnType = Publisher.class.isAssignableFrom(returnType) || isSuspendingFunction
         || hasFlowReturnType;

      Assert.state(hasReactiveReturnType,
         () -> "The returnType " + returnType + " on " + method
            + " must return an instance of org.reactivestreams.Publisher "
            + "(i.e. Mono / Flux) or the function must be a Kotlin coroutine "
            + "function in order to support Reactor Context");
      Class<?> targetClass = invocation.getThis().getClass();
      Collection<ConfigAttribute> attributes = this.attributeSource.getAttributes(method, targetClass);
      PreInvocationAttribute preAttr = findPreInvocationAttribute(attributes);
      // @formatter:off
      Mono<Authentication> toInvoke = ReactiveSecurityContextHolder.getContext()
         .map(SecurityContext::getAuthentication)
         .defaultIfEmpty(this.anonymous)
         .filter((auth) -> this.preInvocationAdvice.before(auth, invocation, preAttr))
         .switchIfEmpty(Mono.defer(() -> Mono.error(new AccessDeniedException("Denied"))));
      // @formatter:on
      PostInvocationAttribute attr = findPostInvocationAttribute(attributes);
      if (Mono.class.isAssignableFrom(returnType)) {
         return toInvoke.flatMap((auth) -> PrePostAdviceReactiveMethodInterceptor.<Mono<?>>proceed(invocation)
            .map((r) -> (attr != null) ? this.postAdvice.after(auth, invocation, attr, r) : r));
      }
      if (Flux.class.isAssignableFrom(returnType)) {
         return toInvoke.flatMapMany((auth) -> PrePostAdviceReactiveMethodInterceptor.<Flux<?>>proceed(invocation)
            .map((r) -> (attr != null) ? this.postAdvice.after(auth, invocation, attr, r) : r));
      }
      if (hasFlowReturnType) {
         Publisher<?> publisher;
         if (isSuspendingFunction) {
            publisher = invokeSuspendingFunction(invocation.getMethod(), invocation.getThis(),
               invocation.getArguments());
         } else {
            ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(returnType);
            Assert.state(adapter != null, () -> "The returnType " + returnType + " on " + method
               + " must have a org.springframework.core.ReactiveAdapter registered");
            publisher = adapter.toPublisher(PrePostAdviceReactiveMethodInterceptor.flowProceed(invocation));
         }
         Flux<?> response = toInvoke.flatMapMany((auth) -> Flux.from(publisher)
            .map((r) -> (attr != null) ? this.postAdvice.after(auth, invocation, attr, r) : r));
         return KotlinDelegate.asFlow(response);
      }
      if (isSuspendingFunction) {
         Mono<?> response = toInvoke.flatMap((auth) -> Mono
            .from(invokeSuspendingFunction(invocation.getMethod(), invocation.getThis(),
               invocation.getArguments()))
            .map((r) -> (attr != null) ? this.postAdvice.after(auth, invocation, attr, r) : r));
         return KotlinDelegate.awaitSingleOrNull(response,
            invocation.getArguments()[invocation.getArguments().length - 1]);
      }
      return toInvoke.flatMapMany(
         (auth) -> Flux.from(PrePostAdviceReactiveMethodInterceptor.<Publisher<?>>proceed(invocation))
            .map((r) -> (attr != null) ? this.postAdvice.after(auth, invocation, attr, r) : r));
   }

   private static <T extends Publisher<?>> T proceed(final MethodInvocation invocation) {
      try {
         return (T) invocation.proceed();
      } catch (Throwable throwable) {
         throw Exceptions.propagate(throwable);
      }
   }

   private static Object flowProceed(final MethodInvocation invocation) {
      try {
         return invocation.proceed();
      } catch (Throwable throwable) {
         throw Exceptions.propagate(throwable);
      }
   }

   private static PostInvocationAttribute findPostInvocationAttribute(Collection<ConfigAttribute> config) {
      for (ConfigAttribute attribute : config) {
         if (attribute instanceof PostInvocationAttribute) {
            return (PostInvocationAttribute) attribute;
         }
      }
      return null;
   }

   private static PreInvocationAttribute findPreInvocationAttribute(Collection<ConfigAttribute> config) {
      for (ConfigAttribute attribute : config) {
         if (attribute instanceof PreInvocationAttribute) {
            return (PreInvocationAttribute) attribute;
         }
      }
      return null;
   }

   /**
    * Inner class to avoid a hard dependency on Kotlin at runtime.
    */
   private static class KotlinDelegate {

      private static Object asFlow(Publisher<?> publisher) {
         return ReactiveFlowKt.asFlow(publisher);
      }

      private static Object awaitSingleOrNull(Publisher<?> publisher, Object continuation) {
         return AwaitKt.awaitSingleOrNull(publisher, (Continuation<Object>) continuation);
      }

   }

   public static boolean isSuspendingFunction(Method method) {
      if (KotlinDetector.isKotlinType(method.getDeclaringClass())) {
         Class<?>[] types = method.getParameterTypes();
         if (types.length > 0 && "kotlin.coroutines.Continuation".equals(types[types.length - 1].getName())) {
            return true;
         }
      }
      return false;
   }

}

