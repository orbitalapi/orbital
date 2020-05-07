/* tslint:disable:max-line-length */
export const sampleParsedSource = [
  {
    'source': {
      'name': 'Common',
      'version': '0.1.2',
      'content': 'namespace imad\n\nenum LifecycleEvent {\n\n    //some events \n}\n\ntype alias TradeNumber as Int\ntype alias OrderNumber as Int\n\ntype TransactionEventDate inherits Date\ntype TransactionEventTime inherits Time',
      'id': 'Common:0.1.2'
    },
    'errors': [],
    'isValid': true,
    'name': 'Common'
  },
  {
    'source': {
      'name': 'Employees',
      'version': '0.1.2',
      'content': 'namespace myBank.common\n\n[[ A EmployeeId is a myBank Internal unique identifier for myBank staff. ]]\ntype alias EmployeeId as String\n',
      'id': 'Employees:0.1.2'
    },
    'errors': [],
    'isValid': true,
    'name': 'Employees'
  },
  {
    'source': {
      'name': 'Enumerations',
      'version': '0.1.2',
      'content': 'namespace myBank.common\n\n[[\n    ISO currency codes are the three-letter alphabetic codes that represent the various [currencies](https://www.investopedia.com/terms/i/isocurrencycode.asp) used\n    throughout the world. When combined in pairs, they make up the symbols and [cross rates](https://www.investopedia.com/terms/c/crossrate.asp) used in currency\n    trading.\n    Each of the country-specific three-letter alphabetic codes also have a corresponding three-digit numeric code. These codes are identified by the\n    [International Organization for Standardization (ISO)](https://www.investopedia.com/terms/i/international-organization-for-standardization-iso.asp) a nongovernmental organization\n    which provides standards for manufacturing, commerce, technology and communication.\n    For currencies, the governing document is called ISO [4217:2015](https://www.iso.org/standard/64758.html).\n]]          \nenum CurrencyCode { \n    // TODO :iso currency list\n}\n\nenum Party {\n    Client,\n    Bank\n}\nenum TradeVerb {\n    Buys,\n    Sells\n}\n[[\n    Defines the direction of a trade, from the perspective of a specific party.\n    This is an abstract type - typically you should declare either a BankDirection, or a PartyDirection\n]]\nenum PartyDirection {\n    // party : Party,  // TODO : Spport attributes in enums\n    // verb : TradeVerb\n}\n\nenum BankDirection { // inherits PartyDirection(party = Party.Bank) */  // TODO : enum support for inheritene\n    BankBuys, // (verb = Buys) // TODO : Support for currying abstract properties in inherited types\n    BankSell // (verb = Sells)\n}\n\nenum ClientDirection /* inherits PartyDirection */ { // TODO : enum support for inheritene\n    ClientBuys, // (verb = Buys) // TODO : Support for currying abstract properties in inherited types\n    ClientSells // (verb = Sells)\n}\n\nenum QuantityType {\n    Notional, \n    Units, \n    Contracts\n}\n\nenum DayCountFraction {\n    //FpML list\n}\n\nenum CountryCode { \n        //iso country code list\n}\n\n[[\nThe asset classes traded by myBank\n]]\nenum myBankAssetClass {\n    Bond, \n    Credit, \n    CrossAsset,\n    Equity, \n    Fx,\n    Inflation,\n    InterestRate, \n    PreciousMetal, \n    Securitiy\n}\n',
      'id': 'Enumerations:0.1.2'
    },
    'errors': [],
    'isValid': true,
    'name': 'Enumerations'
  },
  {
    'source': {
      'name': 'Fpml',
      'version': '0.1.2',
      'content': 'namespace fpml\n\nenum WeeklyRollConvention {\n    MON, TUE, WED, THU, FRI, SAT, SUN, TBILL\n}\n\nenum PeriodMultiplier {\n    D, //Day\n    W, //Week\n    M, //Month\n    Y, //Year\n    T // Term\n}\n\ntype PaymentFrequency {\n    period : Period as Int\n    multiplier : PeriodMultiplier\n}\n\ntype ResetFrequency inherits PaymentFrequency {\n    rollConvention : WeeklyRollConvention\n}',
      'id': 'Fpml:0.1.2'
    },
    'errors': [],
    'isValid': true,
    'name': 'Fpml'
  },
  {
    'source': {
      'name': 'InterestRateSwap',
      'version': '0.1.2',
      'content': 'import myBank.orders.enumerations.OrderSourceSystem\nimport myBank.common.LegalEntityIdentifier\nimport myBank.orders.enumerations.OrderActivityCategory\nimport myBank.common.MaturityDate\nimport myBank.orders.enumerations.OrderMethod\nimport myBank.order.AlgoName\nimport myBank.common.EmployeeId\nimport myBank.common.CountryCode\nimport myBank.order.MicCode\nimport myBank.orders.enumerations.TimeInForce\nimport myBank.common.BankDirection\nimport myBank.order.UnitMultiplier\nimport myBank.common.Price\nimport imad.TransactionEventTime\nimport imad.TransactionEventDate\nimport imad.OrderNumber\nimport imad.TradeNumber\nimport fpml.ResetFrequency\nimport fpml.PaymentFrequency\nimport myBank.common.CurrencyCode\nimport myBank.common.Notional\nimport myBank.common.DayCountFraction\n\nnamespace myBank.imad \n\ntype Leg {\n    dayCountMethodInd : DayCountFraction\n    notional : Notional\n    originalCurrency : CurrencyCode\n    paymentFrequency : PaymentFrequency\n    rate : Rate as Decimal\n    rateSpread : RateSpread as Decimal\n    resetFrequency : ResetFrequency\n}\n\ntype FixedIncomeCrossCurrencySwaps {\n    tradeNumber : TradeNumber\n    orderNo: OrderNumber\n    //EntryType:\n    transactionEventDate : TransactionEventDate\n    transactionEventTime : TransactionEventTime\n    //SecType\n    //Sec b\n    //IdentifierCode\n    //IdentifierValue\n    //SecurityDescription\n    price: Price\n    //Quantity\n    //QuantityType\n    unitMultiplier : UnitMultiplier\n    //Currency : dealtCurrency ??\n    buySellFlag :  BankDirection\n    tif : TimeInForce\n    exchange : MicCode\n    submissionVenueIncorporationCountry :  CountryCode\n    trader : EmployeeId\n    strategy : AlgoName\n    //UnderlyingIdentifierCode\n    //UnderlyingIdentifierValue   \n    method : OrderMethod\n    expiryDate : MaturityDate\n    activityCategory : OrderActivityCategory\n    counterParty : LegalEntityIdentifier // | SCRicos  Need to add language support for multiple types\n    sourceSystem : OrderSourceSystem\n    leg1 : Leg\n    leg2 : Leg\n}\n',
      'id': 'InterestRateSwap:0.1.2'
    },
    'errors': [
      {
        'detailMessage': 'Cannot import myBank.orders.enumerations.OrderSourceSystem as it is not defined',
        'sourceName': 'InterestRateSwap',
        'line': 1,
        'char': 0
      },
      {
        'detailMessage': 'Cannot import myBank.orders.enumerations.OrderActivityCategory as it is not defined',
        'sourceName': 'InterestRateSwap',
        'line': 3,
        'char': 0
      },
      {
        'detailMessage': 'Cannot import myBank.orders.enumerations.OrderMethod as it is not defined',
        'sourceName': 'InterestRateSwap',
        'line': 5,
        'char': 0
      },
      {
        'detailMessage': 'Cannot import myBank.orders.enumerations.TimeInForce as it is not defined',
        'sourceName': 'InterestRateSwap',
        'line': 10,
        'char': 0
      },
      {
        'detailMessage': 'Unresolved type: TimeInForce',
        'sourceName': 'InterestRateSwap',
        'line': 53,
        'char': 10
      },
      {
        'detailMessage': 'Unresolved type: OrderMethod',
        'sourceName': 'InterestRateSwap',
        'line': 60,
        'char': 13
      },
      {
        'detailMessage': 'Unresolved type: OrderActivityCategory',
        'sourceName': 'InterestRateSwap',
        'line': 62,
        'char': 23
      },
      {
        'detailMessage': 'Unresolved type: OrderSourceSystem',
        'sourceName': 'InterestRateSwap',
        'line': 64,
        'char': 19
      }
    ],
    'isValid': false,
    'name': 'InterestRateSwap'
  },
  {
    'source': {
      'name': 'Order',
      'version': '0.1.2',
      'content': 'import myBank.common.VersionedIdentifier\nimport myBank.common.Money\nimport myBank.common.CurrencyCode\nnamespace myBank.order\n\ntype alias OrderStrategyName as String\ntype alias NumberOfContracts as Int\n\n[[\nIn the forex market, currency unit prices are quoted as currency pairs. The base currency – also called the transaction currency - is the first currency appearing in a currency pair quotation,\nfollowed by the second part of the quotation, called the quote currency or the counter currency. For accounting purposes, a firm may use the base currency as the domestic currency or accounting currency\nto represent all profits and losses.\n## Breaking down Base Currency\nIn forex, the base currency represents how much of the quote currency is needed for you to get one unit of the base currency. For example, if you were looking at the CAD/USD currency pair, \nthe Canadian dollar would be the base currency and the U.S. dollar would be the quote currency.\n]]\ntype BaseCurrencyCode inherits CurrencyCode\n\n[[\nThe quote currency, commonly known as "counter currency,"\nis the second currency in both a direct and indirect currency pair and is used to\ndetermine the value of the base currency. In a direct quote, the quote currency is the foreign currency,\nwhile in an indirect quote, the quote currency is the domestic currency. Quote currency\nis also referred to as the "secondary currency."\n#### Key Takeaways\n * The quote currency, commonly known as "counter currency," is the second currency in both a direct and indirect currency pair and is used to value the base currency.\n * In a direct quote, the quote currency is the foreign currency, while in an indirect quote, the quote currency is the domestic currency.\n * As the rate in a currency pair increases, the value of the quote currency is falling, whether the pair is direct or indirect.\n]]\ntype QuoteCurrencyCode inherits CurrencyCode // MP : Interesting - isn\'t this normally an attribute of a price?\ntype alias FxCounterCurrencyCode as QuoteCurrencyCode // MP : Do we need Fx here?.  Alias vs inherits?\n\ntype OriginalCurrencyCode inherits CurrencyCode\n\n\ntype BaseCurrencyPrincipalAmount inherits Money\ntype OriginalCurrencyPrincipalAmount inherits Money\ntype QuoteCurrencyPrincipalAmount inherits Money\ntype alias FxCounterCurrencyPrincipalAmount as QuoteCurrencyPrincipalAmount\ntype BaseVolume inherits Decimal\ntype Quantity inherits Int\n\n//identifiers\ntype InstructionId inherits VersionedIdentifier\ntype WorkingId inherits VersionedIdentifier\ntype alias ExecutionId as String\n\n[[\nSpecifies the ratio or multiply factor to convert from "nominal" units (e.g. contracts) to total units (e.g. shares) (e.g. 1.0, 100, 1000, etc). \nApplicable For Fixed Income, Convertible Bonds, Derivatives, etc.  In general quantities for all classes should be expressed in the basic unit of the instrument, \ne.g. shares for equities, norminal or par amount for bonds, currency for foreign exchange. \nWhen quantity is expressed in contracts, e.g. financing transactions and bond trade reporting, it hould contain the number of units in one contract \nand can be omitted if the multiplier is the default amount for the instrument, i.e. 1,000 par of bonds, 1,000,000 par for financing transactions\n\nSource (Fix) Contract multiplier  \n]]\ntype alias UnitMultiplier as Decimal\n\ntype alias ContractMultipler as UnitMultiplier\n\ntype alias AlgoName as String\ntype alias MicCode as String\n',
      'id': 'Order:0.1.2'
    },
    'errors': [],
    'isValid': true,
    'name': 'Order'
  },
  {
    'source': {
      'name': 'Trades',
      'version': '0.1.2',
      'content': 'import myBank.common.CurrencyCode\nnamespace myBank.common\n\ntype Money {\n    currency : CurrencyCode\n    amount : MoneyAmount as Decimal\n}\n\ntype Notional inherits Money\ntype Price inherits Money\n\n\n[[\n    The ISIN standard is used worldwide to identify specific securities such as bonds, stocks (common and preferred), futures, warrant, \n    rights, trusts, commercial paper and options. ISINs are assigned to securities to facilitate unambiguous clearing and settlement procedures. \n    They are composed of a 12-digit alphanumeric code and act to unify different ticker symbols “which can vary by exchange and currency” for the same security.\n    In the United States, ISINs are extinherits Identifier // alias vs inherits -- this makes all LegalEntityIdentifiers Identifiersended versions of 9-character CUSIP (Committee on Uniform Security Identification Procedures) numbers.\n    ISINs can be formed by adding a country code and check digit to the beginning and end of a CUSIP, respectively.\n    Source: [ISIN](https://www.isin.org/isin/)\n]]\ntype alias Isin as String\n\n[[\n    PUID is short for Product Unique Id.\n    It\'s a number (generally in the range of 100 - 2000) that\n    uniquely identifies a product within myBank.\n]]\ntype alias Puid as Int // TODO : This should become an enum\n\n[[\n    \n    The Legal Entity Identifier is the International ISO standard 17442. \n    LEIs are 20 digit codes that enable consistent and accurate identification of all legal entities that are parties to financial transactions, \n    including non-financial institutions. They enable a legal party to a financial transaction to be identified precisely. \n    It links back to a data set of critical information about the transacting entity, which can also include information on the ultimate ownership of the entity.\n    Source: [Swift](https://www.swift.com/standards/data-standards/lei)\n    \n]]\ntype alias LegalEntityIdentifier as String\n\ntype alias MaturityDate as Date\ntype alias ExecutionDateTime as DateTime\n\n type VersionedIdentifier {\n    version : VersionNumber as Int\n    value : IdnetifierValue as String\n}\n',
      'id': 'Trades:0.1.2'
    },
    'errors': [],
    'isValid': true,
    'name': 'Trades'
  }
];
