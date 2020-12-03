---
description: >-
  Deploy services with constraints, and watch Vyne orchestrate solutions to
  adapt data when constraints aren't satisfied.
---

# Automated problem solving with Constraints & Contracts

## Overview

In this guide, we'll deploy a rules based compliance engine for financial trades, where rules require data sourced from different locations, and in specific formats.

We'll assume you've covered the previous walkthroughs, so we'll skip the basics and get straight to the main course.

{% hint style="info" %}
This walkthrough doesn't have the full code for the three services - we just highlight the parts that are interesting.  The full source for this demo is available on Gitlab, [here](https://gitlab.com/vyne/demos/tree/master/rewards).
{% endhint %}

## Expressing constraints on services

Vyne uses Taxi to allow services to describe constraints they have on input parameters.

These are the classes & services we'll be using for this demo:

{% tabs %}
{% tab title="Kotlin" %}
```kotlin
@Service
class TradeValueRuleService { 
   @Operation
   @PostMapping("/rules/traderLimits")
   fun evaluate(@RequestBody request: TradeValueRuleRequest):TradeValueRuleResponse {
      // Not shown
   }
}
   
@DataType
@ParameterType
data class TradeValueRuleRequest(
   @Constraint("currency = 'USD'")
   val tradeValue: TradeValue,
   val traderLimit: TraderMaxTradeValue
)

@DataType
typealias TradeValue = Money

@DataType("demo.Money")
@ParameterType
data class Money(
   val currency: Currency,
   val value: MoneyAmount
   )
   
```
{% endtab %}

{% tab title="Java" %}
```java
@Service
public class TradeValueRuleService {
    @Operation
    @PostMapping("/rules/traderLimits")
    TradeValueRuleResponse evalute(@RequestBody TradeValueRuleRequest request) {
        // Not shown
    }
}

@DataType("demo.TradeValueRuleRequest")
@ParameterType
public class TradeValueRuleRequest {
    @Constraint("currency = 'USD'")
    private final Money tradeValue;
    private final TraderMaxTradeValue traderLimit;

    public TradeValueRuleRequest(Money tradeValue, TraderMaxTradeValue traderLimit) {
        this.tradeValue = tradeValue;
        this.traderLimit = traderLimit;
    }
}

@DataType("demo.Money")
@ParameterType
public class Money {
    @DataType("demo.Currency")
    private final Currency currency;
    @DataType("demo.Value")
    private final MoneyAmount value;

    public Money(Currency currency, MoneyAmount value) {
        this.currency = currency;
        this.value = value;
    }
}

```
{% endtab %}
{% endtabs %}

This service evaluates a rule to do with TradeValue.  The actual implementation of the rule isn't as interesting as the contract itself - let's take a look in closer detail:

{% tabs %}
{% tab title="Kotlin" %}
```kotlin
@Operation
fun evaluate(@RequestBody request: TradeValueRuleRequest):TradeValueRuleResponse {}

data class TradeValueRuleRequest(
   @Constraint("currency = 'USD'")
   val tradeValue: TradeValue,
   val traderLimit: TraderMaxTradeValue
)
```
{% endtab %}

{% tab title="Java" %}
```java
@Operation
TradeValueRuleResponse evalute(@RequestBody TradeValueRuleRequest request) {}

@DataType("demo.TradeValueRuleRequest")
@ParameterType
public class TradeValueRuleRequest {
    @Constraint("currency = 'USD'")
    private final Money tradeValue;
    private final TraderMaxTradeValue traderLimit;

    public TradeValueRuleRequest(Money tradeValue, TraderMaxTradeValue traderLimit) {
        this.tradeValue = tradeValue;
        this.traderLimit = traderLimit;
    }
}

```
{% endtab %}
{% endtabs %}

Our `Operation` takes a `TradeValueRequest` as it's parameter - which in turn, declares a constraint on the `tradeValue` parameter - the currency of the tradeValue must be expressed in USD.

Let's run a couple of queries, and see how Vyne applies this constraint.

We'll use the `TradeRequest` as a starting point for our query, and populate it as follows:

![](../.gitbook/assets/image%20%2824%29.png)

When we submit this query, you should see the service gets invoked, and we get our response back.  This is possible, because the currency we provided of `USD` satisfies the constraint.

![](../.gitbook/assets/image%20%2810%29.png)

If we change the Currency to `USD` and re-run the same query, we should get a different response:

![](../.gitbook/assets/image%20%2828%29.png)

We need something that can convert currencies!

## Using contracts to describe behaviour

Contracts on Operations describe what functionality an operation provides.  It's how Vyne can discover services that resolve constraints.

Over in another service, we have a RateConverterService, which has the following operation:

{% tabs %}
{% tab title="Kotlin" %}
```kotlin
@PostMapping("/tradeValues/{targetCurrency}")
@ResponseContract(basedOn = "source", constraints = ResponseConstraint("currency = targetCurrency"))
@Operation
fun convert(@PathVariable("targetCurrency") targetCurrency: Currency, @RequestBody source: TradeValue): TradeValue {
```
{% endtab %}

{% tab title="Java" %}
```java
@PostMapping("/tradeValues/{targetCurrency}")
@ResponseContract(basedOn = "source", constraints = new ResponseConstraint("currency = targetCurrency"))
@Operation
TradeValue convert(@PathVariable("targetCurrency") Currency targetCurrency, @RequestBody TradeValue source) {}
```
{% endtab %}
{% endtabs %}

The `@ResponseContract` on this operation tells use that it returns a `TradeValue`, that is based on the `source` parameter, but with it's `currency` value updated to the `targetCurrency` provided in the request.

It's perhaps a little easier to read in the generated Taxi schema:

```kotlin
operation convert( source : Money( currency = "GBP" ),
    targetCurrency : String ) : Money( from source, currency = targetCurrency )
```

In other words - it converts currencies.  Perfect!

With this service deployed, if we re-run the same query, we now get a different response & execution plan:

![](../.gitbook/assets/image%20%2841%29.png)

  
Note that now that a service is exposed that can convert currencies, Vyne automatically invokes it and then uses the returned result to call our `TradeValueRuleService`. 

## Summary

This walkthrough has shown how Vyne understands constraints on operation inputs, and when a constraint isn't satisfied, how Vyne will attempt to leverage existing services to automatically adapt data.



