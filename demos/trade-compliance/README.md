This demo shows a simple trade ticket interacting with a rules engine to find out if the trade is compliant.

The rules change, adding new data requirements, and Vyne adapts by discovering the data across the estate of services.

```
Vyne.given(tradeTicket)
    .discover(TRADE_COMPLIANCE_STATUS)
```

## Demo goals

 - Show Vyne can discover data 
 - Show Vyne can handle changes to services
 - Show Vyne can adapt a UI to discover new attributes
## Scenario

Validate a trade for  



 - Trade notional must be below a certain size
    - Trade notional becomes a per-user attribute?
 - Trade value (in USD) must not exceed a certain size  (requires converting the currency)
 - Client and Trader must be in the same country (requires discovering location from different places)
 - 
 
GetTradeComplianceStatus(TradeTicket):ComplianceStatus

Discover based on return type: RuleEvaluationResult
eg:
  `validateTradeNotionalBelowLimit(Notional):RuleEvaluationResult`, which could then evolve to:
  `validateTradeNotionalBelowLimit(Notional,TraderNotionalLimit):RuleEvaluationResult`
  
  
 
