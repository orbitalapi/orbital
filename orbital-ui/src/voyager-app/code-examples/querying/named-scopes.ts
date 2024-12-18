import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const NamedScopesInQueries: StubQueryMessageWithSlug = {
  "title": "Named scopes in queries",
  "slug": "named-scopes-in-queries",
  "query": {
    "schema": `closed model Film {
    id : FilmId inherits Int
    title : Title inherits String
    headliner : ActorId
    cast: Actor[]
}
closed model Actor {
    id : ActorId inherits Int
    name : ActorName inherits String
}

service Films {
    operation getFilm():Film
    operation getFilms():Film[]
}`,
    "query": `find { Film[] } as (film:Film) -> {
  title : Title
  star : singleBy(film.cast, (Actor) -> Actor::ActorId, film.headliner) as (actor:Actor) -> {
    name : actor.name
    filmTitle : film.title
  }
}[]
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getFilm",
        "response": "{ \n    \"id\": 1,\n    \"title\" : \"Star Wars\", \n    \"headliner\": 1,\n     \"cast\": [ \n        {\"id\": 1, \"name\" : \"Mark\" }, \n        {\"id\": 2, \"name\" : \"Carrie\" }\n     ]\n}"
      },
      {
        "operationName": "getFilms",
        "response": "[\n    { \n        \"title\" : \"Star Wars\", \n        \"id\": 1, \n        \"headliner\": 1,\n        \"cast\": [ \n            {\"id\": 1, \"name\" : \"Mark\" }, \n            {\"id\": 2, \"name\" : \"Carrie\" }\n         ]\n    },\n    { \n        \"title\" : \"Star Wars II\", \n        \"id\": 2, \n        \"headliner\": 2,\n        \"cast\": [ \n            {\"id\": 1, \"name\" : \"Mark\" }, \n            {\"id\": 2, \"name\" : \"Carrie\" }  \n        ]\n    }\n]"
      }
    ],
    "readme": "# Projection Capabilities in TaxiQL\n\nTaxiQL uses named scopes to project and \ntransform data in queries. \n\nYou can select specific fields from models and \nreshape the results to match your needs.\n\n## Core Features\n\nNamed scopes let you access and transform data within a specific context in your query. You can:\n- Define working areas to manipulate model instances\n- Name your scopes to directly access model properties  \n- Project and transform collections of data\n- Use expressions (such as `singleBy()`) to find specific items in collections\n\n### Simple Example: Film Title Projection\n\nHere's a basic query that gets all films and extracts their titles:\n\n```taxiql\nfind { Film[] } as (film:Film) -> {\n  movieName : film.title\n}[]\n```\n\nThe named scope `(film:Film)` gives us direct access to each film's properties.\n\n### Advanced Example: Film and Actor Data\n\nThis more complex query shows how to link films with their lead actors:\n\n```taxiql\nfind { Film[] } as (film:Film) -> {\n  title : Title\n  star : singleBy(film.cast, (Actor) -> Actor::ActorId, film.headliner) as (actor:Actor) -> {\n    name : actor.name\n    filmTitle : film.title\n  }\n}[]\n```\n\nThis query:\n- Finds the lead actor in each film's cast using `singleBy()`\n- Uses nested scopes to access both actor and film data\n- Returns a combined view of actor and film information\n\n"
  }
}
