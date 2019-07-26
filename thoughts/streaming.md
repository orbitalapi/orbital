# Streaming

This doc contains design considerations for Vyne and Streaming API's

## Taxi
Streaming services should publish their availability.
We should examine how gRPC publishes streaming specs, and borrow from there.

```
type PriceTick {
   ccyPair: CcyPair as String,
   price : Price as Decimal
}
type Client {
   clientId : ClientId as String
   name : ClientName as String
}

type Trade {
   symbol: CcyPair
   executedPrice : Price
   clientId : ClientId
}

@RabbitMqPublisher(kafkaInstance = "pricing")
publisher SomeRabbitInstance {
   @Topic("prices.{symbol}.ticks")
   stream currentPriceTicks(symbol:CcySymbol):Stream<PriceTick>
}

@KafkaPublisher(kafkaInstance = "trades")
publisher SomeKafkaBroker {
   @Topic("trades")
   @KafkaTopicKey(symbol)
   stream executedTrades(symbol:CcySymbol):Stream<Trade>
}
```

Discovery:

Discovery requests are executed by clients send a request for a stream to Vyne, outlining the data they'd like.
Vyne checks to see if the data stream already exists.  If so, it sends an instruction to subscribe to a specific topic:

```
given(symbol("GBP/EUR")).discover(Stream<PriceTick>)

// Note - we need to work on our query format.

given {
   Symbol : "GBP/EUR"
}
stream {
   PriceTick
}

response:

Stream {
   broker : RabbitMq,
   address: ["some","list","of",'addresses"]
   topic : "prices.GBP/EUR.ticks"
}
```

## Discovering hybrid streams

Vyne should support discovering enriched streams of data too

```
graphQl query style:
given {
   Symbol : "GBP/EUR"
}
stream {
   trade : Trade,
   name : trade -> ClientName
}

response:

Stream {
   temporary : true // This stream will only stay live until there are 0 subscribers
   broker : RabbitMq, // We shold ship an internal Rabbit broker within the streaming module for servicing adhoc streams like this
   address: ["vynesRabbitMqAddress"]
   topic : "tempTopic$dkk3skd3"
}
```

 
