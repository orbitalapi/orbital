package io.vyne.queryService.schemas

object SubmitEditJson {
   const val JSON = """
      {
  "types": [
    {
      "name": {
        "fullyQualifiedName": "io.vyne.demos.film.StreamingProvider",
        "parameters": [

        ],
        "name": "StreamingProvider",
        "parameterizedName": "io.vyne.demos.film.StreamingProvider",
        "namespace": "io.vyne.demos.film",
        "shortDisplayName": "StreamingProvider",
        "longDisplayName": "io.vyne.demos.film.StreamingProvider"
      },
      "attributes": {
        "name": {
          "type": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [

            ],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "shortDisplayName": "String",
            "longDisplayName": "lang.taxi.String"
          },
          "modifiers": [

          ],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": true,
          "typeDisplayName": "lang.taxi.String",
          "metadata": [

          ],
          "sourcedBy": null,
          "format": null
        },
        "pricePerMonth": {
          "type": {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [

            ],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "shortDisplayName": "Decimal",
            "longDisplayName": "lang.taxi.Decimal"
          },
          "modifiers": [

          ],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": true,
          "typeDisplayName": "lang.taxi.Decimal",
          "metadata": [

          ],
          "sourcedBy": null,
          "format": null
        }
      },
      "modifiers": [

      ],
      "metadata": [

      ],
      "inheritsFrom": [

      ],
      "enumValues": [

      ],
      "typeParameters": [

      ],
      "typeDoc": "",
      "isPrimitive": false,
      "isEnum": false,
      "isCollection": false,
      "isScalar": false,
      "fullyQualifiedName": "io.vyne.demos.film.StreamingProvider",
      "basePrimitiveTypeName": null,
      "format": [

      ],
      "unformattedTypeName": null,
      "offset": null,
      "expression": null,
      "declaresFormat": true,
      "sources": [
        {
          "name": "io/vyne/demos/film/StreamingProvider.taxi",
          "version": "0.0.0",
          "content": "namespace io.vyne.demos.film {\n   model StreamingProvider {\n            name : String?\n            pricePerMonth : Decimal?\n         }\n}",
          "packageIdentifier": {
            "organisation": "io.vyne",
            "name": "films",
            "version": "0.1.0",
            "unversionedId": "io.vyne/films",
            "id": "io.vyne/films/0.1.0",
            "uriSafeId": "io.vyne:films:0.1.0"
          },
          "packageQualifiedName": "[io.vyne/films/0.1.0]/io/vyne/demos/film/StreamingProvider.taxi",
          "id": "io/vyne/demos/film/StreamingProvider.taxi:0.0.0",
          "contentHash": "984f4c"
        }
      ],
      "formatAndZoneOffset": {
        "patterns": [

        ],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      }
    },
    {
      "name": {
        "fullyQualifiedName": "io.vyne.demos.film.AnonymousType_content",
        "parameters": [

        ],
        "name": "AnonymousType_content",
        "parameterizedName": "io.vyne.demos.film.AnonymousType_content",
        "namespace": "io.vyne.demos.film",
        "shortDisplayName": "AnonymousType_content",
        "longDisplayName": "io.vyne.demos.film.AnonymousType_content"
      },
      "attributes": {

      },
      "modifiers": [

      ],
      "metadata": [

      ],
      "inheritsFrom": [

      ],
      "enumValues": [

      ],
      "typeParameters": [

      ],
      "typeDoc": "",
      "isPrimitive": false,
      "isEnum": false,
      "isCollection": false,
      "isScalar": true,
      "fullyQualifiedName": "io.vyne.demos.film.AnonymousType_content",
      "basePrimitiveTypeName": null,
      "format": [

      ],
      "unformattedTypeName": null,
      "offset": null,
      "expression": null,
      "declaresFormat": true,
      "sources": [
        {
          "name": "io/vyne/demos/film/AnonymousType_content.taxi",
          "version": "0.0.0",
          "content": "namespace io.vyne.demos.film {\n   type AnonymousType_content\n}",
          "packageIdentifier": {
            "organisation": "io.vyne",
            "name": "films",
            "version": "0.1.0",
            "unversionedId": "io.vyne/films",
            "id": "io.vyne/films/0.1.0",
            "uriSafeId": "io.vyne:films:0.1.0"
          },
          "packageQualifiedName": "[io.vyne/films/0.1.0]/io/vyne/demos/film/AnonymousType_content.taxi",
          "id": "io/vyne/demos/film/AnonymousType_content.taxi:0.0.0",
          "contentHash": "7feb3f"
        }
      ],
      "formatAndZoneOffset": {
        "patterns": [

        ],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      }
    },
    {
      "name": {
        "fullyQualifiedName": "io.vyne.demos.film.KafkaRecordMetadata",
        "parameters": [

        ],
        "name": "KafkaRecordMetadata",
        "parameterizedName": "io.vyne.demos.film.KafkaRecordMetadata",
        "namespace": "io.vyne.demos.film",
        "shortDisplayName": "KafkaRecordMetadata",
        "longDisplayName": "io.vyne.demos.film.KafkaRecordMetadata"
      },
      "attributes": {
        "offset": {
          "type": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [

            ],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "shortDisplayName": "Int",
            "longDisplayName": "lang.taxi.Int"
          },
          "modifiers": [

          ],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": true,
          "typeDisplayName": "lang.taxi.Int",
          "metadata": [

          ],
          "sourcedBy": null,
          "format": null
        },
        "partition": {
          "type": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [

            ],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "shortDisplayName": "Int",
            "longDisplayName": "lang.taxi.Int"
          },
          "modifiers": [

          ],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": true,
          "typeDisplayName": "lang.taxi.Int",
          "metadata": [

          ],
          "sourcedBy": null,
          "format": null
        },
        "timestamp": {
          "type": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [

            ],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "shortDisplayName": "Int",
            "longDisplayName": "lang.taxi.Int"
          },
          "modifiers": [

          ],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": true,
          "typeDisplayName": "lang.taxi.Int",
          "metadata": [

          ],
          "sourcedBy": null,
          "format": null
        },
        "topic": {
          "type": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [

            ],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "shortDisplayName": "String",
            "longDisplayName": "lang.taxi.String"
          },
          "modifiers": [

          ],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": true,
          "typeDisplayName": "lang.taxi.String",
          "metadata": [

          ],
          "sourcedBy": null,
          "format": null
        }
      },
      "modifiers": [

      ],
      "metadata": [

      ],
      "inheritsFrom": [

      ],
      "enumValues": [

      ],
      "typeParameters": [

      ],
      "typeDoc": "",
      "isPrimitive": false,
      "isEnum": false,
      "isCollection": false,
      "isScalar": false,
      "fullyQualifiedName": "io.vyne.demos.film.KafkaRecordMetadata",
      "basePrimitiveTypeName": null,
      "format": [

      ],
      "unformattedTypeName": null,
      "offset": null,
      "expression": null,
      "declaresFormat": true,
      "sources": [
        {
          "name": "io/vyne/demos/film/KafkaRecordMetadata.taxi",
          "version": "0.0.0",
          "content": "namespace io.vyne.demos.film {\n   model KafkaRecordMetadata {\n            offset : Int?\n            partition : Int?\n            timestamp : Int?\n            topic : String?\n         }\n}",
          "packageIdentifier": {
            "organisation": "io.vyne",
            "name": "films",
            "version": "0.1.0",
            "unversionedId": "io.vyne/films",
            "id": "io.vyne/films/0.1.0",
            "uriSafeId": "io.vyne:films:0.1.0"
          },
          "packageQualifiedName": "[io.vyne/films/0.1.0]/io/vyne/demos/film/KafkaRecordMetadata.taxi",
          "id": "io/vyne/demos/film/KafkaRecordMetadata.taxi:0.0.0",
          "contentHash": "05e6f1"
        }
      ],
      "formatAndZoneOffset": {
        "patterns": [

        ],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      }
    }
  ],
  "services": [
    {
      "name": {
        "fullyQualifiedName": "io.vyne.demos.film.KafkaTopicService",
        "parameters": [

        ],
        "name": "KafkaTopicService",
        "parameterizedName": "io.vyne.demos.film.KafkaTopicService",
        "namespace": "io.vyne.demos.film",
        "shortDisplayName": "KafkaTopicService",
        "longDisplayName": "io.vyne.demos.film.KafkaTopicService"
      },
      "operations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.film.KafkaTopicService@@publishUsingPOST",
            "parameters": [

            ],
            "name": "KafkaTopicService@@publishUsingPOST",
            "parameterizedName": "io.vyne.demos.film.KafkaTopicService@@publishUsingPOST",
            "namespace": "io.vyne.demos.film",
            "shortDisplayName": "publishUsingPOST",
            "longDisplayName": "io.vyne.demos.film.KafkaTopicService / publishUsingPOST"
          },
          "parameters": [
            {
              "name": "content",
              "typeName": {
                "fullyQualifiedName": "io.vyne.demos.film.AnonymousType_content",
                "parameters": [

                ],
                "name": "AnonymousType_content",
                "parameterizedName": "io.vyne.demos.film.AnonymousType_content",
                "namespace": "io.vyne.demos.film",
                "shortDisplayName": "AnonymousType_content",
                "longDisplayName": "io.vyne.demos.film.AnonymousType_content"
              },
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "RequestBody",
                    "parameters": [

                    ],
                    "name": "RequestBody",
                    "parameterizedName": "RequestBody",
                    "namespace": "",
                    "shortDisplayName": "RequestBody",
                    "longDisplayName": "RequestBody"
                  },
                  "params": {

                  }
                }
              ]
            },
            {
              "name": "topic",
              "typeName": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [

                ],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              },
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "PathVariable",
                    "parameters": [

                    ],
                    "name": "PathVariable",
                    "parameterizedName": "PathVariable",
                    "namespace": "",
                    "shortDisplayName": "PathVariable",
                    "longDisplayName": "PathVariable"
                  },
                  "params": {
                    "value": "topic"
                  }
                }
              ]
            }
          ],
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "HttpOperation",
                "parameters": [

                ],
                "name": "HttpOperation",
                "parameterizedName": "HttpOperation",
                "namespace": "",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "HttpOperation"
              },
              "params": {
                "method": "POST",
                "url": "http://localhost:9981//kafka/{topic}"
              }
            }
          ],
          "typeDoc": null,
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.demos.film.KafkaRecordMetadata",
            "parameters": [

            ],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.film.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.film",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.film.KafkaRecordMetadata"
          }
        }
      ],
      "queryOperations": [

      ],
      "streamOperations": [

      ],
      "tableOperations": [

      ],
      "metadata": [

      ],
      "typeDoc": null
    },
    {
      "name": {
        "fullyQualifiedName": "io.vyne.demos.film.KafkaNewReleasesFilmIdService",
        "parameters": [

        ],
        "name": "KafkaNewReleasesFilmIdService",
        "parameterizedName": "io.vyne.demos.film.KafkaNewReleasesFilmIdService",
        "namespace": "io.vyne.demos.film",
        "shortDisplayName": "KafkaNewReleasesFilmIdService",
        "longDisplayName": "io.vyne.demos.film.KafkaNewReleasesFilmIdService"
      },
      "operations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.film.KafkaNewReleasesFilmIdService@@publishNewReleaseAnnouncementUsingPOST",
            "parameters": [

            ],
            "name": "KafkaNewReleasesFilmIdService@@publishNewReleaseAnnouncementUsingPOST",
            "parameterizedName": "io.vyne.demos.film.KafkaNewReleasesFilmIdService@@publishNewReleaseAnnouncementUsingPOST",
            "namespace": "io.vyne.demos.film",
            "shortDisplayName": "publishNewReleaseAnnouncementUsingPOST",
            "longDisplayName": "io.vyne.demos.film.KafkaNewReleasesFilmIdService / publishNewReleaseAnnouncementUsingPOST"
          },
          "parameters": [
            {
              "name": "filmId",
              "typeName": {
                "fullyQualifiedName": "lang.taxi.Int",
                "parameters": [

                ],
                "name": "Int",
                "parameterizedName": "lang.taxi.Int",
                "namespace": "lang.taxi",
                "shortDisplayName": "Int",
                "longDisplayName": "lang.taxi.Int"
              },
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "PathVariable",
                    "parameters": [

                    ],
                    "name": "PathVariable",
                    "parameterizedName": "PathVariable",
                    "namespace": "",
                    "shortDisplayName": "PathVariable",
                    "longDisplayName": "PathVariable"
                  },
                  "params": {
                    "value": "filmId"
                  }
                }
              ]
            }
          ],
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "HttpOperation",
                "parameters": [

                ],
                "name": "HttpOperation",
                "parameterizedName": "HttpOperation",
                "namespace": "",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "HttpOperation"
              },
              "params": {
                "method": "POST",
                "url": "http://localhost:9981//kafka/newReleases/{filmId}"
              }
            }
          ],
          "typeDoc": null,
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.demos.film.KafkaRecordMetadata",
            "parameters": [

            ],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.film.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.film",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.film.KafkaRecordMetadata"
          }
        }
      ],
      "queryOperations": [

      ],
      "streamOperations": [

      ],
      "tableOperations": [

      ],
      "metadata": [

      ],
      "typeDoc": null
    },
    {
      "name": {
        "fullyQualifiedName": "io.vyne.demos.film.ProtoService",
        "parameters": [

        ],
        "name": "ProtoService",
        "parameterizedName": "io.vyne.demos.film.ProtoService",
        "namespace": "io.vyne.demos.film",
        "shortDisplayName": "ProtoService",
        "longDisplayName": "io.vyne.demos.film.ProtoService"
      },
      "operations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.film.ProtoService@@getProtoSpecUsingGET",
            "parameters": [

            ],
            "name": "ProtoService@@getProtoSpecUsingGET",
            "parameterizedName": "io.vyne.demos.film.ProtoService@@getProtoSpecUsingGET",
            "namespace": "io.vyne.demos.film",
            "shortDisplayName": "getProtoSpecUsingGET",
            "longDisplayName": "io.vyne.demos.film.ProtoService / getProtoSpecUsingGET"
          },
          "parameters": [

          ],
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "HttpOperation",
                "parameters": [

                ],
                "name": "HttpOperation",
                "parameterizedName": "HttpOperation",
                "namespace": "",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9981//proto"
              }
            }
          ],
          "typeDoc": null,
          "returnTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [

            ],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "shortDisplayName": "String",
            "longDisplayName": "lang.taxi.String"
          }
        }
      ],
      "queryOperations": [

      ],
      "streamOperations": [

      ],
      "tableOperations": [

      ],
      "metadata": [

      ],
      "typeDoc": null
    },
    {
      "name": {
        "fullyQualifiedName": "io.vyne.demos.film.FilmsFilmIdStreamingProvidersService",
        "parameters": [

        ],
        "name": "FilmsFilmIdStreamingProvidersService",
        "parameterizedName": "io.vyne.demos.film.FilmsFilmIdStreamingProvidersService",
        "namespace": "io.vyne.demos.film",
        "shortDisplayName": "FilmsFilmIdStreamingProvidersService",
        "longDisplayName": "io.vyne.demos.film.FilmsFilmIdStreamingProvidersService"
      },
      "operations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.film.FilmsFilmIdStreamingProvidersService@@getStreamingProvidersForFilmUsingGET",
            "parameters": [

            ],
            "name": "FilmsFilmIdStreamingProvidersService@@getStreamingProvidersForFilmUsingGET",
            "parameterizedName": "io.vyne.demos.film.FilmsFilmIdStreamingProvidersService@@getStreamingProvidersForFilmUsingGET",
            "namespace": "io.vyne.demos.film",
            "shortDisplayName": "getStreamingProvidersForFilmUsingGET",
            "longDisplayName": "io.vyne.demos.film.FilmsFilmIdStreamingProvidersService / getStreamingProvidersForFilmUsingGET"
          },
          "parameters": [
            {
              "name": "filmId",
              "typeName": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [

                ],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              },
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "PathVariable",
                    "parameters": [

                    ],
                    "name": "PathVariable",
                    "parameterizedName": "PathVariable",
                    "namespace": "",
                    "shortDisplayName": "PathVariable",
                    "longDisplayName": "PathVariable"
                  },
                  "params": {
                    "value": "filmId"
                  }
                }
              ]
            }
          ],
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "HttpOperation",
                "parameters": [

                ],
                "name": "HttpOperation",
                "parameterizedName": "HttpOperation",
                "namespace": "",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9981//films/{filmId}/streamingProviders"
              }
            }
          ],
          "typeDoc": null,
          "returnTypeName": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [
              {
                "fullyQualifiedName": "io.vyne.demos.film.StreamingProvider",
                "parameters": [

                ],
                "name": "StreamingProvider",
                "parameterizedName": "io.vyne.demos.film.StreamingProvider",
                "namespace": "io.vyne.demos.film",
                "shortDisplayName": "StreamingProvider",
                "longDisplayName": "io.vyne.demos.film.StreamingProvider"
              }
            ],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array<io.vyne.demos.film.StreamingProvider>",
            "namespace": "lang.taxi",
            "shortDisplayName": "StreamingProvider[]",
            "longDisplayName": "io.vyne.demos.film.StreamingProvider[]"
          }
        }
      ],
      "queryOperations": [

      ],
      "streamOperations": [

      ],
      "tableOperations": [

      ],
      "metadata": [

      ],
      "typeDoc": null
    }
  ],
  "removedTypes": [

  ],
  "removedServices": [

  ],
  "typeNames": [
    {
      "fullyQualifiedName": "io.vyne.demos.film.StreamingProvider",
      "parameters": [

      ],
      "name": "StreamingProvider",
      "parameterizedName": "io.vyne.demos.film.StreamingProvider",
      "namespace": "io.vyne.demos.film",
      "shortDisplayName": "StreamingProvider",
      "longDisplayName": "io.vyne.demos.film.StreamingProvider"
    },
    {
      "fullyQualifiedName": "io.vyne.demos.film.AnonymousType_content",
      "parameters": [

      ],
      "name": "AnonymousType_content",
      "parameterizedName": "io.vyne.demos.film.AnonymousType_content",
      "namespace": "io.vyne.demos.film",
      "shortDisplayName": "AnonymousType_content",
      "longDisplayName": "io.vyne.demos.film.AnonymousType_content"
    },
    {
      "fullyQualifiedName": "io.vyne.demos.film.KafkaRecordMetadata",
      "parameters": [

      ],
      "name": "KafkaRecordMetadata",
      "parameterizedName": "io.vyne.demos.film.KafkaRecordMetadata",
      "namespace": "io.vyne.demos.film",
      "shortDisplayName": "KafkaRecordMetadata",
      "longDisplayName": "io.vyne.demos.film.KafkaRecordMetadata"
    }
  ]
}
   """
}
