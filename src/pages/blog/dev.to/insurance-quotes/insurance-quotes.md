

In this post, we're going to look at how to eliminate orchestration code between microservices.

We'll use the example of request a car insurance quote.

At it's simplest level, getting a car insurance quote looks a little something like this:


However, that "Enrich request with risk data" box is actually doing a bunch of heavy lifting.

When we explode it out, it looks a lot more like this:



Each of those nodes is a microservice, or serverless function that is executing to
look up risk data.

Note that in some cases, it's a multi-hop process, where we need to look up some
additional information before we can get the actual information we need.

For example, to calculate the risk associated with the make and model of a car, we
first need to look up car information from the license plate number.

There's a lot of orchestration code that gets written to wire these services together.

Today, we're going to look at using Semantic Metadata and Kotlin to eliminate the integration
between our microservices.  We'll be building our metadata in Taxi, and using Orbital to handle the orchestration.

## What is Taxi and Semantic Metadata?
[Taxi](https://github.com/taxilang/taxilang) is a Semantic Metadata language - which means it allows teams to create a shared
contract of what data means, beyond their field names. (see also: [Field names are a bad proxy for semantics](https://orbitalhq.com/blog/2023-01-16-using-semantic-metadata#field-names-are-a-bad-proxy-for-semantics))







## What is Orbital?
Orbital is a platform for orchestrating services, without integration code.

In comparison to other integration middleware (such as Mule, or Spring Integration) which are **imperative**, Orbital takes a **declarative** approach.

So, rather than writing code to explicitly wire services together, Orbital uses [Taxi](https://github.com/taxilang/taxilang) metadata (often embedded in other API specs) to understand what services can do,
and developers use the same metadata to describe what they want done.

The actual integration is generated on-the-fly.  This makes integration both faster to build, and more resilient to change - Orbital
automatically adapts integrations as services change.

## Building Spring Boot services with Taxi metadata

Adding

