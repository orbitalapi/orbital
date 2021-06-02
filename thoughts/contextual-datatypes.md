

note: this is a continuation of the thoughts captured in the contextual-datatypes paper, stored on Google Drive.

```
// Whats the difference between:

type LegIsin ( LegIndex )

and 

type LegIsin {
    LegIndex
}
```
```
type LegIndex inherits Int
type Isin inherits String
type OrderIsin inherits Isin
// Can declare context of a type through parenthesis
type LegIsin ( LegIndex ) inherits Isin

// Can curry out the context through inheritence
type Leg1Isin : LegIsin ( LegIndex = 1 )

Note isAssignable:
Leg1Isin .isAssignableTo ( LegIsin(LegIndex = 1)) == true
Leg1Isin .isAssignableTo ( LegIsin(LegIndex = 2)) == false

type Order {
    isin : Isin
    isins : LegIsin[] ( LegIndex = $index )
}

isins : LegIsin[]( LegIndex = $index )

isins : [Leg1Index? , Leg2Index]


type OrderParty( PartyRole )
```

## Querying / Projecting
We should be able to query an object based on context of attributes

```
// Polymorphic mapping?
namesapce bgc {
    enum OrderType {
        FxOptionNdf("fx.optionFX.vanilla.european.call.ndf")
        FxOutright("fx.spotOrForward.outrightForward")
    }
    
    abstract model Order {
        orderType : OrderType
    }
    model FxOutrightOrder( orderType = OrderType.FxOutright ) inherits Order {
        // Outright specific attributes
    }
    model FxOptionNdf( orderType = OrderType.OptionNdf ) inherits Order {
     // ... Ndf specific attributes
    }
}
.. then query somehow to extract leg data .. 

model Leg( legIndex : LegIndex ) {
    isin : LegIsin ( LegIndex = legIndex )
    settlementDate : LegSettlementDate ( LegIndex = legIndex )
} 
```
