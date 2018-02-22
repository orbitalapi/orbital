# CreditInc - Demo App

This app shows a demo usage of Polymer.

CreditInc. is an invoice financing company.

The InvoiceApp allows clients to submit an invoice, and see
what markup CreditInc. will charge.

The expectedException is simple:

```
Find CreditMarkup based on IndustryCode (UKSIC2003) and InvoiceValue (GBP)
```

However:

 * Invoices can be in any currency (require conversion)
 * Invoices don't have an Industry Code, but do have a ClientId
 * Clients can be looked up, but only have an IndustryCode (UKSIC2008)
 
Therefore, to invoke this profilerOperation, we need to apply a couple of solvers:

InvoiceValue -convertTo-> GBP
InvoiceClient -lookUp-> Client
Client.sicCode -convertTo-> UKSICK2003
 
