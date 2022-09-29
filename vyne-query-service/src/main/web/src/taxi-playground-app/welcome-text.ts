

export default `/*
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
`;