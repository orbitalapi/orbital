import {StubQueryMessageWithSlug} from "../../app/services/query.service";

export const DatabaseApiEnrich: StubQueryMessageWithSlug = {
  "title": "Combining APIs and a Database",
  "slug": "database-enrich-against-api",
  "query": {
    "layout": {
      "showSchema": true,
      "showDiagram": false,
      "showReadme": true,
      "showQuery": true
    },
    "schema": `import MovieId

// A model describing our movies.
// It matches the table schema from our database.
model Movie {
  id : MovieId inherits Int
  releaseYear: ReleaseYear inherits Int
  title : Title inherits String
}

// A database containing our movies data.
// As this is a playground, we've left out the
// db connection details, and
// responses have been stubbed out in the stubs panel.
service MoviesDatabase {
  table movies : Movie[]
}

closed model FilmReview {
  review: ReviewText inherits String
  score: ReviewScore inherits Decimal
}

// A REST API returning film reviews.
// As this is a playground, we've left out the
// API connection details, and
// responses have been stubbed out in the stubs panel.
service ReviewsApi {
  operation getFilmReview(MovieId):FilmReview
}

type ScreeningDate inherits Instant

closed model CinemaListing {
  cinema: CinemaName inherits String
  upcomingScreenings : ScreeningDate[]
}
// A rest API returning movie screening times.
// As this is a playground, we've left out the
// API connection details, and
// responses have been stubbed out in the stubs panel.
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
    "query": `find { Movie[](ReleaseYear > 2020) } as {
   title, // shorthand to include the fields from the original response
   releaseYear, // shorthand to include the fields from the original response
   // Adding fields by type.
   // TaxiQL engines will find the appropriate data source, and
   // fetch the data
   review: ReviewScore,
   upcomingListings: CinemaListing
}[]
`,
    "parameters": {},
    "readme": "## Enriching database info with API calls\nThis example shows fetching data from a database\nof movies, and enriching it with data from two\nAPIs:\n* An API of upcoming screening times\n* An API of film reviews\n\n\n> [!TIP]\n> We've hidden the standard diagram by default in this example. <br />\n> You can turn it back on in the sidebar\n\nHere's a digram of our services, and how\nthe data relates:\n\n```components\n{\n\"showTypeToolbar\" : false,\n\"members\" : {\n    \"MoviesDatabase\" : {},\n    \"Movie\" : {},\n    \"FilmReview\" : {},\n    \"ReviewsApi\" : {},\n    \"CinemaListing\" : {},\n    \"ShowListingsApi\" : {}\n\n}\n}\n```\n\nLet's start by asking for data from our database.\n\n> [!TIP]\n> These queries are runnable - click to execute\n> them in the query panel.\n\n```taxiql\nfind { Movie[] }\n```\n\n## Adding criteria\n\n> [!TIP]\n> Our database responses are stubbed, so adding this criteria doesn't affect\n> the output in this playground - but we wanted to show you the syntax\n\n```taxiql\nfind { Movie[](ReleaseYear > 2020) }\n```\n\n## Enrichg with API calls\nTaxiQL is a declarative language - we ask for the data we \nwant, and leave it to the query engine to work out how to \nconnect the data sources.\n\nTo ask for data enriched from API sources, we simply restructure the \nresponse (called a \"Projection\"), adding in the fields we want\n\n```taxiql\nfind { Movie[](ReleaseYear > 2020) } as {\n   title, // shorthand to include the fields from the original response\n   releaseYear, // shorthand to include the fields from the original response\n   // Adding fields by type.\n   // TaxiQL engines will find the appropriate data source, and\n   // fetch the data\n   review: ReviewScore, \n   upcomingListings: CinemaListing\n}[]\n```\n\n> [!TIP]\n> After running this query, check out the Requests panel\n> to see the actual requests that were performed, and the Lineage\n> diagram that shows where data was sourced from\n"
  }
}
