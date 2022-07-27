import { QuerySankeyChartRow } from '../services/query.service';

export const lineageSankeyChartData = [{
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'TraderService@@lookupTrader',
  'targetNodeType': 'AttributeName',
  'targetNode': 'firstName',
  'count': 4,
  'id': 68
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'ExpressionInput',
  'sourceNode': 'TraderFirstName',
  'targetNodeType': 'Expression',
  'targetNode': 'by taxi.stdlib.concat( this.firstName," ",this.lastName )',
  'count': 4,
  'id': 69
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'TraderService@@lookupTrader',
  'targetNodeType': 'ExpressionInput',
  'targetNode': 'TraderFirstName',
  'count': 4,
  'id': 70
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'Expression',
  'sourceNode': 'by taxi.stdlib.concat( this.firstName," ",this.lastName )',
  'targetNodeType': 'AttributeName',
  'targetNode': 'name',
  'count': 4,
  'id': 71
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'ExpressionInput',
  'sourceNode': 'TraderLastName',
  'targetNodeType': 'Expression',
  'targetNode': 'by taxi.stdlib.concat( this.firstName," ",this.lastName )',
  'count': 4,
  'id': 72
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'ReutersTraderService@@resolveReutersTraderId',
  'targetNodeType': 'QualifiedName',
  'targetNode': 'TraderService@@lookupTrader',
  'count': 4,
  'id': 73
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'ReutersOrders@@findReutersOrders',
  'targetNodeType': 'QualifiedName',
  'targetNode': 'ReutersTraderService@@resolveReutersTraderId',
  'count': 4,
  'id': 74
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'BloombergTraderService@@resolveBbgTraderId',
  'targetNodeType': 'QualifiedName',
  'targetNode': 'TraderService@@lookupTrader',
  'count': 4,
  'id': 75
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'ProvidedInput',
  'sourceNode': '',
  'targetNodeType': 'ExpressionInput',
  'targetNode': 'String',
  'count': 4,
  'id': 76
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'TraderService@@lookupTrader',
  'targetNodeType': 'ExpressionInput',
  'targetNode': 'TraderLastName',
  'count': 4,
  'id': 77
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'ExpressionInput',
  'sourceNode': 'String',
  'targetNodeType': 'Expression',
  'targetNode': 'by taxi.stdlib.concat( this.firstName," ",this.lastName )',
  'count': 4,
  'id': 78
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'BloombergOrders@@findBbgOrders',
  'targetNodeType': 'QualifiedName',
  'targetNode': 'BloombergTraderService@@resolveBbgTraderId',
  'count': 4,
  'id': 79
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'ReutersOrders@@findReutersOrders',
  'targetNodeType': 'AttributeName',
  'targetNode': 'orderId',
  'count': 2,
  'id': 80
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'BloombergOrders@@findBbgOrders',
  'targetNodeType': 'AttributeName',
  'targetNode': 'orderId',
  'count': 2,
  'id': 81
}, {
  'queryId': '3750e9ed-0a6d-4313-aa4c-d92fd8654b77',
  'sourceNodeType': 'QualifiedName',
  'sourceNode': 'TraderService@@lookupTrader',
  'targetNodeType': 'AttributeName',
  'targetNode': 'lastName',
  'count': 4,
  'id': 82
}] as QuerySankeyChartRow[];


export const lineageSankeyChartDataFromKafkaTopic =
  [
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
      "sourceNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "parameters": [],
          "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "namespace": "io.vyne.demos.films",
          "shortDisplayName": "getStreamingProvidersForFilm",
          "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
          "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm"
        },
        "verb": "GET",
        "path": "http://localhost:9981/films/{films.FilmId}/streamingProviders",
        "operationType": "Http"
      },
      "targetNodeType": "AttributeName",
      "targetNode": "provider/platformName",
      "targetNodeOperationData": null,
      "count": 5
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "film.FilmDatabase@@findOneFilm",
      "sourceNodeOperationData": {
        "connectionName": "films",
        "tableNames": [
          "film"
        ],
        "operationType": "Database"
      },
      "targetNodeType": "AttributeName",
      "targetNode": "film/description",
      "targetNodeOperationData": null,
      "count": 5
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.reviews.ReviewsService@@getReview",
      "sourceNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
          "parameters": [],
          "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
          "namespace": "io.vyne.reviews",
          "shortDisplayName": "getReview",
          "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
          "name": "ReviewsService@@getReview"
        },
        "verb": "GET",
        "path": "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}",
        "operationType": "Http"
      },
      "targetNodeType": "AttributeName",
      "targetNode": "review/review",
      "targetNodeOperationData": null,
      "count": 5
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
      "sourceNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "parameters": [],
          "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "namespace": "io.vyne.demos.films",
          "shortDisplayName": "getStreamingProvidersForFilm",
          "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
          "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm"
        },
        "verb": "GET",
        "path": "http://localhost:9981/films/{films.FilmId}/streamingProviders",
        "operationType": "Http"
      },
      "targetNodeType": "AttributeName",
      "targetNode": "provider/price",
      "targetNodeOperationData": null,
      "count": 5
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "film.FilmDatabase@@findOneFilm",
      "sourceNodeOperationData": {
        "connectionName": "films",
        "tableNames": [
          "film"
        ],
        "operationType": "Database"
      },
      "targetNodeType": "AttributeName",
      "targetNode": "film/name",
      "targetNodeOperationData": null,
      "count": 5
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
      "sourceNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "parameters": [],
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "namespace": "io.vyne.films.idlookup",
          "shortDisplayName": "lookupFromNetflixFilmId",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
          "name": "IdLookupService@@lookupFromNetflixFilmId"
        },
        "verb": "GET",
        "path": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}",
        "operationType": "Http"
      },
      "targetNodeType": "QualifiedName",
      "targetNode": "io.vyne.reviews.ReviewsService@@getReview",
      "targetNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
          "parameters": [],
          "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
          "namespace": "io.vyne.reviews",
          "shortDisplayName": "getReview",
          "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
          "name": "ReviewsService@@getReview"
        },
        "verb": "GET",
        "path": "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}",
        "operationType": "Http"
      },
      "count": 10
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.films.announcements.KafkaService@@consumeFromReleases",
      "sourceNodeOperationData": {
        "connectionName": "kafka",
        "topic": "releases",
        "operationType": "KafkaTopic"
      },
      "targetNodeType": "QualifiedName",
      "targetNode": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
      "targetNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "parameters": [],
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "namespace": "io.vyne.films.idlookup",
          "shortDisplayName": "lookupFromNetflixFilmId",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
          "name": "IdLookupService@@lookupFromNetflixFilmId"
        },
        "verb": "GET",
        "path": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}",
        "operationType": "Http"
      },
      "count": 35
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.films.announcements.KafkaService@@consumeFromReleases",
      "sourceNodeOperationData": {
        "connectionName": "kafka",
        "topic": "releases",
        "operationType": "KafkaTopic"
      },
      "targetNodeType": "AttributeName",
      "targetNode": "announcement/announcement",
      "targetNodeOperationData": null,
      "count": 5
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
      "sourceNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "parameters": [],
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "namespace": "io.vyne.films.idlookup",
          "shortDisplayName": "lookupFromNetflixFilmId",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
          "name": "IdLookupService@@lookupFromNetflixFilmId"
        },
        "verb": "GET",
        "path": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}",
        "operationType": "Http"
      },
      "targetNodeType": "AttributeName",
      "targetNode": "film/id",
      "targetNodeOperationData": null,
      "count": 5
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.reviews.ReviewsService@@getReview",
      "sourceNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
          "parameters": [],
          "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
          "namespace": "io.vyne.reviews",
          "shortDisplayName": "getReview",
          "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
          "name": "ReviewsService@@getReview"
        },
        "verb": "GET",
        "path": "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}",
        "operationType": "Http"
      },
      "targetNodeType": "AttributeName",
      "targetNode": "review/rating",
      "targetNodeOperationData": null,
      "count": 5
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.films.announcements.KafkaService@@consumeFromReleases",
      "sourceNodeOperationData": {
        "connectionName": "kafka",
        "topic": "releases",
        "operationType": "KafkaTopic"
      },
      "targetNodeType": "AttributeName",
      "targetNode": "announcement/filmId",
      "targetNodeOperationData": null,
      "count": 5
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
      "sourceNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "parameters": [],
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "namespace": "io.vyne.films.idlookup",
          "shortDisplayName": "lookupFromNetflixFilmId",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
          "name": "IdLookupService@@lookupFromNetflixFilmId"
        },
        "verb": "GET",
        "path": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}",
        "operationType": "Http"
      },
      "targetNodeType": "QualifiedName",
      "targetNode": "film.FilmDatabase@@findOneFilm",
      "targetNodeOperationData": {
        "connectionName": "films",
        "tableNames": [
          "film"
        ],
        "operationType": "Database"
      },
      "count": 10
    },
    {
      "queryId": "c5791dfd-a02f-4061-a5b0-275328c5bde0",
      "sourceNodeType": "QualifiedName",
      "sourceNode": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
      "sourceNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "parameters": [],
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "namespace": "io.vyne.films.idlookup",
          "shortDisplayName": "lookupFromNetflixFilmId",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
          "name": "IdLookupService@@lookupFromNetflixFilmId"
        },
        "verb": "GET",
        "path": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}",
        "operationType": "Http"
      },
      "targetNodeType": "QualifiedName",
      "targetNode": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
      "targetNodeOperationData": {
        "operationName": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "parameters": [],
          "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "namespace": "io.vyne.demos.films",
          "shortDisplayName": "getStreamingProvidersForFilm",
          "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
          "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm"
        },
        "verb": "GET",
        "path": "http://localhost:9981/films/{films.FilmId}/streamingProviders",
        "operationType": "Http"
      },
      "count": 10
    }
  ] as QuerySankeyChartRow[];
