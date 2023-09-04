package com.orbitalhq.queryService.schemas

object SubmitEditJson {
   const val JSON = """
      {
  "types": [
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.demos.film.StreamingProvider",
        "parameters": [

        ],
        "name": "StreamingProvider",
        "parameterizedName": "com.orbitalhq.demos.film.StreamingProvider",
        "namespace": "com.orbitalhq.demos.film",
        "shortDisplayName": "StreamingProvider",
        "longDisplayName": "com.orbitalhq.demos.film.StreamingProvider"
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
      "fullyQualifiedName": "com.orbitalhq.demos.film.StreamingProvider",
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
          "content": "namespace com.orbitalhq.demos.film {\n   model StreamingProvider {\n            name : String?\n            pricePerMonth : Decimal?\n         }\n}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "films",
            "version": "0.1.0",
            "unversionedId": "com.orbitalhq/films",
            "id": "com.orbitalhq/films/0.1.0",
            "uriSafeId": "com.orbitalhq:films:0.1.0"
          },
          "packageQualifiedName": "[com.orbitalhq/films/0.1.0]/io/vyne/demos/film/StreamingProvider.taxi",
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
        "fullyQualifiedName": "com.orbitalhq.demos.film.AnonymousType_content",
        "parameters": [

        ],
        "name": "AnonymousType_content",
        "parameterizedName": "com.orbitalhq.demos.film.AnonymousType_content",
        "namespace": "com.orbitalhq.demos.film",
        "shortDisplayName": "AnonymousType_content",
        "longDisplayName": "com.orbitalhq.demos.film.AnonymousType_content"
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
      "fullyQualifiedName": "com.orbitalhq.demos.film.AnonymousType_content",
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
          "content": "namespace com.orbitalhq.demos.film {\n   type AnonymousType_content\n}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "films",
            "version": "0.1.0",
            "unversionedId": "com.orbitalhq/films",
            "id": "com.orbitalhq/films/0.1.0",
            "uriSafeId": "com.orbitalhq:films:0.1.0"
          },
          "packageQualifiedName": "[com.orbitalhq/films/0.1.0]/io/vyne/demos/film/AnonymousType_content.taxi",
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
        "fullyQualifiedName": "com.orbitalhq.demos.film.KafkaRecordMetadata",
        "parameters": [

        ],
        "name": "KafkaRecordMetadata",
        "parameterizedName": "com.orbitalhq.demos.film.KafkaRecordMetadata",
        "namespace": "com.orbitalhq.demos.film",
        "shortDisplayName": "KafkaRecordMetadata",
        "longDisplayName": "com.orbitalhq.demos.film.KafkaRecordMetadata"
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
      "fullyQualifiedName": "com.orbitalhq.demos.film.KafkaRecordMetadata",
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
          "content": "namespace com.orbitalhq.demos.film {\n   model KafkaRecordMetadata {\n            offset : Int?\n            partition : Int?\n            timestamp : Int?\n            topic : String?\n         }\n}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "films",
            "version": "0.1.0",
            "unversionedId": "com.orbitalhq/films",
            "id": "com.orbitalhq/films/0.1.0",
            "uriSafeId": "com.orbitalhq:films:0.1.0"
          },
          "packageQualifiedName": "[com.orbitalhq/films/0.1.0]/io/vyne/demos/film/KafkaRecordMetadata.taxi",
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
        "fullyQualifiedName": "com.orbitalhq.demos.film.KafkaTopicService",
        "parameters": [

        ],
        "name": "KafkaTopicService",
        "parameterizedName": "com.orbitalhq.demos.film.KafkaTopicService",
        "namespace": "com.orbitalhq.demos.film",
        "shortDisplayName": "KafkaTopicService",
        "longDisplayName": "com.orbitalhq.demos.film.KafkaTopicService"
      },
      "operations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "com.orbitalhq.demos.film.KafkaTopicService@@publishUsingPOST",
            "parameters": [

            ],
            "name": "KafkaTopicService@@publishUsingPOST",
            "parameterizedName": "com.orbitalhq.demos.film.KafkaTopicService@@publishUsingPOST",
            "namespace": "com.orbitalhq.demos.film",
            "shortDisplayName": "publishUsingPOST",
            "longDisplayName": "com.orbitalhq.demos.film.KafkaTopicService / publishUsingPOST"
          },
          "parameters": [
            {
              "name": "content",
              "typeName": {
                "fullyQualifiedName": "com.orbitalhq.demos.film.AnonymousType_content",
                "parameters": [

                ],
                "name": "AnonymousType_content",
                "parameterizedName": "com.orbitalhq.demos.film.AnonymousType_content",
                "namespace": "com.orbitalhq.demos.film",
                "shortDisplayName": "AnonymousType_content",
                "longDisplayName": "com.orbitalhq.demos.film.AnonymousType_content"
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
            "fullyQualifiedName": "com.orbitalhq.demos.film.KafkaRecordMetadata",
            "parameters": [

            ],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "com.orbitalhq.demos.film.KafkaRecordMetadata",
            "namespace": "com.orbitalhq.demos.film",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "com.orbitalhq.demos.film.KafkaRecordMetadata"
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
        "fullyQualifiedName": "com.orbitalhq.demos.film.KafkaNewReleasesFilmIdService",
        "parameters": [

        ],
        "name": "KafkaNewReleasesFilmIdService",
        "parameterizedName": "com.orbitalhq.demos.film.KafkaNewReleasesFilmIdService",
        "namespace": "com.orbitalhq.demos.film",
        "shortDisplayName": "KafkaNewReleasesFilmIdService",
        "longDisplayName": "com.orbitalhq.demos.film.KafkaNewReleasesFilmIdService"
      },
      "operations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "com.orbitalhq.demos.film.KafkaNewReleasesFilmIdService@@publishNewReleaseAnnouncementUsingPOST",
            "parameters": [

            ],
            "name": "KafkaNewReleasesFilmIdService@@publishNewReleaseAnnouncementUsingPOST",
            "parameterizedName": "com.orbitalhq.demos.film.KafkaNewReleasesFilmIdService@@publishNewReleaseAnnouncementUsingPOST",
            "namespace": "com.orbitalhq.demos.film",
            "shortDisplayName": "publishNewReleaseAnnouncementUsingPOST",
            "longDisplayName": "com.orbitalhq.demos.film.KafkaNewReleasesFilmIdService / publishNewReleaseAnnouncementUsingPOST"
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
            "fullyQualifiedName": "com.orbitalhq.demos.film.KafkaRecordMetadata",
            "parameters": [

            ],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "com.orbitalhq.demos.film.KafkaRecordMetadata",
            "namespace": "com.orbitalhq.demos.film",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "com.orbitalhq.demos.film.KafkaRecordMetadata"
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
        "fullyQualifiedName": "com.orbitalhq.demos.film.ProtoService",
        "parameters": [

        ],
        "name": "ProtoService",
        "parameterizedName": "com.orbitalhq.demos.film.ProtoService",
        "namespace": "com.orbitalhq.demos.film",
        "shortDisplayName": "ProtoService",
        "longDisplayName": "com.orbitalhq.demos.film.ProtoService"
      },
      "operations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "com.orbitalhq.demos.film.ProtoService@@getProtoSpecUsingGET",
            "parameters": [

            ],
            "name": "ProtoService@@getProtoSpecUsingGET",
            "parameterizedName": "com.orbitalhq.demos.film.ProtoService@@getProtoSpecUsingGET",
            "namespace": "com.orbitalhq.demos.film",
            "shortDisplayName": "getProtoSpecUsingGET",
            "longDisplayName": "com.orbitalhq.demos.film.ProtoService / getProtoSpecUsingGET"
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
        "fullyQualifiedName": "com.orbitalhq.demos.film.FilmsFilmIdStreamingProvidersService",
        "parameters": [

        ],
        "name": "FilmsFilmIdStreamingProvidersService",
        "parameterizedName": "com.orbitalhq.demos.film.FilmsFilmIdStreamingProvidersService",
        "namespace": "com.orbitalhq.demos.film",
        "shortDisplayName": "FilmsFilmIdStreamingProvidersService",
        "longDisplayName": "com.orbitalhq.demos.film.FilmsFilmIdStreamingProvidersService"
      },
      "operations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "com.orbitalhq.demos.film.FilmsFilmIdStreamingProvidersService@@getStreamingProvidersForFilmUsingGET",
            "parameters": [

            ],
            "name": "FilmsFilmIdStreamingProvidersService@@getStreamingProvidersForFilmUsingGET",
            "parameterizedName": "com.orbitalhq.demos.film.FilmsFilmIdStreamingProvidersService@@getStreamingProvidersForFilmUsingGET",
            "namespace": "com.orbitalhq.demos.film",
            "shortDisplayName": "getStreamingProvidersForFilmUsingGET",
            "longDisplayName": "com.orbitalhq.demos.film.FilmsFilmIdStreamingProvidersService / getStreamingProvidersForFilmUsingGET"
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
                "fullyQualifiedName": "com.orbitalhq.demos.film.StreamingProvider",
                "parameters": [

                ],
                "name": "StreamingProvider",
                "parameterizedName": "com.orbitalhq.demos.film.StreamingProvider",
                "namespace": "com.orbitalhq.demos.film",
                "shortDisplayName": "StreamingProvider",
                "longDisplayName": "com.orbitalhq.demos.film.StreamingProvider"
              }
            ],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array<com.orbitalhq.demos.film.StreamingProvider>",
            "namespace": "lang.taxi",
            "shortDisplayName": "StreamingProvider[]",
            "longDisplayName": "com.orbitalhq.demos.film.StreamingProvider[]"
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
      "fullyQualifiedName": "com.orbitalhq.demos.film.StreamingProvider",
      "parameters": [

      ],
      "name": "StreamingProvider",
      "parameterizedName": "com.orbitalhq.demos.film.StreamingProvider",
      "namespace": "com.orbitalhq.demos.film",
      "shortDisplayName": "StreamingProvider",
      "longDisplayName": "com.orbitalhq.demos.film.StreamingProvider"
    },
    {
      "fullyQualifiedName": "com.orbitalhq.demos.film.AnonymousType_content",
      "parameters": [

      ],
      "name": "AnonymousType_content",
      "parameterizedName": "com.orbitalhq.demos.film.AnonymousType_content",
      "namespace": "com.orbitalhq.demos.film",
      "shortDisplayName": "AnonymousType_content",
      "longDisplayName": "com.orbitalhq.demos.film.AnonymousType_content"
    },
    {
      "fullyQualifiedName": "com.orbitalhq.demos.film.KafkaRecordMetadata",
      "parameters": [

      ],
      "name": "KafkaRecordMetadata",
      "parameterizedName": "com.orbitalhq.demos.film.KafkaRecordMetadata",
      "namespace": "com.orbitalhq.demos.film",
      "shortDisplayName": "KafkaRecordMetadata",
      "longDisplayName": "com.orbitalhq.demos.film.KafkaRecordMetadata"
    }
  ]
}
   """
}
