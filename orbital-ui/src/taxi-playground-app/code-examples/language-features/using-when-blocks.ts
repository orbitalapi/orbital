import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const UsingWhenClauses: StubQueryMessageWithSlug = {
  "title": "Using when clauses",
  "slug": "using-when-clauses",
  "query": {
    "schema": `import taxi.stdlib.dates.currentDate

type ReleaseDate inherits Date
type FilmId inherits String
type FilmTitle inherits String
type FilmRating inherits String

enum ChildrenRatings {
  G,
  PG,
  PG13
}

closed model Film {
  id : FilmId
  title : FilmTitle
  rating : FilmRating
  releaseDate : ReleaseDate
}

type ChildFriendlyMovie inherits String by (ReleaseDate, FilmRating) ->
  when {
    ReleaseDate < currentDate() &&
        FilmRating != null &&
            ChildrenRatings.hasEnumNamed(FilmRating) -> "yes"
    else -> "no"
  }

service FilmService {
  operation getFilms(): Film[]
}`,
    "stubs": [
      {
        "operationName": "getFilms",
        "response": "[\n  {\n    \"id\": \"F001\",\n    \"title\": \"Inception\",\n    \"rating\": \"PG13\",\n    \"releaseDate\": \"2010-07-16\"\n  },\n  {\n    \"id\": \"F002\", \n    \"title\": \"The Shawshank Redemption\",\n    \"rating\": \"R\",\n    \"releaseDate\": \"1994-09-23\"\n  },\n  {\n    \"id\": \"F003\",\n    \"title\": \"Spirited Away\",\n    \"rating\": \"PG\",\n    \"releaseDate\": \"2001-07-20\"\n  },\n  {\n    \"id\": \"F004\",\n    \"title\": \"Pulp Fiction\",\n    \"rating\": \"R\",\n    \"releaseDate\": \"1994-10-14\"\n  },\n  {\n    \"id\": \"F005\",\n    \"title\": \"The Lion King\",\n    \"rating\": \"G\",\n    \"releaseDate\": \"1994-06-15\"\n  }\n]"
      }
    ],
    "query": `find { Film[] } as {
    title : FilmTitle
    rating : FilmRating
    releaseDate : ReleaseDate
    childFriendly : String by when {
        ReleaseDate < currentDate() &&
            FilmRating != null &&
                ChildrenRatings.hasEnumNamed(FilmRating) -> "yes"
        else -> "no"
    }
}[]
`,
    "parameters": {},
    "readme": "# Calculating Values with When Blocks\n\nWhen blocks let you calculate values based on conditions. These conditions can include enum checks and date comparisons.\n\n## Examples\n\n### Our dataset\nWe're using some films data, and will be \nadding a field to determine if the film is child friendly.\n\nHere's our raw dataset:\n\n```taxiql\nfind { Film[] }\n```\n\n### Inline When Block\n```taxiql\nfind { Film[] } as {\n    title : FilmTitle\n    rating : FilmRating\n    releaseDate : ReleaseDate\n    childFriendly : String by when {\n        ReleaseDate < currentDate() && \n            FilmRating != null && \n                ChildrenRatings.hasEnumNamed(FilmRating) -> \"yes\"\n        else -> \"no\"\n    }\n}[]\n```\n- Conditions are defined directly in the projection\n- Evaluates multiple conditions in sequence\n- Returns \"yes\" or \"no\" based on the conditions\n\n### Extracting to a Reusable Type\n```taxi\ntype ChildFriendlyMovie inherits String by (ReleaseDate, FilmRating) -> \n  when {\n    ReleaseDate < currentDate() && \n        FilmRating != null && \n            ChildrenRatings.hasEnumNamed(FilmRating) -> \"yes\"\n    else -> \"no\"\n  }\n\nfind { Film[] } as {\n    title : FilmTitle\n    rating : FilmRating\n    releaseDate : ReleaseDate\n    childFriendly : ChildFriendlyMovie\n}[]\n```\n- Same logic moved to a reusable type\n- Type can be used in multiple queries\n- Makes the projection cleaner and more maintainable\n\nWhen blocks provide a type-safe way to calculate values based on multiple conditions and inputs. They can be used inline for simple cases or extracted to types for reuse."
  }
}
