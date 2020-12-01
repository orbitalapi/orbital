---
description: >-
  Build a self-evolving system that discovers and integrates functionality at
  runtime
---

# Polymorphic service discovery

## Overview

Vyne lets you look up & invoke services at runtime.  In this walkthrough, we'll continue with our compliance engine from our [previous example](exploring-vynes-problem-solving.md), and deploy a new rule, observing how Vyne automatically adjusts to include it our calls.

## Vyne.gather\(\) - Invoking services by Type

Vyne allows us to invoke services based on their return type, and works out the parameters to pass.  The type we use for discovery can be anywhere in the type hierarchy. 

We've already used this approach in our previous example, but let's take a deeper look.

First, we defined an interface called `RuleEvaluationResult`.  Here's the definition in both Kotlin and Taxi:

{% tabs %}
{% tab title="Taxi" %}
```text
type RuleEvaluationResult {
    message : String
    ruleId : String
    status : RagStatus
}
```
{% endtab %}

{% tab title="Kotlin" %}
```kotlin
interface RuleEvaluationResult {
    val ruleId: String
    val status: RuleEvaluationStatus
    val message: String
}
```
{% endtab %}
{% endtabs %}

We then defined some services that return implementations of this interface:

{% tabs %}
{% tab title="Taxi" %}
```text
type JurisdictionRuleResponse inherits RuleEvaluationResult {}
type NotionalLimitRuleResponse inherits RuleEvaluationResult {}
type TradeValueRuleResponse inherits RuleEvaluationResult P{

operation evaluate(JurisdictionRuleRequest):JurisdictionRuleResponse {}
operation evaluate(NotionalLimitRuleRequest):NotionalLimitRuleResponse {}
operation evaluate(TradeValueRuleRequest):TradeValueRuleResponse {}
```
{% endtab %}

{% tab title="Kotlin" %}
```kotlin
@DataType
data class JurisdictionRuleResponse(
   override val ruleId: String,
   override val status: RuleEvaluationStatus,
   override val message: String
) : RuleEvaluationResult

@Operation
fun evaluate(@RequestBody request: JurisdictionRuleRequest):JurisdictionRuleResponse {}

@DataType
data class NotionalLimitRuleResponse(
   override val ruleId: String,
   override val status: RuleEvaluationStatus,
   override val message: String
) : RuleEvaluationResult

@Operation
fun evaluate(@RequestBody request: NotionalLimitRuleRequest):NotionalLimitRuleResponse {}

@DataType
data class TradeValueRuleResponse(
   override val ruleId: String,
   override val status: RuleEvaluationStatus,
   override val message: String
) : RuleEvaluationResult

@PostMapping("/rules/traderLimits")
fun evaluate(@RequestBody request: TradeValueRuleRequest):TradeValueRuleResponse {}

```
{% endtab %}
{% endtabs %}

To invoke these services, we simply asked Vyne to find us all the implementations `RuleEvaluationResult`:

```kotlin
fun evaluate(@RequestBody tradeRequest: TradeRequest): TradeComplianceResult {
   val ruleEvaluations = vyne
      .given(tradeRequest)
      .gather<RuleEvaluationResult>()

   return TradeComplianceResult(ruleEvaluations)
}
```

Here's the resulting call graph:

![](../.gitbook/assets/image%20%2812%29.png)

You can see that Vyne has called all three of our services, constructing their request objects, and collating their responses.

## Summary

By using `Vyne.gather(T)`, Vyne is able to discover services to invoke that return either:

* An instance of type `T`. 
* A subclass \(or implementation\) of type `T`.

Vyne will construct the parameter objects necessary to invoke the services, and collate the results back.



