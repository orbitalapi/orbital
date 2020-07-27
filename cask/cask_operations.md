# How Cask Generates Service operations

Cask uses field annotations when generating service operations for the given type.

Example:

Consider, below type definition:

<pre><code>
 type TransactionEventDateTime inherits Instant
 type OrderId inherits String
 
 model Order {
  @Id
  orderId: OrderId
  instrumentId: String
  price: Decimal
  @Between
  orderDateTime : TransactionEventDateTime( @format ="MMM dd, yyyy HH:mm:ss")
}
</code></pre>

Cask will create the following service definition for `Order`:

<pre><code>
 @ServiceDiscoveryClient(serviceName = "cask")
   service OrderCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/Order/orderId/{id}")
      operation findSingleByOrderID( @PathVariable(name = "id") id : OrderId ) : Order( OrderId = id )
      @HttpOperation(method = "GET" , url = "/api/cask/Order/orderDateTime/Between/{start}/{end}")
      operation findByOrderDateTimeBetween( @PathVariable(name = "start") start : TransactionEventDateTime, 
                                            @PathVariable(name = "end") end : TransactionEventDateTime ) 
                                            : Order[]( TransactionEventDateTime >= start, TransactionEventDateTime < end )
   }
</code></pre>

As you can see, cask generates two methods for a model with four fields as only 'orderId' and 'orderDateTime' fields are annotated with
`@Id` and `@Between` correspondingly.

## Operation Generator Annotations:

* Id

creates a method as:

@HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/<Parent Type Name>/<field name>/{id}")
operation findSingleBy<field name>( @PathVariable(name = "id") id : <field type> ) : <field type>( <field type> = id )

example:

<pre>
<code>
namespace vyne.examples {
   type OrderId inherits String
   model Order {
    @Id
    orderId: OrderId
   }
}
</code>
</pre>

will yield:

<pre>
<code>
import vyne.examples.OrderId
import vyne.examples.Order

namespace vyne.casks.vyne.examples {
   @ServiceDiscoveryClient(serviceName = "cask")
   service OrderCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/vyne/examples/Order/orderId/{id}")
      operation findSingleByOrderID( @PathVariable(name = "id") id : vyne.examples.OrderId ) : vyne.examples.Order( vyne.examples.OrderId = id )
    }
}
</code>
</pre>

* Between

Applicable to only fields with Instant or Date types!

creates a method as:

@HttpOperation(method = "GET" , url = "/api/cask/<Parent Type Name>/<field name>/Between/{start}/{end}")
operation findBy<field name>Between( @PathVariable(name = "start") start : <field type>,
                                     @PathVariable(name = "end") end : <field type> ) :
                                      <parent type name>[]( <field type> >= start, <field type> < end )

example:

<pre>
<code>
    type alias MaturityDate as Date
    type TransactionEventDateTime inherits Instant
    type OrderWindowSummary {
    @Between
    maturityDate: MaturityDate
    @Between
    orderDateTime : TransactionEventDateTime( @format = "yyyy-MM-dd HH:mm:ss.SSSSSSS")
}
</code>
</pre>

will yield:

<pre>
<code>
"""
import OrderWindowSummary
import Symbol
import MaturityDate
import TransactionEventDateTime

namespace vyne.casks {
   @ServiceDiscoveryClient(serviceName = "cask")
   service OrderWindowSummaryCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/Between/{start}/{end}")
      operation findByMaturityDateBetween( @PathVariable(name = "start") start : MaturityDate, @PathVariable(name = "end") end : MaturityDate ) : OrderWindowSummary[]( MaturityDate >= start, MaturityDate < end )
      
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Between/{start}/{end}")
      operation findByOrderDateTimeBetween( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime >= start, TransactionEventDateTime < end )
   }
}
"""
</code>
</pre>

* After

Applicable to only fields with Instant or Date types!

example:

<pre>
<code>
    type alias MaturityDate as Date
    type TransactionEventDateTime inherits Instant
    type OrderWindowSummary {
    @After
    maturityDate: MaturityDate
    @After
    orderDateTime : TransactionEventDateTime( @format = "yyyy-MM-dd HH:mm:ss.SSSSSSS")
}
</code>
</pre>

will yield:

<pre>
<code>
"""
import OrderWindowSummary
import MaturityDate
import TransactionEventDateTime

namespace vyne.casks {
   @ServiceDiscoveryClient(serviceName = "cask")
   service OrderWindowSummaryCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/After/{after}")
      operation findByMaturityDateAfter( @PathVariable(name = "after") after : MaturityDate ) : OrderWindowSummary[]( MaturityDate > after )
      
     @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/After/{after}")
     operation findByOrderDateTimeAfter( @PathVariable(name = "after") after : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime > after )
   }
}
"""
</code>
</pre>

* Before

Applicable to only fields with Instant or Date types!

example:

<pre>
<code>
    type alias MaturityDate as Date
    type TransactionEventDateTime inherits Instant
    type OrderWindowSummary {
    @Before
    maturityDate: MaturityDate
    @Before
    orderDateTime : TransactionEventDateTime( @format = "yyyy-MM-dd HH:mm:ss.SSSSSSS")
}
</code>
</pre>

will yield:

<pre>
<code>
"""
import OrderWindowSummary
import MaturityDate
import TransactionEventDateTime

namespace vyne.casks {
   @ServiceDiscoveryClient(serviceName = "cask")
   service OrderWindowSummaryCaskService {
       @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/Before/{before}")
       operation findByMaturityDateBefore( @PathVariable(name = "before") before : MaturityDate ) : OrderWindowSummary[]( MaturityDate < before )   
       
       operation findByOrderDateTimeBefore( @PathVariable(name = "before") before : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime < before )
       @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Between/{start}/{end}")
   }
}
"""
</code>
</pre>

* Association

example:

<pre>
<code>
    type Symbol inherits String
    type OrderWindowSummary {
    @Association
    symbol : Symbol
}
</code>
</pre>

will yield:

<pre>
<code>
"""
import OrderWindowSummary
import Symbol

namespace vyne.casks {
   @ServiceDiscoveryClient(serviceName = "cask")
   service OrderWindowSummaryCaskService {
        @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/symbol/{symbol}")
        operation findBySymbol( @PathVariable(name = "symbol") symbol : Symbol ) : OrderWindowSummary[] ( Symbol = symbol )

        @HttpOperation(method = "GET" , url = "/api/cask/findOneBy/OrderWindowSummary/symbol/{Symbol}")
        operation findOneBySymbol( @PathVariable(name = "symbol") symbol : Symbol ) : OrderWindowSummary
        
        @HttpOperation(method = "POST" , url = "/api/cask/findMultipleBy/OrderWindowSummary/symbol")
        operation findMultipleBySymbol( @RequestBody symbol : Symbol[] ) : OrderWindowSummary[]
   }
}
"""
</code>
</pre>
