export interface CodeSample {
  title: string;
  code: string;
}

const HelloWorld: CodeSample = {
  title: 'Hello, world',
  code: `/**
Welcome to Voyager - a microservices diagramming tool.

Easily create diagrams that visualize the connections between services and data sources in your stack

How it works:

- Describe your services and model

That's it! Links between servies will automatically be made where types are shared.

e.g. In the example below, we have a Reviews service which accepts an Input of FilmId and returns a FilmReview.

Also described is the FilmReview model, and we can see that a connection has been added between the two objects in the diagram.

We'd love to hear what you think. Head over to our GitHub repo to report issues, or jump on our Slack channel to chat.
*/

model Film {
  filmId : FilmId inherits String
}

service FilmsDatabase {
  table films : Film[]
}

model FilmReview {
  id : FilmId
  reviewScore: Int
}

service Reviews {
  operation getReview(FilmId): FilmReview
}
  `
}

const DatabaseWithTables: CodeSample = {
  title: 'Database with tables',
  code: `/**
This diagram shows a database, with two tables (and their associated models) exposed.

*/
model Film {
  filmId : FilmId inherits String
  title: FilmTitle inherits String
  duration: FilmDuration inherits Minutes inherits Int
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
export const CodeSamples: CodeSample[] = [
  HelloWorld,
  ModelWithApis,
  DatabaseWithTables,
  MessageQueueAndDatabase
];
