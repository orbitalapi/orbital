import {StubQueryMessageWithSlug} from "../app/services/query.service";
import {KafkaApiWebsocket} from "./code-examples/kafka-api-websocket";
import {KafkaDbApi} from "./code-examples/kafka-db-api";
import {DatabaseApiEnrich} from "./code-examples/database-api-enrich";
import {SimpleDataPolicy} from "./code-examples/policies/simple-data-policy";
import {PolicyOnBaseType} from "./code-examples/policies/policy-on-base-type";
import {RoleBasedKafka} from "./code-examples/policies/policy-on-kafka-stream";
import {RelationshipBasedAccessControl} from "./code-examples/policies/relationship-based-access-control";
import {BasicCsvReading} from "./code-examples/etl/basic-csv-reading";
import {NormalizingCsv} from "./code-examples/etl/normalizing-csv";
import {EnrichingCsvData} from "./code-examples/etl/enriching-csv";
import {AggregateAndStoreCsv} from "./code-examples/etl/aggregate-and-store-csv";

export interface CodeSample {
  title: string;
  slug: string;
  code: string;
  query?: string;
}

const HelloWorld: CodeSample = {
  title: 'Hello, universe',
  slug: 'hello-universe',
  code: `/**
Welcome to Taxi Playground!

Taxi is a powerful language designed for schema definition and data querying.
Use it to express and explore relationships between your data sources.

In this interactive panel, you can define schemas that describe your
 data sources and their contracts - whether they're APIs, databases,
 or other services.

 As you write, the visualization updates in real-time,
 illustrating the connections between your services and data structures.

Take our example: The Reviews service accepts a FilmId as input and returns a FilmReview.
Notice how the diagram automatically shows the relationship between these components,
making it easy to understand the data flow.

Want to get involved? Join our community on Slack to chat with other users, or visit our GitHub repository to report issues and contribute. We value your feedback!
*/

model Film {
  filmId : FilmId inherits String
}

service FilmsDatabase {
  table films : Film[]
}

model FilmReview {
  id : FilmId

  // reviewScore is typed as an Int.
  // This is fine, but could be more descriptive by using a
  // semantic type (such as ReviewScore inherits Int),
  // which lets other services indicate that they share the same attribute.
  // The linter offers a warning to let us know.
  reviewScore: Int
}

service Reviews {
  operation getReview(FilmId): FilmReview
}

find { Film[] }
  `
}

const DatabaseWithTables: CodeSample = {
  title: 'Database with tables',
  slug: 'database-with-tables',
  code: `/**
This diagram shows a database, with two tables (and their associated models) exposed.
*/

type Minutes inherits Int

model Film {
  filmId : FilmId inherits String
  title: FilmTitle inherits String
  duration: FilmDuration inherits Minutes
}

model Actor {
    firstName : FirstName inherits String
    lastName : LastName inherits String
    films : FilmId[]

}
service FilmsDatabase {
  table films : Film[]
  table actors : Actor[]
}
`
}


const ModelWithApis: CodeSample = {
  title: 'Two APIs with related data',
  slug: 'two-apis-with-related-data',
  code: `/*
This shows the basics of a couple of domain models,
two seperate microservices, and how data between them is related.

Here, we have two domains - Film and Review, along with two
microservices, which return their data.
*/

// Types can be defined as top-level declarations...
type FilmId inherits String

model Film {
    filmId: FilmId // Uses the FilmId defined above.

    // ... You can also use a short-hand to declare a type inline
    filmTitle: FilmTitle inherits String
}

service FilmsApi {
    /**
    * An Operation is capability exposed by a service.
    * Often, this is an HTTP call.
    */
    operation getFilm(FilmId):Film
}

model FilmReview {
    filmId: FilmId
    reviewText: ReviewText inherits String
    // You don't need to use semantic types for everything, just
    // where it makes sense
    reviewScore: Int
}
service ReviewsApi {
    operation getFilmReview(FilmId): FilmReview
}
`
}

const MessageQueueAndDatabase: CodeSample = {
  title: 'Message queue and database',
  slug: 'message-queue-and-database',
  code: `/**
This shows a message queue which contains streams of messages (eg., A Kafka instance).

The messages contain events submitted each time a new review is posted by a user.
The id can also be looked up from a database
*/

model Film {
  filmId : FilmId inherits String
}

service FilmsDatabase {
  table films : Film[]
}

model NewReviewSubmittedEvent {
    filmId : FilmId
}

service FilmEvents {
    stream newReleases: Stream<NewReviewSubmittedEvent>
}
  `
}


// Note: The 0th item in this array is selected on startup
const CodeSamples: CodeSample[] = [
  HelloWorld,
  ModelWithApis,
  DatabaseWithTables,
  MessageQueueAndDatabase
];

const StubExamples = codeSamplesToStubExamples(CodeSamples);
export const ExampleGroups = [
  {
    title: 'Modelling',
    snippets: StubExamples
  },
  {
    title: 'Querying & Streaming',
    snippets: [
      DatabaseApiEnrich,
      KafkaApiWebsocket,
      KafkaDbApi
    ]
  },
  {
    title: 'Data authorization',
    snippets: [
      SimpleDataPolicy,
      PolicyOnBaseType,
      RoleBasedKafka,
      RelationshipBasedAccessControl
    ]
  },
  {
    title: "Transforming data",
    snippets: [
      BasicCsvReading,
      NormalizingCsv,
      EnrichingCsvData,
      AggregateAndStoreCsv
    ]
  }
  // {
  //   title: 'Mutations',
  //   snippets: StubExamples
  // },
  // {
  //   title: 'Data Manipulation',
  //   snippets: StubExamples
  // },
]

function codeSamplesToStubExamples(codeSample: CodeSample[]): StubQueryMessageWithSlug[] {
  return codeSample.map(m => codeSampleToQueryMessage(m));
}
function codeSampleToQueryMessage(codeSample: CodeSample): StubQueryMessageWithSlug {
  return {
    title : codeSample.title,
    slug: codeSample.slug,
    query: {
      schema: codeSample.code,
      stubs: [],
      query: codeSample.query || '',
      parameters: {}
    }
  }
}
