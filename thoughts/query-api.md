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
given {
   symbol: Symbol = "GBP/EUR"
}
[discover|gather|stream] {
   trade : Trade
}

### Modelling relationships between data
given {
   symbol:Symbol = "GBP/EUR"
}
[discover|gather|stream] {
   trade : Trade
   clientName : trade -> ClientName // From the trade, find any ClientName
}

A more advanced example:
given {
   symbol:Symbol = "GBP/EUR"
}
[discover|gather|stream] {
   trade : Trade
   clientName : trade -[is]> ClientName // From the trade, find any ClientName
}
