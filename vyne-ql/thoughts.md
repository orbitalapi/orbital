service {
    query(vyneQl):Foo { .. } with capabilities {
        filter(=,in,like,),
        sum,
        count,
        avg,
        min,
        max,
        discover
    }
    stream(vyneQl against Bar) with capabilities {
       ... etc ...
    }
}


# Given vs Find
// Use Given to discover with facts from outside of the query type.
// eg:  given { customerEmailAddress : ... } find { RewardsBalance }

// Use find to restrict the elements on the return type. 

# Discovery / Enrichment
Discovery / enrichment happens when a field is requested on the return type, but not present on the
discovered object.

(see productId below, which isn't a member of Order).

If no context for discovery is provided, then enrichment uses the current / parent entity (tbc).
Further hints for discovery can be provided using the syntax `Type(from this.propertyName)` or `Type(from TypeName)`

see `traderEmail` and `salesPerson`.

find {
   Order ( 
    (startDate >= "" and endDate <= "") or foo is bar 
   ) 
} as {
    orderId, // if orderId is defined on the Order type, then the type is inferrable
    productId: ProductId // Discovered, using something in the query context, it's up to Vyne to decide how.
    traderEmail : EmailAddress(from this.traderUtCode)
    salesPerson {
        firstName : FirstName
        lastName : LastName
    }(from this.salesUtCode)
}
