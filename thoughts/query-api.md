# Query API

The API should be expressive, and allow consumers to describe 
what they know, and what they want to discover, with minimal
ceremony.

GraphQL and JSON are useful starting points, but not enough.

Currently, the Query API looks like this:

```kotlin
data class Query(val expression: QueryExpression, val facts: List<Fact> = emptyList(), val queryMode: QueryMode = QueryMode.DISCOVER) {

data class TypeNameQueryExpression(val typeName: String) : QueryExpression
data class TypeNameListQueryExpression(val typeNames: List<String>) : QueryExpression
data class GraphQlQueryExpression(val shape: Map<String, Object>) : QueryExpression
```

which results in queries like:

```json
{
  "expression" : "Trade",
  "facts" : [
    { "typeName" : "com.acme.Symbol", "value" : "GBP/EUR" }   
  ],
  "queryMode" : "DISCOVER"
}
```

## Alternatives to consider
I'd rather try a more bespoke query syntax, which is expressive

given {
   Symbol : "GBP/EUR"
}
discover {
   trade : Trade,
}
### Given
The types listed in the given section can contain variable names (for referencing 
elsewhere in the query), and a mandatory type, with a value:

```
given {
    Symbol = 'GBP/EUR' // Symbol is a Typename
}
// or
given {
    symbol : Symbol = 'GBP/EUR' // Symbol is a Typename, symbol is a variable name
}
```



### Resolving types
When types can be unambiguously resolved, we apply smart resoltion.

```
given {
    Symbol = 'GBP/EUR' // Symbol is unambiguously resolve to com.acme.Symbol
}
```

If ambiguity exists, then types must be explicit, or imported:

```
given {
    com.acme.Symbol = 'GBP/EUR' // Symbol explicitly defined
}

// or
import com.acme.Symbol

given {
    Symbol = 'GBP/EUR' // Symbol is resolved via import
}
```


### QueryMode
Move the queryMode to a top-level element, and model the various different modes.

```
given {
   symbol: Symbol = "GBP/EUR"
}
[discover|gather|stream] {
   trade : Trade
}
```

### Being selective about what's returned
Clients can request the shape of data they're interested in

```
discover {
    trade : Trade // give me everything in the trade object
}

discover {
    trade : Trade {
        tradeDate, price // just give me the tradeDate and price attributes of trade
    }
}

discover {
    trade : Trade {
        tradeDate, price, 
        clientName : trade -> ClientName // also, give me a property called ClientName, resolved using data within the trade
    }
}

discover {
    trade : Trade {
        tradeDate, price, 
        traderName : trade -[wasBookedBy]-> EmployeeName // also, give me a property called ClientName, resolved using data within the trade
    }
}

```


### Modelling relationships between data
```
given {
   symbol:Symbol = "GBP/EUR"
}
[discover|gather|stream] {
   trade : Trade
   clientName : trade -> ClientName // From the trade, find any ClientName
}
```

A more advanced example:

```
given {
   symbol:Symbol = "GBP/EUR"
}
[discover|gather|stream] {
   trade : Trade
   clientName : trade -> ClientName // From the trade, find any ClientName
}
```

```
given {
    isin : Isin = 'X30032'
}
discover {
    Trade ( comment contains isin )
}
```

## Specifying criteria

Most initial use-cases focussed on pure equality - where values in the `given` 
clause are evluated as `equalTo` a value.  However, to support search and discovery
of data, this often isn't enough.

THis requires two parts:
 - The query language must be able to express the criteria
 - Service contracts much be able to express the contract they're publishing.
 
### Operation definition

*See also:* Contextual Datatypes paper.

```
operation findPrices(
    from:StartTimestamp as QuoteTimestamp,
    to:EndTimestamp as QuoteTimestamp,
    markets: Market[],
    isin:Isin) : Price[](
        timestamp between from and to,
        isin -[hasPrice]-> this
    )
```

Breaking this down:
 * `StartTimestamp` and `EndTimestamp` are declared a new types in the operation.  (This is a new concept)
    * Is this required?  Can we infer the relationship sufficiently from the `between` predicate ?
 * `operation` declarations now have bodys, to express context
 * We're expressing that there's a criteria applied to `timestamp` called `between`
    * Initially, we should support a subset of 5-10 criteria expressions, but over time look to see how we can allow publishers to declare their own.
 * The `isin` field has a relationship of `hasPrice` to the Price object.
    * How these relationships are expressed in Taxi is explored in detail in `Contextual Datatypes`.

### Query definition
Now that an operation has published it's capability, we can query it.

```
given {
    myIsin:Isin = 'XS1103286305`
} 
discover {
    PriceTick[]( 
        timestamp between '2019-05-23T08:00:00Z' and '2019-05-23T09:00:00Z',
        this -[isPriceFor]-> myIsin
    )
}
```

Here, the `timestamp` property of `PriceTick` must fall between two values.

Also, the returned set of price ticks will be for the provided Isin.

#### Relationships without attributes
A `PriceTick` doesn't contain an Isin attribute - so it's up to the service to work out how 
to guarantee the relationship.  However, by putting the information in it's contract,
the service is stating that it'll make that guarantee.

This is explored in more detail in `Contextual Datatypes`.

## Aggregation
```
discover {
    averagePrice : average(Price) // Price is a type, average is a function,
    minPrice : min(Price)
    maxPrice : max(Price)
}
```

# 25-May thoughts

```
query { // query vs discover? query|stream?
    Order[](
        TradeDate >= '2019-12-20' AND // Thoughts on logical operators here?
        TradeDate < '2019-12-22'
    )
}
as SomeOrderType[]  // as for Projections

// could also do

query {
..
} as {
    someField : SomeType
    someOtherField : SomeOtherType
}[] // Cardinality -- should be an error to go from A -> B[] or A[] -> B (unless B is a type alias to B[])


```

## Projections
For now, we should error if someone tries to project where cardinality
isn't obvious.

ie.,

should be an error to go from A -> B[] or A[] -> B (unless B is a type alias to B[])

Also for projecting A[] to B[], we're gonna treat that as: 

```
A[].map { A -> vyne.given(A).build(B) }
```
