---
title: Type Aliasing & Microtypes
description: How to have your on custom aliases for different primitives
---

Type Aliasing \(sometimes called "Microtypes"\) is a technique of replacing primitive types with an alias that describes the intent of it's usage. More specifically, type aliasing is a language feature that supports the pattern of Microtypes as a documentation feature.

For example, instead of:

```kotlin
data class Person(val username:String, val password:String)
```

consider:

```kotlin
typealias Username = String
typealias Password = String
data class Person(val username:Username, val password:Password)
```

This isn't a new concept, and different languages have had varied levels of support for type aliasing for some time.

| Language | Language feature / syntax |
| :--- | :--- |
| Java | Not supported |
| Kotlin | `typealias T1 = T2` |
| Scala | [`type T1 = T2`](https://www.oreilly.com/library/view/learning-scala/9781449368814/ch10.html) |
| Typescript | [`type T1 = T2`](https://www.typescriptlang.org/docs/handbook/advanced-types.html) |
| C\# | Not supported |
| F\# | [`type T1 = T2`](https://fsharpforfunandprofit.com/posts/type-abbreviations/) |
| Go | [`type T1 = T2`](https://golang.org/design/18130-type-alias) |
| Taxi | `type alias` |

As an approach, Microtypes focusses on using typealiases as a documentation technique, to help promote cleaner self-documenting code. In the two examples below, the second is more self-documenting, and presents the developers intent more clearly.

```kotlin
fun sendMessage(String,String)
fun sendMessage(EmailAddress,BodyText) // With type aliases.
```

Type aliases simply providing another name to use when describing the type. The compilers typically replace references during compilation phases, such that in the compiled code, only the underlying type is present.

Depending on the level of compiler tooling & reflection support offered by the languages, Vyne is able to leverage these type aliases to produce mappings between types.

Schema generation, which is what powers Vyne's integration engine, is provided by Taxi. Currently, Taxi only supports JVM languages, with work on other languages underway. Here's a quick overview of language support across the JVM stack:

| Language | Level of support |
| :--- | :--- |
| Java | Not supported |
| Kotlin | Some support through reflection.  Compiler plugin exists for capturing data during compile phase |
| Scala | [Under investigation](https://gitlab.com/taxi-lang/taxi-lang/issues/17) |

## 

