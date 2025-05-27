import {StubQueryMessageWithSlug} from "../../app/services/query.service";

export const KafkaDbApi: StubQueryMessageWithSlug = {
  "title": "Enrich Kafka with a DB Query",
  "slug": "kafka-db-api",
  "query": {
    "layout": {
      "showSchema": true,
      "showDiagram": true,
      "showReadme": false,
      "showQuery": true
    },
    readme: '',
    "schema": `import FilmId
/**
This shows a message queue which contains streams of messages (eg., A Kafka instance).

The messages contain events submitted each time a new review is posted by a user.
The id can also be looked up from a database
*/

model Film {
  @Id
  filmId : FilmId inherits String
  title : Title inherits String
}

service FilmsDatabase {
  table films : Film[]
}

model NewReviewSubmittedEvent {
    filmId : FilmId
}

service FilmEvents {
    stream reviews: Stream<NewReviewSubmittedEvent>
}

service ReviewsApi {
  operation getReviews(FilmId):Review[]
}
closed model Review {
  filmId: FilmId
  score: ReviewScore inherits Int
  text: ReviewText inherits String
}`,
    "stubs": [
      {
        "operationName": "films_findOneFilm",
        "response": "{\n  \"filmId\": \"F12345\",\n  \"title\": \"Inception\"\n}\n"
      },
      {
        "operationName": "reviews",
        "response": "[\n    { \"filmId\" : \"F12345\"},\n    { \"filmId\" : \"F12345\"},\n    { \"filmId\" : \"F12345\"}\n]"
      },
      {
        "operationName": "getReviews",
        "response": "[\n{\n  \"filmId\": \"F12345\",\n  \"score\": 8,\n  \"text\": \"An exceptional film with a compelling storyline and stunning visuals.\"\n}\n]"
      }
    ],
    "query": `import Review
import ReviewScore
import Title
import FilmId
import NewReviewSubmittedEvent
stream { NewReviewSubmittedEvent } as {
    id: FilmId
    title : Title
    reviews: Review[]
}[]
`,
    "parameters": {}
  }
}
