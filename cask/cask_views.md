# Generating views in casks

> :warning: This feature is experimental, and may be removed in a future release.

Views provide a way of building a typed view over other casks.

Currently, these are used primarily to polyfill gaps in the VyneQL query language.  As such,
we expect views in casks to be removed once the query language matures.

## Configuration
Cask views are defined through configuration.  In practice this requires
you are running spring config server in addition to the vyne stack.

Configure by exposing a set of views in `cask.views`.  Here's an example:

```yaml
cask:
   views:
      - typeName: com.test.OrderEvent
        distinct: true
        inherits:
          - test.TransactionEvent
          - test.SomeOtherEvent
        join:
           kind: LEFT_OUTER
           left: test.orders.Order
           right: test.trade.Trade
           joinOn:
              - leftField: orderId
                rightField: orderId
        where: test.Order:tradeStatus = 'Open'
``` 

This generates a view that links between two casks - `test.orders.Order` and `test.trade.Trade`.

## Mapping between type and db table
The cask internals handle the mapping from types to actual db tables and column names.

## Where clause
Where clauses are specified using the syntax of `typeName:fieldName` - such as `test.Order:tradeStatus` refers
to the `tradeStatus` field on the `test.Order` type.

The where clause may contain any valid WHERE statement against Postgres.  column names
are converted in-place.
