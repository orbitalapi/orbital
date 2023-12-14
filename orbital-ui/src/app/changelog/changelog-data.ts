import { ChangeLogDiffEntry, ChangeLogEntry } from './changelog.service';

export const CHANGELOG_DATA = [
  {
    "timestamp": "2022-08-11T13:42:54.626725Z",
    "affectedPackages": [
      "io.vyne.demos/films-service"
    ],
    "diffs": [
      {
        "displayName": "StreamingProviderRequest",
        "kind": "ModelAdded",
        "schemaMember": "io.vyne.demos.films.StreamingProviderRequest",
        "children": [
          {
            "displayName": "filmId",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingProviderRequest",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingServiceCosts",
        "kind": "ModelAdded",
        "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
        "children": [
          {
            "displayName": "annualCost",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "cancellationFee",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "monthlyCost",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingProvider",
        "kind": "ModelChanged",
        "schemaMember": "io.vyne.demos.films.StreamingProvider",
        "children": [
          {
            "displayName": "costs",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "service",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "name",
            "kind": "FieldRemovedFromModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "pricePerMonth",
            "kind": "FieldRemovedFromModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingMoviesProvider",
        "kind": "ServiceChanged",
        "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider",
        "children": [
          {
            "displayName": "getStreamingProvidersForFilm",
            "kind": "OperationReturnValueChanged",
            "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "children": [],
            "oldDetails": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
              "parameters": [],
              "parameterizedName": "io.vyne.demos.films.StreamingProvider",
              "longDisplayName": "io.vyne.demos.films.StreamingProvider",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "StreamingProvider",
              "name": "StreamingProvider"
            },
            "newDetails": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
              "parameters": [],
              "parameterizedName": "io.vyne.demos.films.StreamingProvider",
              "longDisplayName": "io.vyne.demos.films.StreamingProvider",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "StreamingProvider",
              "name": "StreamingProvider"
            }
          },
          {
            "displayName": "getStreamingProvidersForFilm",
            "kind": "OperationParametersChanged",
            "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "children": [],
            "oldDetails": [
              {
                "name": "filmId",
                "type": {
                  "fullyQualifiedName": "films.FilmId",
                  "parameters": [],
                  "parameterizedName": "films.FilmId",
                  "longDisplayName": "films.FilmId",
                  "namespace": "films",
                  "shortDisplayName": "FilmId",
                  "name": "FilmId"
                }
              }
            ],
            "newDetails": [
              {
                "name": "request",
                "type": {
                  "fullyQualifiedName": "io.vyne.demos.films.StreamingProviderRequest",
                  "parameters": [],
                  "parameterizedName": "io.vyne.demos.films.StreamingProviderRequest",
                  "longDisplayName": "io.vyne.demos.films.StreamingProviderRequest",
                  "namespace": "io.vyne.demos.films",
                  "shortDisplayName": "StreamingProviderRequest",
                  "name": "StreamingProviderRequest"
                }
              }
            ]
          },
          {
            "displayName": "getStreamingProvidersForFilm",
            "kind": "OperationMetadataChanged",
            "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "children": [],
            "oldDetails": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "parameterizedName": "HttpOperation",
                  "longDisplayName": "HttpOperation",
                  "namespace": "",
                  "shortDisplayName": "HttpOperation",
                  "name": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9981/films/{films.FilmId}/streamingProviders"
                }
              }
            ],
            "newDetails": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "parameterizedName": "HttpOperation",
                  "longDisplayName": "HttpOperation",
                  "namespace": "",
                  "shortDisplayName": "HttpOperation",
                  "name": "HttpOperation"
                },
                "params": {
                  "method": "POST",
                  "url": "http://localhost:9981/films/streamingServices"
                }
              }
            ]
          }
        ],
        "oldDetails": null,
        "newDetails": null
      }
    ]
  },
  {
    "timestamp": "2022-08-11T13:38:33.662326Z",
    "affectedPackages": [
      "io.vyne.demos/films-service"
    ],
    "diffs": [
      {
        "displayName": "StreamingProviderRequest",
        "kind": "ModelRemoved",
        "schemaMember": "io.vyne.demos.films.StreamingProviderRequest",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingServiceCosts",
        "kind": "ModelRemoved",
        "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingProvider",
        "kind": "ModelChanged",
        "schemaMember": "io.vyne.demos.films.StreamingProvider",
        "children": [
          {
            "displayName": "name",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "pricePerMonth",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "costs",
            "kind": "FieldRemovedFromModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "service",
            "kind": "FieldRemovedFromModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingMoviesProvider",
        "kind": "ServiceChanged",
        "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider",
        "children": [
          {
            "displayName": "getStreamingProvidersForFilm",
            "kind": "OperationAdded",
            "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "getStreamingProvidersForFilmV2",
            "kind": "OperationRemoved",
            "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilmV2",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      }
    ]
  },
  {
    "timestamp": "2022-08-11T13:37:00.999380Z",
    "affectedPackages": [
      "io.vyne/core-types"
    ],
    "diffs": [
      {
        "displayName": "Actor",
        "kind": "ModelAdded",
        "schemaMember": "actor.Actor",
        "children": [
          {
            "displayName": "actor_id",
            "kind": "FieldAddedToModel",
            "schemaMember": "actor.Actor",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "first_name",
            "kind": "FieldAddedToModel",
            "schemaMember": "actor.Actor",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "last_name",
            "kind": "FieldAddedToModel",
            "schemaMember": "actor.Actor",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "last_update",
            "kind": "FieldAddedToModel",
            "schemaMember": "actor.Actor",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "FirstName",
        "kind": "TypeAdded",
        "schemaMember": "actor.types.FirstName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "LastUpdate",
        "kind": "TypeAdded",
        "schemaMember": "actor.types.LastUpdate",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "LastName",
        "kind": "TypeAdded",
        "schemaMember": "actor.types.LastName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ActorId",
        "kind": "TypeAdded",
        "schemaMember": "actor.types.ActorId",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "NewFilmReleaseAnnouncement",
        "kind": "ModelAdded",
        "schemaMember": "NewFilmReleaseAnnouncement",
        "children": [
          {
            "displayName": "filmId",
            "kind": "FieldAddedToModel",
            "schemaMember": "NewFilmReleaseAnnouncement",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "announcement",
            "kind": "FieldAddedToModel",
            "schemaMember": "NewFilmReleaseAnnouncement",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "LanguageId",
        "kind": "TypeAdded",
        "schemaMember": "language.types.LanguageId",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "VyneQlQuery",
        "kind": "TypeAdded",
        "schemaMember": "vyne.vyneQl.VyneQlQuery",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "KafkaService",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.kafka.KafkaService",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "TopicOffset",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.kafka.TopicOffset",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "KafkaOperation",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.kafka.KafkaOperation",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ReplacementCost",
        "kind": "TypeAdded",
        "schemaMember": "film.types.ReplacementCost",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "LastUpdate",
        "kind": "TypeAdded",
        "schemaMember": "film.types.LastUpdate",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "Title",
        "kind": "TypeAdded",
        "schemaMember": "film.types.Title",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "Fulltext",
        "kind": "TypeAdded",
        "schemaMember": "film.types.Fulltext",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "Description",
        "kind": "TypeAdded",
        "schemaMember": "film.types.Description",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ReleaseYear",
        "kind": "TypeAdded",
        "schemaMember": "film.types.ReleaseYear",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "SpecialFeatures",
        "kind": "TypeAdded",
        "schemaMember": "film.types.SpecialFeatures",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "Length",
        "kind": "TypeAdded",
        "schemaMember": "film.types.Length",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "RentalRate",
        "kind": "TypeAdded",
        "schemaMember": "film.types.RentalRate",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "Rating",
        "kind": "TypeAdded",
        "schemaMember": "film.types.Rating",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "RentalDuration",
        "kind": "TypeAdded",
        "schemaMember": "film.types.RentalDuration",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "Film",
        "kind": "ModelAdded",
        "schemaMember": "film.Film",
        "children": [
          {
            "displayName": "film_id",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "title",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "description",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "release_year",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "language_id",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "original_language_id",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "rental_duration",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "rental_rate",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "length",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "replacement_cost",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "rating",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "last_update",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "special_features",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "fulltext",
            "kind": "FieldAddedToModel",
            "schemaMember": "film.Film",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "NetflixFilmId",
        "kind": "TypeAdded",
        "schemaMember": "demo.netflix.NetflixFilmId",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "SquashedTomatoesFilmId",
        "kind": "TypeAdded",
        "schemaMember": "films.reviews.SquashedTomatoesFilmId",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "FilmReviewScore",
        "kind": "TypeAdded",
        "schemaMember": "films.reviews.FilmReviewScore",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ReviewText",
        "kind": "TypeAdded",
        "schemaMember": "films.reviews.ReviewText",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingProviderName",
        "kind": "TypeAdded",
        "schemaMember": "films.StreamingProviderName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingProviderPrice",
        "kind": "TypeAdded",
        "schemaMember": "films.StreamingProviderPrice",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "FilmId",
        "kind": "TypeAdded",
        "schemaMember": "films.FilmId",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ConnectionName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.jdbc.ConnectionName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "DatabaseService",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.jdbc.DatabaseService",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "Table",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.jdbc.Table",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "DataOwner",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.catalog.DataOwner",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "S3Service",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.s3.S3Service",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "S3Operation",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.s3.S3Operation",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "S3EntryKey",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.s3.S3EntryKey",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "SqsService",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.sqs.SqsService",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "SqsOperation",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.sqs.SqsOperation",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "BlobService",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.azure.store.BlobService",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "AzureStoreOperation",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.azure.store.AzureStoreOperation",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "AzureStoreBlob",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.azure.store.AzureStoreBlob",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "AwsLambdaService",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.lambda.AwsLambdaService",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "LambdaOperation",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.lambda.LambdaOperation",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "Csv",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.formats.Csv",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "TableName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.jdbc.TableName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "SchemaName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.jdbc.SchemaName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ConnectionName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.kafka.ConnectionName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "TopicName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.kafka.TopicName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ConnectionName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.s3.ConnectionName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "BucketName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.s3.BucketName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ConnectionName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.sqs.ConnectionName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "QueueName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.sqs.QueueName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ConnectionName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.azure.store.ConnectionName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "AzureStoreContainer",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.azure.store.AzureStoreContainer",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ConnectionName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.lambda.ConnectionName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "OperationName",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.aws.lambda.OperationName",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingProviderRequest",
        "kind": "ModelChanged",
        "schemaMember": "io.vyne.demos.films.StreamingProviderRequest",
        "children": [
          {
            "displayName": "filmId",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingProviderRequest",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingProvider",
        "kind": "ModelChanged",
        "schemaMember": "io.vyne.demos.films.StreamingProvider",
        "children": [
          {
            "displayName": "service",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingServiceCosts",
        "kind": "ModelChanged",
        "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
        "children": [
          {
            "displayName": "monthlyCost",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "ActorService",
        "kind": "ServiceAdded",
        "schemaMember": "actor.ActorService",
        "children": [
          {
            "displayName": "findManyActor",
            "kind": "OperationAdded",
            "schemaMember": "actor.ActorService",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "findOneActor",
            "kind": "OperationAdded",
            "schemaMember": "actor.ActorService",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "KafkaService",
        "kind": "ServiceAdded",
        "schemaMember": "io.vyne.films.announcements.KafkaService",
        "children": [
          {
            "displayName": "consumeFromReleases",
            "kind": "OperationAdded",
            "schemaMember": "io.vyne.films.announcements.KafkaService",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "FilmDatabase",
        "kind": "ServiceAdded",
        "schemaMember": "film.FilmDatabase",
        "children": [
          {
            "displayName": "findManyFilm",
            "kind": "OperationAdded",
            "schemaMember": "film.FilmDatabase",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "findOneFilm",
            "kind": "OperationAdded",
            "schemaMember": "film.FilmDatabase",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingMoviesProvider",
        "kind": "ServiceChanged",
        "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider",
        "children": [
          {
            "displayName": "getStreamingProvidersForFilmV2",
            "kind": "OperationReturnValueChanged",
            "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilmV2",
            "children": [],
            "oldDetails": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
              "parameters": [],
              "parameterizedName": "io.vyne.demos.films.StreamingProvider",
              "longDisplayName": "io.vyne.demos.films.StreamingProvider",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "StreamingProvider",
              "name": "StreamingProvider"
            },
            "newDetails": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
              "parameters": [],
              "parameterizedName": "io.vyne.demos.films.StreamingProvider",
              "longDisplayName": "io.vyne.demos.films.StreamingProvider",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "StreamingProvider",
              "name": "StreamingProvider"
            }
          },
          {
            "displayName": "getStreamingProvidersForFilmV2",
            "kind": "OperationParametersChanged",
            "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilmV2",
            "children": [],
            "oldDetails": [
              {
                "name": "request",
                "type": {
                  "fullyQualifiedName": "io.vyne.demos.films.StreamingProviderRequest",
                  "parameters": [],
                  "parameterizedName": "io.vyne.demos.films.StreamingProviderRequest",
                  "longDisplayName": "io.vyne.demos.films.StreamingProviderRequest",
                  "namespace": "io.vyne.demos.films",
                  "shortDisplayName": "StreamingProviderRequest",
                  "name": "StreamingProviderRequest"
                }
              }
            ],
            "newDetails": [
              {
                "name": "request",
                "type": {
                  "fullyQualifiedName": "io.vyne.demos.films.StreamingProviderRequest",
                  "parameters": [],
                  "parameterizedName": "io.vyne.demos.films.StreamingProviderRequest",
                  "longDisplayName": "io.vyne.demos.films.StreamingProviderRequest",
                  "namespace": "io.vyne.demos.films",
                  "shortDisplayName": "StreamingProviderRequest",
                  "name": "StreamingProviderRequest"
                }
              }
            ]
          }
        ],
        "oldDetails": null,
        "newDetails": null
      }
    ]
  },
  {
    "timestamp": "2022-08-11T13:37:00.918246Z",
    "affectedPackages": [
      "io.vyne.demos/films-service"
    ],
    "diffs": [
      {
        "displayName": "StreamingProviderRequest",
        "kind": "TypeAdded",
        "schemaMember": "io.vyne.demos.films.StreamingProviderRequest",
        "children": [],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingProvider",
        "kind": "ModelAdded",
        "schemaMember": "io.vyne.demos.films.StreamingProvider",
        "children": [
          {
            "displayName": "costs",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingServiceCosts",
        "kind": "ModelAdded",
        "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
        "children": [
          {
            "displayName": "annualCost",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          },
          {
            "displayName": "cancellationFee",
            "kind": "FieldAddedToModel",
            "schemaMember": "io.vyne.demos.films.StreamingServiceCosts",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      },
      {
        "displayName": "StreamingMoviesProvider",
        "kind": "ServiceAdded",
        "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider",
        "children": [
          {
            "displayName": "getStreamingProvidersForFilmV2",
            "kind": "OperationAdded",
            "schemaMember": "io.vyne.demos.films.StreamingMoviesProvider",
            "children": [],
            "oldDetails": null,
            "newDetails": null
          }
        ],
        "oldDetails": null,
        "newDetails": null
      }
    ]
  }
] as ChangeLogEntry[];
