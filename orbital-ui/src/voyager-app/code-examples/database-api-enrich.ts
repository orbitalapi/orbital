import {StubQueryMessageWithSlug} from "../../app/services/query.service";

export const DatabaseApiEnrich: StubQueryMessageWithSlug = {
  "title": "Multiple APIs and a DB",
  "slug": "database-enrich-against-api",
  "query": {
    "schema": `import MovieId

model Movie {
  id : MovieId inherits Int
  releaseYear: ReleaseYear inherits Int
  title : Title inherits String
}
service MoviesDatabase {
  table movies : Movie[]
}

closed model FilmReview {
  review: ReviewText inherits String
  score: ReviewScore inherits Decimal
}
service ReviewsApi {
  operation getFilmReview(MovieId):FilmReview
}

type ScreeningDate inherits Instant

closed model CinemaListing {
  cinema: CinemaName inherits String
  upcomingScreenings : ScreeningDate[]
}
service ShowListingsApi {
  operation getCinemaListings(MovieId):CinemaListing
}`,
    "stubs": [
      {
        "operationName": "movies_findManyMovie",
        "response": "[\n  {\n    \"id\": 1,\n    \"releaseYear\": 1994,\n    \"title\": \"The Shawshank Redemption\"\n  },\n  {\n    \"id\": 2,\n    \"releaseYear\": 1972,\n    \"title\": \"The Godfather\"\n  },\n  {\n    \"id\": 3,\n    \"releaseYear\": 2008,\n    \"title\": \"The Dark Knight\"\n  },\n  {\n    \"id\": 4,\n    \"releaseYear\": 1999,\n    \"title\": \"The Matrix\"\n  },\n  {\n    \"id\": 5,\n    \"releaseYear\": 2010,\n    \"title\": \"Inception\"\n  }\n]\n"
      },
      {
        "operationName": "getFilmReview",
        "response": "",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "p0",
                "value": 1
              }
            ],
            "response": {
              "body": "{ \"score\" : 8.9 , \"review\" : \"The pacing was good, but the cinematography really stole the show\" }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": 2
              }
            ],
            "response": {
              "body": "{ \"score\" : 9.2 , \"review\" : \"A timeless masterpiece with stunning performances and a gripping story\" }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": 3
              }
            ],
            "response": {
              "body": "{ \"score\" : 9.0 , \"review\" : \"A thrilling experience with intense action sequences and a deep emotional core\" }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": 4
              }
            ],
            "response": {
              "body": "{ \"score\" : 8.7 , \"review\" : \"An innovative and thought-provoking sci-fi film that redefined the genre\" }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": 5
              }
            ],
            "response": {
              "body": "{ \"score\" : 8.8 , \"review\" : \"A mind-bending journey with brilliant visual effects and a clever narrative\" }"
            }
          }
        ]
      },
      {
        "operationName": "getCinemaListings",
        "response": "",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "p0",
                "value": 1
              }
            ],
            "response": {
              "body": "{ \"cinema\": \"Cinema Plaza\", \"upcomingScreenings\": [ \"2024-10-25T19:30:00Z\", \"2024-10-26T14:00:00Z\", \"2024-10-27T21:00:00Z\" ] }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": 2
              }
            ],
            "response": {
              "body": "{ \"cinema\": \"Grand Central Theater\", \"upcomingScreenings\": [ \"2024-10-24T18:00:00Z\", \"2024-10-25T21:00:00Z\", \"2024-10-26T16:00:00Z\" ] }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": 3
              }
            ],
            "response": {
              "body": "{ \"cinema\": \"Downtown Cineplex\", \"upcomingScreenings\": [ \"2024-10-23T17:30:00Z\", \"2024-10-24T20:00:00Z\", \"2024-10-25T22:00:00Z\" ] }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": 4
              }
            ],
            "response": {
              "body": "{ \"cinema\": \"Star Cinema\", \"upcomingScreenings\": [ \"2024-10-23T15:00:00Z\", \"2024-10-24T18:30:00Z\", \"2024-10-25T20:45:00Z\" ] }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": 5
              }
            ],
            "response": {
              "body": "{ \"cinema\": \"Galaxy Screens\", \"upcomingScreenings\": [ \"2024-10-22T14:00:00Z\", \"2024-10-23T19:00:00Z\", \"2024-10-24T21:30:00Z\" ] }"
            }
          }
        ]
      }
    ],
    "query": `import CinemaListing
import ScreeningDate
import ReviewScore
import Movie
find { Movie[]( ReleaseYear > 2020) } as {
    title,
    releaseYear
    review: ReviewScore
    upcomingListings: CinemaListing
}[]`,
    "parameters": {}
  }
}
