import { moduleMetadata, storiesOf } from '@storybook/angular';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { QuerySnippetPanelModule } from 'src/app/query-snippet-panel/query-snippet-panel.module';
import { Snippet } from 'src/app/query-snippet-panel/query-snippet-panel.component';
import { Type } from 'src/app/services/schema';
import { CodeGenRequest, typescriptGenerator } from 'src/app/query-snippet-panel/query-snippet-container.component';

storiesOf('Snippet viewer', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, QuerySnippetPanelModule]
    })
  )
  .add('container', () => {
    return {
      template: `
<tui-root>
<div style='padding: 40px'>
<app-query-snippet-container></app-query-snippet-container>
    </div>
</tui-root>
`
    };
  })
  .add('snippet', () => {
    return {
      template: `
<tui-root>
<div style='padding: 40px'>
<app-query-snippet-panel [snippets]='snippets'></app-query-snippet-panel>
    </div>
</tui-root>`,
      props: {
        snippets: typescriptGenerator(codeGenRequest as any as CodeGenRequest)
      }
    };
  });


const codeGenRequest = {
  "query": "import films.reviews.ReviewText\nimport films.reviews.FilmReviewScore\nimport films.StreamingProviderPrice\nimport films.StreamingProviderName\nimport film.types.Title\nimport films.FilmId\nfind { Film[] } as {\n    id: FilmId\n    title : Title\n\n    // where can I watch this?\n    provider: StreamingProviderName\n    cost: StreamingProviderPrice\n\n    // Is it any good?\n    reviewScore: FilmReviewScore\n    reviewText: ReviewText\n}[]",
  "returnType": {
    "name": {
      "fullyQualifiedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "parameters": [],
      "name": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "parameterizedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "namespace": "",
      "longDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "shortDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8"
    },
    "attributes": {
      "id": {
        "type": {
          "fullyQualifiedName": "films.FilmId",
          "parameters": [],
          "name": "FilmId",
          "parameterizedName": "films.FilmId",
          "namespace": "films",
          "longDisplayName": "films.FilmId",
          "shortDisplayName": "FilmId"
        },
        "modifiers": [],
        "typeDoc": null,
        "defaultValue": null,
        "nullable": false,
        "typeDisplayName": "films.FilmId",
        "metadata": [],
        "sourcedBy": null,
        "format": null
      },
      "title": {
        "type": {
          "fullyQualifiedName": "film.types.Title",
          "parameters": [],
          "name": "Title",
          "parameterizedName": "film.types.Title",
          "namespace": "film.types",
          "longDisplayName": "film.types.Title",
          "shortDisplayName": "Title"
        },
        "modifiers": [],
        "typeDoc": null,
        "defaultValue": null,
        "nullable": false,
        "typeDisplayName": "film.types.Title",
        "metadata": [],
        "sourcedBy": null,
        "format": null
      },
      "provider": {
        "type": {
          "fullyQualifiedName": "films.StreamingProviderName",
          "parameters": [],
          "name": "StreamingProviderName",
          "parameterizedName": "films.StreamingProviderName",
          "namespace": "films",
          "longDisplayName": "films.StreamingProviderName",
          "shortDisplayName": "StreamingProviderName"
        },
        "modifiers": [],
        "typeDoc": null,
        "defaultValue": null,
        "nullable": false,
        "typeDisplayName": "films.StreamingProviderName",
        "metadata": [],
        "sourcedBy": null,
        "format": null
      },
      "cost": {
        "type": {
          "fullyQualifiedName": "films.StreamingProviderPrice",
          "parameters": [],
          "name": "StreamingProviderPrice",
          "parameterizedName": "films.StreamingProviderPrice",
          "namespace": "films",
          "longDisplayName": "films.StreamingProviderPrice",
          "shortDisplayName": "StreamingProviderPrice"
        },
        "modifiers": [],
        "typeDoc": null,
        "defaultValue": null,
        "nullable": false,
        "typeDisplayName": "films.StreamingProviderPrice",
        "metadata": [],
        "sourcedBy": null,
        "format": null
      },
      "reviewScore": {
        "type": {
          "fullyQualifiedName": "films.reviews.FilmReviewScore",
          "parameters": [],
          "name": "FilmReviewScore",
          "parameterizedName": "films.reviews.FilmReviewScore",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.FilmReviewScore",
          "shortDisplayName": "FilmReviewScore"
        },
        "modifiers": [],
        "typeDoc": null,
        "defaultValue": null,
        "nullable": false,
        "typeDisplayName": "films.reviews.FilmReviewScore",
        "metadata": [],
        "sourcedBy": null,
        "format": null
      },
      "reviewText": {
        "type": {
          "fullyQualifiedName": "films.reviews.ReviewText",
          "parameters": [],
          "name": "ReviewText",
          "parameterizedName": "films.reviews.ReviewText",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.ReviewText",
          "shortDisplayName": "ReviewText"
        },
        "modifiers": [],
        "typeDoc": null,
        "defaultValue": null,
        "nullable": false,
        "typeDisplayName": "films.reviews.ReviewText",
        "metadata": [],
        "sourcedBy": null,
        "format": null
      }
    },
    "modifiers": [],
    "metadata": [],
    "aliasForType": null,
    "inheritsFrom": [],
    "enumValues": [],
    "sources": [
      {
        "name": "UnknownSource",
        "version": "0.0.0",
        "content": "{\n    id: FilmId\n    title : Title\n    // where can I watch this?\n    provider: StreamingProviderName\n    cost: StreamingProviderPrice\n    // Is it any good?\n    reviewScore: FilmReviewScore\n    reviewText: ReviewText\n}[]",
        "packageIdentifier": null,
        "packageQualifiedName": "UnknownSource",
        "id": "UnknownSource:0.0.0",
        "contentHash": "9cfc9c"
      }
    ],
    "typeParameters": [],
    "typeDoc": "",
    "paramaterizedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
    "isTypeAlias": false,
    "formatAndZoneOffset": {
      "patterns": [],
      "utcZoneOffsetInMinutes": null,
      "definesPattern": false,
      "isEmpty": true
    },
    "offset": null,
    "format": [],
    "hasFormat": false,
    "declaresFormat": true,
    "basePrimitiveTypeName": null,
    "hasExpression": false,
    "unformattedTypeName": null,
    "isParameterType": false,
    "isClosed": false,
    "isPrimitive": false,
    "longDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
    "fullyQualifiedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
    "memberQualifiedName": {
      "fullyQualifiedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "parameters": [],
      "name": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "parameterizedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "namespace": "",
      "longDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "shortDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8"
    },
    "underlyingTypeParameters": [],
    "isCollection": false,
    "isStream": false,
    "collectionType": null,
    "isScalar": false
  },
  "schema": {
    "types": [
      {
        "name": {
          "fullyQualifiedName": "NewFilmReleaseAnnouncement",
          "parameters": [],
          "name": "NewFilmReleaseAnnouncement",
          "parameterizedName": "NewFilmReleaseAnnouncement",
          "namespace": "",
          "longDisplayName": "NewFilmReleaseAnnouncement",
          "shortDisplayName": "NewFilmReleaseAnnouncement"
        },
        "attributes": {
          "filmId": {
            "type": {
              "fullyQualifiedName": "demo.netflix.NetflixFilmId",
              "parameters": [],
              "name": "NetflixFilmId",
              "parameterizedName": "demo.netflix.NetflixFilmId",
              "namespace": "demo.netflix",
              "longDisplayName": "demo.netflix.NetflixFilmId",
              "shortDisplayName": "NetflixFilmId"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": true,
            "typeDisplayName": "demo.netflix.NetflixFilmId",
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "lang.taxi.formats.ProtobufField",
                  "parameters": [],
                  "name": "ProtobufField",
                  "parameterizedName": "lang.taxi.formats.ProtobufField",
                  "namespace": "lang.taxi.formats",
                  "longDisplayName": "lang.taxi.formats.ProtobufField",
                  "shortDisplayName": "ProtobufField"
                },
                "params": {
                  "tag": 1,
                  "protoType": "int32"
                }
              },
              {
                "name": {
                  "fullyQualifiedName": "Id",
                  "parameters": [],
                  "name": "Id",
                  "parameterizedName": "Id",
                  "namespace": "",
                  "longDisplayName": "Id",
                  "shortDisplayName": "Id"
                },
                "params": {}
              }
            ],
            "sourcedBy": null,
            "format": null
          },
          "announcement": {
            "type": {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": true,
            "typeDisplayName": "lang.taxi.String",
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "lang.taxi.formats.ProtobufField",
                  "parameters": [],
                  "name": "ProtobufField",
                  "parameterizedName": "lang.taxi.formats.ProtobufField",
                  "namespace": "lang.taxi.formats",
                  "longDisplayName": "lang.taxi.formats.ProtobufField",
                  "shortDisplayName": "ProtobufField"
                },
                "params": {
                  "tag": 2,
                  "protoType": "string"
                }
              }
            ],
            "sourcedBy": null,
            "format": null
          }
        },
        "modifiers": [],
        "metadata": [
          {
            "name": {
              "fullyQualifiedName": "lang.taxi.formats.ProtobufMessage",
              "parameters": [],
              "name": "ProtobufMessage",
              "parameterizedName": "lang.taxi.formats.ProtobufMessage",
              "namespace": "lang.taxi.formats",
              "longDisplayName": "lang.taxi.formats.ProtobufMessage",
              "shortDisplayName": "ProtobufMessage"
            },
            "params": {
              "packageName": "",
              "messageName": "NewFilmReleaseAnnouncement"
            }
          }
        ],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/NewFilmReleaseAnnouncement.taxi",
            "version": "0.0.0",
            "content": "import demo.netflix.NetflixFilmId\n@lang.taxi.formats.ProtobufMessage(packageName = \"\" , messageName = \"NewFilmReleaseAnnouncement\")\nmodel NewFilmReleaseAnnouncement {\n   @lang.taxi.formats.ProtobufField(tag = 1 , protoType = \"int32\") @Id filmId : NetflixFilmId?\n   @lang.taxi.formats.ProtobufField(tag = 2 , protoType = \"string\") announcement : String?\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/NewFilmReleaseAnnouncement.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/NewFilmReleaseAnnouncement.taxi:0.0.0",
            "contentHash": "7c672c"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "NewFilmReleaseAnnouncement",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "NewFilmReleaseAnnouncement",
        "fullyQualifiedName": "NewFilmReleaseAnnouncement",
        "memberQualifiedName": {
          "fullyQualifiedName": "NewFilmReleaseAnnouncement",
          "parameters": [],
          "name": "NewFilmReleaseAnnouncement",
          "parameterizedName": "NewFilmReleaseAnnouncement",
          "namespace": "",
          "longDisplayName": "NewFilmReleaseAnnouncement",
          "shortDisplayName": "NewFilmReleaseAnnouncement"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": false
      },
      {
        "name": {
          "fullyQualifiedName": "address.types.AddressId",
          "parameters": [],
          "name": "AddressId",
          "parameterizedName": "address.types.AddressId",
          "namespace": "address.types",
          "longDisplayName": "address.types.AddressId",
          "shortDisplayName": "AddressId"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/address/types/AddressId.taxi",
            "version": "0.0.0",
            "content": "namespace address.types {\n   type AddressId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/address/types/AddressId.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/address/types/AddressId.taxi:0.0.0",
            "contentHash": "87062e"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "address.types.AddressId",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "address.types.AddressId",
        "fullyQualifiedName": "address.types.AddressId",
        "memberQualifiedName": {
          "fullyQualifiedName": "address.types.AddressId",
          "parameters": [],
          "name": "AddressId",
          "parameterizedName": "address.types.AddressId",
          "namespace": "address.types",
          "longDisplayName": "address.types.AddressId",
          "shortDisplayName": "AddressId"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "demo.netflix.NetflixFilmId",
          "parameters": [],
          "name": "NetflixFilmId",
          "parameterizedName": "demo.netflix.NetflixFilmId",
          "namespace": "demo.netflix",
          "longDisplayName": "demo.netflix.NetflixFilmId",
          "shortDisplayName": "NetflixFilmId"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/netflix/netflix-types.taxi",
            "version": "0.0.0",
            "content": "namespace demo.netflix {\n   type NetflixFilmId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/netflix/netflix-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/netflix/netflix-types.taxi:0.0.0",
            "contentHash": "086c9e"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "demo.netflix.NetflixFilmId",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "demo.netflix.NetflixFilmId",
        "fullyQualifiedName": "demo.netflix.NetflixFilmId",
        "memberQualifiedName": {
          "fullyQualifiedName": "demo.netflix.NetflixFilmId",
          "parameters": [],
          "name": "NetflixFilmId",
          "parameterizedName": "demo.netflix.NetflixFilmId",
          "namespace": "demo.netflix",
          "longDisplayName": "demo.netflix.NetflixFilmId",
          "shortDisplayName": "NetflixFilmId"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.Film",
          "parameters": [],
          "name": "Film",
          "parameterizedName": "film.Film",
          "namespace": "film",
          "longDisplayName": "film.Film",
          "shortDisplayName": "Film"
        },
        "attributes": {
          "film_id": {
            "type": {
              "fullyQualifiedName": "films.FilmId",
              "parameters": [],
              "name": "FilmId",
              "parameterizedName": "films.FilmId",
              "namespace": "films",
              "longDisplayName": "films.FilmId",
              "shortDisplayName": "FilmId"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.FilmId",
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "Id",
                  "parameters": [],
                  "name": "Id",
                  "parameterizedName": "Id",
                  "namespace": "",
                  "longDisplayName": "Id",
                  "shortDisplayName": "Id"
                },
                "params": {}
              }
            ],
            "sourcedBy": null,
            "format": null
          },
          "title": {
            "type": {
              "fullyQualifiedName": "film.types.Title",
              "parameters": [],
              "name": "Title",
              "parameterizedName": "film.types.Title",
              "namespace": "film.types",
              "longDisplayName": "film.types.Title",
              "shortDisplayName": "Title"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "film.types.Title",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "description": {
            "type": {
              "fullyQualifiedName": "film.types.Description",
              "parameters": [],
              "name": "Description",
              "parameterizedName": "film.types.Description",
              "namespace": "film.types",
              "longDisplayName": "film.types.Description",
              "shortDisplayName": "Description"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": true,
            "typeDisplayName": "film.types.Description",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "release_year": {
            "type": {
              "fullyQualifiedName": "film.types.ReleaseYear",
              "parameters": [],
              "name": "ReleaseYear",
              "parameterizedName": "film.types.ReleaseYear",
              "namespace": "film.types",
              "longDisplayName": "film.types.ReleaseYear",
              "shortDisplayName": "ReleaseYear"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": true,
            "typeDisplayName": "film.types.ReleaseYear",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "language_id": {
            "type": {
              "fullyQualifiedName": "language.types.LanguageId",
              "parameters": [],
              "name": "LanguageId",
              "parameterizedName": "language.types.LanguageId",
              "namespace": "language.types",
              "longDisplayName": "language.types.LanguageId",
              "shortDisplayName": "LanguageId"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "language.types.LanguageId",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "original_language_id": {
            "type": {
              "fullyQualifiedName": "language.types.LanguageId",
              "parameters": [],
              "name": "LanguageId",
              "parameterizedName": "language.types.LanguageId",
              "namespace": "language.types",
              "longDisplayName": "language.types.LanguageId",
              "shortDisplayName": "LanguageId"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": true,
            "typeDisplayName": "language.types.LanguageId",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "rental_duration": {
            "type": {
              "fullyQualifiedName": "film.types.RentalDuration",
              "parameters": [],
              "name": "RentalDuration",
              "parameterizedName": "film.types.RentalDuration",
              "namespace": "film.types",
              "longDisplayName": "film.types.RentalDuration",
              "shortDisplayName": "RentalDuration"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "film.types.RentalDuration",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "rental_rate": {
            "type": {
              "fullyQualifiedName": "film.types.RentalRate",
              "parameters": [],
              "name": "RentalRate",
              "parameterizedName": "film.types.RentalRate",
              "namespace": "film.types",
              "longDisplayName": "film.types.RentalRate",
              "shortDisplayName": "RentalRate"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "film.types.RentalRate",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "length": {
            "type": {
              "fullyQualifiedName": "film.types.Length",
              "parameters": [],
              "name": "Length",
              "parameterizedName": "film.types.Length",
              "namespace": "film.types",
              "longDisplayName": "film.types.Length",
              "shortDisplayName": "Length"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": true,
            "typeDisplayName": "film.types.Length",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "replacement_cost": {
            "type": {
              "fullyQualifiedName": "film.types.ReplacementCost",
              "parameters": [],
              "name": "ReplacementCost",
              "parameterizedName": "film.types.ReplacementCost",
              "namespace": "film.types",
              "longDisplayName": "film.types.ReplacementCost",
              "shortDisplayName": "ReplacementCost"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "film.types.ReplacementCost",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "rating": {
            "type": {
              "fullyQualifiedName": "film.types.Rating",
              "parameters": [],
              "name": "Rating",
              "parameterizedName": "film.types.Rating",
              "namespace": "film.types",
              "longDisplayName": "film.types.Rating",
              "shortDisplayName": "Rating"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": true,
            "typeDisplayName": "film.types.Rating",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "last_update": {
            "type": {
              "fullyQualifiedName": "film.types.LastUpdate",
              "parameters": [],
              "name": "LastUpdate",
              "parameterizedName": "film.types.LastUpdate",
              "namespace": "film.types",
              "longDisplayName": "film.types.LastUpdate",
              "shortDisplayName": "LastUpdate"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "film.types.LastUpdate",
            "metadata": [],
            "sourcedBy": null,
            "format": {
              "patterns": [
                "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
              ],
              "utcZoneOffsetInMinutes": null,
              "definesPattern": true,
              "isEmpty": false
            }
          },
          "special_features": {
            "type": {
              "fullyQualifiedName": "lang.taxi.Array",
              "parameters": [
                {
                  "fullyQualifiedName": "film.types.SpecialFeatures",
                  "parameters": [],
                  "name": "SpecialFeatures",
                  "parameterizedName": "film.types.SpecialFeatures",
                  "namespace": "film.types",
                  "longDisplayName": "film.types.SpecialFeatures",
                  "shortDisplayName": "SpecialFeatures"
                }
              ],
              "name": "Array",
              "parameterizedName": "lang.taxi.Array<film.types.SpecialFeatures>",
              "namespace": "lang.taxi",
              "longDisplayName": "film.types.SpecialFeatures[]",
              "shortDisplayName": "SpecialFeatures[]"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": true,
            "typeDisplayName": "film.types.SpecialFeatures[]",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "fulltext": {
            "type": {
              "fullyQualifiedName": "film.types.Fulltext",
              "parameters": [],
              "name": "Fulltext",
              "parameterizedName": "film.types.Fulltext",
              "namespace": "film.types",
              "longDisplayName": "film.types.Fulltext",
              "shortDisplayName": "Fulltext"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "film.types.Fulltext",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          }
        },
        "modifiers": [],
        "metadata": [
          {
            "name": {
              "fullyQualifiedName": "io.vyne.jdbc.Table",
              "parameters": [],
              "name": "Table",
              "parameterizedName": "io.vyne.jdbc.Table",
              "namespace": "io.vyne.jdbc",
              "longDisplayName": "io.vyne.jdbc.Table",
              "shortDisplayName": "Table"
            },
            "params": {
              "table": "film",
              "schema": "public",
              "connection": "films"
            }
          },
          {
            "name": {
              "fullyQualifiedName": "Ann",
              "parameters": [],
              "name": "Ann",
              "parameterizedName": "Ann",
              "namespace": "",
              "longDisplayName": "Ann",
              "shortDisplayName": "Ann"
            },
            "params": {}
          },
          {
            "name": {
              "fullyQualifiedName": "io.vyne.catalog.DataOwner",
              "parameters": [],
              "name": "DataOwner",
              "parameterizedName": "io.vyne.catalog.DataOwner",
              "namespace": "io.vyne.catalog",
              "longDisplayName": "io.vyne.catalog.DataOwner",
              "shortDisplayName": "DataOwner"
            },
            "params": {
              "id": "michael.stone",
              "name": "Michael Stone"
            }
          },
          {
            "name": {
              "fullyQualifiedName": "io.vyne.jdbc.Table",
              "parameters": [],
              "name": "Table",
              "parameterizedName": "io.vyne.jdbc.Table",
              "namespace": "io.vyne.jdbc",
              "longDisplayName": "io.vyne.jdbc.Table",
              "shortDisplayName": "Table"
            },
            "params": {
              "table": "film",
              "schema": "public",
              "connection": "films"
            }
          }
        ],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.annotations.taxi",
            "version": "0.0.0",
            "content": "namespace film {\n   @io.vyne.jdbc.Table(table = \"film\", schema = \"public\", connection = \"films\")\n   @Ann\n   type extension Film {}\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.annotations.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.annotations.taxi:0.0.0",
            "contentHash": "209d3b"
          },
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.dataOwner.taxi",
            "version": "0.0.0",
            "content": "namespace film {\n   @io.vyne.catalog.DataOwner( id = \"michael.stone\" , name = \"Michael Stone\" )\n   type extension Film {}\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.dataOwner.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.dataOwner.taxi:0.0.0",
            "contentHash": "2ca56e"
          },
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.taxi",
            "version": "0.0.0",
            "content": "import films.FilmId\nimport film.types.Title\nimport film.types.Description\nimport film.types.ReleaseYear\nimport language.types.LanguageId\nimport language.types.LanguageId\nimport film.types.RentalDuration\nimport film.types.RentalRate\nimport film.types.Length\nimport film.types.ReplacementCost\nimport film.types.Rating\nimport film.types.LastUpdate\nimport film.types.Fulltext\nimport io.vyne.jdbc.Table\nimport film.types.SpecialFeatures\nnamespace film {\n   @io.vyne.jdbc.Table(table = \"film\" , schema = \"public\" , connection = \"films\")\n         model Film {\n            @Id film_id : films.FilmId\n            title : film.types.Title\n            description : film.types.Description?\n            release_year : film.types.ReleaseYear?\n            language_id : language.types.LanguageId\n            original_language_id : language.types.LanguageId?\n            rental_duration : film.types.RentalDuration\n            rental_rate : film.types.RentalRate\n            length : film.types.Length?\n            replacement_cost : film.types.ReplacementCost\n            rating : film.types.Rating?\n            last_update : film.types.LastUpdate\n            special_features : film.types.SpecialFeatures[]?\n            fulltext : film.types.Fulltext\n         }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.taxi:0.0.0",
            "contentHash": "fc40ac"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.Film",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.Film",
        "fullyQualifiedName": "film.Film",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.Film",
          "parameters": [],
          "name": "Film",
          "parameterizedName": "film.Film",
          "namespace": "film",
          "longDisplayName": "film.Film",
          "shortDisplayName": "Film"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": false
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Description",
          "parameters": [],
          "name": "Description",
          "parameterizedName": "film.types.Description",
          "namespace": "film.types",
          "longDisplayName": "film.types.Description",
          "shortDisplayName": "Description"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Description.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Description inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Description.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Description.taxi:0.0.0",
            "contentHash": "ec3849"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.Description",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.Description",
        "fullyQualifiedName": "film.types.Description",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.Description",
          "parameters": [],
          "name": "Description",
          "parameterizedName": "film.types.Description",
          "namespace": "film.types",
          "longDisplayName": "film.types.Description",
          "shortDisplayName": "Description"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.FilmId",
          "parameters": [],
          "name": "FilmId",
          "parameterizedName": "film.types.FilmId",
          "namespace": "film.types",
          "longDisplayName": "film.types.FilmId",
          "shortDisplayName": "FilmId"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/FilmId.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type FilmId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/FilmId.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/FilmId.taxi:0.0.0",
            "contentHash": "a0b9f0"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.FilmId",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.FilmId",
        "fullyQualifiedName": "film.types.FilmId",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.FilmId",
          "parameters": [],
          "name": "FilmId",
          "parameterizedName": "film.types.FilmId",
          "namespace": "film.types",
          "longDisplayName": "film.types.FilmId",
          "shortDisplayName": "FilmId"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Fulltext",
          "parameters": [],
          "name": "Fulltext",
          "parameterizedName": "film.types.Fulltext",
          "namespace": "film.types",
          "longDisplayName": "film.types.Fulltext",
          "shortDisplayName": "Fulltext"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Any",
            "parameters": [],
            "name": "Any",
            "parameterizedName": "lang.taxi.Any",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Any",
            "shortDisplayName": "Any"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Fulltext.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Fulltext inherits Any\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Fulltext.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Fulltext.taxi:0.0.0",
            "contentHash": "15bfd0"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.Fulltext",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Any",
          "shortDisplayName": "Any"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.Fulltext",
        "fullyQualifiedName": "film.types.Fulltext",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.Fulltext",
          "parameters": [],
          "name": "Fulltext",
          "parameterizedName": "film.types.Fulltext",
          "namespace": "film.types",
          "longDisplayName": "film.types.Fulltext",
          "shortDisplayName": "Fulltext"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.LastUpdate",
          "parameters": [],
          "name": "LastUpdate",
          "parameterizedName": "film.types.LastUpdate",
          "namespace": "film.types",
          "longDisplayName": "film.types.LastUpdate",
          "shortDisplayName": "LastUpdate"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Instant",
            "parameters": [],
            "name": "Instant",
            "parameterizedName": "lang.taxi.Instant",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Instant",
            "shortDisplayName": "Instant"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/LastUpdate.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type LastUpdate inherits Instant\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/LastUpdate.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/LastUpdate.taxi:0.0.0",
            "contentHash": "a4f44f"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.LastUpdate",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [
            "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
          ],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": true,
          "isEmpty": false
        },
        "offset": null,
        "format": [
          "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
        ],
        "hasFormat": true,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Instant",
          "parameters": [],
          "name": "Instant",
          "parameterizedName": "lang.taxi.Instant",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Instant",
          "shortDisplayName": "Instant"
        },
        "hasExpression": false,
        "unformattedTypeName": {
          "fullyQualifiedName": "film.types.LastUpdate",
          "parameters": [],
          "name": "LastUpdate",
          "parameterizedName": "film.types.LastUpdate",
          "namespace": "film.types",
          "longDisplayName": "film.types.LastUpdate",
          "shortDisplayName": "LastUpdate"
        },
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.LastUpdate",
        "fullyQualifiedName": "film.types.LastUpdate",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.LastUpdate",
          "parameters": [],
          "name": "LastUpdate",
          "parameterizedName": "film.types.LastUpdate",
          "namespace": "film.types",
          "longDisplayName": "film.types.LastUpdate",
          "shortDisplayName": "LastUpdate"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Length",
          "parameters": [],
          "name": "Length",
          "parameterizedName": "film.types.Length",
          "namespace": "film.types",
          "longDisplayName": "film.types.Length",
          "shortDisplayName": "Length"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Length.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Length inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Length.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Length.taxi:0.0.0",
            "contentHash": "651a25"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.Length",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.Length",
        "fullyQualifiedName": "film.types.Length",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.Length",
          "parameters": [],
          "name": "Length",
          "parameterizedName": "film.types.Length",
          "namespace": "film.types",
          "longDisplayName": "film.types.Length",
          "shortDisplayName": "Length"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Rating",
          "parameters": [],
          "name": "Rating",
          "parameterizedName": "film.types.Rating",
          "namespace": "film.types",
          "longDisplayName": "film.types.Rating",
          "shortDisplayName": "Rating"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Rating.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Rating inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Rating.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Rating.taxi:0.0.0",
            "contentHash": "5f228f"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.Rating",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.Rating",
        "fullyQualifiedName": "film.types.Rating",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.Rating",
          "parameters": [],
          "name": "Rating",
          "parameterizedName": "film.types.Rating",
          "namespace": "film.types",
          "longDisplayName": "film.types.Rating",
          "shortDisplayName": "Rating"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.ReleaseYear",
          "parameters": [],
          "name": "ReleaseYear",
          "parameterizedName": "film.types.ReleaseYear",
          "namespace": "film.types",
          "longDisplayName": "film.types.ReleaseYear",
          "shortDisplayName": "ReleaseYear"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Any",
            "parameters": [],
            "name": "Any",
            "parameterizedName": "lang.taxi.Any",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Any",
            "shortDisplayName": "Any"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReleaseYear.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type ReleaseYear inherits Any\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReleaseYear.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReleaseYear.taxi:0.0.0",
            "contentHash": "49725b"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.ReleaseYear",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Any",
          "shortDisplayName": "Any"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.ReleaseYear",
        "fullyQualifiedName": "film.types.ReleaseYear",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.ReleaseYear",
          "parameters": [],
          "name": "ReleaseYear",
          "parameterizedName": "film.types.ReleaseYear",
          "namespace": "film.types",
          "longDisplayName": "film.types.ReleaseYear",
          "shortDisplayName": "ReleaseYear"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.RentalDuration",
          "parameters": [],
          "name": "RentalDuration",
          "parameterizedName": "film.types.RentalDuration",
          "namespace": "film.types",
          "longDisplayName": "film.types.RentalDuration",
          "shortDisplayName": "RentalDuration"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalDuration.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type RentalDuration inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalDuration.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalDuration.taxi:0.0.0",
            "contentHash": "584e8a"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.RentalDuration",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.RentalDuration",
        "fullyQualifiedName": "film.types.RentalDuration",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.RentalDuration",
          "parameters": [],
          "name": "RentalDuration",
          "parameterizedName": "film.types.RentalDuration",
          "namespace": "film.types",
          "longDisplayName": "film.types.RentalDuration",
          "shortDisplayName": "RentalDuration"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.RentalRate",
          "parameters": [],
          "name": "RentalRate",
          "parameterizedName": "film.types.RentalRate",
          "namespace": "film.types",
          "longDisplayName": "film.types.RentalRate",
          "shortDisplayName": "RentalRate"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalRate.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type RentalRate inherits Decimal\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalRate.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalRate.taxi:0.0.0",
            "contentHash": "e1cc8c"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.RentalRate",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Decimal",
          "shortDisplayName": "Decimal"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.RentalRate",
        "fullyQualifiedName": "film.types.RentalRate",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.RentalRate",
          "parameters": [],
          "name": "RentalRate",
          "parameterizedName": "film.types.RentalRate",
          "namespace": "film.types",
          "longDisplayName": "film.types.RentalRate",
          "shortDisplayName": "RentalRate"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.ReplacementCost",
          "parameters": [],
          "name": "ReplacementCost",
          "parameterizedName": "film.types.ReplacementCost",
          "namespace": "film.types",
          "longDisplayName": "film.types.ReplacementCost",
          "shortDisplayName": "ReplacementCost"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReplacementCost.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type ReplacementCost inherits Decimal\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReplacementCost.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReplacementCost.taxi:0.0.0",
            "contentHash": "f71714"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.ReplacementCost",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Decimal",
          "shortDisplayName": "Decimal"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.ReplacementCost",
        "fullyQualifiedName": "film.types.ReplacementCost",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.ReplacementCost",
          "parameters": [],
          "name": "ReplacementCost",
          "parameterizedName": "film.types.ReplacementCost",
          "namespace": "film.types",
          "longDisplayName": "film.types.ReplacementCost",
          "shortDisplayName": "ReplacementCost"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.SpecialFeatures",
          "parameters": [],
          "name": "SpecialFeatures",
          "parameterizedName": "film.types.SpecialFeatures",
          "namespace": "film.types",
          "longDisplayName": "film.types.SpecialFeatures",
          "shortDisplayName": "SpecialFeatures"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/SpecialFeatures.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type SpecialFeatures inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/SpecialFeatures.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/SpecialFeatures.taxi:0.0.0",
            "contentHash": "2c1071"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.SpecialFeatures",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.SpecialFeatures",
        "fullyQualifiedName": "film.types.SpecialFeatures",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.SpecialFeatures",
          "parameters": [],
          "name": "SpecialFeatures",
          "parameterizedName": "film.types.SpecialFeatures",
          "namespace": "film.types",
          "longDisplayName": "film.types.SpecialFeatures",
          "shortDisplayName": "SpecialFeatures"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Title",
          "parameters": [],
          "name": "Title",
          "parameterizedName": "film.types.Title",
          "namespace": "film.types",
          "longDisplayName": "film.types.Title",
          "shortDisplayName": "Title"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Title.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Title inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Title.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Title.taxi:0.0.0",
            "contentHash": "af88ac"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "film.types.Title",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "film.types.Title",
        "fullyQualifiedName": "film.types.Title",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.types.Title",
          "parameters": [],
          "name": "Title",
          "parameterizedName": "film.types.Title",
          "namespace": "film.types",
          "longDisplayName": "film.types.Title",
          "shortDisplayName": "Title"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "films.FilmId",
          "parameters": [],
          "name": "FilmId",
          "parameterizedName": "films.FilmId",
          "namespace": "films",
          "longDisplayName": "films.FilmId",
          "shortDisplayName": "FilmId"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "version": "0.0.0",
            "content": "namespace films {\n   type FilmId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi:0.0.0",
            "contentHash": "b5803c"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "films.FilmId",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "films.FilmId",
        "fullyQualifiedName": "films.FilmId",
        "memberQualifiedName": {
          "fullyQualifiedName": "films.FilmId",
          "parameters": [],
          "name": "FilmId",
          "parameterizedName": "films.FilmId",
          "namespace": "films",
          "longDisplayName": "films.FilmId",
          "shortDisplayName": "FilmId"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "films.StreamingProviderName",
          "parameters": [],
          "name": "StreamingProviderName",
          "parameterizedName": "films.StreamingProviderName",
          "namespace": "films",
          "longDisplayName": "films.StreamingProviderName",
          "shortDisplayName": "StreamingProviderName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "version": "0.0.0",
            "content": "namespace films {\n   type StreamingProviderName inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi:0.0.0",
            "contentHash": "3454df"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "films.StreamingProviderName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "films.StreamingProviderName",
        "fullyQualifiedName": "films.StreamingProviderName",
        "memberQualifiedName": {
          "fullyQualifiedName": "films.StreamingProviderName",
          "parameters": [],
          "name": "StreamingProviderName",
          "parameterizedName": "films.StreamingProviderName",
          "namespace": "films",
          "longDisplayName": "films.StreamingProviderName",
          "shortDisplayName": "StreamingProviderName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "films.StreamingProviderPrice",
          "parameters": [],
          "name": "StreamingProviderPrice",
          "parameterizedName": "films.StreamingProviderPrice",
          "namespace": "films",
          "longDisplayName": "films.StreamingProviderPrice",
          "shortDisplayName": "StreamingProviderPrice"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "version": "0.0.0",
            "content": "namespace films {\n   type StreamingProviderPrice inherits Decimal\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi:0.0.0",
            "contentHash": "ec7078"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "films.StreamingProviderPrice",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Decimal",
          "shortDisplayName": "Decimal"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "films.StreamingProviderPrice",
        "fullyQualifiedName": "films.StreamingProviderPrice",
        "memberQualifiedName": {
          "fullyQualifiedName": "films.StreamingProviderPrice",
          "parameters": [],
          "name": "StreamingProviderPrice",
          "parameterizedName": "films.StreamingProviderPrice",
          "namespace": "films",
          "longDisplayName": "films.StreamingProviderPrice",
          "shortDisplayName": "StreamingProviderPrice"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "films.reviews.FilmReviewScore",
          "parameters": [],
          "name": "FilmReviewScore",
          "parameterizedName": "films.reviews.FilmReviewScore",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.FilmReviewScore",
          "shortDisplayName": "FilmReviewScore"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "version": "0.0.0",
            "content": "namespace films.reviews {\n   type FilmReviewScore inherits Decimal\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi:0.0.0",
            "contentHash": "883e32"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "films.reviews.FilmReviewScore",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Decimal",
          "shortDisplayName": "Decimal"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "films.reviews.FilmReviewScore",
        "fullyQualifiedName": "films.reviews.FilmReviewScore",
        "memberQualifiedName": {
          "fullyQualifiedName": "films.reviews.FilmReviewScore",
          "parameters": [],
          "name": "FilmReviewScore",
          "parameterizedName": "films.reviews.FilmReviewScore",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.FilmReviewScore",
          "shortDisplayName": "FilmReviewScore"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "films.reviews.ReviewText",
          "parameters": [],
          "name": "ReviewText",
          "parameterizedName": "films.reviews.ReviewText",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.ReviewText",
          "shortDisplayName": "ReviewText"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "version": "0.0.0",
            "content": "namespace films.reviews {\n   type ReviewText inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi:0.0.0",
            "contentHash": "1927dc"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "films.reviews.ReviewText",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "films.reviews.ReviewText",
        "fullyQualifiedName": "films.reviews.ReviewText",
        "memberQualifiedName": {
          "fullyQualifiedName": "films.reviews.ReviewText",
          "parameters": [],
          "name": "ReviewText",
          "parameterizedName": "films.reviews.ReviewText",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.ReviewText",
          "shortDisplayName": "ReviewText"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
          "parameters": [],
          "name": "SquashedTomatoesFilmId",
          "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
          "shortDisplayName": "SquashedTomatoesFilmId"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "version": "0.0.0",
            "content": "namespace films.reviews {\n   type SquashedTomatoesFilmId inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi:0.0.0",
            "contentHash": "f01c20"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "films.reviews.SquashedTomatoesFilmId",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
        "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
        "memberQualifiedName": {
          "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
          "parameters": [],
          "name": "SquashedTomatoesFilmId",
          "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
          "shortDisplayName": "SquashedTomatoesFilmId"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.Error",
          "parameters": [],
          "name": "Error",
          "parameterizedName": "io.vyne.Error",
          "namespace": "io.vyne",
          "longDisplayName": "io.vyne.Error",
          "shortDisplayName": "Error"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "VyneQueryError",
            "version": "0.0.0",
            "content": "namespace io.vyne {\n   type Error inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/VyneQueryError",
            "id": "VyneQueryError:0.0.0",
            "contentHash": "f2be1f"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.Error",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.Error",
        "fullyQualifiedName": "io.vyne.Error",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.Error",
          "parameters": [],
          "name": "Error",
          "parameterizedName": "io.vyne.Error",
          "namespace": "io.vyne",
          "longDisplayName": "io.vyne.Error",
          "shortDisplayName": "Error"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.Username",
          "parameters": [],
          "name": "Username",
          "parameterizedName": "io.vyne.Username",
          "namespace": "io.vyne",
          "longDisplayName": "io.vyne.Username",
          "shortDisplayName": "Username"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "UserTypes",
            "version": "0.0.0",
            "content": "namespace io.vyne {\n   type Username inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/UserTypes",
            "id": "UserTypes:0.0.0",
            "contentHash": "00a414"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.Username",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.Username",
        "fullyQualifiedName": "io.vyne.Username",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.Username",
          "parameters": [],
          "name": "Username",
          "parameterizedName": "io.vyne.Username",
          "namespace": "io.vyne",
          "longDisplayName": "io.vyne.Username",
          "shortDisplayName": "Username"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.lambda.AwsLambdaService",
          "parameters": [],
          "name": "AwsLambdaService",
          "parameterizedName": "io.vyne.aws.lambda.AwsLambdaService",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.AwsLambdaService",
          "shortDisplayName": "AwsLambdaService"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsLambdaConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.lambda {\n   annotation AwsLambdaService {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
            "id": "AwsLambdaConnectors:0.0.0",
            "contentHash": "4f9d53"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.aws.lambda.AwsLambdaService",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.lambda.AwsLambdaService",
        "fullyQualifiedName": "io.vyne.aws.lambda.AwsLambdaService",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.lambda.AwsLambdaService",
          "parameters": [],
          "name": "AwsLambdaService",
          "parameterizedName": "io.vyne.aws.lambda.AwsLambdaService",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.AwsLambdaService",
          "shortDisplayName": "AwsLambdaService"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.lambda.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.aws.lambda.ConnectionName",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsLambdaConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.lambda {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
            "id": "AwsLambdaConnectors:0.0.0",
            "contentHash": "ba8822"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.aws.lambda.ConnectionName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.lambda.ConnectionName",
        "fullyQualifiedName": "io.vyne.aws.lambda.ConnectionName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.lambda.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.aws.lambda.ConnectionName",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.lambda.LambdaOperation",
          "parameters": [],
          "name": "LambdaOperation",
          "parameterizedName": "io.vyne.aws.lambda.LambdaOperation",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.LambdaOperation",
          "shortDisplayName": "LambdaOperation"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsLambdaConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.lambda {\n   annotation LambdaOperation {\n         name : OperationName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
            "id": "AwsLambdaConnectors:0.0.0",
            "contentHash": "12a268"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.aws.lambda.LambdaOperation",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.lambda.LambdaOperation",
        "fullyQualifiedName": "io.vyne.aws.lambda.LambdaOperation",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.lambda.LambdaOperation",
          "parameters": [],
          "name": "LambdaOperation",
          "parameterizedName": "io.vyne.aws.lambda.LambdaOperation",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.LambdaOperation",
          "shortDisplayName": "LambdaOperation"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.lambda.OperationName",
          "parameters": [],
          "name": "OperationName",
          "parameterizedName": "io.vyne.aws.lambda.OperationName",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.OperationName",
          "shortDisplayName": "OperationName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsLambdaConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.lambda {\n   OperationName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
            "id": "AwsLambdaConnectors:0.0.0",
            "contentHash": "f4ed37"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.aws.lambda.OperationName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.lambda.OperationName",
        "fullyQualifiedName": "io.vyne.aws.lambda.OperationName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.lambda.OperationName",
          "parameters": [],
          "name": "OperationName",
          "parameterizedName": "io.vyne.aws.lambda.OperationName",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.OperationName",
          "shortDisplayName": "OperationName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.BucketName",
          "parameters": [],
          "name": "BucketName",
          "parameterizedName": "io.vyne.aws.s3.BucketName",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.BucketName",
          "shortDisplayName": "BucketName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   BucketName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "7ac5e3"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.aws.s3.BucketName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.s3.BucketName",
        "fullyQualifiedName": "io.vyne.aws.s3.BucketName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.s3.BucketName",
          "parameters": [],
          "name": "BucketName",
          "parameterizedName": "io.vyne.aws.s3.BucketName",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.BucketName",
          "shortDisplayName": "BucketName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.aws.s3.ConnectionName",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "e2d418"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.aws.s3.ConnectionName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.s3.ConnectionName",
        "fullyQualifiedName": "io.vyne.aws.s3.ConnectionName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.s3.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.aws.s3.ConnectionName",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.S3EntryKey",
          "parameters": [],
          "name": "S3EntryKey",
          "parameterizedName": "io.vyne.aws.s3.S3EntryKey",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.S3EntryKey",
          "shortDisplayName": "S3EntryKey"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   type S3EntryKey inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "b1a22b"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.aws.s3.S3EntryKey",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.s3.S3EntryKey",
        "fullyQualifiedName": "io.vyne.aws.s3.S3EntryKey",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.s3.S3EntryKey",
          "parameters": [],
          "name": "S3EntryKey",
          "parameterizedName": "io.vyne.aws.s3.S3EntryKey",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.S3EntryKey",
          "shortDisplayName": "S3EntryKey"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.S3Operation",
          "parameters": [],
          "name": "S3Operation",
          "parameterizedName": "io.vyne.aws.s3.S3Operation",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.S3Operation",
          "shortDisplayName": "S3Operation"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   annotation S3Operation {\n         bucket : BucketName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "2d48b3"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.aws.s3.S3Operation",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.s3.S3Operation",
        "fullyQualifiedName": "io.vyne.aws.s3.S3Operation",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.s3.S3Operation",
          "parameters": [],
          "name": "S3Operation",
          "parameterizedName": "io.vyne.aws.s3.S3Operation",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.S3Operation",
          "shortDisplayName": "S3Operation"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.S3Service",
          "parameters": [],
          "name": "S3Service",
          "parameterizedName": "io.vyne.aws.s3.S3Service",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.S3Service",
          "shortDisplayName": "S3Service"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   annotation S3Service {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "c94220"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.aws.s3.S3Service",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.s3.S3Service",
        "fullyQualifiedName": "io.vyne.aws.s3.S3Service",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.s3.S3Service",
          "parameters": [],
          "name": "S3Service",
          "parameterizedName": "io.vyne.aws.s3.S3Service",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.S3Service",
          "shortDisplayName": "S3Service"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.sqs.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.aws.sqs.ConnectionName",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsSqsConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.sqs {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
            "id": "AwsSqsConnectors:0.0.0",
            "contentHash": "2997f6"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.aws.sqs.ConnectionName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.sqs.ConnectionName",
        "fullyQualifiedName": "io.vyne.aws.sqs.ConnectionName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.sqs.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.aws.sqs.ConnectionName",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.sqs.QueueName",
          "parameters": [],
          "name": "QueueName",
          "parameterizedName": "io.vyne.aws.sqs.QueueName",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.QueueName",
          "shortDisplayName": "QueueName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsSqsConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.sqs {\n   QueueName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
            "id": "AwsSqsConnectors:0.0.0",
            "contentHash": "e52a2b"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.aws.sqs.QueueName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.sqs.QueueName",
        "fullyQualifiedName": "io.vyne.aws.sqs.QueueName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.sqs.QueueName",
          "parameters": [],
          "name": "QueueName",
          "parameterizedName": "io.vyne.aws.sqs.QueueName",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.QueueName",
          "shortDisplayName": "QueueName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.sqs.SqsOperation",
          "parameters": [],
          "name": "SqsOperation",
          "parameterizedName": "io.vyne.aws.sqs.SqsOperation",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.SqsOperation",
          "shortDisplayName": "SqsOperation"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsSqsConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.sqs {\n   annotation SqsOperation {\n         queue : QueueName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
            "id": "AwsSqsConnectors:0.0.0",
            "contentHash": "e9ab78"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.aws.sqs.SqsOperation",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.sqs.SqsOperation",
        "fullyQualifiedName": "io.vyne.aws.sqs.SqsOperation",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.sqs.SqsOperation",
          "parameters": [],
          "name": "SqsOperation",
          "parameterizedName": "io.vyne.aws.sqs.SqsOperation",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.SqsOperation",
          "shortDisplayName": "SqsOperation"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.sqs.SqsService",
          "parameters": [],
          "name": "SqsService",
          "parameterizedName": "io.vyne.aws.sqs.SqsService",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.SqsService",
          "shortDisplayName": "SqsService"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "AwsSqsConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.sqs {\n   annotation SqsService {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
            "id": "AwsSqsConnectors:0.0.0",
            "contentHash": "bb0399"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.aws.sqs.SqsService",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.aws.sqs.SqsService",
        "fullyQualifiedName": "io.vyne.aws.sqs.SqsService",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.aws.sqs.SqsService",
          "parameters": [],
          "name": "SqsService",
          "parameterizedName": "io.vyne.aws.sqs.SqsService",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.SqsService",
          "shortDisplayName": "SqsService"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreBlob",
          "parameters": [],
          "name": "AzureStoreBlob",
          "parameterizedName": "io.vyne.azure.store.AzureStoreBlob",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.AzureStoreBlob",
          "shortDisplayName": "AzureStoreBlob"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   type AzureStoreBlob inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "30cf88"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.azure.store.AzureStoreBlob",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.azure.store.AzureStoreBlob",
        "fullyQualifiedName": "io.vyne.azure.store.AzureStoreBlob",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreBlob",
          "parameters": [],
          "name": "AzureStoreBlob",
          "parameterizedName": "io.vyne.azure.store.AzureStoreBlob",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.AzureStoreBlob",
          "shortDisplayName": "AzureStoreBlob"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreContainer",
          "parameters": [],
          "name": "AzureStoreContainer",
          "parameterizedName": "io.vyne.azure.store.AzureStoreContainer",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.AzureStoreContainer",
          "shortDisplayName": "AzureStoreContainer"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   AzureStoreContainer inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "1bb1cc"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.azure.store.AzureStoreContainer",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.azure.store.AzureStoreContainer",
        "fullyQualifiedName": "io.vyne.azure.store.AzureStoreContainer",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreContainer",
          "parameters": [],
          "name": "AzureStoreContainer",
          "parameterizedName": "io.vyne.azure.store.AzureStoreContainer",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.AzureStoreContainer",
          "shortDisplayName": "AzureStoreContainer"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreOperation",
          "parameters": [],
          "name": "AzureStoreOperation",
          "parameterizedName": "io.vyne.azure.store.AzureStoreOperation",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.AzureStoreOperation",
          "shortDisplayName": "AzureStoreOperation"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   annotation AzureStoreOperation {\n         container : AzureStoreContainer inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "1305ac"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.azure.store.AzureStoreOperation",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.azure.store.AzureStoreOperation",
        "fullyQualifiedName": "io.vyne.azure.store.AzureStoreOperation",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreOperation",
          "parameters": [],
          "name": "AzureStoreOperation",
          "parameterizedName": "io.vyne.azure.store.AzureStoreOperation",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.AzureStoreOperation",
          "shortDisplayName": "AzureStoreOperation"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.BlobService",
          "parameters": [],
          "name": "BlobService",
          "parameterizedName": "io.vyne.azure.store.BlobService",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.BlobService",
          "shortDisplayName": "BlobService"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   annotation BlobService {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "7804e6"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.azure.store.BlobService",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.azure.store.BlobService",
        "fullyQualifiedName": "io.vyne.azure.store.BlobService",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.azure.store.BlobService",
          "parameters": [],
          "name": "BlobService",
          "parameterizedName": "io.vyne.azure.store.BlobService",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.BlobService",
          "shortDisplayName": "BlobService"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.azure.store.ConnectionName",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "93d191"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.azure.store.ConnectionName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.azure.store.ConnectionName",
        "fullyQualifiedName": "io.vyne.azure.store.ConnectionName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.azure.store.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.azure.store.ConnectionName",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.catalog.DataOwner",
          "parameters": [],
          "name": "DataOwner",
          "parameterizedName": "io.vyne.catalog.DataOwner",
          "namespace": "io.vyne.catalog",
          "longDisplayName": "io.vyne.catalog.DataOwner",
          "shortDisplayName": "DataOwner"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "Catalog",
            "version": "0.0.0",
            "content": "namespace io.vyne.catalog {\n   annotation DataOwner {\n         id : io.vyne.Username\n         name : String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/Catalog",
            "id": "Catalog:0.0.0",
            "contentHash": "2368b8"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.catalog.DataOwner",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.catalog.DataOwner",
        "fullyQualifiedName": "io.vyne.catalog.DataOwner",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.catalog.DataOwner",
          "parameters": [],
          "name": "DataOwner",
          "parameterizedName": "io.vyne.catalog.DataOwner",
          "namespace": "io.vyne.catalog",
          "longDisplayName": "io.vyne.catalog.DataOwner",
          "shortDisplayName": "DataOwner"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
          "parameters": [],
          "name": "StreamingProvider",
          "parameterizedName": "io.vyne.demos.films.StreamingProvider",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingProvider",
          "shortDisplayName": "StreamingProvider"
        },
        "attributes": {
          "name": {
            "type": {
              "fullyQualifiedName": "films.StreamingProviderName",
              "parameters": [],
              "name": "StreamingProviderName",
              "parameterizedName": "films.StreamingProviderName",
              "namespace": "films",
              "longDisplayName": "films.StreamingProviderName",
              "shortDisplayName": "StreamingProviderName"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.StreamingProviderName",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "pricePerMonth": {
            "type": {
              "fullyQualifiedName": "films.StreamingProviderPrice",
              "parameters": [],
              "name": "StreamingProviderPrice",
              "parameterizedName": "films.StreamingProviderPrice",
              "namespace": "films",
              "longDisplayName": "films.StreamingProviderPrice",
              "shortDisplayName": "StreamingProviderPrice"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.StreamingProviderPrice",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          }
        },
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "films-service",
            "version": "0.0.0",
            "content": "import films.StreamingProviderName\nimport films.StreamingProviderPrice\nnamespace io.vyne.demos.films {\n   model StreamingProvider {\n         name : films.StreamingProviderName\n         pricePerMonth : films.StreamingProviderPrice\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.petflix.demos",
              "name": "films-api",
              "version": "0.0.0",
              "unversionedId": "io.petflix.demos/films-api",
              "id": "io.petflix.demos/films-api/0.0.0",
              "uriSafeId": "io.petflix.demos:films-api:0.0.0"
            },
            "packageQualifiedName": "[io.petflix.demos/films-api/0.0.0]/films-service",
            "id": "films-service:0.0.0",
            "contentHash": "fa5db1"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.demos.films.StreamingProvider",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.demos.films.StreamingProvider",
        "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
          "parameters": [],
          "name": "StreamingProvider",
          "parameterizedName": "io.vyne.demos.films.StreamingProvider",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingProvider",
          "shortDisplayName": "StreamingProvider"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": false
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "parameters": [],
          "name": "IdResolution",
          "parameterizedName": "io.vyne.films.idlookup.IdResolution",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "shortDisplayName": "IdResolution"
        },
        "attributes": {
          "filmId": {
            "type": {
              "fullyQualifiedName": "films.FilmId",
              "parameters": [],
              "name": "FilmId",
              "parameterizedName": "films.FilmId",
              "namespace": "films",
              "longDisplayName": "films.FilmId",
              "shortDisplayName": "FilmId"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.FilmId",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "netflixId": {
            "type": {
              "fullyQualifiedName": "demo.netflix.NetflixFilmId",
              "parameters": [],
              "name": "NetflixFilmId",
              "parameterizedName": "demo.netflix.NetflixFilmId",
              "namespace": "demo.netflix",
              "longDisplayName": "demo.netflix.NetflixFilmId",
              "shortDisplayName": "NetflixFilmId"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "demo.netflix.NetflixFilmId",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "squashedTomatoesFilmId": {
            "type": {
              "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
              "parameters": [],
              "name": "SquashedTomatoesFilmId",
              "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
              "namespace": "films.reviews",
              "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
              "shortDisplayName": "SquashedTomatoesFilmId"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.reviews.SquashedTomatoesFilmId",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          }
        },
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "id-resolution-service",
            "version": "0.0.0",
            "content": "import films.FilmId\nimport demo.netflix.NetflixFilmId\nimport films.reviews.SquashedTomatoesFilmId\nnamespace io.vyne.films.idlookup {\n   model IdResolution {\n         filmId : films.FilmId\n         netflixId : demo.netflix.NetflixFilmId\n         squashedTomatoesFilmId : films.reviews.SquashedTomatoesFilmId\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.petflex.demos",
              "name": "id-lookup-service",
              "version": "0.0.0",
              "unversionedId": "io.petflex.demos/id-lookup-service",
              "id": "io.petflex.demos/id-lookup-service/0.0.0",
              "uriSafeId": "io.petflex.demos:id-lookup-service:0.0.0"
            },
            "packageQualifiedName": "[io.petflex.demos/id-lookup-service/0.0.0]/id-resolution-service",
            "id": "id-resolution-service:0.0.0",
            "contentHash": "683376"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.films.idlookup.IdResolution",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.films.idlookup.IdResolution",
        "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "parameters": [],
          "name": "IdResolution",
          "parameterizedName": "io.vyne.films.idlookup.IdResolution",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "shortDisplayName": "IdResolution"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": false
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.formats.Csv",
          "parameters": [],
          "name": "Csv",
          "parameterizedName": "io.vyne.formats.Csv",
          "namespace": "io.vyne.formats",
          "longDisplayName": "io.vyne.formats.Csv",
          "shortDisplayName": "Csv"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "CsvFormat",
            "version": "0.0.0",
            "content": "namespace io.vyne.formats {\n   annotation Csv {\n         delimiter : String?\n         firstRecordAsHeader : Boolean?\n         nullValue : String?\n         containsTrailingDelimiters : Boolean?\n         ignoreContentBefore : String?\n         useFieldNamesAsColumnNames: Boolean?\n         withQuote: String?\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/CsvFormat",
            "id": "CsvFormat:0.0.0",
            "contentHash": "25616d"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.formats.Csv",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.formats.Csv",
        "fullyQualifiedName": "io.vyne.formats.Csv",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.formats.Csv",
          "parameters": [],
          "name": "Csv",
          "parameterizedName": "io.vyne.formats.Csv",
          "namespace": "io.vyne.formats",
          "longDisplayName": "io.vyne.formats.Csv",
          "shortDisplayName": "Csv"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.jdbc.ConnectionName",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   type ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "b0f9b0"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.jdbc.ConnectionName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.jdbc.ConnectionName",
        "fullyQualifiedName": "io.vyne.jdbc.ConnectionName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.jdbc.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.jdbc.ConnectionName",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
          "parameters": [],
          "name": "DatabaseService",
          "parameterizedName": "io.vyne.jdbc.DatabaseService",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.DatabaseService",
          "shortDisplayName": "DatabaseService"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   annotation DatabaseService {\n         connection : ConnectionName\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "56bb57"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.jdbc.DatabaseService",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.jdbc.DatabaseService",
        "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
          "parameters": [],
          "name": "DatabaseService",
          "parameterizedName": "io.vyne.jdbc.DatabaseService",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.DatabaseService",
          "shortDisplayName": "DatabaseService"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.SchemaName",
          "parameters": [],
          "name": "SchemaName",
          "parameterizedName": "io.vyne.jdbc.SchemaName",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.SchemaName",
          "shortDisplayName": "SchemaName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   SchemaName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "49e2fe"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.jdbc.SchemaName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.jdbc.SchemaName",
        "fullyQualifiedName": "io.vyne.jdbc.SchemaName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.jdbc.SchemaName",
          "parameters": [],
          "name": "SchemaName",
          "parameterizedName": "io.vyne.jdbc.SchemaName",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.SchemaName",
          "shortDisplayName": "SchemaName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.Table",
          "parameters": [],
          "name": "Table",
          "parameterizedName": "io.vyne.jdbc.Table",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.Table",
          "shortDisplayName": "Table"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   annotation Table {\n         connection : ConnectionName\n         table : TableName inherits String\n         schema: SchemaName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "e8ee48"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.jdbc.Table",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.jdbc.Table",
        "fullyQualifiedName": "io.vyne.jdbc.Table",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.jdbc.Table",
          "parameters": [],
          "name": "Table",
          "parameterizedName": "io.vyne.jdbc.Table",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.Table",
          "shortDisplayName": "Table"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.TableName",
          "parameters": [],
          "name": "TableName",
          "parameterizedName": "io.vyne.jdbc.TableName",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.TableName",
          "shortDisplayName": "TableName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   TableName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "df61fc"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.jdbc.TableName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.jdbc.TableName",
        "fullyQualifiedName": "io.vyne.jdbc.TableName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.jdbc.TableName",
          "parameters": [],
          "name": "TableName",
          "parameterizedName": "io.vyne.jdbc.TableName",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.TableName",
          "shortDisplayName": "TableName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.kafka.ConnectionName",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "28e454"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.kafka.ConnectionName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.kafka.ConnectionName",
        "fullyQualifiedName": "io.vyne.kafka.ConnectionName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.kafka.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.kafka.ConnectionName",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.KafkaOperation",
          "parameters": [],
          "name": "KafkaOperation",
          "parameterizedName": "io.vyne.kafka.KafkaOperation",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.KafkaOperation",
          "shortDisplayName": "KafkaOperation"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   annotation KafkaOperation {\n         topic : TopicName inherits String\n         offset : TopicOffset\n      }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "890f9c"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.kafka.KafkaOperation",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.kafka.KafkaOperation",
        "fullyQualifiedName": "io.vyne.kafka.KafkaOperation",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.kafka.KafkaOperation",
          "parameters": [],
          "name": "KafkaOperation",
          "parameterizedName": "io.vyne.kafka.KafkaOperation",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.KafkaOperation",
          "shortDisplayName": "KafkaOperation"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.KafkaService",
          "parameters": [],
          "name": "KafkaService",
          "parameterizedName": "io.vyne.kafka.KafkaService",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.KafkaService",
          "shortDisplayName": "KafkaService"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   annotation KafkaService {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "6826ff"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "io.vyne.kafka.KafkaService",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.kafka.KafkaService",
        "fullyQualifiedName": "io.vyne.kafka.KafkaService",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.kafka.KafkaService",
          "parameters": [],
          "name": "KafkaService",
          "parameterizedName": "io.vyne.kafka.KafkaService",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.KafkaService",
          "shortDisplayName": "KafkaService"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.TopicName",
          "parameters": [],
          "name": "TopicName",
          "parameterizedName": "io.vyne.kafka.TopicName",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.TopicName",
          "shortDisplayName": "TopicName"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   TopicName inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "c6ba94"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.kafka.TopicName",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.kafka.TopicName",
        "fullyQualifiedName": "io.vyne.kafka.TopicName",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.kafka.TopicName",
          "parameters": [],
          "name": "TopicName",
          "parameterizedName": "io.vyne.kafka.TopicName",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.TopicName",
          "shortDisplayName": "TopicName"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.TopicOffset",
          "parameters": [],
          "name": "TopicOffset",
          "parameterizedName": "io.vyne.kafka.TopicOffset",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.TopicOffset",
          "shortDisplayName": "TopicOffset"
        },
        "attributes": {},
        "modifiers": [
          "ENUM"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [
          {
            "name": "earliest",
            "value": "earliest",
            "synonyms": [],
            "typeDoc": ""
          },
          {
            "name": "latest",
            "value": "latest",
            "synonyms": [],
            "typeDoc": ""
          },
          {
            "name": "none",
            "value": "none",
            "synonyms": [],
            "typeDoc": ""
          }
        ],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   enum TopicOffset {\n         earliest,\n         latest,\n         none\n      }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "d31705"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.kafka.TopicOffset",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.kafka.TopicOffset",
        "fullyQualifiedName": "io.vyne.kafka.TopicOffset",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.kafka.TopicOffset",
          "parameters": [],
          "name": "TopicOffset",
          "parameterizedName": "io.vyne.kafka.TopicOffset",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.TopicOffset",
          "shortDisplayName": "TopicOffset"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.reviews.FilmReview",
          "parameters": [],
          "name": "FilmReview",
          "parameterizedName": "io.vyne.reviews.FilmReview",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.FilmReview",
          "shortDisplayName": "FilmReview"
        },
        "attributes": {
          "filmId": {
            "type": {
              "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
              "parameters": [],
              "name": "SquashedTomatoesFilmId",
              "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
              "namespace": "films.reviews",
              "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
              "shortDisplayName": "SquashedTomatoesFilmId"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.reviews.SquashedTomatoesFilmId",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "filmReview": {
            "type": {
              "fullyQualifiedName": "films.reviews.ReviewText",
              "parameters": [],
              "name": "ReviewText",
              "parameterizedName": "films.reviews.ReviewText",
              "namespace": "films.reviews",
              "longDisplayName": "films.reviews.ReviewText",
              "shortDisplayName": "ReviewText"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.reviews.ReviewText",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "score": {
            "type": {
              "fullyQualifiedName": "films.reviews.FilmReviewScore",
              "parameters": [],
              "name": "FilmReviewScore",
              "parameterizedName": "films.reviews.FilmReviewScore",
              "namespace": "films.reviews",
              "longDisplayName": "films.reviews.FilmReviewScore",
              "shortDisplayName": "FilmReviewScore"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.reviews.FilmReviewScore",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          }
        },
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "squashed-tomatoes",
            "version": "0.0.0",
            "content": "import films.reviews.SquashedTomatoesFilmId\nimport films.reviews.ReviewText\nimport films.reviews.FilmReviewScore\nnamespace io.vyne.reviews {\n   model FilmReview {\n         filmId : films.reviews.SquashedTomatoesFilmId\n         filmReview : films.reviews.ReviewText\n         score : films.reviews.FilmReviewScore\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.petflix.demos",
              "name": "films-reviews",
              "version": "0.0.0",
              "unversionedId": "io.petflix.demos/films-reviews",
              "id": "io.petflix.demos/films-reviews/0.0.0",
              "uriSafeId": "io.petflix.demos:films-reviews:0.0.0"
            },
            "packageQualifiedName": "[io.petflix.demos/films-reviews/0.0.0]/squashed-tomatoes",
            "id": "squashed-tomatoes:0.0.0",
            "contentHash": "b7a47b"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "io.vyne.reviews.FilmReview",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "io.vyne.reviews.FilmReview",
        "fullyQualifiedName": "io.vyne.reviews.FilmReview",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.reviews.FilmReview",
          "parameters": [],
          "name": "FilmReview",
          "parameterizedName": "io.vyne.reviews.FilmReview",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.FilmReview",
          "shortDisplayName": "FilmReview"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": false
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Any",
          "shortDisplayName": "Any"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "Can be anything.  Try to avoid using 'Any' as it's not descriptive - favour using a strongly typed approach instead",
        "paramaterizedName": "lang.taxi.Any",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Any",
          "shortDisplayName": "Any"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.Any",
        "fullyQualifiedName": "lang.taxi.Any",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Any",
          "shortDisplayName": "Any"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Array",
          "parameters": [],
          "name": "Array",
          "parameterizedName": "lang.taxi.Array",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Array",
          "shortDisplayName": "Array"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "A collection of things",
        "paramaterizedName": "lang.taxi.Array",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "lang.taxi.Array",
        "fullyQualifiedName": "lang.taxi.Array",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Array",
          "parameters": [],
          "name": "Array",
          "parameterizedName": "lang.taxi.Array",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Array",
          "shortDisplayName": "Array"
        },
        "underlyingTypeParameters": [],
        "isCollection": true,
        "isStream": false,
        "collectionType": {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Any",
          "shortDisplayName": "Any"
        },
        "isScalar": false
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Boolean",
          "parameters": [],
          "name": "Boolean",
          "parameterizedName": "lang.taxi.Boolean",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Boolean",
          "shortDisplayName": "Boolean"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "Represents a value which is either `true` or `false`.",
        "paramaterizedName": "lang.taxi.Boolean",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Boolean",
          "parameters": [],
          "name": "Boolean",
          "parameterizedName": "lang.taxi.Boolean",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Boolean",
          "shortDisplayName": "Boolean"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.Boolean",
        "fullyQualifiedName": "lang.taxi.Boolean",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Boolean",
          "parameters": [],
          "name": "Boolean",
          "parameterizedName": "lang.taxi.Boolean",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Boolean",
          "shortDisplayName": "Boolean"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Date",
          "parameters": [],
          "name": "Date",
          "parameterizedName": "lang.taxi.Date",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Date",
          "shortDisplayName": "Date"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "A date, without a time or timezone.",
        "paramaterizedName": "lang.taxi.Date",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [
            "yyyy-MM-dd"
          ],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": true,
          "isEmpty": false
        },
        "offset": null,
        "format": [
          "yyyy-MM-dd"
        ],
        "hasFormat": true,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Date",
          "parameters": [],
          "name": "Date",
          "parameterizedName": "lang.taxi.Date",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Date",
          "shortDisplayName": "Date"
        },
        "hasExpression": false,
        "unformattedTypeName": {
          "fullyQualifiedName": "lang.taxi.Date",
          "parameters": [],
          "name": "Date",
          "parameterizedName": "lang.taxi.Date",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Date",
          "shortDisplayName": "Date"
        },
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.Date",
        "fullyQualifiedName": "lang.taxi.Date",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Date",
          "parameters": [],
          "name": "Date",
          "parameterizedName": "lang.taxi.Date",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Date",
          "shortDisplayName": "Date"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.DateTime",
          "parameters": [],
          "name": "DateTime",
          "parameterizedName": "lang.taxi.DateTime",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.DateTime",
          "shortDisplayName": "DateTime"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "A date and time, without a timezone.  Generally, favour using Instant which represents a point-in-time, as it has a timezone attached",
        "paramaterizedName": "lang.taxi.DateTime",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [
            "yyyy-MM-dd'T'HH:mm:ss.SSS"
          ],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": true,
          "isEmpty": false
        },
        "offset": null,
        "format": [
          "yyyy-MM-dd'T'HH:mm:ss.SSS"
        ],
        "hasFormat": true,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.DateTime",
          "parameters": [],
          "name": "DateTime",
          "parameterizedName": "lang.taxi.DateTime",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.DateTime",
          "shortDisplayName": "DateTime"
        },
        "hasExpression": false,
        "unformattedTypeName": {
          "fullyQualifiedName": "lang.taxi.DateTime",
          "parameters": [],
          "name": "DateTime",
          "parameterizedName": "lang.taxi.DateTime",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.DateTime",
          "shortDisplayName": "DateTime"
        },
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.DateTime",
        "fullyQualifiedName": "lang.taxi.DateTime",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.DateTime",
          "parameters": [],
          "name": "DateTime",
          "parameterizedName": "lang.taxi.DateTime",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.DateTime",
          "shortDisplayName": "DateTime"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Decimal",
          "shortDisplayName": "Decimal"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "A signed decimal number - ie., a whole number with decimal places.",
        "paramaterizedName": "lang.taxi.Decimal",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Decimal",
          "shortDisplayName": "Decimal"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.Decimal",
        "fullyQualifiedName": "lang.taxi.Decimal",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Decimal",
          "shortDisplayName": "Decimal"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Double",
          "parameters": [],
          "name": "Double",
          "parameterizedName": "lang.taxi.Double",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Double",
          "shortDisplayName": "Double"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "Represents a double-precision 64-bit IEEE 754 floating point number.",
        "paramaterizedName": "lang.taxi.Double",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Double",
          "parameters": [],
          "name": "Double",
          "parameterizedName": "lang.taxi.Double",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Double",
          "shortDisplayName": "Double"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.Double",
        "fullyQualifiedName": "lang.taxi.Double",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Double",
          "parameters": [],
          "name": "Double",
          "parameterizedName": "lang.taxi.Double",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Double",
          "shortDisplayName": "Double"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Instant",
          "parameters": [],
          "name": "Instant",
          "parameterizedName": "lang.taxi.Instant",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Instant",
          "shortDisplayName": "Instant"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "A point in time, with date, time and timezone.  Follows ISO standard convention of yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "paramaterizedName": "lang.taxi.Instant",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [
            "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
          ],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": true,
          "isEmpty": false
        },
        "offset": null,
        "format": [
          "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
        ],
        "hasFormat": true,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Instant",
          "parameters": [],
          "name": "Instant",
          "parameterizedName": "lang.taxi.Instant",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Instant",
          "shortDisplayName": "Instant"
        },
        "hasExpression": false,
        "unformattedTypeName": {
          "fullyQualifiedName": "lang.taxi.Instant",
          "parameters": [],
          "name": "Instant",
          "parameterizedName": "lang.taxi.Instant",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Instant",
          "shortDisplayName": "Instant"
        },
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.Instant",
        "fullyQualifiedName": "lang.taxi.Instant",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Instant",
          "parameters": [],
          "name": "Instant",
          "parameterizedName": "lang.taxi.Instant",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Instant",
          "shortDisplayName": "Instant"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "A signed integer - ie. a whole number (positive or negative), with no decimal places",
        "paramaterizedName": "lang.taxi.Int",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.Int",
        "fullyQualifiedName": "lang.taxi.Int",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Stream",
          "parameters": [],
          "name": "Stream",
          "parameterizedName": "lang.taxi.Stream",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Stream",
          "shortDisplayName": "Stream"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "Result of a service publishing sequence of events",
        "paramaterizedName": "lang.taxi.Stream",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "lang.taxi.Stream",
        "fullyQualifiedName": "lang.taxi.Stream",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Stream",
          "parameters": [],
          "name": "Stream",
          "parameterizedName": "lang.taxi.Stream",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Stream",
          "shortDisplayName": "Stream"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": true,
        "collectionType": {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Any",
          "shortDisplayName": "Any"
        },
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "A collection of characters.",
        "paramaterizedName": "lang.taxi.String",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.String",
        "fullyQualifiedName": "lang.taxi.String",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Time",
          "parameters": [],
          "name": "Time",
          "parameterizedName": "lang.taxi.Time",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Time",
          "shortDisplayName": "Time"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "Time only, excluding the date part",
        "paramaterizedName": "lang.taxi.Time",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [
            "HH:mm:ss"
          ],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": true,
          "isEmpty": false
        },
        "offset": null,
        "format": [
          "HH:mm:ss"
        ],
        "hasFormat": true,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Time",
          "parameters": [],
          "name": "Time",
          "parameterizedName": "lang.taxi.Time",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Time",
          "shortDisplayName": "Time"
        },
        "hasExpression": false,
        "unformattedTypeName": {
          "fullyQualifiedName": "lang.taxi.Time",
          "parameters": [],
          "name": "Time",
          "parameterizedName": "lang.taxi.Time",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Time",
          "shortDisplayName": "Time"
        },
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.Time",
        "fullyQualifiedName": "lang.taxi.Time",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Time",
          "parameters": [],
          "name": "Time",
          "parameterizedName": "lang.taxi.Time",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Time",
          "shortDisplayName": "Time"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Void",
          "parameters": [],
          "name": "Void",
          "parameterizedName": "lang.taxi.Void",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Void",
          "shortDisplayName": "Void"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeParameters": [],
        "typeDoc": "Nothing.  Represents the return value of operations that don't return anything.",
        "paramaterizedName": "lang.taxi.Void",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": null,
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Void",
          "parameters": [],
          "name": "Void",
          "parameterizedName": "lang.taxi.Void",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Void",
          "shortDisplayName": "Void"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": true,
        "longDisplayName": "lang.taxi.Void",
        "fullyQualifiedName": "lang.taxi.Void",
        "memberQualifiedName": {
          "fullyQualifiedName": "lang.taxi.Void",
          "parameters": [],
          "name": "Void",
          "parameterizedName": "lang.taxi.Void",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Void",
          "shortDisplayName": "Void"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "language.types.LanguageId",
          "parameters": [],
          "name": "LanguageId",
          "parameterizedName": "language.types.LanguageId",
          "namespace": "language.types",
          "longDisplayName": "language.types.LanguageId",
          "shortDisplayName": "LanguageId"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/language/types/LanguageId.taxi",
            "version": "0.0.0",
            "content": "namespace language.types {\n   type LanguageId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/language/types/LanguageId.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/language/types/LanguageId.taxi:0.0.0",
            "contentHash": "abbc59"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "language.types.LanguageId",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "language.types.LanguageId",
        "fullyQualifiedName": "language.types.LanguageId",
        "memberQualifiedName": {
          "fullyQualifiedName": "language.types.LanguageId",
          "parameters": [],
          "name": "LanguageId",
          "parameterizedName": "language.types.LanguageId",
          "namespace": "language.types",
          "longDisplayName": "language.types.LanguageId",
          "shortDisplayName": "LanguageId"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "staff.types.StaffId",
          "parameters": [],
          "name": "StaffId",
          "parameterizedName": "staff.types.StaffId",
          "namespace": "staff.types",
          "longDisplayName": "staff.types.StaffId",
          "shortDisplayName": "StaffId"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/staff/types/StaffId.taxi",
            "version": "0.0.0",
            "content": "namespace staff.types {\n   type StaffId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/staff/types/StaffId.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/staff/types/StaffId.taxi:0.0.0",
            "contentHash": "d4ebc0"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "staff.types.StaffId",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "staff.types.StaffId",
        "fullyQualifiedName": "staff.types.StaffId",
        "memberQualifiedName": {
          "fullyQualifiedName": "staff.types.StaffId",
          "parameters": [],
          "name": "StaffId",
          "parameterizedName": "staff.types.StaffId",
          "namespace": "staff.types",
          "longDisplayName": "staff.types.StaffId",
          "shortDisplayName": "StaffId"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "taxi.stdlib.Format",
          "parameters": [],
          "name": "Format",
          "parameterizedName": "taxi.stdlib.Format",
          "namespace": "taxi.stdlib",
          "longDisplayName": "taxi.stdlib.Format",
          "shortDisplayName": "Format"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "Native StdLib",
            "version": "0.0.0",
            "content": "namespace taxi.stdlib {\n   [[ Declares a format (and optionally an offset)\n      for date formats\n      ]]\n      annotation Format {\n          value : String?\n          offset : Int by default(0)\n      }\n}",
            "packageIdentifier": null,
            "packageQualifiedName": "Native StdLib",
            "id": "Native StdLib:0.0.0",
            "contentHash": "cc73a6"
          }
        ],
        "typeParameters": [],
        "typeDoc": null,
        "paramaterizedName": "taxi.stdlib.Format",
        "isTypeAlias": false,
        "formatAndZoneOffset": null,
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": false,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "taxi.stdlib.Format",
        "fullyQualifiedName": "taxi.stdlib.Format",
        "memberQualifiedName": {
          "fullyQualifiedName": "taxi.stdlib.Format",
          "parameters": [],
          "name": "Format",
          "parameterizedName": "taxi.stdlib.Format",
          "namespace": "taxi.stdlib",
          "longDisplayName": "taxi.stdlib.Format",
          "shortDisplayName": "Format"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      },
      {
        "name": {
          "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
          "parameters": [],
          "name": "VyneQlQuery",
          "parameterizedName": "vyne.vyneQl.VyneQlQuery",
          "namespace": "vyne.vyneQl",
          "longDisplayName": "vyne.vyneQl.VyneQlQuery",
          "shortDisplayName": "VyneQlQuery"
        },
        "attributes": {},
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [
          {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          }
        ],
        "enumValues": [],
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace vyne.vyneQl {\n   type VyneQlQuery inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "79e033"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "vyne.vyneQl.VyneQlQuery",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "vyne.vyneQl.VyneQlQuery",
        "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
        "memberQualifiedName": {
          "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
          "parameters": [],
          "name": "VyneQlQuery",
          "parameterizedName": "vyne.vyneQl.VyneQlQuery",
          "namespace": "vyne.vyneQl",
          "longDisplayName": "vyne.vyneQl.VyneQlQuery",
          "shortDisplayName": "VyneQlQuery"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": true
      }
    ],
    "services": [
      {
        "name": {
          "fullyQualifiedName": "io.vyne.films.announcements.KafkaService",
          "parameters": [],
          "name": "KafkaService",
          "parameterizedName": "io.vyne.films.announcements.KafkaService",
          "namespace": "io.vyne.films.announcements",
          "longDisplayName": "io.vyne.films.announcements.KafkaService",
          "shortDisplayName": "KafkaService"
        },
        "operations": [],
        "queryOperations": [],
        "streamOperations": [
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.films.announcements.KafkaService@@newReleases",
              "parameters": [],
              "name": "KafkaService@@newReleases",
              "parameterizedName": "io.vyne.films.announcements.KafkaService@@newReleases",
              "namespace": "io.vyne.films.announcements",
              "longDisplayName": "io.vyne.films.announcements.KafkaService / newReleases",
              "shortDisplayName": "newReleases"
            },
            "returnType": {
              "fullyQualifiedName": "lang.taxi.Stream",
              "parameters": [
                {
                  "fullyQualifiedName": "NewFilmReleaseAnnouncement",
                  "parameters": [],
                  "name": "NewFilmReleaseAnnouncement",
                  "parameterizedName": "NewFilmReleaseAnnouncement",
                  "namespace": "",
                  "longDisplayName": "NewFilmReleaseAnnouncement",
                  "shortDisplayName": "NewFilmReleaseAnnouncement"
                }
              ],
              "name": "Stream",
              "parameterizedName": "lang.taxi.Stream<NewFilmReleaseAnnouncement>",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>",
              "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>"
            },
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "io.vyne.kafka.KafkaOperation",
                  "parameters": [],
                  "name": "KafkaOperation",
                  "parameterizedName": "io.vyne.kafka.KafkaOperation",
                  "namespace": "io.vyne.kafka",
                  "longDisplayName": "io.vyne.kafka.KafkaOperation",
                  "shortDisplayName": "KafkaOperation"
                },
                "params": {
                  "topic": "releases",
                  "offset": "latest"
                }
              }
            ],
            "typeDoc": null,
            "parameters": [],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "lang.taxi.Stream",
                "parameters": [
                  {
                    "fullyQualifiedName": "NewFilmReleaseAnnouncement",
                    "parameters": [],
                    "name": "NewFilmReleaseAnnouncement",
                    "parameterizedName": "NewFilmReleaseAnnouncement",
                    "namespace": "",
                    "longDisplayName": "NewFilmReleaseAnnouncement",
                    "shortDisplayName": "NewFilmReleaseAnnouncement"
                  }
                ],
                "name": "Stream",
                "parameterizedName": "lang.taxi.Stream<NewFilmReleaseAnnouncement>",
                "namespace": "lang.taxi",
                "longDisplayName": "lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>",
                "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>"
              },
              "constraints": []
            },
            "operationType": null,
            "returnTypeName": {
              "fullyQualifiedName": "lang.taxi.Stream",
              "parameters": [
                {
                  "fullyQualifiedName": "NewFilmReleaseAnnouncement",
                  "parameters": [],
                  "name": "NewFilmReleaseAnnouncement",
                  "parameterizedName": "NewFilmReleaseAnnouncement",
                  "namespace": "",
                  "longDisplayName": "NewFilmReleaseAnnouncement",
                  "shortDisplayName": "NewFilmReleaseAnnouncement"
                }
              ],
              "name": "Stream",
              "parameterizedName": "lang.taxi.Stream<NewFilmReleaseAnnouncement>",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>",
              "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>"
            },
            "name": "newReleases",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.films.announcements.KafkaService@@newReleases",
              "parameters": [],
              "name": "KafkaService@@newReleases",
              "parameterizedName": "io.vyne.films.announcements.KafkaService@@newReleases",
              "namespace": "io.vyne.films.announcements",
              "longDisplayName": "io.vyne.films.announcements.KafkaService / newReleases",
              "shortDisplayName": "newReleases"
            }
          }
        ],
        "tableOperations": [],
        "metadata": [
          {
            "name": {
              "fullyQualifiedName": "io.vyne.kafka.KafkaService",
              "parameters": [],
              "name": "KafkaService",
              "parameterizedName": "io.vyne.kafka.KafkaService",
              "namespace": "io.vyne.kafka",
              "longDisplayName": "io.vyne.kafka.KafkaService",
              "shortDisplayName": "KafkaService"
            },
            "params": {
              "connectionName": "kafka"
            }
          }
        ],
        "sourceCode": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/io/vyne/films/announcements/KafkaService.taxi",
            "version": "0.0.0",
            "content": "import io.vyne.kafka.KafkaOperation\nimport lang.taxi.Stream\nimport NewFilmReleaseAnnouncement\nnamespace io.vyne.films.announcements {\n   @io.vyne.kafka.KafkaService(connectionName = \"kafka\")\n         service KafkaService {\n            @io.vyne.kafka.KafkaOperation(topic = \"releases\" , offset = \"latest\")\n            stream newReleases : Stream<NewFilmReleaseAnnouncement>\n         }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/io/vyne/films/announcements/KafkaService.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/io/vyne/films/announcements/KafkaService.taxi:0.0.0",
            "contentHash": "6ce573"
          }
        ],
        "typeDoc": null,
        "lineage": null,
        "serviceKind": "Kafka",
        "remoteOperations": [],
        "qualifiedName": "io.vyne.films.announcements.KafkaService",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.films.announcements.KafkaService",
          "parameters": [],
          "name": "KafkaService",
          "parameterizedName": "io.vyne.films.announcements.KafkaService",
          "namespace": "io.vyne.films.announcements",
          "longDisplayName": "io.vyne.films.announcements.KafkaService",
          "shortDisplayName": "KafkaService"
        }
      },
      {
        "name": {
          "fullyQualifiedName": "film.FilmDatabase",
          "parameters": [],
          "name": "FilmDatabase",
          "parameterizedName": "film.FilmDatabase",
          "namespace": "film",
          "longDisplayName": "film.FilmDatabase",
          "shortDisplayName": "FilmDatabase"
        },
        "operations": [],
        "queryOperations": [],
        "streamOperations": [],
        "tableOperations": [
          {
            "qualifiedName": {
              "fullyQualifiedName": "film.FilmDatabase@@films",
              "parameters": [],
              "name": "FilmDatabase@@films",
              "parameterizedName": "film.FilmDatabase@@films",
              "namespace": "film",
              "longDisplayName": "film.FilmDatabase / films",
              "shortDisplayName": "films"
            },
            "returnType": {
              "fullyQualifiedName": "lang.taxi.Array",
              "parameters": [
                {
                  "fullyQualifiedName": "film.Film",
                  "parameters": [],
                  "name": "Film",
                  "parameterizedName": "film.Film",
                  "namespace": "film",
                  "longDisplayName": "film.Film",
                  "shortDisplayName": "Film"
                }
              ],
              "name": "Array",
              "parameterizedName": "lang.taxi.Array<film.Film>",
              "namespace": "lang.taxi",
              "longDisplayName": "film.Film[]",
              "shortDisplayName": "Film[]"
            },
            "metadata": [],
            "typeDoc": null,
            "parameters": [],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "lang.taxi.Array",
                "parameters": [
                  {
                    "fullyQualifiedName": "film.Film",
                    "parameters": [],
                    "name": "Film",
                    "parameterizedName": "film.Film",
                    "namespace": "film",
                    "longDisplayName": "film.Film",
                    "shortDisplayName": "Film"
                  }
                ],
                "name": "Array",
                "parameterizedName": "lang.taxi.Array<film.Film>",
                "namespace": "lang.taxi",
                "longDisplayName": "film.Film[]",
                "shortDisplayName": "Film[]"
              },
              "constraints": []
            },
            "operationType": null,
            "returnTypeName": {
              "fullyQualifiedName": "lang.taxi.Array",
              "parameters": [
                {
                  "fullyQualifiedName": "film.Film",
                  "parameters": [],
                  "name": "Film",
                  "parameterizedName": "film.Film",
                  "namespace": "film",
                  "longDisplayName": "film.Film",
                  "shortDisplayName": "Film"
                }
              ],
              "name": "Array",
              "parameterizedName": "lang.taxi.Array<film.Film>",
              "namespace": "lang.taxi",
              "longDisplayName": "film.Film[]",
              "shortDisplayName": "Film[]"
            },
            "name": "films",
            "memberQualifiedName": {
              "fullyQualifiedName": "film.FilmDatabase@@films",
              "parameters": [],
              "name": "FilmDatabase@@films",
              "parameterizedName": "film.FilmDatabase@@films",
              "namespace": "film",
              "longDisplayName": "film.FilmDatabase / films",
              "shortDisplayName": "films"
            }
          }
        ],
        "metadata": [
          {
            "name": {
              "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
              "parameters": [],
              "name": "DatabaseService",
              "parameterizedName": "io.vyne.jdbc.DatabaseService",
              "namespace": "io.vyne.jdbc",
              "longDisplayName": "io.vyne.jdbc.DatabaseService",
              "shortDisplayName": "DatabaseService"
            },
            "params": {
              "connection": "films"
            }
          }
        ],
        "sourceCode": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/FilmService.taxi",
            "version": "0.0.0",
            "content": "namespace film {\n   @io.vyne.jdbc.DatabaseService(connection = \"films\")\n         service FilmDatabase {\n            table films : Film[]\n         }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/FilmService.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/FilmService.taxi:0.0.0",
            "contentHash": "e12e52"
          }
        ],
        "typeDoc": null,
        "lineage": null,
        "serviceKind": "Database",
        "remoteOperations": [
          {
            "qualifiedName": {
              "fullyQualifiedName": "film.FilmDatabase@@films_FindOne",
              "parameters": [],
              "name": "FilmDatabase@@films_FindOne",
              "parameterizedName": "film.FilmDatabase@@films_FindOne",
              "namespace": "film",
              "longDisplayName": "film.FilmDatabase / films_FindOne",
              "shortDisplayName": "films_FindOne"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                  "parameters": [],
                  "name": "VyneQlQuery",
                  "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                  "namespace": "vyne.vyneQl",
                  "longDisplayName": "vyne.vyneQl.VyneQlQuery",
                  "shortDisplayName": "VyneQlQuery"
                },
                "name": "body",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                  "parameters": [],
                  "name": "VyneQlQuery",
                  "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                  "namespace": "vyne.vyneQl",
                  "longDisplayName": "vyne.vyneQl.VyneQlQuery",
                  "shortDisplayName": "VyneQlQuery"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "film.Film",
              "parameters": [],
              "name": "Film",
              "parameterizedName": "film.Film",
              "namespace": "film",
              "longDisplayName": "film.Film",
              "shortDisplayName": "Film"
            },
            "metadata": [],
            "grammar": "vyneQl",
            "capabilities": [
              "SUM",
              "COUNT",
              "AVG",
              "MIN",
              "MAX",
              {
                "supportedOperations": [
                  "EQUAL",
                  "NOT_EQUAL",
                  "IN",
                  "LIKE",
                  "GREATER_THAN",
                  "LESS_THAN",
                  "GREATER_THAN_OR_EQUAL_TO",
                  "LESS_THAN_OR_EQUAL_TO"
                ]
              }
            ],
            "typeDoc": null,
            "contract": {
              "returnType": {
                "fullyQualifiedName": "film.Film",
                "parameters": [],
                "name": "Film",
                "parameterizedName": "film.Film",
                "namespace": "film",
                "longDisplayName": "film.Film",
                "shortDisplayName": "Film"
              },
              "constraints": []
            },
            "operationType": null,
            "hasFilterCapability": true,
            "supportedFilterOperations": [
              "EQUAL",
              "NOT_EQUAL",
              "IN",
              "LIKE",
              "GREATER_THAN",
              "LESS_THAN",
              "GREATER_THAN_OR_EQUAL_TO",
              "LESS_THAN_OR_EQUAL_TO"
            ],
            "returnTypeName": {
              "fullyQualifiedName": "film.Film",
              "parameters": [],
              "name": "Film",
              "parameterizedName": "film.Film",
              "namespace": "film",
              "longDisplayName": "film.Film",
              "shortDisplayName": "Film"
            },
            "name": "films_FindOne",
            "memberQualifiedName": {
              "fullyQualifiedName": "film.FilmDatabase@@films_FindOne",
              "parameters": [],
              "name": "FilmDatabase@@films_FindOne",
              "parameterizedName": "film.FilmDatabase@@films_FindOne",
              "namespace": "film",
              "longDisplayName": "film.FilmDatabase / films_FindOne",
              "shortDisplayName": "films_FindOne"
            }
          },
          {
            "qualifiedName": {
              "fullyQualifiedName": "film.FilmDatabase@@films_FindMany",
              "parameters": [],
              "name": "FilmDatabase@@films_FindMany",
              "parameterizedName": "film.FilmDatabase@@films_FindMany",
              "namespace": "film",
              "longDisplayName": "film.FilmDatabase / films_FindMany",
              "shortDisplayName": "films_FindMany"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                  "parameters": [],
                  "name": "VyneQlQuery",
                  "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                  "namespace": "vyne.vyneQl",
                  "longDisplayName": "vyne.vyneQl.VyneQlQuery",
                  "shortDisplayName": "VyneQlQuery"
                },
                "name": "body",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                  "parameters": [],
                  "name": "VyneQlQuery",
                  "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                  "namespace": "vyne.vyneQl",
                  "longDisplayName": "vyne.vyneQl.VyneQlQuery",
                  "shortDisplayName": "VyneQlQuery"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "lang.taxi.Array",
              "parameters": [
                {
                  "fullyQualifiedName": "film.Film",
                  "parameters": [],
                  "name": "Film",
                  "parameterizedName": "film.Film",
                  "namespace": "film",
                  "longDisplayName": "film.Film",
                  "shortDisplayName": "Film"
                }
              ],
              "name": "Array",
              "parameterizedName": "lang.taxi.Array<film.Film>",
              "namespace": "lang.taxi",
              "longDisplayName": "film.Film[]",
              "shortDisplayName": "Film[]"
            },
            "metadata": [],
            "grammar": "vyneQl",
            "capabilities": [
              "SUM",
              "COUNT",
              "AVG",
              "MIN",
              "MAX",
              {
                "supportedOperations": [
                  "EQUAL",
                  "NOT_EQUAL",
                  "IN",
                  "LIKE",
                  "GREATER_THAN",
                  "LESS_THAN",
                  "GREATER_THAN_OR_EQUAL_TO",
                  "LESS_THAN_OR_EQUAL_TO"
                ]
              }
            ],
            "typeDoc": null,
            "contract": {
              "returnType": {
                "fullyQualifiedName": "lang.taxi.Array",
                "parameters": [
                  {
                    "fullyQualifiedName": "film.Film",
                    "parameters": [],
                    "name": "Film",
                    "parameterizedName": "film.Film",
                    "namespace": "film",
                    "longDisplayName": "film.Film",
                    "shortDisplayName": "Film"
                  }
                ],
                "name": "Array",
                "parameterizedName": "lang.taxi.Array<film.Film>",
                "namespace": "lang.taxi",
                "longDisplayName": "film.Film[]",
                "shortDisplayName": "Film[]"
              },
              "constraints": []
            },
            "operationType": null,
            "hasFilterCapability": true,
            "supportedFilterOperations": [
              "EQUAL",
              "NOT_EQUAL",
              "IN",
              "LIKE",
              "GREATER_THAN",
              "LESS_THAN",
              "GREATER_THAN_OR_EQUAL_TO",
              "LESS_THAN_OR_EQUAL_TO"
            ],
            "returnTypeName": {
              "fullyQualifiedName": "lang.taxi.Array",
              "parameters": [
                {
                  "fullyQualifiedName": "film.Film",
                  "parameters": [],
                  "name": "Film",
                  "parameterizedName": "film.Film",
                  "namespace": "film",
                  "longDisplayName": "film.Film",
                  "shortDisplayName": "Film"
                }
              ],
              "name": "Array",
              "parameterizedName": "lang.taxi.Array<film.Film>",
              "namespace": "lang.taxi",
              "longDisplayName": "film.Film[]",
              "shortDisplayName": "Film[]"
            },
            "name": "films_FindMany",
            "memberQualifiedName": {
              "fullyQualifiedName": "film.FilmDatabase@@films_FindMany",
              "parameters": [],
              "name": "FilmDatabase@@films_FindMany",
              "parameterizedName": "film.FilmDatabase@@films_FindMany",
              "namespace": "film",
              "longDisplayName": "film.FilmDatabase / films_FindMany",
              "shortDisplayName": "films_FindMany"
            }
          }
        ],
        "qualifiedName": "film.FilmDatabase",
        "memberQualifiedName": {
          "fullyQualifiedName": "film.FilmDatabase",
          "parameters": [],
          "name": "FilmDatabase",
          "parameterizedName": "film.FilmDatabase",
          "namespace": "film",
          "longDisplayName": "film.FilmDatabase",
          "shortDisplayName": "FilmDatabase"
        }
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService",
          "parameters": [],
          "name": "IdLookupService",
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService",
          "shortDisplayName": "IdLookupService"
        },
        "operations": [
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromSquashedTomatoesId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
              "shortDisplayName": "lookupFromSquashedTomatoesId"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "lang.taxi.String",
                  "parameters": [],
                  "name": "String",
                  "parameterizedName": "lang.taxi.String",
                  "namespace": "lang.taxi",
                  "longDisplayName": "lang.taxi.String",
                  "shortDisplayName": "String"
                },
                "name": "id",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "lang.taxi.String",
                  "parameters": [],
                  "name": "String",
                  "parameterizedName": "lang.taxi.String",
                  "namespace": "lang.taxi",
                  "longDisplayName": "lang.taxi.String",
                  "shortDisplayName": "String"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "name": "lookupFromSquashedTomatoesId",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromSquashedTomatoesId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
              "shortDisplayName": "lookupFromSquashedTomatoesId"
            }
          },
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromInternalFilmId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
              "shortDisplayName": "lookupFromInternalFilmId"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "films.FilmId",
                  "parameters": [],
                  "name": "FilmId",
                  "parameterizedName": "films.FilmId",
                  "namespace": "films",
                  "longDisplayName": "films.FilmId",
                  "shortDisplayName": "FilmId"
                },
                "name": "id",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "films.FilmId",
                  "parameters": [],
                  "name": "FilmId",
                  "parameterizedName": "films.FilmId",
                  "namespace": "films",
                  "longDisplayName": "films.FilmId",
                  "shortDisplayName": "FilmId"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9986/ids/internal/{films.FilmId}"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "name": "lookupFromInternalFilmId",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromInternalFilmId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
              "shortDisplayName": "lookupFromInternalFilmId"
            }
          },
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromNetflixFilmId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
              "shortDisplayName": "lookupFromNetflixFilmId"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                  "parameters": [],
                  "name": "NetflixFilmId",
                  "parameterizedName": "demo.netflix.NetflixFilmId",
                  "namespace": "demo.netflix",
                  "longDisplayName": "demo.netflix.NetflixFilmId",
                  "shortDisplayName": "NetflixFilmId"
                },
                "name": "id",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                  "parameters": [],
                  "name": "NetflixFilmId",
                  "parameterizedName": "demo.netflix.NetflixFilmId",
                  "namespace": "demo.netflix",
                  "longDisplayName": "demo.netflix.NetflixFilmId",
                  "shortDisplayName": "NetflixFilmId"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "name": "lookupFromNetflixFilmId",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromNetflixFilmId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
              "shortDisplayName": "lookupFromNetflixFilmId"
            }
          }
        ],
        "queryOperations": [],
        "streamOperations": [],
        "tableOperations": [],
        "metadata": [],
        "sourceCode": [
          {
            "name": "id-resolution-service",
            "version": "0.0.0",
            "content": "import films.FilmId\nimport demo.netflix.NetflixFilmId\nnamespace io.vyne.films.idlookup {\n   service IdLookupService {\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}\")\n         operation lookupFromSquashedTomatoesId(  id : String ) : IdResolution\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9986/ids/internal/{films.FilmId}\")\n         operation lookupFromInternalFilmId(  id : films.FilmId ) : IdResolution\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}\")\n         operation lookupFromNetflixFilmId(  id : demo.netflix.NetflixFilmId ) : IdResolution\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.petflex.demos",
              "name": "id-lookup-service",
              "version": "0.0.0",
              "unversionedId": "io.petflex.demos/id-lookup-service",
              "id": "io.petflex.demos/id-lookup-service/0.0.0",
              "uriSafeId": "io.petflex.demos:id-lookup-service:0.0.0"
            },
            "packageQualifiedName": "[io.petflex.demos/id-lookup-service/0.0.0]/id-resolution-service",
            "id": "id-resolution-service:0.0.0",
            "contentHash": "c320e3"
          }
        ],
        "typeDoc": null,
        "lineage": null,
        "serviceKind": "API",
        "remoteOperations": [
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromSquashedTomatoesId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
              "shortDisplayName": "lookupFromSquashedTomatoesId"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "lang.taxi.String",
                  "parameters": [],
                  "name": "String",
                  "parameterizedName": "lang.taxi.String",
                  "namespace": "lang.taxi",
                  "longDisplayName": "lang.taxi.String",
                  "shortDisplayName": "String"
                },
                "name": "id",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "lang.taxi.String",
                  "parameters": [],
                  "name": "String",
                  "parameterizedName": "lang.taxi.String",
                  "namespace": "lang.taxi",
                  "longDisplayName": "lang.taxi.String",
                  "shortDisplayName": "String"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "name": "lookupFromSquashedTomatoesId",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromSquashedTomatoesId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
              "shortDisplayName": "lookupFromSquashedTomatoesId"
            }
          },
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromInternalFilmId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
              "shortDisplayName": "lookupFromInternalFilmId"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "films.FilmId",
                  "parameters": [],
                  "name": "FilmId",
                  "parameterizedName": "films.FilmId",
                  "namespace": "films",
                  "longDisplayName": "films.FilmId",
                  "shortDisplayName": "FilmId"
                },
                "name": "id",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "films.FilmId",
                  "parameters": [],
                  "name": "FilmId",
                  "parameterizedName": "films.FilmId",
                  "namespace": "films",
                  "longDisplayName": "films.FilmId",
                  "shortDisplayName": "FilmId"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9986/ids/internal/{films.FilmId}"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "name": "lookupFromInternalFilmId",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromInternalFilmId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
              "shortDisplayName": "lookupFromInternalFilmId"
            }
          },
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromNetflixFilmId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
              "shortDisplayName": "lookupFromNetflixFilmId"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                  "parameters": [],
                  "name": "NetflixFilmId",
                  "parameterizedName": "demo.netflix.NetflixFilmId",
                  "namespace": "demo.netflix",
                  "longDisplayName": "demo.netflix.NetflixFilmId",
                  "shortDisplayName": "NetflixFilmId"
                },
                "name": "id",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                  "parameters": [],
                  "name": "NetflixFilmId",
                  "parameterizedName": "demo.netflix.NetflixFilmId",
                  "namespace": "demo.netflix",
                  "longDisplayName": "demo.netflix.NetflixFilmId",
                  "shortDisplayName": "NetflixFilmId"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "name": "lookupFromNetflixFilmId",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
              "parameters": [],
              "name": "IdLookupService@@lookupFromNetflixFilmId",
              "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
              "shortDisplayName": "lookupFromNetflixFilmId"
            }
          }
        ],
        "qualifiedName": "io.vyne.films.idlookup.IdLookupService",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService",
          "parameters": [],
          "name": "IdLookupService",
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService",
          "shortDisplayName": "IdLookupService"
        }
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider",
          "parameters": [],
          "name": "StreamingMoviesProvider",
          "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider",
          "shortDisplayName": "StreamingMoviesProvider"
        },
        "operations": [
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "parameters": [],
              "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "namespace": "io.vyne.demos.films",
              "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
              "shortDisplayName": "getStreamingProvidersForFilm"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "films.FilmId",
                  "parameters": [],
                  "name": "FilmId",
                  "parameterizedName": "films.FilmId",
                  "namespace": "films",
                  "longDisplayName": "films.FilmId",
                  "shortDisplayName": "FilmId"
                },
                "name": "filmId",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "films.FilmId",
                  "parameters": [],
                  "name": "FilmId",
                  "parameterizedName": "films.FilmId",
                  "namespace": "films",
                  "longDisplayName": "films.FilmId",
                  "shortDisplayName": "FilmId"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
              "parameters": [],
              "name": "StreamingProvider",
              "parameterizedName": "io.vyne.demos.films.StreamingProvider",
              "namespace": "io.vyne.demos.films",
              "longDisplayName": "io.vyne.demos.films.StreamingProvider",
              "shortDisplayName": "StreamingProvider"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9981/films/{films.FilmId}/streamingProviders"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
                "parameters": [],
                "name": "StreamingProvider",
                "parameterizedName": "io.vyne.demos.films.StreamingProvider",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingProvider",
                "shortDisplayName": "StreamingProvider"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
              "parameters": [],
              "name": "StreamingProvider",
              "parameterizedName": "io.vyne.demos.films.StreamingProvider",
              "namespace": "io.vyne.demos.films",
              "longDisplayName": "io.vyne.demos.films.StreamingProvider",
              "shortDisplayName": "StreamingProvider"
            },
            "name": "getStreamingProvidersForFilm",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "parameters": [],
              "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "namespace": "io.vyne.demos.films",
              "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
              "shortDisplayName": "getStreamingProvidersForFilm"
            }
          }
        ],
        "queryOperations": [],
        "streamOperations": [],
        "tableOperations": [],
        "metadata": [],
        "sourceCode": [
          {
            "name": "films-service",
            "version": "0.0.0",
            "content": "import films.FilmId\nnamespace io.vyne.demos.films {\n   service StreamingMoviesProvider {\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9981/films/{films.FilmId}/streamingProviders\")\n         operation getStreamingProvidersForFilm(  filmId : films.FilmId ) : StreamingProvider\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.petflix.demos",
              "name": "films-api",
              "version": "0.0.0",
              "unversionedId": "io.petflix.demos/films-api",
              "id": "io.petflix.demos/films-api/0.0.0",
              "uriSafeId": "io.petflix.demos:films-api:0.0.0"
            },
            "packageQualifiedName": "[io.petflix.demos/films-api/0.0.0]/films-service",
            "id": "films-service:0.0.0",
            "contentHash": "a807dc"
          }
        ],
        "typeDoc": null,
        "lineage": null,
        "serviceKind": "API",
        "remoteOperations": [
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "parameters": [],
              "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "namespace": "io.vyne.demos.films",
              "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
              "shortDisplayName": "getStreamingProvidersForFilm"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "films.FilmId",
                  "parameters": [],
                  "name": "FilmId",
                  "parameterizedName": "films.FilmId",
                  "namespace": "films",
                  "longDisplayName": "films.FilmId",
                  "shortDisplayName": "FilmId"
                },
                "name": "filmId",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "films.FilmId",
                  "parameters": [],
                  "name": "FilmId",
                  "parameterizedName": "films.FilmId",
                  "namespace": "films",
                  "longDisplayName": "films.FilmId",
                  "shortDisplayName": "FilmId"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
              "parameters": [],
              "name": "StreamingProvider",
              "parameterizedName": "io.vyne.demos.films.StreamingProvider",
              "namespace": "io.vyne.demos.films",
              "longDisplayName": "io.vyne.demos.films.StreamingProvider",
              "shortDisplayName": "StreamingProvider"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9981/films/{films.FilmId}/streamingProviders"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
                "parameters": [],
                "name": "StreamingProvider",
                "parameterizedName": "io.vyne.demos.films.StreamingProvider",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingProvider",
                "shortDisplayName": "StreamingProvider"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
              "parameters": [],
              "name": "StreamingProvider",
              "parameterizedName": "io.vyne.demos.films.StreamingProvider",
              "namespace": "io.vyne.demos.films",
              "longDisplayName": "io.vyne.demos.films.StreamingProvider",
              "shortDisplayName": "StreamingProvider"
            },
            "name": "getStreamingProvidersForFilm",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "parameters": [],
              "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
              "namespace": "io.vyne.demos.films",
              "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
              "shortDisplayName": "getStreamingProvidersForFilm"
            }
          }
        ],
        "qualifiedName": "io.vyne.demos.films.StreamingMoviesProvider",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider",
          "parameters": [],
          "name": "StreamingMoviesProvider",
          "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider",
          "shortDisplayName": "StreamingMoviesProvider"
        }
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.reviews.ReviewsService",
          "parameters": [],
          "name": "ReviewsService",
          "parameterizedName": "io.vyne.reviews.ReviewsService",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.ReviewsService",
          "shortDisplayName": "ReviewsService"
        },
        "operations": [
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
              "parameters": [],
              "name": "ReviewsService@@getReview",
              "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
              "namespace": "io.vyne.reviews",
              "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
              "shortDisplayName": "getReview"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                  "parameters": [],
                  "name": "SquashedTomatoesFilmId",
                  "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                  "namespace": "films.reviews",
                  "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                  "shortDisplayName": "SquashedTomatoesFilmId"
                },
                "name": "filmId",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                  "parameters": [],
                  "name": "SquashedTomatoesFilmId",
                  "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                  "namespace": "films.reviews",
                  "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                  "shortDisplayName": "SquashedTomatoesFilmId"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.reviews.FilmReview",
              "parameters": [],
              "name": "FilmReview",
              "parameterizedName": "io.vyne.reviews.FilmReview",
              "namespace": "io.vyne.reviews",
              "longDisplayName": "io.vyne.reviews.FilmReview",
              "shortDisplayName": "FilmReview"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.reviews.FilmReview",
                "parameters": [],
                "name": "FilmReview",
                "parameterizedName": "io.vyne.reviews.FilmReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.FilmReview",
                "shortDisplayName": "FilmReview"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.reviews.FilmReview",
              "parameters": [],
              "name": "FilmReview",
              "parameterizedName": "io.vyne.reviews.FilmReview",
              "namespace": "io.vyne.reviews",
              "longDisplayName": "io.vyne.reviews.FilmReview",
              "shortDisplayName": "FilmReview"
            },
            "name": "getReview",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
              "parameters": [],
              "name": "ReviewsService@@getReview",
              "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
              "namespace": "io.vyne.reviews",
              "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
              "shortDisplayName": "getReview"
            }
          }
        ],
        "queryOperations": [],
        "streamOperations": [],
        "tableOperations": [],
        "metadata": [],
        "sourceCode": [
          {
            "name": "squashed-tomatoes",
            "version": "0.0.0",
            "content": "import films.reviews.SquashedTomatoesFilmId\nnamespace io.vyne.reviews {\n   service ReviewsService {\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}\")\n         operation getReview(  filmId : films.reviews.SquashedTomatoesFilmId ) : FilmReview\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.petflix.demos",
              "name": "films-reviews",
              "version": "0.0.0",
              "unversionedId": "io.petflix.demos/films-reviews",
              "id": "io.petflix.demos/films-reviews/0.0.0",
              "uriSafeId": "io.petflix.demos:films-reviews:0.0.0"
            },
            "packageQualifiedName": "[io.petflix.demos/films-reviews/0.0.0]/squashed-tomatoes",
            "id": "squashed-tomatoes:0.0.0",
            "contentHash": "0945a0"
          }
        ],
        "typeDoc": null,
        "lineage": null,
        "serviceKind": "API",
        "remoteOperations": [
          {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
              "parameters": [],
              "name": "ReviewsService@@getReview",
              "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
              "namespace": "io.vyne.reviews",
              "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
              "shortDisplayName": "getReview"
            },
            "parameters": [
              {
                "type": {
                  "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                  "parameters": [],
                  "name": "SquashedTomatoesFilmId",
                  "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                  "namespace": "films.reviews",
                  "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                  "shortDisplayName": "SquashedTomatoesFilmId"
                },
                "name": "filmId",
                "metadata": [],
                "constraints": [],
                "typeName": {
                  "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                  "parameters": [],
                  "name": "SquashedTomatoesFilmId",
                  "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                  "namespace": "films.reviews",
                  "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                  "shortDisplayName": "SquashedTomatoesFilmId"
                }
              }
            ],
            "returnType": {
              "fullyQualifiedName": "io.vyne.reviews.FilmReview",
              "parameters": [],
              "name": "FilmReview",
              "parameterizedName": "io.vyne.reviews.FilmReview",
              "namespace": "io.vyne.reviews",
              "longDisplayName": "io.vyne.reviews.FilmReview",
              "shortDisplayName": "FilmReview"
            },
            "operationType": null,
            "metadata": [
              {
                "name": {
                  "fullyQualifiedName": "HttpOperation",
                  "parameters": [],
                  "name": "HttpOperation",
                  "parameterizedName": "HttpOperation",
                  "namespace": "",
                  "longDisplayName": "HttpOperation",
                  "shortDisplayName": "HttpOperation"
                },
                "params": {
                  "method": "GET",
                  "url": "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}"
                }
              }
            ],
            "contract": {
              "returnType": {
                "fullyQualifiedName": "io.vyne.reviews.FilmReview",
                "parameters": [],
                "name": "FilmReview",
                "parameterizedName": "io.vyne.reviews.FilmReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.FilmReview",
                "shortDisplayName": "FilmReview"
              },
              "constraints": []
            },
            "typeDoc": null,
            "returnTypeName": {
              "fullyQualifiedName": "io.vyne.reviews.FilmReview",
              "parameters": [],
              "name": "FilmReview",
              "parameterizedName": "io.vyne.reviews.FilmReview",
              "namespace": "io.vyne.reviews",
              "longDisplayName": "io.vyne.reviews.FilmReview",
              "shortDisplayName": "FilmReview"
            },
            "name": "getReview",
            "memberQualifiedName": {
              "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
              "parameters": [],
              "name": "ReviewsService@@getReview",
              "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
              "namespace": "io.vyne.reviews",
              "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
              "shortDisplayName": "getReview"
            }
          }
        ],
        "qualifiedName": "io.vyne.reviews.ReviewsService",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.reviews.ReviewsService",
          "parameters": [],
          "name": "ReviewsService",
          "parameterizedName": "io.vyne.reviews.ReviewsService",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.ReviewsService",
          "shortDisplayName": "ReviewsService"
        }
      }
    ],
    "dynamicMetadata": [
      {
        "fullyQualifiedName": "HttpOperation",
        "parameters": [],
        "name": "HttpOperation",
        "parameterizedName": "HttpOperation",
        "namespace": "",
        "longDisplayName": "HttpOperation",
        "shortDisplayName": "HttpOperation"
      },
      {
        "fullyQualifiedName": "lang.taxi.formats.ProtobufMessage",
        "parameters": [],
        "name": "ProtobufMessage",
        "parameterizedName": "lang.taxi.formats.ProtobufMessage",
        "namespace": "lang.taxi.formats",
        "longDisplayName": "lang.taxi.formats.ProtobufMessage",
        "shortDisplayName": "ProtobufMessage"
      },
      {
        "fullyQualifiedName": "lang.taxi.formats.ProtobufField",
        "parameters": [],
        "name": "ProtobufField",
        "parameterizedName": "lang.taxi.formats.ProtobufField",
        "namespace": "lang.taxi.formats",
        "longDisplayName": "lang.taxi.formats.ProtobufField",
        "shortDisplayName": "ProtobufField"
      },
      {
        "fullyQualifiedName": "Id",
        "parameters": [],
        "name": "Id",
        "parameterizedName": "Id",
        "namespace": "",
        "longDisplayName": "Id",
        "shortDisplayName": "Id"
      },
      {
        "fullyQualifiedName": "Ann",
        "parameters": [],
        "name": "Ann",
        "parameterizedName": "Ann",
        "namespace": "",
        "longDisplayName": "Ann",
        "shortDisplayName": "Ann"
      }
    ],
    "metadataTypes": [],
    "queryOperations": [],
    "operations": [
      {
        "qualifiedName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
          "parameters": [],
          "name": "IdLookupService@@lookupFromSquashedTomatoesId",
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
          "shortDisplayName": "lookupFromSquashedTomatoesId"
        },
        "parameters": [
          {
            "type": {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            },
            "name": "id",
            "metadata": [],
            "constraints": [],
            "typeName": {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          }
        ],
        "returnType": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "parameters": [],
          "name": "IdResolution",
          "parameterizedName": "io.vyne.films.idlookup.IdResolution",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "shortDisplayName": "IdResolution"
        },
        "operationType": null,
        "metadata": [
          {
            "name": {
              "fullyQualifiedName": "HttpOperation",
              "parameters": [],
              "name": "HttpOperation",
              "parameterizedName": "HttpOperation",
              "namespace": "",
              "longDisplayName": "HttpOperation",
              "shortDisplayName": "HttpOperation"
            },
            "params": {
              "method": "GET",
              "url": "http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}"
            }
          }
        ],
        "contract": {
          "returnType": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "constraints": []
        },
        "typeDoc": null,
        "returnTypeName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "parameters": [],
          "name": "IdResolution",
          "parameterizedName": "io.vyne.films.idlookup.IdResolution",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "shortDisplayName": "IdResolution"
        },
        "name": "lookupFromSquashedTomatoesId",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
          "parameters": [],
          "name": "IdLookupService@@lookupFromSquashedTomatoesId",
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
          "shortDisplayName": "lookupFromSquashedTomatoesId"
        }
      },
      {
        "qualifiedName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
          "parameters": [],
          "name": "IdLookupService@@lookupFromInternalFilmId",
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
          "shortDisplayName": "lookupFromInternalFilmId"
        },
        "parameters": [
          {
            "type": {
              "fullyQualifiedName": "films.FilmId",
              "parameters": [],
              "name": "FilmId",
              "parameterizedName": "films.FilmId",
              "namespace": "films",
              "longDisplayName": "films.FilmId",
              "shortDisplayName": "FilmId"
            },
            "name": "id",
            "metadata": [],
            "constraints": [],
            "typeName": {
              "fullyQualifiedName": "films.FilmId",
              "parameters": [],
              "name": "FilmId",
              "parameterizedName": "films.FilmId",
              "namespace": "films",
              "longDisplayName": "films.FilmId",
              "shortDisplayName": "FilmId"
            }
          }
        ],
        "returnType": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "parameters": [],
          "name": "IdResolution",
          "parameterizedName": "io.vyne.films.idlookup.IdResolution",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "shortDisplayName": "IdResolution"
        },
        "operationType": null,
        "metadata": [
          {
            "name": {
              "fullyQualifiedName": "HttpOperation",
              "parameters": [],
              "name": "HttpOperation",
              "parameterizedName": "HttpOperation",
              "namespace": "",
              "longDisplayName": "HttpOperation",
              "shortDisplayName": "HttpOperation"
            },
            "params": {
              "method": "GET",
              "url": "http://localhost:9986/ids/internal/{films.FilmId}"
            }
          }
        ],
        "contract": {
          "returnType": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "constraints": []
        },
        "typeDoc": null,
        "returnTypeName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "parameters": [],
          "name": "IdResolution",
          "parameterizedName": "io.vyne.films.idlookup.IdResolution",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "shortDisplayName": "IdResolution"
        },
        "name": "lookupFromInternalFilmId",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
          "parameters": [],
          "name": "IdLookupService@@lookupFromInternalFilmId",
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
          "shortDisplayName": "lookupFromInternalFilmId"
        }
      },
      {
        "qualifiedName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "parameters": [],
          "name": "IdLookupService@@lookupFromNetflixFilmId",
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
          "shortDisplayName": "lookupFromNetflixFilmId"
        },
        "parameters": [
          {
            "type": {
              "fullyQualifiedName": "demo.netflix.NetflixFilmId",
              "parameters": [],
              "name": "NetflixFilmId",
              "parameterizedName": "demo.netflix.NetflixFilmId",
              "namespace": "demo.netflix",
              "longDisplayName": "demo.netflix.NetflixFilmId",
              "shortDisplayName": "NetflixFilmId"
            },
            "name": "id",
            "metadata": [],
            "constraints": [],
            "typeName": {
              "fullyQualifiedName": "demo.netflix.NetflixFilmId",
              "parameters": [],
              "name": "NetflixFilmId",
              "parameterizedName": "demo.netflix.NetflixFilmId",
              "namespace": "demo.netflix",
              "longDisplayName": "demo.netflix.NetflixFilmId",
              "shortDisplayName": "NetflixFilmId"
            }
          }
        ],
        "returnType": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "parameters": [],
          "name": "IdResolution",
          "parameterizedName": "io.vyne.films.idlookup.IdResolution",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "shortDisplayName": "IdResolution"
        },
        "operationType": null,
        "metadata": [
          {
            "name": {
              "fullyQualifiedName": "HttpOperation",
              "parameters": [],
              "name": "HttpOperation",
              "parameterizedName": "HttpOperation",
              "namespace": "",
              "longDisplayName": "HttpOperation",
              "shortDisplayName": "HttpOperation"
            },
            "params": {
              "method": "GET",
              "url": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}"
            }
          }
        ],
        "contract": {
          "returnType": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "constraints": []
        },
        "typeDoc": null,
        "returnTypeName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "parameters": [],
          "name": "IdResolution",
          "parameterizedName": "io.vyne.films.idlookup.IdResolution",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "shortDisplayName": "IdResolution"
        },
        "name": "lookupFromNetflixFilmId",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "parameters": [],
          "name": "IdLookupService@@lookupFromNetflixFilmId",
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
          "shortDisplayName": "lookupFromNetflixFilmId"
        }
      },
      {
        "qualifiedName": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "parameters": [],
          "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
          "shortDisplayName": "getStreamingProvidersForFilm"
        },
        "parameters": [
          {
            "type": {
              "fullyQualifiedName": "films.FilmId",
              "parameters": [],
              "name": "FilmId",
              "parameterizedName": "films.FilmId",
              "namespace": "films",
              "longDisplayName": "films.FilmId",
              "shortDisplayName": "FilmId"
            },
            "name": "filmId",
            "metadata": [],
            "constraints": [],
            "typeName": {
              "fullyQualifiedName": "films.FilmId",
              "parameters": [],
              "name": "FilmId",
              "parameterizedName": "films.FilmId",
              "namespace": "films",
              "longDisplayName": "films.FilmId",
              "shortDisplayName": "FilmId"
            }
          }
        ],
        "returnType": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
          "parameters": [],
          "name": "StreamingProvider",
          "parameterizedName": "io.vyne.demos.films.StreamingProvider",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingProvider",
          "shortDisplayName": "StreamingProvider"
        },
        "operationType": null,
        "metadata": [
          {
            "name": {
              "fullyQualifiedName": "HttpOperation",
              "parameters": [],
              "name": "HttpOperation",
              "parameterizedName": "HttpOperation",
              "namespace": "",
              "longDisplayName": "HttpOperation",
              "shortDisplayName": "HttpOperation"
            },
            "params": {
              "method": "GET",
              "url": "http://localhost:9981/films/{films.FilmId}/streamingProviders"
            }
          }
        ],
        "contract": {
          "returnType": {
            "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
            "parameters": [],
            "name": "StreamingProvider",
            "parameterizedName": "io.vyne.demos.films.StreamingProvider",
            "namespace": "io.vyne.demos.films",
            "longDisplayName": "io.vyne.demos.films.StreamingProvider",
            "shortDisplayName": "StreamingProvider"
          },
          "constraints": []
        },
        "typeDoc": null,
        "returnTypeName": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
          "parameters": [],
          "name": "StreamingProvider",
          "parameterizedName": "io.vyne.demos.films.StreamingProvider",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingProvider",
          "shortDisplayName": "StreamingProvider"
        },
        "name": "getStreamingProvidersForFilm",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "parameters": [],
          "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
          "shortDisplayName": "getStreamingProvidersForFilm"
        }
      },
      {
        "qualifiedName": {
          "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
          "parameters": [],
          "name": "ReviewsService@@getReview",
          "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
          "shortDisplayName": "getReview"
        },
        "parameters": [
          {
            "type": {
              "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
              "parameters": [],
              "name": "SquashedTomatoesFilmId",
              "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
              "namespace": "films.reviews",
              "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
              "shortDisplayName": "SquashedTomatoesFilmId"
            },
            "name": "filmId",
            "metadata": [],
            "constraints": [],
            "typeName": {
              "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
              "parameters": [],
              "name": "SquashedTomatoesFilmId",
              "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
              "namespace": "films.reviews",
              "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
              "shortDisplayName": "SquashedTomatoesFilmId"
            }
          }
        ],
        "returnType": {
          "fullyQualifiedName": "io.vyne.reviews.FilmReview",
          "parameters": [],
          "name": "FilmReview",
          "parameterizedName": "io.vyne.reviews.FilmReview",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.FilmReview",
          "shortDisplayName": "FilmReview"
        },
        "operationType": null,
        "metadata": [
          {
            "name": {
              "fullyQualifiedName": "HttpOperation",
              "parameters": [],
              "name": "HttpOperation",
              "parameterizedName": "HttpOperation",
              "namespace": "",
              "longDisplayName": "HttpOperation",
              "shortDisplayName": "HttpOperation"
            },
            "params": {
              "method": "GET",
              "url": "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}"
            }
          }
        ],
        "contract": {
          "returnType": {
            "fullyQualifiedName": "io.vyne.reviews.FilmReview",
            "parameters": [],
            "name": "FilmReview",
            "parameterizedName": "io.vyne.reviews.FilmReview",
            "namespace": "io.vyne.reviews",
            "longDisplayName": "io.vyne.reviews.FilmReview",
            "shortDisplayName": "FilmReview"
          },
          "constraints": []
        },
        "typeDoc": null,
        "returnTypeName": {
          "fullyQualifiedName": "io.vyne.reviews.FilmReview",
          "parameters": [],
          "name": "FilmReview",
          "parameterizedName": "io.vyne.reviews.FilmReview",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.FilmReview",
          "shortDisplayName": "FilmReview"
        },
        "name": "getReview",
        "memberQualifiedName": {
          "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
          "parameters": [],
          "name": "ReviewsService@@getReview",
          "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
          "shortDisplayName": "getReview"
        }
      }
    ],
    "members": [
      {
        "name": {
          "fullyQualifiedName": "NewFilmReleaseAnnouncement",
          "parameters": [],
          "name": "NewFilmReleaseAnnouncement",
          "parameterizedName": "NewFilmReleaseAnnouncement",
          "namespace": "",
          "longDisplayName": "NewFilmReleaseAnnouncement",
          "shortDisplayName": "NewFilmReleaseAnnouncement"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "NewFilmReleaseAnnouncement",
            "parameters": [],
            "name": "NewFilmReleaseAnnouncement",
            "parameterizedName": "NewFilmReleaseAnnouncement",
            "namespace": "",
            "longDisplayName": "NewFilmReleaseAnnouncement",
            "shortDisplayName": "NewFilmReleaseAnnouncement"
          },
          "attributes": {
            "filmId": {
              "type": {
                "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                "parameters": [],
                "name": "NetflixFilmId",
                "parameterizedName": "demo.netflix.NetflixFilmId",
                "namespace": "demo.netflix",
                "longDisplayName": "demo.netflix.NetflixFilmId",
                "shortDisplayName": "NetflixFilmId"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": true,
              "typeDisplayName": "demo.netflix.NetflixFilmId",
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "lang.taxi.formats.ProtobufField",
                    "parameters": [],
                    "name": "ProtobufField",
                    "parameterizedName": "lang.taxi.formats.ProtobufField",
                    "namespace": "lang.taxi.formats",
                    "longDisplayName": "lang.taxi.formats.ProtobufField",
                    "shortDisplayName": "ProtobufField"
                  },
                  "params": {
                    "tag": 1,
                    "protoType": "int32"
                  }
                },
                {
                  "name": {
                    "fullyQualifiedName": "Id",
                    "parameters": [],
                    "name": "Id",
                    "parameterizedName": "Id",
                    "namespace": "",
                    "longDisplayName": "Id",
                    "shortDisplayName": "Id"
                  },
                  "params": {}
                }
              ],
              "sourcedBy": null,
              "format": null
            },
            "announcement": {
              "type": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "longDisplayName": "lang.taxi.String",
                "shortDisplayName": "String"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": true,
              "typeDisplayName": "lang.taxi.String",
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "lang.taxi.formats.ProtobufField",
                    "parameters": [],
                    "name": "ProtobufField",
                    "parameterizedName": "lang.taxi.formats.ProtobufField",
                    "namespace": "lang.taxi.formats",
                    "longDisplayName": "lang.taxi.formats.ProtobufField",
                    "shortDisplayName": "ProtobufField"
                  },
                  "params": {
                    "tag": 2,
                    "protoType": "string"
                  }
                }
              ],
              "sourcedBy": null,
              "format": null
            }
          },
          "modifiers": [],
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "lang.taxi.formats.ProtobufMessage",
                "parameters": [],
                "name": "ProtobufMessage",
                "parameterizedName": "lang.taxi.formats.ProtobufMessage",
                "namespace": "lang.taxi.formats",
                "longDisplayName": "lang.taxi.formats.ProtobufMessage",
                "shortDisplayName": "ProtobufMessage"
              },
              "params": {
                "packageName": "",
                "messageName": "NewFilmReleaseAnnouncement"
              }
            }
          ],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/NewFilmReleaseAnnouncement.taxi",
              "version": "0.0.0",
              "content": "import demo.netflix.NetflixFilmId\n@lang.taxi.formats.ProtobufMessage(packageName = \"\" , messageName = \"NewFilmReleaseAnnouncement\")\nmodel NewFilmReleaseAnnouncement {\n   @lang.taxi.formats.ProtobufField(tag = 1 , protoType = \"int32\") @Id filmId : NetflixFilmId?\n   @lang.taxi.formats.ProtobufField(tag = 2 , protoType = \"string\") announcement : String?\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/NewFilmReleaseAnnouncement.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/NewFilmReleaseAnnouncement.taxi:0.0.0",
              "contentHash": "7c672c"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "NewFilmReleaseAnnouncement",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "NewFilmReleaseAnnouncement",
          "fullyQualifiedName": "NewFilmReleaseAnnouncement",
          "memberQualifiedName": {
            "fullyQualifiedName": "NewFilmReleaseAnnouncement",
            "parameters": [],
            "name": "NewFilmReleaseAnnouncement",
            "parameterizedName": "NewFilmReleaseAnnouncement",
            "namespace": "",
            "longDisplayName": "NewFilmReleaseAnnouncement",
            "shortDisplayName": "NewFilmReleaseAnnouncement"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": false
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/NewFilmReleaseAnnouncement.taxi",
            "version": "0.0.0",
            "content": "import demo.netflix.NetflixFilmId\n@lang.taxi.formats.ProtobufMessage(packageName = \"\" , messageName = \"NewFilmReleaseAnnouncement\")\nmodel NewFilmReleaseAnnouncement {\n   @lang.taxi.formats.ProtobufField(tag = 1 , protoType = \"int32\") @Id filmId : NetflixFilmId?\n   @lang.taxi.formats.ProtobufField(tag = 2 , protoType = \"string\") announcement : String?\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/NewFilmReleaseAnnouncement.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/NewFilmReleaseAnnouncement.taxi:0.0.0",
            "contentHash": "7c672c"
          }
        ],
        "typeDoc": "",
        "attributeNames": [
          "filmId",
          "announcement"
        ]
      },
      {
        "name": {
          "fullyQualifiedName": "address.types.AddressId",
          "parameters": [],
          "name": "AddressId",
          "parameterizedName": "address.types.AddressId",
          "namespace": "address.types",
          "longDisplayName": "address.types.AddressId",
          "shortDisplayName": "AddressId"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "address.types.AddressId",
            "parameters": [],
            "name": "AddressId",
            "parameterizedName": "address.types.AddressId",
            "namespace": "address.types",
            "longDisplayName": "address.types.AddressId",
            "shortDisplayName": "AddressId"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Int",
              "parameters": [],
              "name": "Int",
              "parameterizedName": "lang.taxi.Int",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Int",
              "shortDisplayName": "Int"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/address/types/AddressId.taxi",
              "version": "0.0.0",
              "content": "namespace address.types {\n   type AddressId inherits Int\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/address/types/AddressId.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/address/types/AddressId.taxi:0.0.0",
              "contentHash": "87062e"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "address.types.AddressId",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "address.types.AddressId",
          "fullyQualifiedName": "address.types.AddressId",
          "memberQualifiedName": {
            "fullyQualifiedName": "address.types.AddressId",
            "parameters": [],
            "name": "AddressId",
            "parameterizedName": "address.types.AddressId",
            "namespace": "address.types",
            "longDisplayName": "address.types.AddressId",
            "shortDisplayName": "AddressId"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/address/types/AddressId.taxi",
            "version": "0.0.0",
            "content": "namespace address.types {\n   type AddressId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/address/types/AddressId.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/address/types/AddressId.taxi:0.0.0",
            "contentHash": "87062e"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "demo.netflix.NetflixFilmId",
          "parameters": [],
          "name": "NetflixFilmId",
          "parameterizedName": "demo.netflix.NetflixFilmId",
          "namespace": "demo.netflix",
          "longDisplayName": "demo.netflix.NetflixFilmId",
          "shortDisplayName": "NetflixFilmId"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "demo.netflix.NetflixFilmId",
            "parameters": [],
            "name": "NetflixFilmId",
            "parameterizedName": "demo.netflix.NetflixFilmId",
            "namespace": "demo.netflix",
            "longDisplayName": "demo.netflix.NetflixFilmId",
            "shortDisplayName": "NetflixFilmId"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Int",
              "parameters": [],
              "name": "Int",
              "parameterizedName": "lang.taxi.Int",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Int",
              "shortDisplayName": "Int"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/netflix/netflix-types.taxi",
              "version": "0.0.0",
              "content": "namespace demo.netflix {\n   type NetflixFilmId inherits Int\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/netflix/netflix-types.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/netflix/netflix-types.taxi:0.0.0",
              "contentHash": "086c9e"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "demo.netflix.NetflixFilmId",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "demo.netflix.NetflixFilmId",
          "fullyQualifiedName": "demo.netflix.NetflixFilmId",
          "memberQualifiedName": {
            "fullyQualifiedName": "demo.netflix.NetflixFilmId",
            "parameters": [],
            "name": "NetflixFilmId",
            "parameterizedName": "demo.netflix.NetflixFilmId",
            "namespace": "demo.netflix",
            "longDisplayName": "demo.netflix.NetflixFilmId",
            "shortDisplayName": "NetflixFilmId"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/netflix/netflix-types.taxi",
            "version": "0.0.0",
            "content": "namespace demo.netflix {\n   type NetflixFilmId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/netflix/netflix-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/netflix/netflix-types.taxi:0.0.0",
            "contentHash": "086c9e"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.Film",
          "parameters": [],
          "name": "Film",
          "parameterizedName": "film.Film",
          "namespace": "film",
          "longDisplayName": "film.Film",
          "shortDisplayName": "Film"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.Film",
            "parameters": [],
            "name": "Film",
            "parameterizedName": "film.Film",
            "namespace": "film",
            "longDisplayName": "film.Film",
            "shortDisplayName": "Film"
          },
          "attributes": {
            "film_id": {
              "type": {
                "fullyQualifiedName": "films.FilmId",
                "parameters": [],
                "name": "FilmId",
                "parameterizedName": "films.FilmId",
                "namespace": "films",
                "longDisplayName": "films.FilmId",
                "shortDisplayName": "FilmId"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "films.FilmId",
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "Id",
                    "parameters": [],
                    "name": "Id",
                    "parameterizedName": "Id",
                    "namespace": "",
                    "longDisplayName": "Id",
                    "shortDisplayName": "Id"
                  },
                  "params": {}
                }
              ],
              "sourcedBy": null,
              "format": null
            },
            "title": {
              "type": {
                "fullyQualifiedName": "film.types.Title",
                "parameters": [],
                "name": "Title",
                "parameterizedName": "film.types.Title",
                "namespace": "film.types",
                "longDisplayName": "film.types.Title",
                "shortDisplayName": "Title"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "film.types.Title",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "description": {
              "type": {
                "fullyQualifiedName": "film.types.Description",
                "parameters": [],
                "name": "Description",
                "parameterizedName": "film.types.Description",
                "namespace": "film.types",
                "longDisplayName": "film.types.Description",
                "shortDisplayName": "Description"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": true,
              "typeDisplayName": "film.types.Description",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "release_year": {
              "type": {
                "fullyQualifiedName": "film.types.ReleaseYear",
                "parameters": [],
                "name": "ReleaseYear",
                "parameterizedName": "film.types.ReleaseYear",
                "namespace": "film.types",
                "longDisplayName": "film.types.ReleaseYear",
                "shortDisplayName": "ReleaseYear"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": true,
              "typeDisplayName": "film.types.ReleaseYear",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "language_id": {
              "type": {
                "fullyQualifiedName": "language.types.LanguageId",
                "parameters": [],
                "name": "LanguageId",
                "parameterizedName": "language.types.LanguageId",
                "namespace": "language.types",
                "longDisplayName": "language.types.LanguageId",
                "shortDisplayName": "LanguageId"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "language.types.LanguageId",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "original_language_id": {
              "type": {
                "fullyQualifiedName": "language.types.LanguageId",
                "parameters": [],
                "name": "LanguageId",
                "parameterizedName": "language.types.LanguageId",
                "namespace": "language.types",
                "longDisplayName": "language.types.LanguageId",
                "shortDisplayName": "LanguageId"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": true,
              "typeDisplayName": "language.types.LanguageId",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "rental_duration": {
              "type": {
                "fullyQualifiedName": "film.types.RentalDuration",
                "parameters": [],
                "name": "RentalDuration",
                "parameterizedName": "film.types.RentalDuration",
                "namespace": "film.types",
                "longDisplayName": "film.types.RentalDuration",
                "shortDisplayName": "RentalDuration"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "film.types.RentalDuration",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "rental_rate": {
              "type": {
                "fullyQualifiedName": "film.types.RentalRate",
                "parameters": [],
                "name": "RentalRate",
                "parameterizedName": "film.types.RentalRate",
                "namespace": "film.types",
                "longDisplayName": "film.types.RentalRate",
                "shortDisplayName": "RentalRate"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "film.types.RentalRate",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "length": {
              "type": {
                "fullyQualifiedName": "film.types.Length",
                "parameters": [],
                "name": "Length",
                "parameterizedName": "film.types.Length",
                "namespace": "film.types",
                "longDisplayName": "film.types.Length",
                "shortDisplayName": "Length"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": true,
              "typeDisplayName": "film.types.Length",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "replacement_cost": {
              "type": {
                "fullyQualifiedName": "film.types.ReplacementCost",
                "parameters": [],
                "name": "ReplacementCost",
                "parameterizedName": "film.types.ReplacementCost",
                "namespace": "film.types",
                "longDisplayName": "film.types.ReplacementCost",
                "shortDisplayName": "ReplacementCost"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "film.types.ReplacementCost",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "rating": {
              "type": {
                "fullyQualifiedName": "film.types.Rating",
                "parameters": [],
                "name": "Rating",
                "parameterizedName": "film.types.Rating",
                "namespace": "film.types",
                "longDisplayName": "film.types.Rating",
                "shortDisplayName": "Rating"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": true,
              "typeDisplayName": "film.types.Rating",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "last_update": {
              "type": {
                "fullyQualifiedName": "film.types.LastUpdate",
                "parameters": [],
                "name": "LastUpdate",
                "parameterizedName": "film.types.LastUpdate",
                "namespace": "film.types",
                "longDisplayName": "film.types.LastUpdate",
                "shortDisplayName": "LastUpdate"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "film.types.LastUpdate",
              "metadata": [],
              "sourcedBy": null,
              "format": {
                "patterns": [
                  "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
                ],
                "utcZoneOffsetInMinutes": null,
                "definesPattern": true,
                "isEmpty": false
              }
            },
            "special_features": {
              "type": {
                "fullyQualifiedName": "lang.taxi.Array",
                "parameters": [
                  {
                    "fullyQualifiedName": "film.types.SpecialFeatures",
                    "parameters": [],
                    "name": "SpecialFeatures",
                    "parameterizedName": "film.types.SpecialFeatures",
                    "namespace": "film.types",
                    "longDisplayName": "film.types.SpecialFeatures",
                    "shortDisplayName": "SpecialFeatures"
                  }
                ],
                "name": "Array",
                "parameterizedName": "lang.taxi.Array<film.types.SpecialFeatures>",
                "namespace": "lang.taxi",
                "longDisplayName": "film.types.SpecialFeatures[]",
                "shortDisplayName": "SpecialFeatures[]"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": true,
              "typeDisplayName": "film.types.SpecialFeatures[]",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "fulltext": {
              "type": {
                "fullyQualifiedName": "film.types.Fulltext",
                "parameters": [],
                "name": "Fulltext",
                "parameterizedName": "film.types.Fulltext",
                "namespace": "film.types",
                "longDisplayName": "film.types.Fulltext",
                "shortDisplayName": "Fulltext"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "film.types.Fulltext",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            }
          },
          "modifiers": [],
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "io.vyne.jdbc.Table",
                "parameters": [],
                "name": "Table",
                "parameterizedName": "io.vyne.jdbc.Table",
                "namespace": "io.vyne.jdbc",
                "longDisplayName": "io.vyne.jdbc.Table",
                "shortDisplayName": "Table"
              },
              "params": {
                "table": "film",
                "schema": "public",
                "connection": "films"
              }
            },
            {
              "name": {
                "fullyQualifiedName": "Ann",
                "parameters": [],
                "name": "Ann",
                "parameterizedName": "Ann",
                "namespace": "",
                "longDisplayName": "Ann",
                "shortDisplayName": "Ann"
              },
              "params": {}
            },
            {
              "name": {
                "fullyQualifiedName": "io.vyne.catalog.DataOwner",
                "parameters": [],
                "name": "DataOwner",
                "parameterizedName": "io.vyne.catalog.DataOwner",
                "namespace": "io.vyne.catalog",
                "longDisplayName": "io.vyne.catalog.DataOwner",
                "shortDisplayName": "DataOwner"
              },
              "params": {
                "id": "michael.stone",
                "name": "Michael Stone"
              }
            },
            {
              "name": {
                "fullyQualifiedName": "io.vyne.jdbc.Table",
                "parameters": [],
                "name": "Table",
                "parameterizedName": "io.vyne.jdbc.Table",
                "namespace": "io.vyne.jdbc",
                "longDisplayName": "io.vyne.jdbc.Table",
                "shortDisplayName": "Table"
              },
              "params": {
                "table": "film",
                "schema": "public",
                "connection": "films"
              }
            }
          ],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.annotations.taxi",
              "version": "0.0.0",
              "content": "namespace film {\n   @io.vyne.jdbc.Table(table = \"film\", schema = \"public\", connection = \"films\")\n   @Ann\n   type extension Film {}\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.annotations.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.annotations.taxi:0.0.0",
              "contentHash": "209d3b"
            },
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.dataOwner.taxi",
              "version": "0.0.0",
              "content": "namespace film {\n   @io.vyne.catalog.DataOwner( id = \"michael.stone\" , name = \"Michael Stone\" )\n   type extension Film {}\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.dataOwner.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.dataOwner.taxi:0.0.0",
              "contentHash": "2ca56e"
            },
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.taxi",
              "version": "0.0.0",
              "content": "import films.FilmId\nimport film.types.Title\nimport film.types.Description\nimport film.types.ReleaseYear\nimport language.types.LanguageId\nimport language.types.LanguageId\nimport film.types.RentalDuration\nimport film.types.RentalRate\nimport film.types.Length\nimport film.types.ReplacementCost\nimport film.types.Rating\nimport film.types.LastUpdate\nimport film.types.Fulltext\nimport io.vyne.jdbc.Table\nimport film.types.SpecialFeatures\nnamespace film {\n   @io.vyne.jdbc.Table(table = \"film\" , schema = \"public\" , connection = \"films\")\n         model Film {\n            @Id film_id : films.FilmId\n            title : film.types.Title\n            description : film.types.Description?\n            release_year : film.types.ReleaseYear?\n            language_id : language.types.LanguageId\n            original_language_id : language.types.LanguageId?\n            rental_duration : film.types.RentalDuration\n            rental_rate : film.types.RentalRate\n            length : film.types.Length?\n            replacement_cost : film.types.ReplacementCost\n            rating : film.types.Rating?\n            last_update : film.types.LastUpdate\n            special_features : film.types.SpecialFeatures[]?\n            fulltext : film.types.Fulltext\n         }\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.taxi:0.0.0",
              "contentHash": "fc40ac"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.Film",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.Film",
          "fullyQualifiedName": "film.Film",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.Film",
            "parameters": [],
            "name": "Film",
            "parameterizedName": "film.Film",
            "namespace": "film",
            "longDisplayName": "film.Film",
            "shortDisplayName": "Film"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": false
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.annotations.taxi",
            "version": "0.0.0",
            "content": "namespace film {\n   @io.vyne.jdbc.Table(table = \"film\", schema = \"public\", connection = \"films\")\n   @Ann\n   type extension Film {}\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.annotations.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.annotations.taxi:0.0.0",
            "contentHash": "209d3b"
          },
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.dataOwner.taxi",
            "version": "0.0.0",
            "content": "namespace film {\n   @io.vyne.catalog.DataOwner( id = \"michael.stone\" , name = \"Michael Stone\" )\n   type extension Film {}\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.dataOwner.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.dataOwner.taxi:0.0.0",
            "contentHash": "2ca56e"
          },
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.taxi",
            "version": "0.0.0",
            "content": "import films.FilmId\nimport film.types.Title\nimport film.types.Description\nimport film.types.ReleaseYear\nimport language.types.LanguageId\nimport language.types.LanguageId\nimport film.types.RentalDuration\nimport film.types.RentalRate\nimport film.types.Length\nimport film.types.ReplacementCost\nimport film.types.Rating\nimport film.types.LastUpdate\nimport film.types.Fulltext\nimport io.vyne.jdbc.Table\nimport film.types.SpecialFeatures\nnamespace film {\n   @io.vyne.jdbc.Table(table = \"film\" , schema = \"public\" , connection = \"films\")\n         model Film {\n            @Id film_id : films.FilmId\n            title : film.types.Title\n            description : film.types.Description?\n            release_year : film.types.ReleaseYear?\n            language_id : language.types.LanguageId\n            original_language_id : language.types.LanguageId?\n            rental_duration : film.types.RentalDuration\n            rental_rate : film.types.RentalRate\n            length : film.types.Length?\n            replacement_cost : film.types.ReplacementCost\n            rating : film.types.Rating?\n            last_update : film.types.LastUpdate\n            special_features : film.types.SpecialFeatures[]?\n            fulltext : film.types.Fulltext\n         }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/Film.taxi:0.0.0",
            "contentHash": "fc40ac"
          }
        ],
        "typeDoc": "",
        "attributeNames": [
          "film_id",
          "title",
          "description",
          "release_year",
          "language_id",
          "original_language_id",
          "rental_duration",
          "rental_rate",
          "length",
          "replacement_cost",
          "rating",
          "last_update",
          "special_features",
          "fulltext"
        ]
      },
      {
        "name": {
          "fullyQualifiedName": "film.FilmDatabase",
          "parameters": [],
          "name": "FilmDatabase",
          "parameterizedName": "film.FilmDatabase",
          "namespace": "film",
          "longDisplayName": "film.FilmDatabase",
          "shortDisplayName": "FilmDatabase"
        },
        "kind": "SERVICE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.FilmDatabase",
            "parameters": [],
            "name": "FilmDatabase",
            "parameterizedName": "film.FilmDatabase",
            "namespace": "film",
            "longDisplayName": "film.FilmDatabase",
            "shortDisplayName": "FilmDatabase"
          },
          "operations": [],
          "queryOperations": [],
          "streamOperations": [],
          "tableOperations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films",
                "parameters": [],
                "name": "FilmDatabase@@films",
                "parameterizedName": "film.FilmDatabase@@films",
                "namespace": "film",
                "longDisplayName": "film.FilmDatabase / films",
                "shortDisplayName": "films"
              },
              "returnType": {
                "fullyQualifiedName": "lang.taxi.Array",
                "parameters": [
                  {
                    "fullyQualifiedName": "film.Film",
                    "parameters": [],
                    "name": "Film",
                    "parameterizedName": "film.Film",
                    "namespace": "film",
                    "longDisplayName": "film.Film",
                    "shortDisplayName": "Film"
                  }
                ],
                "name": "Array",
                "parameterizedName": "lang.taxi.Array<film.Film>",
                "namespace": "lang.taxi",
                "longDisplayName": "film.Film[]",
                "shortDisplayName": "Film[]"
              },
              "metadata": [],
              "typeDoc": null,
              "parameters": [],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "lang.taxi.Array",
                  "parameters": [
                    {
                      "fullyQualifiedName": "film.Film",
                      "parameters": [],
                      "name": "Film",
                      "parameterizedName": "film.Film",
                      "namespace": "film",
                      "longDisplayName": "film.Film",
                      "shortDisplayName": "Film"
                    }
                  ],
                  "name": "Array",
                  "parameterizedName": "lang.taxi.Array<film.Film>",
                  "namespace": "lang.taxi",
                  "longDisplayName": "film.Film[]",
                  "shortDisplayName": "Film[]"
                },
                "constraints": []
              },
              "operationType": null,
              "returnTypeName": {
                "fullyQualifiedName": "lang.taxi.Array",
                "parameters": [
                  {
                    "fullyQualifiedName": "film.Film",
                    "parameters": [],
                    "name": "Film",
                    "parameterizedName": "film.Film",
                    "namespace": "film",
                    "longDisplayName": "film.Film",
                    "shortDisplayName": "Film"
                  }
                ],
                "name": "Array",
                "parameterizedName": "lang.taxi.Array<film.Film>",
                "namespace": "lang.taxi",
                "longDisplayName": "film.Film[]",
                "shortDisplayName": "Film[]"
              },
              "name": "films",
              "memberQualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films",
                "parameters": [],
                "name": "FilmDatabase@@films",
                "parameterizedName": "film.FilmDatabase@@films",
                "namespace": "film",
                "longDisplayName": "film.FilmDatabase / films",
                "shortDisplayName": "films"
              }
            }
          ],
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
                "parameters": [],
                "name": "DatabaseService",
                "parameterizedName": "io.vyne.jdbc.DatabaseService",
                "namespace": "io.vyne.jdbc",
                "longDisplayName": "io.vyne.jdbc.DatabaseService",
                "shortDisplayName": "DatabaseService"
              },
              "params": {
                "connection": "films"
              }
            }
          ],
          "sourceCode": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/FilmService.taxi",
              "version": "0.0.0",
              "content": "namespace film {\n   @io.vyne.jdbc.DatabaseService(connection = \"films\")\n         service FilmDatabase {\n            table films : Film[]\n         }\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/FilmService.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/FilmService.taxi:0.0.0",
              "contentHash": "e12e52"
            }
          ],
          "typeDoc": null,
          "lineage": null,
          "serviceKind": "Database",
          "remoteOperations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films_FindOne",
                "parameters": [],
                "name": "FilmDatabase@@films_FindOne",
                "parameterizedName": "film.FilmDatabase@@films_FindOne",
                "namespace": "film",
                "longDisplayName": "film.FilmDatabase / films_FindOne",
                "shortDisplayName": "films_FindOne"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                    "parameters": [],
                    "name": "VyneQlQuery",
                    "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                    "namespace": "vyne.vyneQl",
                    "longDisplayName": "vyne.vyneQl.VyneQlQuery",
                    "shortDisplayName": "VyneQlQuery"
                  },
                  "name": "body",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                    "parameters": [],
                    "name": "VyneQlQuery",
                    "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                    "namespace": "vyne.vyneQl",
                    "longDisplayName": "vyne.vyneQl.VyneQlQuery",
                    "shortDisplayName": "VyneQlQuery"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "film.Film",
                "parameters": [],
                "name": "Film",
                "parameterizedName": "film.Film",
                "namespace": "film",
                "longDisplayName": "film.Film",
                "shortDisplayName": "Film"
              },
              "metadata": [],
              "grammar": "vyneQl",
              "capabilities": [
                "SUM",
                "COUNT",
                "AVG",
                "MIN",
                "MAX",
                {
                  "supportedOperations": [
                    "EQUAL",
                    "NOT_EQUAL",
                    "IN",
                    "LIKE",
                    "GREATER_THAN",
                    "LESS_THAN",
                    "GREATER_THAN_OR_EQUAL_TO",
                    "LESS_THAN_OR_EQUAL_TO"
                  ]
                }
              ],
              "typeDoc": null,
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "film.Film",
                  "parameters": [],
                  "name": "Film",
                  "parameterizedName": "film.Film",
                  "namespace": "film",
                  "longDisplayName": "film.Film",
                  "shortDisplayName": "Film"
                },
                "constraints": []
              },
              "operationType": null,
              "hasFilterCapability": true,
              "supportedFilterOperations": [
                "EQUAL",
                "NOT_EQUAL",
                "IN",
                "LIKE",
                "GREATER_THAN",
                "LESS_THAN",
                "GREATER_THAN_OR_EQUAL_TO",
                "LESS_THAN_OR_EQUAL_TO"
              ],
              "returnTypeName": {
                "fullyQualifiedName": "film.Film",
                "parameters": [],
                "name": "Film",
                "parameterizedName": "film.Film",
                "namespace": "film",
                "longDisplayName": "film.Film",
                "shortDisplayName": "Film"
              },
              "name": "films_FindOne",
              "memberQualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films_FindOne",
                "parameters": [],
                "name": "FilmDatabase@@films_FindOne",
                "parameterizedName": "film.FilmDatabase@@films_FindOne",
                "namespace": "film",
                "longDisplayName": "film.FilmDatabase / films_FindOne",
                "shortDisplayName": "films_FindOne"
              }
            },
            {
              "qualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films_FindMany",
                "parameters": [],
                "name": "FilmDatabase@@films_FindMany",
                "parameterizedName": "film.FilmDatabase@@films_FindMany",
                "namespace": "film",
                "longDisplayName": "film.FilmDatabase / films_FindMany",
                "shortDisplayName": "films_FindMany"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                    "parameters": [],
                    "name": "VyneQlQuery",
                    "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                    "namespace": "vyne.vyneQl",
                    "longDisplayName": "vyne.vyneQl.VyneQlQuery",
                    "shortDisplayName": "VyneQlQuery"
                  },
                  "name": "body",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                    "parameters": [],
                    "name": "VyneQlQuery",
                    "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                    "namespace": "vyne.vyneQl",
                    "longDisplayName": "vyne.vyneQl.VyneQlQuery",
                    "shortDisplayName": "VyneQlQuery"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "lang.taxi.Array",
                "parameters": [
                  {
                    "fullyQualifiedName": "film.Film",
                    "parameters": [],
                    "name": "Film",
                    "parameterizedName": "film.Film",
                    "namespace": "film",
                    "longDisplayName": "film.Film",
                    "shortDisplayName": "Film"
                  }
                ],
                "name": "Array",
                "parameterizedName": "lang.taxi.Array<film.Film>",
                "namespace": "lang.taxi",
                "longDisplayName": "film.Film[]",
                "shortDisplayName": "Film[]"
              },
              "metadata": [],
              "grammar": "vyneQl",
              "capabilities": [
                "SUM",
                "COUNT",
                "AVG",
                "MIN",
                "MAX",
                {
                  "supportedOperations": [
                    "EQUAL",
                    "NOT_EQUAL",
                    "IN",
                    "LIKE",
                    "GREATER_THAN",
                    "LESS_THAN",
                    "GREATER_THAN_OR_EQUAL_TO",
                    "LESS_THAN_OR_EQUAL_TO"
                  ]
                }
              ],
              "typeDoc": null,
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "lang.taxi.Array",
                  "parameters": [
                    {
                      "fullyQualifiedName": "film.Film",
                      "parameters": [],
                      "name": "Film",
                      "parameterizedName": "film.Film",
                      "namespace": "film",
                      "longDisplayName": "film.Film",
                      "shortDisplayName": "Film"
                    }
                  ],
                  "name": "Array",
                  "parameterizedName": "lang.taxi.Array<film.Film>",
                  "namespace": "lang.taxi",
                  "longDisplayName": "film.Film[]",
                  "shortDisplayName": "Film[]"
                },
                "constraints": []
              },
              "operationType": null,
              "hasFilterCapability": true,
              "supportedFilterOperations": [
                "EQUAL",
                "NOT_EQUAL",
                "IN",
                "LIKE",
                "GREATER_THAN",
                "LESS_THAN",
                "GREATER_THAN_OR_EQUAL_TO",
                "LESS_THAN_OR_EQUAL_TO"
              ],
              "returnTypeName": {
                "fullyQualifiedName": "lang.taxi.Array",
                "parameters": [
                  {
                    "fullyQualifiedName": "film.Film",
                    "parameters": [],
                    "name": "Film",
                    "parameterizedName": "film.Film",
                    "namespace": "film",
                    "longDisplayName": "film.Film",
                    "shortDisplayName": "Film"
                  }
                ],
                "name": "Array",
                "parameterizedName": "lang.taxi.Array<film.Film>",
                "namespace": "lang.taxi",
                "longDisplayName": "film.Film[]",
                "shortDisplayName": "Film[]"
              },
              "name": "films_FindMany",
              "memberQualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films_FindMany",
                "parameters": [],
                "name": "FilmDatabase@@films_FindMany",
                "parameterizedName": "film.FilmDatabase@@films_FindMany",
                "namespace": "film",
                "longDisplayName": "film.FilmDatabase / films_FindMany",
                "shortDisplayName": "films_FindMany"
              }
            }
          ],
          "qualifiedName": "film.FilmDatabase",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.FilmDatabase",
            "parameters": [],
            "name": "FilmDatabase",
            "parameterizedName": "film.FilmDatabase",
            "namespace": "film",
            "longDisplayName": "film.FilmDatabase",
            "shortDisplayName": "FilmDatabase"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Description",
          "parameters": [],
          "name": "Description",
          "parameterizedName": "film.types.Description",
          "namespace": "film.types",
          "longDisplayName": "film.types.Description",
          "shortDisplayName": "Description"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.Description",
            "parameters": [],
            "name": "Description",
            "parameterizedName": "film.types.Description",
            "namespace": "film.types",
            "longDisplayName": "film.types.Description",
            "shortDisplayName": "Description"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Description.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type Description inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Description.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Description.taxi:0.0.0",
              "contentHash": "ec3849"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.Description",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.Description",
          "fullyQualifiedName": "film.types.Description",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.Description",
            "parameters": [],
            "name": "Description",
            "parameterizedName": "film.types.Description",
            "namespace": "film.types",
            "longDisplayName": "film.types.Description",
            "shortDisplayName": "Description"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Description.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Description inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Description.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Description.taxi:0.0.0",
            "contentHash": "ec3849"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.FilmId",
          "parameters": [],
          "name": "FilmId",
          "parameterizedName": "film.types.FilmId",
          "namespace": "film.types",
          "longDisplayName": "film.types.FilmId",
          "shortDisplayName": "FilmId"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.FilmId",
            "parameters": [],
            "name": "FilmId",
            "parameterizedName": "film.types.FilmId",
            "namespace": "film.types",
            "longDisplayName": "film.types.FilmId",
            "shortDisplayName": "FilmId"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Int",
              "parameters": [],
              "name": "Int",
              "parameterizedName": "lang.taxi.Int",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Int",
              "shortDisplayName": "Int"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/FilmId.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type FilmId inherits Int\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/FilmId.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/FilmId.taxi:0.0.0",
              "contentHash": "a0b9f0"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.FilmId",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.FilmId",
          "fullyQualifiedName": "film.types.FilmId",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.FilmId",
            "parameters": [],
            "name": "FilmId",
            "parameterizedName": "film.types.FilmId",
            "namespace": "film.types",
            "longDisplayName": "film.types.FilmId",
            "shortDisplayName": "FilmId"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/FilmId.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type FilmId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/FilmId.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/FilmId.taxi:0.0.0",
            "contentHash": "a0b9f0"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Fulltext",
          "parameters": [],
          "name": "Fulltext",
          "parameterizedName": "film.types.Fulltext",
          "namespace": "film.types",
          "longDisplayName": "film.types.Fulltext",
          "shortDisplayName": "Fulltext"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.Fulltext",
            "parameters": [],
            "name": "Fulltext",
            "parameterizedName": "film.types.Fulltext",
            "namespace": "film.types",
            "longDisplayName": "film.types.Fulltext",
            "shortDisplayName": "Fulltext"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Any",
              "parameters": [],
              "name": "Any",
              "parameterizedName": "lang.taxi.Any",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Any",
              "shortDisplayName": "Any"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Fulltext.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type Fulltext inherits Any\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Fulltext.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Fulltext.taxi:0.0.0",
              "contentHash": "15bfd0"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.Fulltext",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Any",
            "parameters": [],
            "name": "Any",
            "parameterizedName": "lang.taxi.Any",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Any",
            "shortDisplayName": "Any"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.Fulltext",
          "fullyQualifiedName": "film.types.Fulltext",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.Fulltext",
            "parameters": [],
            "name": "Fulltext",
            "parameterizedName": "film.types.Fulltext",
            "namespace": "film.types",
            "longDisplayName": "film.types.Fulltext",
            "shortDisplayName": "Fulltext"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Fulltext.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Fulltext inherits Any\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Fulltext.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Fulltext.taxi:0.0.0",
            "contentHash": "15bfd0"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.LastUpdate",
          "parameters": [],
          "name": "LastUpdate",
          "parameterizedName": "film.types.LastUpdate",
          "namespace": "film.types",
          "longDisplayName": "film.types.LastUpdate",
          "shortDisplayName": "LastUpdate"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.LastUpdate",
            "parameters": [],
            "name": "LastUpdate",
            "parameterizedName": "film.types.LastUpdate",
            "namespace": "film.types",
            "longDisplayName": "film.types.LastUpdate",
            "shortDisplayName": "LastUpdate"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Instant",
              "parameters": [],
              "name": "Instant",
              "parameterizedName": "lang.taxi.Instant",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Instant",
              "shortDisplayName": "Instant"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/LastUpdate.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type LastUpdate inherits Instant\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/LastUpdate.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/LastUpdate.taxi:0.0.0",
              "contentHash": "a4f44f"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.LastUpdate",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [
              "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
            ],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": true,
            "isEmpty": false
          },
          "offset": null,
          "format": [
            "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
          ],
          "hasFormat": true,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Instant",
            "parameters": [],
            "name": "Instant",
            "parameterizedName": "lang.taxi.Instant",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Instant",
            "shortDisplayName": "Instant"
          },
          "hasExpression": false,
          "unformattedTypeName": {
            "fullyQualifiedName": "film.types.LastUpdate",
            "parameters": [],
            "name": "LastUpdate",
            "parameterizedName": "film.types.LastUpdate",
            "namespace": "film.types",
            "longDisplayName": "film.types.LastUpdate",
            "shortDisplayName": "LastUpdate"
          },
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.LastUpdate",
          "fullyQualifiedName": "film.types.LastUpdate",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.LastUpdate",
            "parameters": [],
            "name": "LastUpdate",
            "parameterizedName": "film.types.LastUpdate",
            "namespace": "film.types",
            "longDisplayName": "film.types.LastUpdate",
            "shortDisplayName": "LastUpdate"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/LastUpdate.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type LastUpdate inherits Instant\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/LastUpdate.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/LastUpdate.taxi:0.0.0",
            "contentHash": "a4f44f"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Length",
          "parameters": [],
          "name": "Length",
          "parameterizedName": "film.types.Length",
          "namespace": "film.types",
          "longDisplayName": "film.types.Length",
          "shortDisplayName": "Length"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.Length",
            "parameters": [],
            "name": "Length",
            "parameterizedName": "film.types.Length",
            "namespace": "film.types",
            "longDisplayName": "film.types.Length",
            "shortDisplayName": "Length"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Int",
              "parameters": [],
              "name": "Int",
              "parameterizedName": "lang.taxi.Int",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Int",
              "shortDisplayName": "Int"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Length.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type Length inherits Int\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Length.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Length.taxi:0.0.0",
              "contentHash": "651a25"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.Length",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.Length",
          "fullyQualifiedName": "film.types.Length",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.Length",
            "parameters": [],
            "name": "Length",
            "parameterizedName": "film.types.Length",
            "namespace": "film.types",
            "longDisplayName": "film.types.Length",
            "shortDisplayName": "Length"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Length.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Length inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Length.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Length.taxi:0.0.0",
            "contentHash": "651a25"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Rating",
          "parameters": [],
          "name": "Rating",
          "parameterizedName": "film.types.Rating",
          "namespace": "film.types",
          "longDisplayName": "film.types.Rating",
          "shortDisplayName": "Rating"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.Rating",
            "parameters": [],
            "name": "Rating",
            "parameterizedName": "film.types.Rating",
            "namespace": "film.types",
            "longDisplayName": "film.types.Rating",
            "shortDisplayName": "Rating"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Rating.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type Rating inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Rating.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Rating.taxi:0.0.0",
              "contentHash": "5f228f"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.Rating",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.Rating",
          "fullyQualifiedName": "film.types.Rating",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.Rating",
            "parameters": [],
            "name": "Rating",
            "parameterizedName": "film.types.Rating",
            "namespace": "film.types",
            "longDisplayName": "film.types.Rating",
            "shortDisplayName": "Rating"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Rating.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Rating inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Rating.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Rating.taxi:0.0.0",
            "contentHash": "5f228f"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.ReleaseYear",
          "parameters": [],
          "name": "ReleaseYear",
          "parameterizedName": "film.types.ReleaseYear",
          "namespace": "film.types",
          "longDisplayName": "film.types.ReleaseYear",
          "shortDisplayName": "ReleaseYear"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.ReleaseYear",
            "parameters": [],
            "name": "ReleaseYear",
            "parameterizedName": "film.types.ReleaseYear",
            "namespace": "film.types",
            "longDisplayName": "film.types.ReleaseYear",
            "shortDisplayName": "ReleaseYear"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Any",
              "parameters": [],
              "name": "Any",
              "parameterizedName": "lang.taxi.Any",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Any",
              "shortDisplayName": "Any"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReleaseYear.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type ReleaseYear inherits Any\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReleaseYear.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReleaseYear.taxi:0.0.0",
              "contentHash": "49725b"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.ReleaseYear",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Any",
            "parameters": [],
            "name": "Any",
            "parameterizedName": "lang.taxi.Any",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Any",
            "shortDisplayName": "Any"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.ReleaseYear",
          "fullyQualifiedName": "film.types.ReleaseYear",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.ReleaseYear",
            "parameters": [],
            "name": "ReleaseYear",
            "parameterizedName": "film.types.ReleaseYear",
            "namespace": "film.types",
            "longDisplayName": "film.types.ReleaseYear",
            "shortDisplayName": "ReleaseYear"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReleaseYear.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type ReleaseYear inherits Any\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReleaseYear.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReleaseYear.taxi:0.0.0",
            "contentHash": "49725b"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.RentalDuration",
          "parameters": [],
          "name": "RentalDuration",
          "parameterizedName": "film.types.RentalDuration",
          "namespace": "film.types",
          "longDisplayName": "film.types.RentalDuration",
          "shortDisplayName": "RentalDuration"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.RentalDuration",
            "parameters": [],
            "name": "RentalDuration",
            "parameterizedName": "film.types.RentalDuration",
            "namespace": "film.types",
            "longDisplayName": "film.types.RentalDuration",
            "shortDisplayName": "RentalDuration"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Int",
              "parameters": [],
              "name": "Int",
              "parameterizedName": "lang.taxi.Int",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Int",
              "shortDisplayName": "Int"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalDuration.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type RentalDuration inherits Int\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalDuration.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalDuration.taxi:0.0.0",
              "contentHash": "584e8a"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.RentalDuration",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.RentalDuration",
          "fullyQualifiedName": "film.types.RentalDuration",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.RentalDuration",
            "parameters": [],
            "name": "RentalDuration",
            "parameterizedName": "film.types.RentalDuration",
            "namespace": "film.types",
            "longDisplayName": "film.types.RentalDuration",
            "shortDisplayName": "RentalDuration"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalDuration.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type RentalDuration inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalDuration.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalDuration.taxi:0.0.0",
            "contentHash": "584e8a"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.RentalRate",
          "parameters": [],
          "name": "RentalRate",
          "parameterizedName": "film.types.RentalRate",
          "namespace": "film.types",
          "longDisplayName": "film.types.RentalRate",
          "shortDisplayName": "RentalRate"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.RentalRate",
            "parameters": [],
            "name": "RentalRate",
            "parameterizedName": "film.types.RentalRate",
            "namespace": "film.types",
            "longDisplayName": "film.types.RentalRate",
            "shortDisplayName": "RentalRate"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Decimal",
              "parameters": [],
              "name": "Decimal",
              "parameterizedName": "lang.taxi.Decimal",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Decimal",
              "shortDisplayName": "Decimal"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalRate.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type RentalRate inherits Decimal\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalRate.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalRate.taxi:0.0.0",
              "contentHash": "e1cc8c"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.RentalRate",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.RentalRate",
          "fullyQualifiedName": "film.types.RentalRate",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.RentalRate",
            "parameters": [],
            "name": "RentalRate",
            "parameterizedName": "film.types.RentalRate",
            "namespace": "film.types",
            "longDisplayName": "film.types.RentalRate",
            "shortDisplayName": "RentalRate"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalRate.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type RentalRate inherits Decimal\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalRate.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/RentalRate.taxi:0.0.0",
            "contentHash": "e1cc8c"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.ReplacementCost",
          "parameters": [],
          "name": "ReplacementCost",
          "parameterizedName": "film.types.ReplacementCost",
          "namespace": "film.types",
          "longDisplayName": "film.types.ReplacementCost",
          "shortDisplayName": "ReplacementCost"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.ReplacementCost",
            "parameters": [],
            "name": "ReplacementCost",
            "parameterizedName": "film.types.ReplacementCost",
            "namespace": "film.types",
            "longDisplayName": "film.types.ReplacementCost",
            "shortDisplayName": "ReplacementCost"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Decimal",
              "parameters": [],
              "name": "Decimal",
              "parameterizedName": "lang.taxi.Decimal",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Decimal",
              "shortDisplayName": "Decimal"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReplacementCost.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type ReplacementCost inherits Decimal\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReplacementCost.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReplacementCost.taxi:0.0.0",
              "contentHash": "f71714"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.ReplacementCost",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.ReplacementCost",
          "fullyQualifiedName": "film.types.ReplacementCost",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.ReplacementCost",
            "parameters": [],
            "name": "ReplacementCost",
            "parameterizedName": "film.types.ReplacementCost",
            "namespace": "film.types",
            "longDisplayName": "film.types.ReplacementCost",
            "shortDisplayName": "ReplacementCost"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReplacementCost.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type ReplacementCost inherits Decimal\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReplacementCost.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/ReplacementCost.taxi:0.0.0",
            "contentHash": "f71714"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.SpecialFeatures",
          "parameters": [],
          "name": "SpecialFeatures",
          "parameterizedName": "film.types.SpecialFeatures",
          "namespace": "film.types",
          "longDisplayName": "film.types.SpecialFeatures",
          "shortDisplayName": "SpecialFeatures"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.SpecialFeatures",
            "parameters": [],
            "name": "SpecialFeatures",
            "parameterizedName": "film.types.SpecialFeatures",
            "namespace": "film.types",
            "longDisplayName": "film.types.SpecialFeatures",
            "shortDisplayName": "SpecialFeatures"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/SpecialFeatures.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type SpecialFeatures inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/SpecialFeatures.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/SpecialFeatures.taxi:0.0.0",
              "contentHash": "2c1071"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.SpecialFeatures",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.SpecialFeatures",
          "fullyQualifiedName": "film.types.SpecialFeatures",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.SpecialFeatures",
            "parameters": [],
            "name": "SpecialFeatures",
            "parameterizedName": "film.types.SpecialFeatures",
            "namespace": "film.types",
            "longDisplayName": "film.types.SpecialFeatures",
            "shortDisplayName": "SpecialFeatures"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/SpecialFeatures.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type SpecialFeatures inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/SpecialFeatures.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/SpecialFeatures.taxi:0.0.0",
            "contentHash": "2c1071"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "film.types.Title",
          "parameters": [],
          "name": "Title",
          "parameterizedName": "film.types.Title",
          "namespace": "film.types",
          "longDisplayName": "film.types.Title",
          "shortDisplayName": "Title"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "film.types.Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "film.types.Title",
            "namespace": "film.types",
            "longDisplayName": "film.types.Title",
            "shortDisplayName": "Title"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Title.taxi",
              "version": "0.0.0",
              "content": "namespace film.types {\n   type Title inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Title.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Title.taxi:0.0.0",
              "contentHash": "af88ac"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "film.types.Title",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "film.types.Title",
          "fullyQualifiedName": "film.types.Title",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.types.Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "film.types.Title",
            "namespace": "film.types",
            "longDisplayName": "film.types.Title",
            "shortDisplayName": "Title"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Title.taxi",
            "version": "0.0.0",
            "content": "namespace film.types {\n   type Title inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Title.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/film/types/Title.taxi:0.0.0",
            "contentHash": "af88ac"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "films.FilmId",
          "parameters": [],
          "name": "FilmId",
          "parameterizedName": "films.FilmId",
          "namespace": "films",
          "longDisplayName": "films.FilmId",
          "shortDisplayName": "FilmId"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "films.FilmId",
            "parameters": [],
            "name": "FilmId",
            "parameterizedName": "films.FilmId",
            "namespace": "films",
            "longDisplayName": "films.FilmId",
            "shortDisplayName": "FilmId"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Int",
              "parameters": [],
              "name": "Int",
              "parameterizedName": "lang.taxi.Int",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Int",
              "shortDisplayName": "Int"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
              "version": "0.0.0",
              "content": "namespace films {\n   type FilmId inherits Int\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi:0.0.0",
              "contentHash": "b5803c"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "films.FilmId",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "films.FilmId",
          "fullyQualifiedName": "films.FilmId",
          "memberQualifiedName": {
            "fullyQualifiedName": "films.FilmId",
            "parameters": [],
            "name": "FilmId",
            "parameterizedName": "films.FilmId",
            "namespace": "films",
            "longDisplayName": "films.FilmId",
            "shortDisplayName": "FilmId"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "version": "0.0.0",
            "content": "namespace films {\n   type FilmId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi:0.0.0",
            "contentHash": "b5803c"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "films.StreamingProviderName",
          "parameters": [],
          "name": "StreamingProviderName",
          "parameterizedName": "films.StreamingProviderName",
          "namespace": "films",
          "longDisplayName": "films.StreamingProviderName",
          "shortDisplayName": "StreamingProviderName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "films.StreamingProviderName",
            "parameters": [],
            "name": "StreamingProviderName",
            "parameterizedName": "films.StreamingProviderName",
            "namespace": "films",
            "longDisplayName": "films.StreamingProviderName",
            "shortDisplayName": "StreamingProviderName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
              "version": "0.0.0",
              "content": "namespace films {\n   type StreamingProviderName inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi:0.0.0",
              "contentHash": "3454df"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "films.StreamingProviderName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "films.StreamingProviderName",
          "fullyQualifiedName": "films.StreamingProviderName",
          "memberQualifiedName": {
            "fullyQualifiedName": "films.StreamingProviderName",
            "parameters": [],
            "name": "StreamingProviderName",
            "parameterizedName": "films.StreamingProviderName",
            "namespace": "films",
            "longDisplayName": "films.StreamingProviderName",
            "shortDisplayName": "StreamingProviderName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "version": "0.0.0",
            "content": "namespace films {\n   type StreamingProviderName inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi:0.0.0",
            "contentHash": "3454df"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "films.StreamingProviderPrice",
          "parameters": [],
          "name": "StreamingProviderPrice",
          "parameterizedName": "films.StreamingProviderPrice",
          "namespace": "films",
          "longDisplayName": "films.StreamingProviderPrice",
          "shortDisplayName": "StreamingProviderPrice"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "films.StreamingProviderPrice",
            "parameters": [],
            "name": "StreamingProviderPrice",
            "parameterizedName": "films.StreamingProviderPrice",
            "namespace": "films",
            "longDisplayName": "films.StreamingProviderPrice",
            "shortDisplayName": "StreamingProviderPrice"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Decimal",
              "parameters": [],
              "name": "Decimal",
              "parameterizedName": "lang.taxi.Decimal",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Decimal",
              "shortDisplayName": "Decimal"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
              "version": "0.0.0",
              "content": "namespace films {\n   type StreamingProviderPrice inherits Decimal\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi:0.0.0",
              "contentHash": "ec7078"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "films.StreamingProviderPrice",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "films.StreamingProviderPrice",
          "fullyQualifiedName": "films.StreamingProviderPrice",
          "memberQualifiedName": {
            "fullyQualifiedName": "films.StreamingProviderPrice",
            "parameters": [],
            "name": "StreamingProviderPrice",
            "parameterizedName": "films.StreamingProviderPrice",
            "namespace": "films",
            "longDisplayName": "films.StreamingProviderPrice",
            "shortDisplayName": "StreamingProviderPrice"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "version": "0.0.0",
            "content": "namespace films {\n   type StreamingProviderPrice inherits Decimal\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/films.taxi:0.0.0",
            "contentHash": "ec7078"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "films.reviews.FilmReviewScore",
          "parameters": [],
          "name": "FilmReviewScore",
          "parameterizedName": "films.reviews.FilmReviewScore",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.FilmReviewScore",
          "shortDisplayName": "FilmReviewScore"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "films.reviews.FilmReviewScore",
            "parameters": [],
            "name": "FilmReviewScore",
            "parameterizedName": "films.reviews.FilmReviewScore",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.FilmReviewScore",
            "shortDisplayName": "FilmReviewScore"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Decimal",
              "parameters": [],
              "name": "Decimal",
              "parameterizedName": "lang.taxi.Decimal",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Decimal",
              "shortDisplayName": "Decimal"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
              "version": "0.0.0",
              "content": "namespace films.reviews {\n   type FilmReviewScore inherits Decimal\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi:0.0.0",
              "contentHash": "883e32"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "films.reviews.FilmReviewScore",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "films.reviews.FilmReviewScore",
          "fullyQualifiedName": "films.reviews.FilmReviewScore",
          "memberQualifiedName": {
            "fullyQualifiedName": "films.reviews.FilmReviewScore",
            "parameters": [],
            "name": "FilmReviewScore",
            "parameterizedName": "films.reviews.FilmReviewScore",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.FilmReviewScore",
            "shortDisplayName": "FilmReviewScore"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "version": "0.0.0",
            "content": "namespace films.reviews {\n   type FilmReviewScore inherits Decimal\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi:0.0.0",
            "contentHash": "883e32"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "films.reviews.ReviewText",
          "parameters": [],
          "name": "ReviewText",
          "parameterizedName": "films.reviews.ReviewText",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.ReviewText",
          "shortDisplayName": "ReviewText"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "films.reviews.ReviewText",
            "parameters": [],
            "name": "ReviewText",
            "parameterizedName": "films.reviews.ReviewText",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.ReviewText",
            "shortDisplayName": "ReviewText"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
              "version": "0.0.0",
              "content": "namespace films.reviews {\n   type ReviewText inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi:0.0.0",
              "contentHash": "1927dc"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "films.reviews.ReviewText",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "films.reviews.ReviewText",
          "fullyQualifiedName": "films.reviews.ReviewText",
          "memberQualifiedName": {
            "fullyQualifiedName": "films.reviews.ReviewText",
            "parameters": [],
            "name": "ReviewText",
            "parameterizedName": "films.reviews.ReviewText",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.ReviewText",
            "shortDisplayName": "ReviewText"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "version": "0.0.0",
            "content": "namespace films.reviews {\n   type ReviewText inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi:0.0.0",
            "contentHash": "1927dc"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
          "parameters": [],
          "name": "SquashedTomatoesFilmId",
          "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
          "namespace": "films.reviews",
          "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
          "shortDisplayName": "SquashedTomatoesFilmId"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
            "parameters": [],
            "name": "SquashedTomatoesFilmId",
            "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
            "shortDisplayName": "SquashedTomatoesFilmId"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
              "version": "0.0.0",
              "content": "namespace films.reviews {\n   type SquashedTomatoesFilmId inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi:0.0.0",
              "contentHash": "f01c20"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "films.reviews.SquashedTomatoesFilmId",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
          "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
          "memberQualifiedName": {
            "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
            "parameters": [],
            "name": "SquashedTomatoesFilmId",
            "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
            "shortDisplayName": "SquashedTomatoesFilmId"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "version": "0.0.0",
            "content": "namespace films.reviews {\n   type SquashedTomatoesFilmId inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/reviews/review-site.taxi:0.0.0",
            "contentHash": "f01c20"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.Error",
          "parameters": [],
          "name": "Error",
          "parameterizedName": "io.vyne.Error",
          "namespace": "io.vyne",
          "longDisplayName": "io.vyne.Error",
          "shortDisplayName": "Error"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.Error",
            "parameters": [],
            "name": "Error",
            "parameterizedName": "io.vyne.Error",
            "namespace": "io.vyne",
            "longDisplayName": "io.vyne.Error",
            "shortDisplayName": "Error"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "VyneQueryError",
              "version": "0.0.0",
              "content": "namespace io.vyne {\n   type Error inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/VyneQueryError",
              "id": "VyneQueryError:0.0.0",
              "contentHash": "f2be1f"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.Error",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.Error",
          "fullyQualifiedName": "io.vyne.Error",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.Error",
            "parameters": [],
            "name": "Error",
            "parameterizedName": "io.vyne.Error",
            "namespace": "io.vyne",
            "longDisplayName": "io.vyne.Error",
            "shortDisplayName": "Error"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "VyneQueryError",
            "version": "0.0.0",
            "content": "namespace io.vyne {\n   type Error inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/VyneQueryError",
            "id": "VyneQueryError:0.0.0",
            "contentHash": "f2be1f"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.Username",
          "parameters": [],
          "name": "Username",
          "parameterizedName": "io.vyne.Username",
          "namespace": "io.vyne",
          "longDisplayName": "io.vyne.Username",
          "shortDisplayName": "Username"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.Username",
            "parameters": [],
            "name": "Username",
            "parameterizedName": "io.vyne.Username",
            "namespace": "io.vyne",
            "longDisplayName": "io.vyne.Username",
            "shortDisplayName": "Username"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "UserTypes",
              "version": "0.0.0",
              "content": "namespace io.vyne {\n   type Username inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/UserTypes",
              "id": "UserTypes:0.0.0",
              "contentHash": "00a414"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.Username",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.Username",
          "fullyQualifiedName": "io.vyne.Username",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.Username",
            "parameters": [],
            "name": "Username",
            "parameterizedName": "io.vyne.Username",
            "namespace": "io.vyne",
            "longDisplayName": "io.vyne.Username",
            "shortDisplayName": "Username"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "UserTypes",
            "version": "0.0.0",
            "content": "namespace io.vyne {\n   type Username inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/UserTypes",
            "id": "UserTypes:0.0.0",
            "contentHash": "00a414"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.lambda.AwsLambdaService",
          "parameters": [],
          "name": "AwsLambdaService",
          "parameterizedName": "io.vyne.aws.lambda.AwsLambdaService",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.AwsLambdaService",
          "shortDisplayName": "AwsLambdaService"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.lambda.AwsLambdaService",
            "parameters": [],
            "name": "AwsLambdaService",
            "parameterizedName": "io.vyne.aws.lambda.AwsLambdaService",
            "namespace": "io.vyne.aws.lambda",
            "longDisplayName": "io.vyne.aws.lambda.AwsLambdaService",
            "shortDisplayName": "AwsLambdaService"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsLambdaConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.lambda {\n   annotation AwsLambdaService {\n         connectionName : ConnectionName inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
              "id": "AwsLambdaConnectors:0.0.0",
              "contentHash": "4f9d53"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.aws.lambda.AwsLambdaService",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.lambda.AwsLambdaService",
          "fullyQualifiedName": "io.vyne.aws.lambda.AwsLambdaService",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.lambda.AwsLambdaService",
            "parameters": [],
            "name": "AwsLambdaService",
            "parameterizedName": "io.vyne.aws.lambda.AwsLambdaService",
            "namespace": "io.vyne.aws.lambda",
            "longDisplayName": "io.vyne.aws.lambda.AwsLambdaService",
            "shortDisplayName": "AwsLambdaService"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsLambdaConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.lambda {\n   annotation AwsLambdaService {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
            "id": "AwsLambdaConnectors:0.0.0",
            "contentHash": "4f9d53"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.lambda.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.aws.lambda.ConnectionName",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.lambda.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.aws.lambda.ConnectionName",
            "namespace": "io.vyne.aws.lambda",
            "longDisplayName": "io.vyne.aws.lambda.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsLambdaConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.lambda {\n   ConnectionName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
              "id": "AwsLambdaConnectors:0.0.0",
              "contentHash": "ba8822"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.aws.lambda.ConnectionName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.lambda.ConnectionName",
          "fullyQualifiedName": "io.vyne.aws.lambda.ConnectionName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.lambda.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.aws.lambda.ConnectionName",
            "namespace": "io.vyne.aws.lambda",
            "longDisplayName": "io.vyne.aws.lambda.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsLambdaConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.lambda {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
            "id": "AwsLambdaConnectors:0.0.0",
            "contentHash": "ba8822"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.lambda.LambdaOperation",
          "parameters": [],
          "name": "LambdaOperation",
          "parameterizedName": "io.vyne.aws.lambda.LambdaOperation",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.LambdaOperation",
          "shortDisplayName": "LambdaOperation"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.lambda.LambdaOperation",
            "parameters": [],
            "name": "LambdaOperation",
            "parameterizedName": "io.vyne.aws.lambda.LambdaOperation",
            "namespace": "io.vyne.aws.lambda",
            "longDisplayName": "io.vyne.aws.lambda.LambdaOperation",
            "shortDisplayName": "LambdaOperation"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsLambdaConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.lambda {\n   annotation LambdaOperation {\n         name : OperationName inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
              "id": "AwsLambdaConnectors:0.0.0",
              "contentHash": "12a268"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.aws.lambda.LambdaOperation",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.lambda.LambdaOperation",
          "fullyQualifiedName": "io.vyne.aws.lambda.LambdaOperation",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.lambda.LambdaOperation",
            "parameters": [],
            "name": "LambdaOperation",
            "parameterizedName": "io.vyne.aws.lambda.LambdaOperation",
            "namespace": "io.vyne.aws.lambda",
            "longDisplayName": "io.vyne.aws.lambda.LambdaOperation",
            "shortDisplayName": "LambdaOperation"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsLambdaConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.lambda {\n   annotation LambdaOperation {\n         name : OperationName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
            "id": "AwsLambdaConnectors:0.0.0",
            "contentHash": "12a268"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.lambda.OperationName",
          "parameters": [],
          "name": "OperationName",
          "parameterizedName": "io.vyne.aws.lambda.OperationName",
          "namespace": "io.vyne.aws.lambda",
          "longDisplayName": "io.vyne.aws.lambda.OperationName",
          "shortDisplayName": "OperationName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.lambda.OperationName",
            "parameters": [],
            "name": "OperationName",
            "parameterizedName": "io.vyne.aws.lambda.OperationName",
            "namespace": "io.vyne.aws.lambda",
            "longDisplayName": "io.vyne.aws.lambda.OperationName",
            "shortDisplayName": "OperationName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsLambdaConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.lambda {\n   OperationName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
              "id": "AwsLambdaConnectors:0.0.0",
              "contentHash": "f4ed37"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.aws.lambda.OperationName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.lambda.OperationName",
          "fullyQualifiedName": "io.vyne.aws.lambda.OperationName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.lambda.OperationName",
            "parameters": [],
            "name": "OperationName",
            "parameterizedName": "io.vyne.aws.lambda.OperationName",
            "namespace": "io.vyne.aws.lambda",
            "longDisplayName": "io.vyne.aws.lambda.OperationName",
            "shortDisplayName": "OperationName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsLambdaConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.lambda {\n   OperationName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsLambdaConnectors",
            "id": "AwsLambdaConnectors:0.0.0",
            "contentHash": "f4ed37"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.BucketName",
          "parameters": [],
          "name": "BucketName",
          "parameterizedName": "io.vyne.aws.s3.BucketName",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.BucketName",
          "shortDisplayName": "BucketName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.s3.BucketName",
            "parameters": [],
            "name": "BucketName",
            "parameterizedName": "io.vyne.aws.s3.BucketName",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.BucketName",
            "shortDisplayName": "BucketName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsS3Connectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.s3 {\n   BucketName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
              "id": "AwsS3Connectors:0.0.0",
              "contentHash": "7ac5e3"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.aws.s3.BucketName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.s3.BucketName",
          "fullyQualifiedName": "io.vyne.aws.s3.BucketName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.s3.BucketName",
            "parameters": [],
            "name": "BucketName",
            "parameterizedName": "io.vyne.aws.s3.BucketName",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.BucketName",
            "shortDisplayName": "BucketName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   BucketName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "7ac5e3"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.aws.s3.ConnectionName",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.s3.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.aws.s3.ConnectionName",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsS3Connectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.s3 {\n   ConnectionName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
              "id": "AwsS3Connectors:0.0.0",
              "contentHash": "e2d418"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.aws.s3.ConnectionName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.s3.ConnectionName",
          "fullyQualifiedName": "io.vyne.aws.s3.ConnectionName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.s3.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.aws.s3.ConnectionName",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "e2d418"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.S3EntryKey",
          "parameters": [],
          "name": "S3EntryKey",
          "parameterizedName": "io.vyne.aws.s3.S3EntryKey",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.S3EntryKey",
          "shortDisplayName": "S3EntryKey"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.s3.S3EntryKey",
            "parameters": [],
            "name": "S3EntryKey",
            "parameterizedName": "io.vyne.aws.s3.S3EntryKey",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.S3EntryKey",
            "shortDisplayName": "S3EntryKey"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsS3Connectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.s3 {\n   type S3EntryKey inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
              "id": "AwsS3Connectors:0.0.0",
              "contentHash": "b1a22b"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.aws.s3.S3EntryKey",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.s3.S3EntryKey",
          "fullyQualifiedName": "io.vyne.aws.s3.S3EntryKey",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.s3.S3EntryKey",
            "parameters": [],
            "name": "S3EntryKey",
            "parameterizedName": "io.vyne.aws.s3.S3EntryKey",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.S3EntryKey",
            "shortDisplayName": "S3EntryKey"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   type S3EntryKey inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "b1a22b"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.S3Operation",
          "parameters": [],
          "name": "S3Operation",
          "parameterizedName": "io.vyne.aws.s3.S3Operation",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.S3Operation",
          "shortDisplayName": "S3Operation"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.s3.S3Operation",
            "parameters": [],
            "name": "S3Operation",
            "parameterizedName": "io.vyne.aws.s3.S3Operation",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.S3Operation",
            "shortDisplayName": "S3Operation"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsS3Connectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.s3 {\n   annotation S3Operation {\n         bucket : BucketName inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
              "id": "AwsS3Connectors:0.0.0",
              "contentHash": "2d48b3"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.aws.s3.S3Operation",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.s3.S3Operation",
          "fullyQualifiedName": "io.vyne.aws.s3.S3Operation",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.s3.S3Operation",
            "parameters": [],
            "name": "S3Operation",
            "parameterizedName": "io.vyne.aws.s3.S3Operation",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.S3Operation",
            "shortDisplayName": "S3Operation"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   annotation S3Operation {\n         bucket : BucketName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "2d48b3"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.s3.S3Service",
          "parameters": [],
          "name": "S3Service",
          "parameterizedName": "io.vyne.aws.s3.S3Service",
          "namespace": "io.vyne.aws.s3",
          "longDisplayName": "io.vyne.aws.s3.S3Service",
          "shortDisplayName": "S3Service"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.s3.S3Service",
            "parameters": [],
            "name": "S3Service",
            "parameterizedName": "io.vyne.aws.s3.S3Service",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.S3Service",
            "shortDisplayName": "S3Service"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsS3Connectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.s3 {\n   annotation S3Service {\n         connectionName : ConnectionName inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
              "id": "AwsS3Connectors:0.0.0",
              "contentHash": "c94220"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.aws.s3.S3Service",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.s3.S3Service",
          "fullyQualifiedName": "io.vyne.aws.s3.S3Service",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.s3.S3Service",
            "parameters": [],
            "name": "S3Service",
            "parameterizedName": "io.vyne.aws.s3.S3Service",
            "namespace": "io.vyne.aws.s3",
            "longDisplayName": "io.vyne.aws.s3.S3Service",
            "shortDisplayName": "S3Service"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsS3Connectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.s3 {\n   annotation S3Service {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsS3Connectors",
            "id": "AwsS3Connectors:0.0.0",
            "contentHash": "c94220"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.sqs.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.aws.sqs.ConnectionName",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.sqs.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.aws.sqs.ConnectionName",
            "namespace": "io.vyne.aws.sqs",
            "longDisplayName": "io.vyne.aws.sqs.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsSqsConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.sqs {\n   ConnectionName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
              "id": "AwsSqsConnectors:0.0.0",
              "contentHash": "2997f6"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.aws.sqs.ConnectionName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.sqs.ConnectionName",
          "fullyQualifiedName": "io.vyne.aws.sqs.ConnectionName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.sqs.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.aws.sqs.ConnectionName",
            "namespace": "io.vyne.aws.sqs",
            "longDisplayName": "io.vyne.aws.sqs.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsSqsConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.sqs {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
            "id": "AwsSqsConnectors:0.0.0",
            "contentHash": "2997f6"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.sqs.QueueName",
          "parameters": [],
          "name": "QueueName",
          "parameterizedName": "io.vyne.aws.sqs.QueueName",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.QueueName",
          "shortDisplayName": "QueueName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.sqs.QueueName",
            "parameters": [],
            "name": "QueueName",
            "parameterizedName": "io.vyne.aws.sqs.QueueName",
            "namespace": "io.vyne.aws.sqs",
            "longDisplayName": "io.vyne.aws.sqs.QueueName",
            "shortDisplayName": "QueueName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsSqsConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.sqs {\n   QueueName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
              "id": "AwsSqsConnectors:0.0.0",
              "contentHash": "e52a2b"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.aws.sqs.QueueName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.sqs.QueueName",
          "fullyQualifiedName": "io.vyne.aws.sqs.QueueName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.sqs.QueueName",
            "parameters": [],
            "name": "QueueName",
            "parameterizedName": "io.vyne.aws.sqs.QueueName",
            "namespace": "io.vyne.aws.sqs",
            "longDisplayName": "io.vyne.aws.sqs.QueueName",
            "shortDisplayName": "QueueName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsSqsConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.sqs {\n   QueueName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
            "id": "AwsSqsConnectors:0.0.0",
            "contentHash": "e52a2b"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.sqs.SqsOperation",
          "parameters": [],
          "name": "SqsOperation",
          "parameterizedName": "io.vyne.aws.sqs.SqsOperation",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.SqsOperation",
          "shortDisplayName": "SqsOperation"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.sqs.SqsOperation",
            "parameters": [],
            "name": "SqsOperation",
            "parameterizedName": "io.vyne.aws.sqs.SqsOperation",
            "namespace": "io.vyne.aws.sqs",
            "longDisplayName": "io.vyne.aws.sqs.SqsOperation",
            "shortDisplayName": "SqsOperation"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsSqsConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.sqs {\n   annotation SqsOperation {\n         queue : QueueName inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
              "id": "AwsSqsConnectors:0.0.0",
              "contentHash": "e9ab78"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.aws.sqs.SqsOperation",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.sqs.SqsOperation",
          "fullyQualifiedName": "io.vyne.aws.sqs.SqsOperation",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.sqs.SqsOperation",
            "parameters": [],
            "name": "SqsOperation",
            "parameterizedName": "io.vyne.aws.sqs.SqsOperation",
            "namespace": "io.vyne.aws.sqs",
            "longDisplayName": "io.vyne.aws.sqs.SqsOperation",
            "shortDisplayName": "SqsOperation"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsSqsConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.sqs {\n   annotation SqsOperation {\n         queue : QueueName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
            "id": "AwsSqsConnectors:0.0.0",
            "contentHash": "e9ab78"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.aws.sqs.SqsService",
          "parameters": [],
          "name": "SqsService",
          "parameterizedName": "io.vyne.aws.sqs.SqsService",
          "namespace": "io.vyne.aws.sqs",
          "longDisplayName": "io.vyne.aws.sqs.SqsService",
          "shortDisplayName": "SqsService"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.aws.sqs.SqsService",
            "parameters": [],
            "name": "SqsService",
            "parameterizedName": "io.vyne.aws.sqs.SqsService",
            "namespace": "io.vyne.aws.sqs",
            "longDisplayName": "io.vyne.aws.sqs.SqsService",
            "shortDisplayName": "SqsService"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "AwsSqsConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.aws.sqs {\n   annotation SqsService {\n         connectionName : ConnectionName inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
              "id": "AwsSqsConnectors:0.0.0",
              "contentHash": "bb0399"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.aws.sqs.SqsService",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.aws.sqs.SqsService",
          "fullyQualifiedName": "io.vyne.aws.sqs.SqsService",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.aws.sqs.SqsService",
            "parameters": [],
            "name": "SqsService",
            "parameterizedName": "io.vyne.aws.sqs.SqsService",
            "namespace": "io.vyne.aws.sqs",
            "longDisplayName": "io.vyne.aws.sqs.SqsService",
            "shortDisplayName": "SqsService"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AwsSqsConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.aws.sqs {\n   annotation SqsService {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AwsSqsConnectors",
            "id": "AwsSqsConnectors:0.0.0",
            "contentHash": "bb0399"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreBlob",
          "parameters": [],
          "name": "AzureStoreBlob",
          "parameterizedName": "io.vyne.azure.store.AzureStoreBlob",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.AzureStoreBlob",
          "shortDisplayName": "AzureStoreBlob"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.azure.store.AzureStoreBlob",
            "parameters": [],
            "name": "AzureStoreBlob",
            "parameterizedName": "io.vyne.azure.store.AzureStoreBlob",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.AzureStoreBlob",
            "shortDisplayName": "AzureStoreBlob"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AzureStoreConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.azure.store {\n   type AzureStoreBlob inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
              "id": "AzureStoreConnectors:0.0.0",
              "contentHash": "30cf88"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.azure.store.AzureStoreBlob",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.azure.store.AzureStoreBlob",
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreBlob",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.azure.store.AzureStoreBlob",
            "parameters": [],
            "name": "AzureStoreBlob",
            "parameterizedName": "io.vyne.azure.store.AzureStoreBlob",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.AzureStoreBlob",
            "shortDisplayName": "AzureStoreBlob"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   type AzureStoreBlob inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "30cf88"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreContainer",
          "parameters": [],
          "name": "AzureStoreContainer",
          "parameterizedName": "io.vyne.azure.store.AzureStoreContainer",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.AzureStoreContainer",
          "shortDisplayName": "AzureStoreContainer"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.azure.store.AzureStoreContainer",
            "parameters": [],
            "name": "AzureStoreContainer",
            "parameterizedName": "io.vyne.azure.store.AzureStoreContainer",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.AzureStoreContainer",
            "shortDisplayName": "AzureStoreContainer"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AzureStoreConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.azure.store {\n   AzureStoreContainer inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
              "id": "AzureStoreConnectors:0.0.0",
              "contentHash": "1bb1cc"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.azure.store.AzureStoreContainer",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.azure.store.AzureStoreContainer",
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreContainer",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.azure.store.AzureStoreContainer",
            "parameters": [],
            "name": "AzureStoreContainer",
            "parameterizedName": "io.vyne.azure.store.AzureStoreContainer",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.AzureStoreContainer",
            "shortDisplayName": "AzureStoreContainer"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   AzureStoreContainer inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "1bb1cc"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreOperation",
          "parameters": [],
          "name": "AzureStoreOperation",
          "parameterizedName": "io.vyne.azure.store.AzureStoreOperation",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.AzureStoreOperation",
          "shortDisplayName": "AzureStoreOperation"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.azure.store.AzureStoreOperation",
            "parameters": [],
            "name": "AzureStoreOperation",
            "parameterizedName": "io.vyne.azure.store.AzureStoreOperation",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.AzureStoreOperation",
            "shortDisplayName": "AzureStoreOperation"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "AzureStoreConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.azure.store {\n   annotation AzureStoreOperation {\n         container : AzureStoreContainer inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
              "id": "AzureStoreConnectors:0.0.0",
              "contentHash": "1305ac"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.azure.store.AzureStoreOperation",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.azure.store.AzureStoreOperation",
          "fullyQualifiedName": "io.vyne.azure.store.AzureStoreOperation",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.azure.store.AzureStoreOperation",
            "parameters": [],
            "name": "AzureStoreOperation",
            "parameterizedName": "io.vyne.azure.store.AzureStoreOperation",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.AzureStoreOperation",
            "shortDisplayName": "AzureStoreOperation"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   annotation AzureStoreOperation {\n         container : AzureStoreContainer inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "1305ac"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.BlobService",
          "parameters": [],
          "name": "BlobService",
          "parameterizedName": "io.vyne.azure.store.BlobService",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.BlobService",
          "shortDisplayName": "BlobService"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.azure.store.BlobService",
            "parameters": [],
            "name": "BlobService",
            "parameterizedName": "io.vyne.azure.store.BlobService",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.BlobService",
            "shortDisplayName": "BlobService"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "AzureStoreConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.azure.store {\n   annotation BlobService {\n         connectionName : ConnectionName inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
              "id": "AzureStoreConnectors:0.0.0",
              "contentHash": "7804e6"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.azure.store.BlobService",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.azure.store.BlobService",
          "fullyQualifiedName": "io.vyne.azure.store.BlobService",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.azure.store.BlobService",
            "parameters": [],
            "name": "BlobService",
            "parameterizedName": "io.vyne.azure.store.BlobService",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.BlobService",
            "shortDisplayName": "BlobService"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   annotation BlobService {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "7804e6"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.azure.store.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.azure.store.ConnectionName",
          "namespace": "io.vyne.azure.store",
          "longDisplayName": "io.vyne.azure.store.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.azure.store.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.azure.store.ConnectionName",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "AzureStoreConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.azure.store {\n   ConnectionName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
              "id": "AzureStoreConnectors:0.0.0",
              "contentHash": "93d191"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.azure.store.ConnectionName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.azure.store.ConnectionName",
          "fullyQualifiedName": "io.vyne.azure.store.ConnectionName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.azure.store.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.azure.store.ConnectionName",
            "namespace": "io.vyne.azure.store",
            "longDisplayName": "io.vyne.azure.store.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "AzureStoreConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.azure.store {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/AzureStoreConnectors",
            "id": "AzureStoreConnectors:0.0.0",
            "contentHash": "93d191"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.catalog.DataOwner",
          "parameters": [],
          "name": "DataOwner",
          "parameterizedName": "io.vyne.catalog.DataOwner",
          "namespace": "io.vyne.catalog",
          "longDisplayName": "io.vyne.catalog.DataOwner",
          "shortDisplayName": "DataOwner"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.catalog.DataOwner",
            "parameters": [],
            "name": "DataOwner",
            "parameterizedName": "io.vyne.catalog.DataOwner",
            "namespace": "io.vyne.catalog",
            "longDisplayName": "io.vyne.catalog.DataOwner",
            "shortDisplayName": "DataOwner"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "Catalog",
              "version": "0.0.0",
              "content": "namespace io.vyne.catalog {\n   annotation DataOwner {\n         id : io.vyne.Username\n         name : String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/Catalog",
              "id": "Catalog:0.0.0",
              "contentHash": "2368b8"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.catalog.DataOwner",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.catalog.DataOwner",
          "fullyQualifiedName": "io.vyne.catalog.DataOwner",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.catalog.DataOwner",
            "parameters": [],
            "name": "DataOwner",
            "parameterizedName": "io.vyne.catalog.DataOwner",
            "namespace": "io.vyne.catalog",
            "longDisplayName": "io.vyne.catalog.DataOwner",
            "shortDisplayName": "DataOwner"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "Catalog",
            "version": "0.0.0",
            "content": "namespace io.vyne.catalog {\n   annotation DataOwner {\n         id : io.vyne.Username\n         name : String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/Catalog",
            "id": "Catalog:0.0.0",
            "contentHash": "2368b8"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider",
          "parameters": [],
          "name": "StreamingMoviesProvider",
          "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider",
          "shortDisplayName": "StreamingMoviesProvider"
        },
        "kind": "SERVICE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider",
            "parameters": [],
            "name": "StreamingMoviesProvider",
            "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider",
            "namespace": "io.vyne.demos.films",
            "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider",
            "shortDisplayName": "StreamingMoviesProvider"
          },
          "operations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "parameters": [],
                "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
                "shortDisplayName": "getStreamingProvidersForFilm"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "films.FilmId",
                    "parameters": [],
                    "name": "FilmId",
                    "parameterizedName": "films.FilmId",
                    "namespace": "films",
                    "longDisplayName": "films.FilmId",
                    "shortDisplayName": "FilmId"
                  },
                  "name": "filmId",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "films.FilmId",
                    "parameters": [],
                    "name": "FilmId",
                    "parameterizedName": "films.FilmId",
                    "namespace": "films",
                    "longDisplayName": "films.FilmId",
                    "shortDisplayName": "FilmId"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
                "parameters": [],
                "name": "StreamingProvider",
                "parameterizedName": "io.vyne.demos.films.StreamingProvider",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingProvider",
                "shortDisplayName": "StreamingProvider"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9981/films/{films.FilmId}/streamingProviders"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
                  "parameters": [],
                  "name": "StreamingProvider",
                  "parameterizedName": "io.vyne.demos.films.StreamingProvider",
                  "namespace": "io.vyne.demos.films",
                  "longDisplayName": "io.vyne.demos.films.StreamingProvider",
                  "shortDisplayName": "StreamingProvider"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
                "parameters": [],
                "name": "StreamingProvider",
                "parameterizedName": "io.vyne.demos.films.StreamingProvider",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingProvider",
                "shortDisplayName": "StreamingProvider"
              },
              "name": "getStreamingProvidersForFilm",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "parameters": [],
                "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
                "shortDisplayName": "getStreamingProvidersForFilm"
              }
            }
          ],
          "queryOperations": [],
          "streamOperations": [],
          "tableOperations": [],
          "metadata": [],
          "sourceCode": [
            {
              "name": "films-service",
              "version": "0.0.0",
              "content": "import films.FilmId\nnamespace io.vyne.demos.films {\n   service StreamingMoviesProvider {\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9981/films/{films.FilmId}/streamingProviders\")\n         operation getStreamingProvidersForFilm(  filmId : films.FilmId ) : StreamingProvider\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.petflix.demos",
                "name": "films-api",
                "version": "0.0.0",
                "unversionedId": "io.petflix.demos/films-api",
                "id": "io.petflix.demos/films-api/0.0.0",
                "uriSafeId": "io.petflix.demos:films-api:0.0.0"
              },
              "packageQualifiedName": "[io.petflix.demos/films-api/0.0.0]/films-service",
              "id": "films-service:0.0.0",
              "contentHash": "a807dc"
            }
          ],
          "typeDoc": null,
          "lineage": null,
          "serviceKind": "API",
          "remoteOperations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "parameters": [],
                "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
                "shortDisplayName": "getStreamingProvidersForFilm"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "films.FilmId",
                    "parameters": [],
                    "name": "FilmId",
                    "parameterizedName": "films.FilmId",
                    "namespace": "films",
                    "longDisplayName": "films.FilmId",
                    "shortDisplayName": "FilmId"
                  },
                  "name": "filmId",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "films.FilmId",
                    "parameters": [],
                    "name": "FilmId",
                    "parameterizedName": "films.FilmId",
                    "namespace": "films",
                    "longDisplayName": "films.FilmId",
                    "shortDisplayName": "FilmId"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
                "parameters": [],
                "name": "StreamingProvider",
                "parameterizedName": "io.vyne.demos.films.StreamingProvider",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingProvider",
                "shortDisplayName": "StreamingProvider"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9981/films/{films.FilmId}/streamingProviders"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
                  "parameters": [],
                  "name": "StreamingProvider",
                  "parameterizedName": "io.vyne.demos.films.StreamingProvider",
                  "namespace": "io.vyne.demos.films",
                  "longDisplayName": "io.vyne.demos.films.StreamingProvider",
                  "shortDisplayName": "StreamingProvider"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
                "parameters": [],
                "name": "StreamingProvider",
                "parameterizedName": "io.vyne.demos.films.StreamingProvider",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingProvider",
                "shortDisplayName": "StreamingProvider"
              },
              "name": "getStreamingProvidersForFilm",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "parameters": [],
                "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
                "namespace": "io.vyne.demos.films",
                "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
                "shortDisplayName": "getStreamingProvidersForFilm"
              }
            }
          ],
          "qualifiedName": "io.vyne.demos.films.StreamingMoviesProvider",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider",
            "parameters": [],
            "name": "StreamingMoviesProvider",
            "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider",
            "namespace": "io.vyne.demos.films",
            "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider",
            "shortDisplayName": "StreamingMoviesProvider"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "name": "getStreamingProvidersForFilm",
          "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
          "namespace": "io.vyne.demos.films",
          "parameters": [],
          "parameterizedName": "StreamingMoviesProvider / getStreamingProvidersForFilm",
          "shortDisplayName": "getStreamingProvidersForFilm",
          "longDisplayName": "StreamingMoviesProvider / getStreamingProvidersForFilm"
        },
        "kind": "OPERATION",
        "aliasForType": null,
        "member": {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "parameters": [],
            "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "namespace": "io.vyne.demos.films",
            "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
            "shortDisplayName": "getStreamingProvidersForFilm"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "films.FilmId",
                "parameters": [],
                "name": "FilmId",
                "parameterizedName": "films.FilmId",
                "namespace": "films",
                "longDisplayName": "films.FilmId",
                "shortDisplayName": "FilmId"
              },
              "name": "filmId",
              "metadata": [],
              "constraints": [],
              "typeName": {
                "fullyQualifiedName": "films.FilmId",
                "parameters": [],
                "name": "FilmId",
                "parameterizedName": "films.FilmId",
                "namespace": "films",
                "longDisplayName": "films.FilmId",
                "shortDisplayName": "FilmId"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
            "parameters": [],
            "name": "StreamingProvider",
            "parameterizedName": "io.vyne.demos.films.StreamingProvider",
            "namespace": "io.vyne.demos.films",
            "longDisplayName": "io.vyne.demos.films.StreamingProvider",
            "shortDisplayName": "StreamingProvider"
          },
          "operationType": null,
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "HttpOperation",
                "namespace": "",
                "longDisplayName": "HttpOperation",
                "shortDisplayName": "HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9981/films/{films.FilmId}/streamingProviders"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
              "parameters": [],
              "name": "StreamingProvider",
              "parameterizedName": "io.vyne.demos.films.StreamingProvider",
              "namespace": "io.vyne.demos.films",
              "longDisplayName": "io.vyne.demos.films.StreamingProvider",
              "shortDisplayName": "StreamingProvider"
            },
            "constraints": []
          },
          "typeDoc": null,
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
            "parameters": [],
            "name": "StreamingProvider",
            "parameterizedName": "io.vyne.demos.films.StreamingProvider",
            "namespace": "io.vyne.demos.films",
            "longDisplayName": "io.vyne.demos.films.StreamingProvider",
            "shortDisplayName": "StreamingProvider"
          },
          "name": "getStreamingProvidersForFilm",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "parameters": [],
            "name": "StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "parameterizedName": "io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm",
            "namespace": "io.vyne.demos.films",
            "longDisplayName": "io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm",
            "shortDisplayName": "getStreamingProvidersForFilm"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
          "parameters": [],
          "name": "StreamingProvider",
          "parameterizedName": "io.vyne.demos.films.StreamingProvider",
          "namespace": "io.vyne.demos.films",
          "longDisplayName": "io.vyne.demos.films.StreamingProvider",
          "shortDisplayName": "StreamingProvider"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
            "parameters": [],
            "name": "StreamingProvider",
            "parameterizedName": "io.vyne.demos.films.StreamingProvider",
            "namespace": "io.vyne.demos.films",
            "longDisplayName": "io.vyne.demos.films.StreamingProvider",
            "shortDisplayName": "StreamingProvider"
          },
          "attributes": {
            "name": {
              "type": {
                "fullyQualifiedName": "films.StreamingProviderName",
                "parameters": [],
                "name": "StreamingProviderName",
                "parameterizedName": "films.StreamingProviderName",
                "namespace": "films",
                "longDisplayName": "films.StreamingProviderName",
                "shortDisplayName": "StreamingProviderName"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "films.StreamingProviderName",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "pricePerMonth": {
              "type": {
                "fullyQualifiedName": "films.StreamingProviderPrice",
                "parameters": [],
                "name": "StreamingProviderPrice",
                "parameterizedName": "films.StreamingProviderPrice",
                "namespace": "films",
                "longDisplayName": "films.StreamingProviderPrice",
                "shortDisplayName": "StreamingProviderPrice"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "films.StreamingProviderPrice",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            }
          },
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "films-service",
              "version": "0.0.0",
              "content": "import films.StreamingProviderName\nimport films.StreamingProviderPrice\nnamespace io.vyne.demos.films {\n   model StreamingProvider {\n         name : films.StreamingProviderName\n         pricePerMonth : films.StreamingProviderPrice\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.petflix.demos",
                "name": "films-api",
                "version": "0.0.0",
                "unversionedId": "io.petflix.demos/films-api",
                "id": "io.petflix.demos/films-api/0.0.0",
                "uriSafeId": "io.petflix.demos:films-api:0.0.0"
              },
              "packageQualifiedName": "[io.petflix.demos/films-api/0.0.0]/films-service",
              "id": "films-service:0.0.0",
              "contentHash": "fa5db1"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.demos.films.StreamingProvider",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.demos.films.StreamingProvider",
          "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.StreamingProvider",
            "parameters": [],
            "name": "StreamingProvider",
            "parameterizedName": "io.vyne.demos.films.StreamingProvider",
            "namespace": "io.vyne.demos.films",
            "longDisplayName": "io.vyne.demos.films.StreamingProvider",
            "shortDisplayName": "StreamingProvider"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": false
        },
        "sources": [
          {
            "name": "films-service",
            "version": "0.0.0",
            "content": "import films.StreamingProviderName\nimport films.StreamingProviderPrice\nnamespace io.vyne.demos.films {\n   model StreamingProvider {\n         name : films.StreamingProviderName\n         pricePerMonth : films.StreamingProviderPrice\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.petflix.demos",
              "name": "films-api",
              "version": "0.0.0",
              "unversionedId": "io.petflix.demos/films-api",
              "id": "io.petflix.demos/films-api/0.0.0",
              "uriSafeId": "io.petflix.demos:films-api:0.0.0"
            },
            "packageQualifiedName": "[io.petflix.demos/films-api/0.0.0]/films-service",
            "id": "films-service:0.0.0",
            "contentHash": "fa5db1"
          }
        ],
        "typeDoc": "",
        "attributeNames": [
          "name",
          "pricePerMonth"
        ]
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.films.announcements.KafkaService",
          "parameters": [],
          "name": "KafkaService",
          "parameterizedName": "io.vyne.films.announcements.KafkaService",
          "namespace": "io.vyne.films.announcements",
          "longDisplayName": "io.vyne.films.announcements.KafkaService",
          "shortDisplayName": "KafkaService"
        },
        "kind": "SERVICE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.films.announcements.KafkaService",
            "parameters": [],
            "name": "KafkaService",
            "parameterizedName": "io.vyne.films.announcements.KafkaService",
            "namespace": "io.vyne.films.announcements",
            "longDisplayName": "io.vyne.films.announcements.KafkaService",
            "shortDisplayName": "KafkaService"
          },
          "operations": [],
          "queryOperations": [],
          "streamOperations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.films.announcements.KafkaService@@newReleases",
                "parameters": [],
                "name": "KafkaService@@newReleases",
                "parameterizedName": "io.vyne.films.announcements.KafkaService@@newReleases",
                "namespace": "io.vyne.films.announcements",
                "longDisplayName": "io.vyne.films.announcements.KafkaService / newReleases",
                "shortDisplayName": "newReleases"
              },
              "returnType": {
                "fullyQualifiedName": "lang.taxi.Stream",
                "parameters": [
                  {
                    "fullyQualifiedName": "NewFilmReleaseAnnouncement",
                    "parameters": [],
                    "name": "NewFilmReleaseAnnouncement",
                    "parameterizedName": "NewFilmReleaseAnnouncement",
                    "namespace": "",
                    "longDisplayName": "NewFilmReleaseAnnouncement",
                    "shortDisplayName": "NewFilmReleaseAnnouncement"
                  }
                ],
                "name": "Stream",
                "parameterizedName": "lang.taxi.Stream<NewFilmReleaseAnnouncement>",
                "namespace": "lang.taxi",
                "longDisplayName": "lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>",
                "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>"
              },
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "io.vyne.kafka.KafkaOperation",
                    "parameters": [],
                    "name": "KafkaOperation",
                    "parameterizedName": "io.vyne.kafka.KafkaOperation",
                    "namespace": "io.vyne.kafka",
                    "longDisplayName": "io.vyne.kafka.KafkaOperation",
                    "shortDisplayName": "KafkaOperation"
                  },
                  "params": {
                    "topic": "releases",
                    "offset": "latest"
                  }
                }
              ],
              "typeDoc": null,
              "parameters": [],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "lang.taxi.Stream",
                  "parameters": [
                    {
                      "fullyQualifiedName": "NewFilmReleaseAnnouncement",
                      "parameters": [],
                      "name": "NewFilmReleaseAnnouncement",
                      "parameterizedName": "NewFilmReleaseAnnouncement",
                      "namespace": "",
                      "longDisplayName": "NewFilmReleaseAnnouncement",
                      "shortDisplayName": "NewFilmReleaseAnnouncement"
                    }
                  ],
                  "name": "Stream",
                  "parameterizedName": "lang.taxi.Stream<NewFilmReleaseAnnouncement>",
                  "namespace": "lang.taxi",
                  "longDisplayName": "lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>",
                  "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>"
                },
                "constraints": []
              },
              "operationType": null,
              "returnTypeName": {
                "fullyQualifiedName": "lang.taxi.Stream",
                "parameters": [
                  {
                    "fullyQualifiedName": "NewFilmReleaseAnnouncement",
                    "parameters": [],
                    "name": "NewFilmReleaseAnnouncement",
                    "parameterizedName": "NewFilmReleaseAnnouncement",
                    "namespace": "",
                    "longDisplayName": "NewFilmReleaseAnnouncement",
                    "shortDisplayName": "NewFilmReleaseAnnouncement"
                  }
                ],
                "name": "Stream",
                "parameterizedName": "lang.taxi.Stream<NewFilmReleaseAnnouncement>",
                "namespace": "lang.taxi",
                "longDisplayName": "lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>",
                "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>"
              },
              "name": "newReleases",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.films.announcements.KafkaService@@newReleases",
                "parameters": [],
                "name": "KafkaService@@newReleases",
                "parameterizedName": "io.vyne.films.announcements.KafkaService@@newReleases",
                "namespace": "io.vyne.films.announcements",
                "longDisplayName": "io.vyne.films.announcements.KafkaService / newReleases",
                "shortDisplayName": "newReleases"
              }
            }
          ],
          "tableOperations": [],
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "io.vyne.kafka.KafkaService",
                "parameters": [],
                "name": "KafkaService",
                "parameterizedName": "io.vyne.kafka.KafkaService",
                "namespace": "io.vyne.kafka",
                "longDisplayName": "io.vyne.kafka.KafkaService",
                "shortDisplayName": "KafkaService"
              },
              "params": {
                "connectionName": "kafka"
              }
            }
          ],
          "sourceCode": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/io/vyne/films/announcements/KafkaService.taxi",
              "version": "0.0.0",
              "content": "import io.vyne.kafka.KafkaOperation\nimport lang.taxi.Stream\nimport NewFilmReleaseAnnouncement\nnamespace io.vyne.films.announcements {\n   @io.vyne.kafka.KafkaService(connectionName = \"kafka\")\n         service KafkaService {\n            @io.vyne.kafka.KafkaOperation(topic = \"releases\" , offset = \"latest\")\n            stream newReleases : Stream<NewFilmReleaseAnnouncement>\n         }\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/io/vyne/films/announcements/KafkaService.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/io/vyne/films/announcements/KafkaService.taxi:0.0.0",
              "contentHash": "6ce573"
            }
          ],
          "typeDoc": null,
          "lineage": null,
          "serviceKind": "Kafka",
          "remoteOperations": [],
          "qualifiedName": "io.vyne.films.announcements.KafkaService",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.films.announcements.KafkaService",
            "parameters": [],
            "name": "KafkaService",
            "parameterizedName": "io.vyne.films.announcements.KafkaService",
            "namespace": "io.vyne.films.announcements",
            "longDisplayName": "io.vyne.films.announcements.KafkaService",
            "shortDisplayName": "KafkaService"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService",
          "parameters": [],
          "name": "IdLookupService",
          "parameterizedName": "io.vyne.films.idlookup.IdLookupService",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdLookupService",
          "shortDisplayName": "IdLookupService"
        },
        "kind": "SERVICE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService",
            "parameters": [],
            "name": "IdLookupService",
            "parameterizedName": "io.vyne.films.idlookup.IdLookupService",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdLookupService",
            "shortDisplayName": "IdLookupService"
          },
          "operations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromSquashedTomatoesId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
                "shortDisplayName": "lookupFromSquashedTomatoesId"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "lang.taxi.String",
                    "parameters": [],
                    "name": "String",
                    "parameterizedName": "lang.taxi.String",
                    "namespace": "lang.taxi",
                    "longDisplayName": "lang.taxi.String",
                    "shortDisplayName": "String"
                  },
                  "name": "id",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "lang.taxi.String",
                    "parameters": [],
                    "name": "String",
                    "parameterizedName": "lang.taxi.String",
                    "namespace": "lang.taxi",
                    "longDisplayName": "lang.taxi.String",
                    "shortDisplayName": "String"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                  "parameters": [],
                  "name": "IdResolution",
                  "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                  "namespace": "io.vyne.films.idlookup",
                  "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                  "shortDisplayName": "IdResolution"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "name": "lookupFromSquashedTomatoesId",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromSquashedTomatoesId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
                "shortDisplayName": "lookupFromSquashedTomatoesId"
              }
            },
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromInternalFilmId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
                "shortDisplayName": "lookupFromInternalFilmId"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "films.FilmId",
                    "parameters": [],
                    "name": "FilmId",
                    "parameterizedName": "films.FilmId",
                    "namespace": "films",
                    "longDisplayName": "films.FilmId",
                    "shortDisplayName": "FilmId"
                  },
                  "name": "id",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "films.FilmId",
                    "parameters": [],
                    "name": "FilmId",
                    "parameterizedName": "films.FilmId",
                    "namespace": "films",
                    "longDisplayName": "films.FilmId",
                    "shortDisplayName": "FilmId"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9986/ids/internal/{films.FilmId}"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                  "parameters": [],
                  "name": "IdResolution",
                  "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                  "namespace": "io.vyne.films.idlookup",
                  "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                  "shortDisplayName": "IdResolution"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "name": "lookupFromInternalFilmId",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromInternalFilmId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
                "shortDisplayName": "lookupFromInternalFilmId"
              }
            },
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromNetflixFilmId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
                "shortDisplayName": "lookupFromNetflixFilmId"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                    "parameters": [],
                    "name": "NetflixFilmId",
                    "parameterizedName": "demo.netflix.NetflixFilmId",
                    "namespace": "demo.netflix",
                    "longDisplayName": "demo.netflix.NetflixFilmId",
                    "shortDisplayName": "NetflixFilmId"
                  },
                  "name": "id",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                    "parameters": [],
                    "name": "NetflixFilmId",
                    "parameterizedName": "demo.netflix.NetflixFilmId",
                    "namespace": "demo.netflix",
                    "longDisplayName": "demo.netflix.NetflixFilmId",
                    "shortDisplayName": "NetflixFilmId"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                  "parameters": [],
                  "name": "IdResolution",
                  "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                  "namespace": "io.vyne.films.idlookup",
                  "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                  "shortDisplayName": "IdResolution"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "name": "lookupFromNetflixFilmId",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromNetflixFilmId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
                "shortDisplayName": "lookupFromNetflixFilmId"
              }
            }
          ],
          "queryOperations": [],
          "streamOperations": [],
          "tableOperations": [],
          "metadata": [],
          "sourceCode": [
            {
              "name": "id-resolution-service",
              "version": "0.0.0",
              "content": "import films.FilmId\nimport demo.netflix.NetflixFilmId\nnamespace io.vyne.films.idlookup {\n   service IdLookupService {\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}\")\n         operation lookupFromSquashedTomatoesId(  id : String ) : IdResolution\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9986/ids/internal/{films.FilmId}\")\n         operation lookupFromInternalFilmId(  id : films.FilmId ) : IdResolution\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}\")\n         operation lookupFromNetflixFilmId(  id : demo.netflix.NetflixFilmId ) : IdResolution\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.petflex.demos",
                "name": "id-lookup-service",
                "version": "0.0.0",
                "unversionedId": "io.petflex.demos/id-lookup-service",
                "id": "io.petflex.demos/id-lookup-service/0.0.0",
                "uriSafeId": "io.petflex.demos:id-lookup-service:0.0.0"
              },
              "packageQualifiedName": "[io.petflex.demos/id-lookup-service/0.0.0]/id-resolution-service",
              "id": "id-resolution-service:0.0.0",
              "contentHash": "c320e3"
            }
          ],
          "typeDoc": null,
          "lineage": null,
          "serviceKind": "API",
          "remoteOperations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromSquashedTomatoesId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
                "shortDisplayName": "lookupFromSquashedTomatoesId"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "lang.taxi.String",
                    "parameters": [],
                    "name": "String",
                    "parameterizedName": "lang.taxi.String",
                    "namespace": "lang.taxi",
                    "longDisplayName": "lang.taxi.String",
                    "shortDisplayName": "String"
                  },
                  "name": "id",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "lang.taxi.String",
                    "parameters": [],
                    "name": "String",
                    "parameterizedName": "lang.taxi.String",
                    "namespace": "lang.taxi",
                    "longDisplayName": "lang.taxi.String",
                    "shortDisplayName": "String"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                  "parameters": [],
                  "name": "IdResolution",
                  "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                  "namespace": "io.vyne.films.idlookup",
                  "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                  "shortDisplayName": "IdResolution"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "name": "lookupFromSquashedTomatoesId",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromSquashedTomatoesId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
                "shortDisplayName": "lookupFromSquashedTomatoesId"
              }
            },
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromInternalFilmId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
                "shortDisplayName": "lookupFromInternalFilmId"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "films.FilmId",
                    "parameters": [],
                    "name": "FilmId",
                    "parameterizedName": "films.FilmId",
                    "namespace": "films",
                    "longDisplayName": "films.FilmId",
                    "shortDisplayName": "FilmId"
                  },
                  "name": "id",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "films.FilmId",
                    "parameters": [],
                    "name": "FilmId",
                    "parameterizedName": "films.FilmId",
                    "namespace": "films",
                    "longDisplayName": "films.FilmId",
                    "shortDisplayName": "FilmId"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9986/ids/internal/{films.FilmId}"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                  "parameters": [],
                  "name": "IdResolution",
                  "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                  "namespace": "io.vyne.films.idlookup",
                  "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                  "shortDisplayName": "IdResolution"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "name": "lookupFromInternalFilmId",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromInternalFilmId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
                "shortDisplayName": "lookupFromInternalFilmId"
              }
            },
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromNetflixFilmId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
                "shortDisplayName": "lookupFromNetflixFilmId"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                    "parameters": [],
                    "name": "NetflixFilmId",
                    "parameterizedName": "demo.netflix.NetflixFilmId",
                    "namespace": "demo.netflix",
                    "longDisplayName": "demo.netflix.NetflixFilmId",
                    "shortDisplayName": "NetflixFilmId"
                  },
                  "name": "id",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                    "parameters": [],
                    "name": "NetflixFilmId",
                    "parameterizedName": "demo.netflix.NetflixFilmId",
                    "namespace": "demo.netflix",
                    "longDisplayName": "demo.netflix.NetflixFilmId",
                    "shortDisplayName": "NetflixFilmId"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                  "parameters": [],
                  "name": "IdResolution",
                  "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                  "namespace": "io.vyne.films.idlookup",
                  "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                  "shortDisplayName": "IdResolution"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
                "parameters": [],
                "name": "IdResolution",
                "parameterizedName": "io.vyne.films.idlookup.IdResolution",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdResolution",
                "shortDisplayName": "IdResolution"
              },
              "name": "lookupFromNetflixFilmId",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
                "parameters": [],
                "name": "IdLookupService@@lookupFromNetflixFilmId",
                "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
                "namespace": "io.vyne.films.idlookup",
                "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
                "shortDisplayName": "lookupFromNetflixFilmId"
              }
            }
          ],
          "qualifiedName": "io.vyne.films.idlookup.IdLookupService",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService",
            "parameters": [],
            "name": "IdLookupService",
            "parameterizedName": "io.vyne.films.idlookup.IdLookupService",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdLookupService",
            "shortDisplayName": "IdLookupService"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "name": "lookupFromInternalFilmId",
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
          "namespace": "io.vyne.films.idlookup",
          "parameters": [],
          "parameterizedName": "IdLookupService / lookupFromInternalFilmId",
          "shortDisplayName": "lookupFromInternalFilmId",
          "longDisplayName": "IdLookupService / lookupFromInternalFilmId"
        },
        "kind": "OPERATION",
        "aliasForType": null,
        "member": {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
            "parameters": [],
            "name": "IdLookupService@@lookupFromInternalFilmId",
            "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
            "shortDisplayName": "lookupFromInternalFilmId"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "films.FilmId",
                "parameters": [],
                "name": "FilmId",
                "parameterizedName": "films.FilmId",
                "namespace": "films",
                "longDisplayName": "films.FilmId",
                "shortDisplayName": "FilmId"
              },
              "name": "id",
              "metadata": [],
              "constraints": [],
              "typeName": {
                "fullyQualifiedName": "films.FilmId",
                "parameters": [],
                "name": "FilmId",
                "parameterizedName": "films.FilmId",
                "namespace": "films",
                "longDisplayName": "films.FilmId",
                "shortDisplayName": "FilmId"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "operationType": null,
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "HttpOperation",
                "namespace": "",
                "longDisplayName": "HttpOperation",
                "shortDisplayName": "HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9986/ids/internal/{films.FilmId}"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "constraints": []
          },
          "typeDoc": null,
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "name": "lookupFromInternalFilmId",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
            "parameters": [],
            "name": "IdLookupService@@lookupFromInternalFilmId",
            "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId",
            "shortDisplayName": "lookupFromInternalFilmId"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "name": "lookupFromNetflixFilmId",
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
          "namespace": "io.vyne.films.idlookup",
          "parameters": [],
          "parameterizedName": "IdLookupService / lookupFromNetflixFilmId",
          "shortDisplayName": "lookupFromNetflixFilmId",
          "longDisplayName": "IdLookupService / lookupFromNetflixFilmId"
        },
        "kind": "OPERATION",
        "aliasForType": null,
        "member": {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
            "parameters": [],
            "name": "IdLookupService@@lookupFromNetflixFilmId",
            "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
            "shortDisplayName": "lookupFromNetflixFilmId"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                "parameters": [],
                "name": "NetflixFilmId",
                "parameterizedName": "demo.netflix.NetflixFilmId",
                "namespace": "demo.netflix",
                "longDisplayName": "demo.netflix.NetflixFilmId",
                "shortDisplayName": "NetflixFilmId"
              },
              "name": "id",
              "metadata": [],
              "constraints": [],
              "typeName": {
                "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                "parameters": [],
                "name": "NetflixFilmId",
                "parameterizedName": "demo.netflix.NetflixFilmId",
                "namespace": "demo.netflix",
                "longDisplayName": "demo.netflix.NetflixFilmId",
                "shortDisplayName": "NetflixFilmId"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "operationType": null,
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "HttpOperation",
                "namespace": "",
                "longDisplayName": "HttpOperation",
                "shortDisplayName": "HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "constraints": []
          },
          "typeDoc": null,
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "name": "lookupFromNetflixFilmId",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
            "parameters": [],
            "name": "IdLookupService@@lookupFromNetflixFilmId",
            "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId",
            "shortDisplayName": "lookupFromNetflixFilmId"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "name": "lookupFromSquashedTomatoesId",
          "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
          "namespace": "io.vyne.films.idlookup",
          "parameters": [],
          "parameterizedName": "IdLookupService / lookupFromSquashedTomatoesId",
          "shortDisplayName": "lookupFromSquashedTomatoesId",
          "longDisplayName": "IdLookupService / lookupFromSquashedTomatoesId"
        },
        "kind": "OPERATION",
        "aliasForType": null,
        "member": {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
            "parameters": [],
            "name": "IdLookupService@@lookupFromSquashedTomatoesId",
            "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
            "shortDisplayName": "lookupFromSquashedTomatoesId"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "longDisplayName": "lang.taxi.String",
                "shortDisplayName": "String"
              },
              "name": "id",
              "metadata": [],
              "constraints": [],
              "typeName": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "longDisplayName": "lang.taxi.String",
                "shortDisplayName": "String"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "operationType": null,
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "HttpOperation",
                "namespace": "",
                "longDisplayName": "HttpOperation",
                "shortDisplayName": "HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
              "parameters": [],
              "name": "IdResolution",
              "parameterizedName": "io.vyne.films.idlookup.IdResolution",
              "namespace": "io.vyne.films.idlookup",
              "longDisplayName": "io.vyne.films.idlookup.IdResolution",
              "shortDisplayName": "IdResolution"
            },
            "constraints": []
          },
          "typeDoc": null,
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "name": "lookupFromSquashedTomatoesId",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
            "parameters": [],
            "name": "IdLookupService@@lookupFromSquashedTomatoesId",
            "parameterizedName": "io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId",
            "shortDisplayName": "lookupFromSquashedTomatoesId"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "parameters": [],
          "name": "IdResolution",
          "parameterizedName": "io.vyne.films.idlookup.IdResolution",
          "namespace": "io.vyne.films.idlookup",
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "shortDisplayName": "IdResolution"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "attributes": {
            "filmId": {
              "type": {
                "fullyQualifiedName": "films.FilmId",
                "parameters": [],
                "name": "FilmId",
                "parameterizedName": "films.FilmId",
                "namespace": "films",
                "longDisplayName": "films.FilmId",
                "shortDisplayName": "FilmId"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "films.FilmId",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "netflixId": {
              "type": {
                "fullyQualifiedName": "demo.netflix.NetflixFilmId",
                "parameters": [],
                "name": "NetflixFilmId",
                "parameterizedName": "demo.netflix.NetflixFilmId",
                "namespace": "demo.netflix",
                "longDisplayName": "demo.netflix.NetflixFilmId",
                "shortDisplayName": "NetflixFilmId"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "demo.netflix.NetflixFilmId",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "squashedTomatoesFilmId": {
              "type": {
                "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                "parameters": [],
                "name": "SquashedTomatoesFilmId",
                "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                "namespace": "films.reviews",
                "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                "shortDisplayName": "SquashedTomatoesFilmId"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "films.reviews.SquashedTomatoesFilmId",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            }
          },
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "id-resolution-service",
              "version": "0.0.0",
              "content": "import films.FilmId\nimport demo.netflix.NetflixFilmId\nimport films.reviews.SquashedTomatoesFilmId\nnamespace io.vyne.films.idlookup {\n   model IdResolution {\n         filmId : films.FilmId\n         netflixId : demo.netflix.NetflixFilmId\n         squashedTomatoesFilmId : films.reviews.SquashedTomatoesFilmId\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.petflex.demos",
                "name": "id-lookup-service",
                "version": "0.0.0",
                "unversionedId": "io.petflex.demos/id-lookup-service",
                "id": "io.petflex.demos/id-lookup-service/0.0.0",
                "uriSafeId": "io.petflex.demos:id-lookup-service:0.0.0"
              },
              "packageQualifiedName": "[io.petflex.demos/id-lookup-service/0.0.0]/id-resolution-service",
              "id": "id-resolution-service:0.0.0",
              "contentHash": "683376"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.films.idlookup.IdResolution",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.films.idlookup.IdResolution",
          "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.films.idlookup.IdResolution",
            "parameters": [],
            "name": "IdResolution",
            "parameterizedName": "io.vyne.films.idlookup.IdResolution",
            "namespace": "io.vyne.films.idlookup",
            "longDisplayName": "io.vyne.films.idlookup.IdResolution",
            "shortDisplayName": "IdResolution"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": false
        },
        "sources": [
          {
            "name": "id-resolution-service",
            "version": "0.0.0",
            "content": "import films.FilmId\nimport demo.netflix.NetflixFilmId\nimport films.reviews.SquashedTomatoesFilmId\nnamespace io.vyne.films.idlookup {\n   model IdResolution {\n         filmId : films.FilmId\n         netflixId : demo.netflix.NetflixFilmId\n         squashedTomatoesFilmId : films.reviews.SquashedTomatoesFilmId\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.petflex.demos",
              "name": "id-lookup-service",
              "version": "0.0.0",
              "unversionedId": "io.petflex.demos/id-lookup-service",
              "id": "io.petflex.demos/id-lookup-service/0.0.0",
              "uriSafeId": "io.petflex.demos:id-lookup-service:0.0.0"
            },
            "packageQualifiedName": "[io.petflex.demos/id-lookup-service/0.0.0]/id-resolution-service",
            "id": "id-resolution-service:0.0.0",
            "contentHash": "683376"
          }
        ],
        "typeDoc": "",
        "attributeNames": [
          "filmId",
          "netflixId",
          "squashedTomatoesFilmId"
        ]
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.formats.Csv",
          "parameters": [],
          "name": "Csv",
          "parameterizedName": "io.vyne.formats.Csv",
          "namespace": "io.vyne.formats",
          "longDisplayName": "io.vyne.formats.Csv",
          "shortDisplayName": "Csv"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.formats.Csv",
            "parameters": [],
            "name": "Csv",
            "parameterizedName": "io.vyne.formats.Csv",
            "namespace": "io.vyne.formats",
            "longDisplayName": "io.vyne.formats.Csv",
            "shortDisplayName": "Csv"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "CsvFormat",
              "version": "0.0.0",
              "content": "namespace io.vyne.formats {\n   annotation Csv {\n         delimiter : String?\n         firstRecordAsHeader : Boolean?\n         nullValue : String?\n         containsTrailingDelimiters : Boolean?\n         ignoreContentBefore : String?\n         useFieldNamesAsColumnNames: Boolean?\n         withQuote: String?\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/CsvFormat",
              "id": "CsvFormat:0.0.0",
              "contentHash": "25616d"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.formats.Csv",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.formats.Csv",
          "fullyQualifiedName": "io.vyne.formats.Csv",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.formats.Csv",
            "parameters": [],
            "name": "Csv",
            "parameterizedName": "io.vyne.formats.Csv",
            "namespace": "io.vyne.formats",
            "longDisplayName": "io.vyne.formats.Csv",
            "shortDisplayName": "Csv"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "CsvFormat",
            "version": "0.0.0",
            "content": "namespace io.vyne.formats {\n   annotation Csv {\n         delimiter : String?\n         firstRecordAsHeader : Boolean?\n         nullValue : String?\n         containsTrailingDelimiters : Boolean?\n         ignoreContentBefore : String?\n         useFieldNamesAsColumnNames: Boolean?\n         withQuote: String?\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/CsvFormat",
            "id": "CsvFormat:0.0.0",
            "contentHash": "25616d"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.jdbc.ConnectionName",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.jdbc.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.jdbc.ConnectionName",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "JdbcConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.jdbc {\n   type ConnectionName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
              "id": "JdbcConnectors:0.0.0",
              "contentHash": "b0f9b0"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.jdbc.ConnectionName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.jdbc.ConnectionName",
          "fullyQualifiedName": "io.vyne.jdbc.ConnectionName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.jdbc.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.jdbc.ConnectionName",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   type ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "b0f9b0"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
          "parameters": [],
          "name": "DatabaseService",
          "parameterizedName": "io.vyne.jdbc.DatabaseService",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.DatabaseService",
          "shortDisplayName": "DatabaseService"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
            "parameters": [],
            "name": "DatabaseService",
            "parameterizedName": "io.vyne.jdbc.DatabaseService",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.DatabaseService",
            "shortDisplayName": "DatabaseService"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "JdbcConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.jdbc {\n   annotation DatabaseService {\n         connection : ConnectionName\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
              "id": "JdbcConnectors:0.0.0",
              "contentHash": "56bb57"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.jdbc.DatabaseService",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.jdbc.DatabaseService",
          "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
            "parameters": [],
            "name": "DatabaseService",
            "parameterizedName": "io.vyne.jdbc.DatabaseService",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.DatabaseService",
            "shortDisplayName": "DatabaseService"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   annotation DatabaseService {\n         connection : ConnectionName\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "56bb57"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.SchemaName",
          "parameters": [],
          "name": "SchemaName",
          "parameterizedName": "io.vyne.jdbc.SchemaName",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.SchemaName",
          "shortDisplayName": "SchemaName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.jdbc.SchemaName",
            "parameters": [],
            "name": "SchemaName",
            "parameterizedName": "io.vyne.jdbc.SchemaName",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.SchemaName",
            "shortDisplayName": "SchemaName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "JdbcConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.jdbc {\n   SchemaName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
              "id": "JdbcConnectors:0.0.0",
              "contentHash": "49e2fe"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.jdbc.SchemaName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.jdbc.SchemaName",
          "fullyQualifiedName": "io.vyne.jdbc.SchemaName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.jdbc.SchemaName",
            "parameters": [],
            "name": "SchemaName",
            "parameterizedName": "io.vyne.jdbc.SchemaName",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.SchemaName",
            "shortDisplayName": "SchemaName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   SchemaName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "49e2fe"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.Table",
          "parameters": [],
          "name": "Table",
          "parameterizedName": "io.vyne.jdbc.Table",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.Table",
          "shortDisplayName": "Table"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.jdbc.Table",
            "parameters": [],
            "name": "Table",
            "parameterizedName": "io.vyne.jdbc.Table",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.Table",
            "shortDisplayName": "Table"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "JdbcConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.jdbc {\n   annotation Table {\n         connection : ConnectionName\n         table : TableName inherits String\n         schema: SchemaName inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
              "id": "JdbcConnectors:0.0.0",
              "contentHash": "e8ee48"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.jdbc.Table",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.jdbc.Table",
          "fullyQualifiedName": "io.vyne.jdbc.Table",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.jdbc.Table",
            "parameters": [],
            "name": "Table",
            "parameterizedName": "io.vyne.jdbc.Table",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.Table",
            "shortDisplayName": "Table"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   annotation Table {\n         connection : ConnectionName\n         table : TableName inherits String\n         schema: SchemaName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "e8ee48"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.jdbc.TableName",
          "parameters": [],
          "name": "TableName",
          "parameterizedName": "io.vyne.jdbc.TableName",
          "namespace": "io.vyne.jdbc",
          "longDisplayName": "io.vyne.jdbc.TableName",
          "shortDisplayName": "TableName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.jdbc.TableName",
            "parameters": [],
            "name": "TableName",
            "parameterizedName": "io.vyne.jdbc.TableName",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.TableName",
            "shortDisplayName": "TableName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "JdbcConnectors",
              "version": "0.0.0",
              "content": "namespace io.vyne.jdbc {\n   TableName inherits String\n}",
              "packageIdentifier": {
                "organisation": "io.vyne",
                "name": "core-types",
                "version": "1.0.0",
                "unversionedId": "io.vyne/core-types",
                "id": "io.vyne/core-types/1.0.0",
                "uriSafeId": "io.vyne:core-types:1.0.0"
              },
              "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
              "id": "JdbcConnectors:0.0.0",
              "contentHash": "df61fc"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.jdbc.TableName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.jdbc.TableName",
          "fullyQualifiedName": "io.vyne.jdbc.TableName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.jdbc.TableName",
            "parameters": [],
            "name": "TableName",
            "parameterizedName": "io.vyne.jdbc.TableName",
            "namespace": "io.vyne.jdbc",
            "longDisplayName": "io.vyne.jdbc.TableName",
            "shortDisplayName": "TableName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "JdbcConnectors",
            "version": "0.0.0",
            "content": "namespace io.vyne.jdbc {\n   TableName inherits String\n}",
            "packageIdentifier": {
              "organisation": "io.vyne",
              "name": "core-types",
              "version": "1.0.0",
              "unversionedId": "io.vyne/core-types",
              "id": "io.vyne/core-types/1.0.0",
              "uriSafeId": "io.vyne:core-types:1.0.0"
            },
            "packageQualifiedName": "[io.vyne/core-types/1.0.0]/JdbcConnectors",
            "id": "JdbcConnectors:0.0.0",
            "contentHash": "df61fc"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "io.vyne.kafka.ConnectionName",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.ConnectionName",
          "shortDisplayName": "ConnectionName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.kafka.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.kafka.ConnectionName",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "version": "0.0.0",
              "content": "namespace io.vyne.kafka {\n   ConnectionName inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
              "contentHash": "28e454"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.kafka.ConnectionName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.kafka.ConnectionName",
          "fullyQualifiedName": "io.vyne.kafka.ConnectionName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.kafka.ConnectionName",
            "parameters": [],
            "name": "ConnectionName",
            "parameterizedName": "io.vyne.kafka.ConnectionName",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.ConnectionName",
            "shortDisplayName": "ConnectionName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   ConnectionName inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "28e454"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.KafkaOperation",
          "parameters": [],
          "name": "KafkaOperation",
          "parameterizedName": "io.vyne.kafka.KafkaOperation",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.KafkaOperation",
          "shortDisplayName": "KafkaOperation"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.kafka.KafkaOperation",
            "parameters": [],
            "name": "KafkaOperation",
            "parameterizedName": "io.vyne.kafka.KafkaOperation",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.KafkaOperation",
            "shortDisplayName": "KafkaOperation"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "version": "0.0.0",
              "content": "namespace io.vyne.kafka {\n   annotation KafkaOperation {\n         topic : TopicName inherits String\n         offset : TopicOffset\n      }\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
              "contentHash": "890f9c"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.kafka.KafkaOperation",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.kafka.KafkaOperation",
          "fullyQualifiedName": "io.vyne.kafka.KafkaOperation",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.kafka.KafkaOperation",
            "parameters": [],
            "name": "KafkaOperation",
            "parameterizedName": "io.vyne.kafka.KafkaOperation",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.KafkaOperation",
            "shortDisplayName": "KafkaOperation"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   annotation KafkaOperation {\n         topic : TopicName inherits String\n         offset : TopicOffset\n      }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "890f9c"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.KafkaService",
          "parameters": [],
          "name": "KafkaService",
          "parameterizedName": "io.vyne.kafka.KafkaService",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.KafkaService",
          "shortDisplayName": "KafkaService"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.kafka.KafkaService",
            "parameters": [],
            "name": "KafkaService",
            "parameterizedName": "io.vyne.kafka.KafkaService",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.KafkaService",
            "shortDisplayName": "KafkaService"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "version": "0.0.0",
              "content": "namespace io.vyne.kafka {\n   annotation KafkaService {\n         connectionName : ConnectionName inherits String\n      }\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
              "contentHash": "6826ff"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "io.vyne.kafka.KafkaService",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.kafka.KafkaService",
          "fullyQualifiedName": "io.vyne.kafka.KafkaService",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.kafka.KafkaService",
            "parameters": [],
            "name": "KafkaService",
            "parameterizedName": "io.vyne.kafka.KafkaService",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.KafkaService",
            "shortDisplayName": "KafkaService"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   annotation KafkaService {\n         connectionName : ConnectionName inherits String\n      }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "6826ff"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.TopicName",
          "parameters": [],
          "name": "TopicName",
          "parameterizedName": "io.vyne.kafka.TopicName",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.TopicName",
          "shortDisplayName": "TopicName"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.kafka.TopicName",
            "parameters": [],
            "name": "TopicName",
            "parameterizedName": "io.vyne.kafka.TopicName",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.TopicName",
            "shortDisplayName": "TopicName"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "version": "0.0.0",
              "content": "namespace io.vyne.kafka {\n   TopicName inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
              "contentHash": "c6ba94"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.kafka.TopicName",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.kafka.TopicName",
          "fullyQualifiedName": "io.vyne.kafka.TopicName",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.kafka.TopicName",
            "parameters": [],
            "name": "TopicName",
            "parameterizedName": "io.vyne.kafka.TopicName",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.TopicName",
            "shortDisplayName": "TopicName"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   TopicName inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "c6ba94"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.kafka.TopicOffset",
          "parameters": [],
          "name": "TopicOffset",
          "parameterizedName": "io.vyne.kafka.TopicOffset",
          "namespace": "io.vyne.kafka",
          "longDisplayName": "io.vyne.kafka.TopicOffset",
          "shortDisplayName": "TopicOffset"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.kafka.TopicOffset",
            "parameters": [],
            "name": "TopicOffset",
            "parameterizedName": "io.vyne.kafka.TopicOffset",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.TopicOffset",
            "shortDisplayName": "TopicOffset"
          },
          "attributes": {},
          "modifiers": [
            "ENUM"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [
            {
              "name": "earliest",
              "value": "earliest",
              "synonyms": [],
              "typeDoc": ""
            },
            {
              "name": "latest",
              "value": "latest",
              "synonyms": [],
              "typeDoc": ""
            },
            {
              "name": "none",
              "value": "none",
              "synonyms": [],
              "typeDoc": ""
            }
          ],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "version": "0.0.0",
              "content": "namespace io.vyne.kafka {\n   enum TopicOffset {\n         earliest,\n         latest,\n         none\n      }\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
              "contentHash": "d31705"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.kafka.TopicOffset",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.kafka.TopicOffset",
          "fullyQualifiedName": "io.vyne.kafka.TopicOffset",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.kafka.TopicOffset",
            "parameters": [],
            "name": "TopicOffset",
            "parameterizedName": "io.vyne.kafka.TopicOffset",
            "namespace": "io.vyne.kafka",
            "longDisplayName": "io.vyne.kafka.TopicOffset",
            "shortDisplayName": "TopicOffset"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace io.vyne.kafka {\n   enum TopicOffset {\n         earliest,\n         latest,\n         none\n      }\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "d31705"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.reviews.FilmReview",
          "parameters": [],
          "name": "FilmReview",
          "parameterizedName": "io.vyne.reviews.FilmReview",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.FilmReview",
          "shortDisplayName": "FilmReview"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.reviews.FilmReview",
            "parameters": [],
            "name": "FilmReview",
            "parameterizedName": "io.vyne.reviews.FilmReview",
            "namespace": "io.vyne.reviews",
            "longDisplayName": "io.vyne.reviews.FilmReview",
            "shortDisplayName": "FilmReview"
          },
          "attributes": {
            "filmId": {
              "type": {
                "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                "parameters": [],
                "name": "SquashedTomatoesFilmId",
                "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                "namespace": "films.reviews",
                "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                "shortDisplayName": "SquashedTomatoesFilmId"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "films.reviews.SquashedTomatoesFilmId",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "filmReview": {
              "type": {
                "fullyQualifiedName": "films.reviews.ReviewText",
                "parameters": [],
                "name": "ReviewText",
                "parameterizedName": "films.reviews.ReviewText",
                "namespace": "films.reviews",
                "longDisplayName": "films.reviews.ReviewText",
                "shortDisplayName": "ReviewText"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "films.reviews.ReviewText",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            },
            "score": {
              "type": {
                "fullyQualifiedName": "films.reviews.FilmReviewScore",
                "parameters": [],
                "name": "FilmReviewScore",
                "parameterizedName": "films.reviews.FilmReviewScore",
                "namespace": "films.reviews",
                "longDisplayName": "films.reviews.FilmReviewScore",
                "shortDisplayName": "FilmReviewScore"
              },
              "modifiers": [],
              "typeDoc": null,
              "defaultValue": null,
              "nullable": false,
              "typeDisplayName": "films.reviews.FilmReviewScore",
              "metadata": [],
              "sourcedBy": null,
              "format": null
            }
          },
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "squashed-tomatoes",
              "version": "0.0.0",
              "content": "import films.reviews.SquashedTomatoesFilmId\nimport films.reviews.ReviewText\nimport films.reviews.FilmReviewScore\nnamespace io.vyne.reviews {\n   model FilmReview {\n         filmId : films.reviews.SquashedTomatoesFilmId\n         filmReview : films.reviews.ReviewText\n         score : films.reviews.FilmReviewScore\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.petflix.demos",
                "name": "films-reviews",
                "version": "0.0.0",
                "unversionedId": "io.petflix.demos/films-reviews",
                "id": "io.petflix.demos/films-reviews/0.0.0",
                "uriSafeId": "io.petflix.demos:films-reviews:0.0.0"
              },
              "packageQualifiedName": "[io.petflix.demos/films-reviews/0.0.0]/squashed-tomatoes",
              "id": "squashed-tomatoes:0.0.0",
              "contentHash": "b7a47b"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "io.vyne.reviews.FilmReview",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "io.vyne.reviews.FilmReview",
          "fullyQualifiedName": "io.vyne.reviews.FilmReview",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.reviews.FilmReview",
            "parameters": [],
            "name": "FilmReview",
            "parameterizedName": "io.vyne.reviews.FilmReview",
            "namespace": "io.vyne.reviews",
            "longDisplayName": "io.vyne.reviews.FilmReview",
            "shortDisplayName": "FilmReview"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": false
        },
        "sources": [
          {
            "name": "squashed-tomatoes",
            "version": "0.0.0",
            "content": "import films.reviews.SquashedTomatoesFilmId\nimport films.reviews.ReviewText\nimport films.reviews.FilmReviewScore\nnamespace io.vyne.reviews {\n   model FilmReview {\n         filmId : films.reviews.SquashedTomatoesFilmId\n         filmReview : films.reviews.ReviewText\n         score : films.reviews.FilmReviewScore\n      }\n}",
            "packageIdentifier": {
              "organisation": "io.petflix.demos",
              "name": "films-reviews",
              "version": "0.0.0",
              "unversionedId": "io.petflix.demos/films-reviews",
              "id": "io.petflix.demos/films-reviews/0.0.0",
              "uriSafeId": "io.petflix.demos:films-reviews:0.0.0"
            },
            "packageQualifiedName": "[io.petflix.demos/films-reviews/0.0.0]/squashed-tomatoes",
            "id": "squashed-tomatoes:0.0.0",
            "contentHash": "b7a47b"
          }
        ],
        "typeDoc": "",
        "attributeNames": [
          "filmId",
          "filmReview",
          "score"
        ]
      },
      {
        "name": {
          "fullyQualifiedName": "io.vyne.reviews.ReviewsService",
          "parameters": [],
          "name": "ReviewsService",
          "parameterizedName": "io.vyne.reviews.ReviewsService",
          "namespace": "io.vyne.reviews",
          "longDisplayName": "io.vyne.reviews.ReviewsService",
          "shortDisplayName": "ReviewsService"
        },
        "kind": "SERVICE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "io.vyne.reviews.ReviewsService",
            "parameters": [],
            "name": "ReviewsService",
            "parameterizedName": "io.vyne.reviews.ReviewsService",
            "namespace": "io.vyne.reviews",
            "longDisplayName": "io.vyne.reviews.ReviewsService",
            "shortDisplayName": "ReviewsService"
          },
          "operations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
                "parameters": [],
                "name": "ReviewsService@@getReview",
                "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
                "shortDisplayName": "getReview"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                    "parameters": [],
                    "name": "SquashedTomatoesFilmId",
                    "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                    "namespace": "films.reviews",
                    "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                    "shortDisplayName": "SquashedTomatoesFilmId"
                  },
                  "name": "filmId",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                    "parameters": [],
                    "name": "SquashedTomatoesFilmId",
                    "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                    "namespace": "films.reviews",
                    "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                    "shortDisplayName": "SquashedTomatoesFilmId"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.reviews.FilmReview",
                "parameters": [],
                "name": "FilmReview",
                "parameterizedName": "io.vyne.reviews.FilmReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.FilmReview",
                "shortDisplayName": "FilmReview"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.reviews.FilmReview",
                  "parameters": [],
                  "name": "FilmReview",
                  "parameterizedName": "io.vyne.reviews.FilmReview",
                  "namespace": "io.vyne.reviews",
                  "longDisplayName": "io.vyne.reviews.FilmReview",
                  "shortDisplayName": "FilmReview"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.reviews.FilmReview",
                "parameters": [],
                "name": "FilmReview",
                "parameterizedName": "io.vyne.reviews.FilmReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.FilmReview",
                "shortDisplayName": "FilmReview"
              },
              "name": "getReview",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
                "parameters": [],
                "name": "ReviewsService@@getReview",
                "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
                "shortDisplayName": "getReview"
              }
            }
          ],
          "queryOperations": [],
          "streamOperations": [],
          "tableOperations": [],
          "metadata": [],
          "sourceCode": [
            {
              "name": "squashed-tomatoes",
              "version": "0.0.0",
              "content": "import films.reviews.SquashedTomatoesFilmId\nnamespace io.vyne.reviews {\n   service ReviewsService {\n         @HttpOperation(method = \"GET\" , url = \"http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}\")\n         operation getReview(  filmId : films.reviews.SquashedTomatoesFilmId ) : FilmReview\n      }\n}",
              "packageIdentifier": {
                "organisation": "io.petflix.demos",
                "name": "films-reviews",
                "version": "0.0.0",
                "unversionedId": "io.petflix.demos/films-reviews",
                "id": "io.petflix.demos/films-reviews/0.0.0",
                "uriSafeId": "io.petflix.demos:films-reviews:0.0.0"
              },
              "packageQualifiedName": "[io.petflix.demos/films-reviews/0.0.0]/squashed-tomatoes",
              "id": "squashed-tomatoes:0.0.0",
              "contentHash": "0945a0"
            }
          ],
          "typeDoc": null,
          "lineage": null,
          "serviceKind": "API",
          "remoteOperations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
                "parameters": [],
                "name": "ReviewsService@@getReview",
                "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
                "shortDisplayName": "getReview"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                    "parameters": [],
                    "name": "SquashedTomatoesFilmId",
                    "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                    "namespace": "films.reviews",
                    "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                    "shortDisplayName": "SquashedTomatoesFilmId"
                  },
                  "name": "filmId",
                  "metadata": [],
                  "constraints": [],
                  "typeName": {
                    "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                    "parameters": [],
                    "name": "SquashedTomatoesFilmId",
                    "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                    "namespace": "films.reviews",
                    "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                    "shortDisplayName": "SquashedTomatoesFilmId"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "io.vyne.reviews.FilmReview",
                "parameters": [],
                "name": "FilmReview",
                "parameterizedName": "io.vyne.reviews.FilmReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.FilmReview",
                "shortDisplayName": "FilmReview"
              },
              "operationType": null,
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "HttpOperation",
                    "parameters": [],
                    "name": "HttpOperation",
                    "parameterizedName": "HttpOperation",
                    "namespace": "",
                    "longDisplayName": "HttpOperation",
                    "shortDisplayName": "HttpOperation"
                  },
                  "params": {
                    "method": "GET",
                    "url": "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}"
                  }
                }
              ],
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "io.vyne.reviews.FilmReview",
                  "parameters": [],
                  "name": "FilmReview",
                  "parameterizedName": "io.vyne.reviews.FilmReview",
                  "namespace": "io.vyne.reviews",
                  "longDisplayName": "io.vyne.reviews.FilmReview",
                  "shortDisplayName": "FilmReview"
                },
                "constraints": []
              },
              "typeDoc": null,
              "returnTypeName": {
                "fullyQualifiedName": "io.vyne.reviews.FilmReview",
                "parameters": [],
                "name": "FilmReview",
                "parameterizedName": "io.vyne.reviews.FilmReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.FilmReview",
                "shortDisplayName": "FilmReview"
              },
              "name": "getReview",
              "memberQualifiedName": {
                "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
                "parameters": [],
                "name": "ReviewsService@@getReview",
                "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
                "namespace": "io.vyne.reviews",
                "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
                "shortDisplayName": "getReview"
              }
            }
          ],
          "qualifiedName": "io.vyne.reviews.ReviewsService",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.reviews.ReviewsService",
            "parameters": [],
            "name": "ReviewsService",
            "parameterizedName": "io.vyne.reviews.ReviewsService",
            "namespace": "io.vyne.reviews",
            "longDisplayName": "io.vyne.reviews.ReviewsService",
            "shortDisplayName": "ReviewsService"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "name": "getReview",
          "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
          "namespace": "io.vyne.reviews",
          "parameters": [],
          "parameterizedName": "ReviewsService / getReview",
          "shortDisplayName": "getReview",
          "longDisplayName": "ReviewsService / getReview"
        },
        "kind": "OPERATION",
        "aliasForType": null,
        "member": {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
            "parameters": [],
            "name": "ReviewsService@@getReview",
            "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
            "namespace": "io.vyne.reviews",
            "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
            "shortDisplayName": "getReview"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                "parameters": [],
                "name": "SquashedTomatoesFilmId",
                "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                "namespace": "films.reviews",
                "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                "shortDisplayName": "SquashedTomatoesFilmId"
              },
              "name": "filmId",
              "metadata": [],
              "constraints": [],
              "typeName": {
                "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
                "parameters": [],
                "name": "SquashedTomatoesFilmId",
                "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
                "namespace": "films.reviews",
                "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
                "shortDisplayName": "SquashedTomatoesFilmId"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "io.vyne.reviews.FilmReview",
            "parameters": [],
            "name": "FilmReview",
            "parameterizedName": "io.vyne.reviews.FilmReview",
            "namespace": "io.vyne.reviews",
            "longDisplayName": "io.vyne.reviews.FilmReview",
            "shortDisplayName": "FilmReview"
          },
          "operationType": null,
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "HttpOperation",
                "namespace": "",
                "longDisplayName": "HttpOperation",
                "shortDisplayName": "HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "io.vyne.reviews.FilmReview",
              "parameters": [],
              "name": "FilmReview",
              "parameterizedName": "io.vyne.reviews.FilmReview",
              "namespace": "io.vyne.reviews",
              "longDisplayName": "io.vyne.reviews.FilmReview",
              "shortDisplayName": "FilmReview"
            },
            "constraints": []
          },
          "typeDoc": null,
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.reviews.FilmReview",
            "parameters": [],
            "name": "FilmReview",
            "parameterizedName": "io.vyne.reviews.FilmReview",
            "namespace": "io.vyne.reviews",
            "longDisplayName": "io.vyne.reviews.FilmReview",
            "shortDisplayName": "FilmReview"
          },
          "name": "getReview",
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.reviews.ReviewsService@@getReview",
            "parameters": [],
            "name": "ReviewsService@@getReview",
            "parameterizedName": "io.vyne.reviews.ReviewsService@@getReview",
            "namespace": "io.vyne.reviews",
            "longDisplayName": "io.vyne.reviews.ReviewsService / getReview",
            "shortDisplayName": "getReview"
          }
        },
        "sources": [],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Any",
          "shortDisplayName": "Any"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Any",
            "parameters": [],
            "name": "Any",
            "parameterizedName": "lang.taxi.Any",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Any",
            "shortDisplayName": "Any"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "Can be anything.  Try to avoid using 'Any' as it's not descriptive - favour using a strongly typed approach instead",
          "paramaterizedName": "lang.taxi.Any",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Any",
            "parameters": [],
            "name": "Any",
            "parameterizedName": "lang.taxi.Any",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Any",
            "shortDisplayName": "Any"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.Any",
          "fullyQualifiedName": "lang.taxi.Any",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Any",
            "parameters": [],
            "name": "Any",
            "parameterizedName": "lang.taxi.Any",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Any",
            "shortDisplayName": "Any"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "Can be anything.  Try to avoid using 'Any' as it's not descriptive - favour using a strongly typed approach instead",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Array",
          "parameters": [],
          "name": "Array",
          "parameterizedName": "lang.taxi.Array",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Array",
          "shortDisplayName": "Array"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Array",
            "shortDisplayName": "Array"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "A collection of things",
          "paramaterizedName": "lang.taxi.Array",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "lang.taxi.Array",
          "fullyQualifiedName": "lang.taxi.Array",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Array",
            "shortDisplayName": "Array"
          },
          "underlyingTypeParameters": [],
          "isCollection": true,
          "isStream": false,
          "collectionType": {
            "fullyQualifiedName": "lang.taxi.Any",
            "parameters": [],
            "name": "Any",
            "parameterizedName": "lang.taxi.Any",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Any",
            "shortDisplayName": "Any"
          },
          "isScalar": false
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "A collection of things",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Boolean",
          "parameters": [],
          "name": "Boolean",
          "parameterizedName": "lang.taxi.Boolean",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Boolean",
          "shortDisplayName": "Boolean"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Boolean",
            "parameters": [],
            "name": "Boolean",
            "parameterizedName": "lang.taxi.Boolean",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Boolean",
            "shortDisplayName": "Boolean"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "Represents a value which is either `true` or `false`.",
          "paramaterizedName": "lang.taxi.Boolean",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Boolean",
            "parameters": [],
            "name": "Boolean",
            "parameterizedName": "lang.taxi.Boolean",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Boolean",
            "shortDisplayName": "Boolean"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.Boolean",
          "fullyQualifiedName": "lang.taxi.Boolean",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Boolean",
            "parameters": [],
            "name": "Boolean",
            "parameterizedName": "lang.taxi.Boolean",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Boolean",
            "shortDisplayName": "Boolean"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "Represents a value which is either `true` or `false`.",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Date",
          "parameters": [],
          "name": "Date",
          "parameterizedName": "lang.taxi.Date",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Date",
          "shortDisplayName": "Date"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Date",
            "parameters": [],
            "name": "Date",
            "parameterizedName": "lang.taxi.Date",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Date",
            "shortDisplayName": "Date"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "A date, without a time or timezone.",
          "paramaterizedName": "lang.taxi.Date",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [
              "yyyy-MM-dd"
            ],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": true,
            "isEmpty": false
          },
          "offset": null,
          "format": [
            "yyyy-MM-dd"
          ],
          "hasFormat": true,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Date",
            "parameters": [],
            "name": "Date",
            "parameterizedName": "lang.taxi.Date",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Date",
            "shortDisplayName": "Date"
          },
          "hasExpression": false,
          "unformattedTypeName": {
            "fullyQualifiedName": "lang.taxi.Date",
            "parameters": [],
            "name": "Date",
            "parameterizedName": "lang.taxi.Date",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Date",
            "shortDisplayName": "Date"
          },
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.Date",
          "fullyQualifiedName": "lang.taxi.Date",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Date",
            "parameters": [],
            "name": "Date",
            "parameterizedName": "lang.taxi.Date",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Date",
            "shortDisplayName": "Date"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "A date, without a time or timezone.",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.DateTime",
          "parameters": [],
          "name": "DateTime",
          "parameterizedName": "lang.taxi.DateTime",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.DateTime",
          "shortDisplayName": "DateTime"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.DateTime",
            "parameters": [],
            "name": "DateTime",
            "parameterizedName": "lang.taxi.DateTime",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.DateTime",
            "shortDisplayName": "DateTime"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "A date and time, without a timezone.  Generally, favour using Instant which represents a point-in-time, as it has a timezone attached",
          "paramaterizedName": "lang.taxi.DateTime",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [
              "yyyy-MM-dd'T'HH:mm:ss.SSS"
            ],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": true,
            "isEmpty": false
          },
          "offset": null,
          "format": [
            "yyyy-MM-dd'T'HH:mm:ss.SSS"
          ],
          "hasFormat": true,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.DateTime",
            "parameters": [],
            "name": "DateTime",
            "parameterizedName": "lang.taxi.DateTime",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.DateTime",
            "shortDisplayName": "DateTime"
          },
          "hasExpression": false,
          "unformattedTypeName": {
            "fullyQualifiedName": "lang.taxi.DateTime",
            "parameters": [],
            "name": "DateTime",
            "parameterizedName": "lang.taxi.DateTime",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.DateTime",
            "shortDisplayName": "DateTime"
          },
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.DateTime",
          "fullyQualifiedName": "lang.taxi.DateTime",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.DateTime",
            "parameters": [],
            "name": "DateTime",
            "parameterizedName": "lang.taxi.DateTime",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.DateTime",
            "shortDisplayName": "DateTime"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "A date and time, without a timezone.  Generally, favour using Instant which represents a point-in-time, as it has a timezone attached",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Decimal",
          "shortDisplayName": "Decimal"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "A signed decimal number - ie., a whole number with decimal places.",
          "paramaterizedName": "lang.taxi.Decimal",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.Decimal",
          "fullyQualifiedName": "lang.taxi.Decimal",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "parameters": [],
            "name": "Decimal",
            "parameterizedName": "lang.taxi.Decimal",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Decimal",
            "shortDisplayName": "Decimal"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "A signed decimal number - ie., a whole number with decimal places.",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Double",
          "parameters": [],
          "name": "Double",
          "parameterizedName": "lang.taxi.Double",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Double",
          "shortDisplayName": "Double"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Double",
            "parameters": [],
            "name": "Double",
            "parameterizedName": "lang.taxi.Double",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Double",
            "shortDisplayName": "Double"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "Represents a double-precision 64-bit IEEE 754 floating point number.",
          "paramaterizedName": "lang.taxi.Double",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Double",
            "parameters": [],
            "name": "Double",
            "parameterizedName": "lang.taxi.Double",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Double",
            "shortDisplayName": "Double"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.Double",
          "fullyQualifiedName": "lang.taxi.Double",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Double",
            "parameters": [],
            "name": "Double",
            "parameterizedName": "lang.taxi.Double",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Double",
            "shortDisplayName": "Double"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "Represents a double-precision 64-bit IEEE 754 floating point number.",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Instant",
          "parameters": [],
          "name": "Instant",
          "parameterizedName": "lang.taxi.Instant",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Instant",
          "shortDisplayName": "Instant"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Instant",
            "parameters": [],
            "name": "Instant",
            "parameterizedName": "lang.taxi.Instant",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Instant",
            "shortDisplayName": "Instant"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "A point in time, with date, time and timezone.  Follows ISO standard convention of yyyy-MM-dd'T'HH:mm:ss.SSSZ",
          "paramaterizedName": "lang.taxi.Instant",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [
              "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
            ],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": true,
            "isEmpty": false
          },
          "offset": null,
          "format": [
            "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
          ],
          "hasFormat": true,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Instant",
            "parameters": [],
            "name": "Instant",
            "parameterizedName": "lang.taxi.Instant",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Instant",
            "shortDisplayName": "Instant"
          },
          "hasExpression": false,
          "unformattedTypeName": {
            "fullyQualifiedName": "lang.taxi.Instant",
            "parameters": [],
            "name": "Instant",
            "parameterizedName": "lang.taxi.Instant",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Instant",
            "shortDisplayName": "Instant"
          },
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.Instant",
          "fullyQualifiedName": "lang.taxi.Instant",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Instant",
            "parameters": [],
            "name": "Instant",
            "parameterizedName": "lang.taxi.Instant",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Instant",
            "shortDisplayName": "Instant"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "A point in time, with date, time and timezone.  Follows ISO standard convention of yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Int",
          "shortDisplayName": "Int"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "A signed integer - ie. a whole number (positive or negative), with no decimal places",
          "paramaterizedName": "lang.taxi.Int",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.Int",
          "fullyQualifiedName": "lang.taxi.Int",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "A signed integer - ie. a whole number (positive or negative), with no decimal places",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Stream",
          "parameters": [],
          "name": "Stream",
          "parameterizedName": "lang.taxi.Stream",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Stream",
          "shortDisplayName": "Stream"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Stream",
            "parameters": [],
            "name": "Stream",
            "parameterizedName": "lang.taxi.Stream",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Stream",
            "shortDisplayName": "Stream"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "Result of a service publishing sequence of events",
          "paramaterizedName": "lang.taxi.Stream",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "lang.taxi.Stream",
          "fullyQualifiedName": "lang.taxi.Stream",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Stream",
            "parameters": [],
            "name": "Stream",
            "parameterizedName": "lang.taxi.Stream",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Stream",
            "shortDisplayName": "Stream"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": true,
          "collectionType": {
            "fullyQualifiedName": "lang.taxi.Any",
            "parameters": [],
            "name": "Any",
            "parameterizedName": "lang.taxi.Any",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Any",
            "shortDisplayName": "Any"
          },
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "Result of a service publishing sequence of events",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.String",
          "shortDisplayName": "String"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "A collection of characters.",
          "paramaterizedName": "lang.taxi.String",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.String",
          "fullyQualifiedName": "lang.taxi.String",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "A collection of characters.",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Time",
          "parameters": [],
          "name": "Time",
          "parameterizedName": "lang.taxi.Time",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Time",
          "shortDisplayName": "Time"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Time",
            "parameters": [],
            "name": "Time",
            "parameterizedName": "lang.taxi.Time",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Time",
            "shortDisplayName": "Time"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "Time only, excluding the date part",
          "paramaterizedName": "lang.taxi.Time",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [
              "HH:mm:ss"
            ],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": true,
            "isEmpty": false
          },
          "offset": null,
          "format": [
            "HH:mm:ss"
          ],
          "hasFormat": true,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Time",
            "parameters": [],
            "name": "Time",
            "parameterizedName": "lang.taxi.Time",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Time",
            "shortDisplayName": "Time"
          },
          "hasExpression": false,
          "unformattedTypeName": {
            "fullyQualifiedName": "lang.taxi.Time",
            "parameters": [],
            "name": "Time",
            "parameterizedName": "lang.taxi.Time",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Time",
            "shortDisplayName": "Time"
          },
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.Time",
          "fullyQualifiedName": "lang.taxi.Time",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Time",
            "parameters": [],
            "name": "Time",
            "parameterizedName": "lang.taxi.Time",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Time",
            "shortDisplayName": "Time"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "Time only, excluding the date part",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "lang.taxi.Void",
          "parameters": [],
          "name": "Void",
          "parameterizedName": "lang.taxi.Void",
          "namespace": "lang.taxi",
          "longDisplayName": "lang.taxi.Void",
          "shortDisplayName": "Void"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "lang.taxi.Void",
            "parameters": [],
            "name": "Void",
            "parameterizedName": "lang.taxi.Void",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Void",
            "shortDisplayName": "Void"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "<unknown>",
              "version": "0.0.0",
              "content": "Native",
              "packageIdentifier": null,
              "packageQualifiedName": "<unknown>",
              "id": "<unknown>:0.0.0",
              "contentHash": "d509e4"
            }
          ],
          "typeParameters": [],
          "typeDoc": "Nothing.  Represents the return value of operations that don't return anything.",
          "paramaterizedName": "lang.taxi.Void",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": null,
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Void",
            "parameters": [],
            "name": "Void",
            "parameterizedName": "lang.taxi.Void",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Void",
            "shortDisplayName": "Void"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": true,
          "longDisplayName": "lang.taxi.Void",
          "fullyQualifiedName": "lang.taxi.Void",
          "memberQualifiedName": {
            "fullyQualifiedName": "lang.taxi.Void",
            "parameters": [],
            "name": "Void",
            "parameterizedName": "lang.taxi.Void",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Void",
            "shortDisplayName": "Void"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "<unknown>",
            "version": "0.0.0",
            "content": "Native",
            "packageIdentifier": null,
            "packageQualifiedName": "<unknown>",
            "id": "<unknown>:0.0.0",
            "contentHash": "d509e4"
          }
        ],
        "typeDoc": "Nothing.  Represents the return value of operations that don't return anything.",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "language.types.LanguageId",
          "parameters": [],
          "name": "LanguageId",
          "parameterizedName": "language.types.LanguageId",
          "namespace": "language.types",
          "longDisplayName": "language.types.LanguageId",
          "shortDisplayName": "LanguageId"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "language.types.LanguageId",
            "parameters": [],
            "name": "LanguageId",
            "parameterizedName": "language.types.LanguageId",
            "namespace": "language.types",
            "longDisplayName": "language.types.LanguageId",
            "shortDisplayName": "LanguageId"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Int",
              "parameters": [],
              "name": "Int",
              "parameterizedName": "lang.taxi.Int",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Int",
              "shortDisplayName": "Int"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/language/types/LanguageId.taxi",
              "version": "0.0.0",
              "content": "namespace language.types {\n   type LanguageId inherits Int\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/language/types/LanguageId.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/language/types/LanguageId.taxi:0.0.0",
              "contentHash": "abbc59"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "language.types.LanguageId",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "language.types.LanguageId",
          "fullyQualifiedName": "language.types.LanguageId",
          "memberQualifiedName": {
            "fullyQualifiedName": "language.types.LanguageId",
            "parameters": [],
            "name": "LanguageId",
            "parameterizedName": "language.types.LanguageId",
            "namespace": "language.types",
            "longDisplayName": "language.types.LanguageId",
            "shortDisplayName": "LanguageId"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/language/types/LanguageId.taxi",
            "version": "0.0.0",
            "content": "namespace language.types {\n   type LanguageId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/language/types/LanguageId.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/language/types/LanguageId.taxi:0.0.0",
            "contentHash": "abbc59"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "staff.types.StaffId",
          "parameters": [],
          "name": "StaffId",
          "parameterizedName": "staff.types.StaffId",
          "namespace": "staff.types",
          "longDisplayName": "staff.types.StaffId",
          "shortDisplayName": "StaffId"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "staff.types.StaffId",
            "parameters": [],
            "name": "StaffId",
            "parameterizedName": "staff.types.StaffId",
            "namespace": "staff.types",
            "longDisplayName": "staff.types.StaffId",
            "shortDisplayName": "StaffId"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.Int",
              "parameters": [],
              "name": "Int",
              "parameterizedName": "lang.taxi.Int",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.Int",
              "shortDisplayName": "Int"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/staff/types/StaffId.taxi",
              "version": "0.0.0",
              "content": "namespace staff.types {\n   type StaffId inherits Int\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/staff/types/StaffId.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/staff/types/StaffId.taxi:0.0.0",
              "contentHash": "d4ebc0"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "staff.types.StaffId",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.Int",
            "shortDisplayName": "Int"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "staff.types.StaffId",
          "fullyQualifiedName": "staff.types.StaffId",
          "memberQualifiedName": {
            "fullyQualifiedName": "staff.types.StaffId",
            "parameters": [],
            "name": "StaffId",
            "parameterizedName": "staff.types.StaffId",
            "namespace": "staff.types",
            "longDisplayName": "staff.types.StaffId",
            "shortDisplayName": "StaffId"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/staff/types/StaffId.taxi",
            "version": "0.0.0",
            "content": "namespace staff.types {\n   type StaffId inherits Int\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/staff/types/StaffId.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/staff/types/StaffId.taxi:0.0.0",
            "contentHash": "d4ebc0"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "taxi.stdlib.Format",
          "parameters": [],
          "name": "Format",
          "parameterizedName": "taxi.stdlib.Format",
          "namespace": "taxi.stdlib",
          "longDisplayName": "taxi.stdlib.Format",
          "shortDisplayName": "Format"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "taxi.stdlib.Format",
            "parameters": [],
            "name": "Format",
            "parameterizedName": "taxi.stdlib.Format",
            "namespace": "taxi.stdlib",
            "longDisplayName": "taxi.stdlib.Format",
            "shortDisplayName": "Format"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [],
          "enumValues": [],
          "sources": [
            {
              "name": "Native StdLib",
              "version": "0.0.0",
              "content": "namespace taxi.stdlib {\n   [[ Declares a format (and optionally an offset)\n      for date formats\n      ]]\n      annotation Format {\n          value : String?\n          offset : Int by default(0)\n      }\n}",
              "packageIdentifier": null,
              "packageQualifiedName": "Native StdLib",
              "id": "Native StdLib:0.0.0",
              "contentHash": "cc73a6"
            }
          ],
          "typeParameters": [],
          "typeDoc": null,
          "paramaterizedName": "taxi.stdlib.Format",
          "isTypeAlias": false,
          "formatAndZoneOffset": null,
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": false,
          "basePrimitiveTypeName": null,
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "taxi.stdlib.Format",
          "fullyQualifiedName": "taxi.stdlib.Format",
          "memberQualifiedName": {
            "fullyQualifiedName": "taxi.stdlib.Format",
            "parameters": [],
            "name": "Format",
            "parameterizedName": "taxi.stdlib.Format",
            "namespace": "taxi.stdlib",
            "longDisplayName": "taxi.stdlib.Format",
            "shortDisplayName": "Format"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "Native StdLib",
            "version": "0.0.0",
            "content": "namespace taxi.stdlib {\n   [[ Declares a format (and optionally an offset)\n      for date formats\n      ]]\n      annotation Format {\n          value : String?\n          offset : Int by default(0)\n      }\n}",
            "packageIdentifier": null,
            "packageQualifiedName": "Native StdLib",
            "id": "Native StdLib:0.0.0",
            "contentHash": "cc73a6"
          }
        ],
        "typeDoc": null,
        "attributeNames": []
      },
      {
        "name": {
          "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
          "parameters": [],
          "name": "VyneQlQuery",
          "parameterizedName": "vyne.vyneQl.VyneQlQuery",
          "namespace": "vyne.vyneQl",
          "longDisplayName": "vyne.vyneQl.VyneQlQuery",
          "shortDisplayName": "VyneQlQuery"
        },
        "kind": "TYPE",
        "aliasForType": null,
        "member": {
          "name": {
            "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
            "parameters": [],
            "name": "VyneQlQuery",
            "parameterizedName": "vyne.vyneQl.VyneQlQuery",
            "namespace": "vyne.vyneQl",
            "longDisplayName": "vyne.vyneQl.VyneQlQuery",
            "shortDisplayName": "VyneQlQuery"
          },
          "attributes": {},
          "modifiers": [],
          "metadata": [],
          "aliasForType": null,
          "inheritsFrom": [
            {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "longDisplayName": "lang.taxi.String",
              "shortDisplayName": "String"
            }
          ],
          "enumValues": [],
          "sources": [
            {
              "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "version": "0.0.0",
              "content": "namespace vyne.vyneQl {\n   type VyneQlQuery inherits String\n}",
              "packageIdentifier": {
                "organisation": "demo.vyne",
                "name": "films-demo",
                "version": "0.1.0",
                "unversionedId": "demo.vyne/films-demo",
                "id": "demo.vyne/films-demo/0.1.0",
                "uriSafeId": "demo.vyne:films-demo:0.1.0"
              },
              "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
              "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
              "contentHash": "79e033"
            }
          ],
          "typeParameters": [],
          "typeDoc": "",
          "paramaterizedName": "vyne.vyneQl.VyneQlQuery",
          "isTypeAlias": false,
          "formatAndZoneOffset": {
            "patterns": [],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": false,
            "isEmpty": true
          },
          "offset": null,
          "format": [],
          "hasFormat": false,
          "declaresFormat": true,
          "basePrimitiveTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "longDisplayName": "lang.taxi.String",
            "shortDisplayName": "String"
          },
          "hasExpression": false,
          "unformattedTypeName": null,
          "isParameterType": false,
          "isClosed": false,
          "isPrimitive": false,
          "longDisplayName": "vyne.vyneQl.VyneQlQuery",
          "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
          "memberQualifiedName": {
            "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
            "parameters": [],
            "name": "VyneQlQuery",
            "parameterizedName": "vyne.vyneQl.VyneQlQuery",
            "namespace": "vyne.vyneQl",
            "longDisplayName": "vyne.vyneQl.VyneQlQuery",
            "shortDisplayName": "VyneQlQuery"
          },
          "underlyingTypeParameters": [],
          "isCollection": false,
          "isStream": false,
          "collectionType": null,
          "isScalar": true
        },
        "sources": [
          {
            "name": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "version": "0.0.0",
            "content": "namespace vyne.vyneQl {\n   type VyneQlQuery inherits String\n}",
            "packageIdentifier": {
              "organisation": "demo.vyne",
              "name": "films-demo",
              "version": "0.1.0",
              "unversionedId": "demo.vyne/films-demo",
              "id": "demo.vyne/films-demo/0.1.0",
              "uriSafeId": "demo.vyne:films-demo:0.1.0"
            },
            "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi",
            "id": "file:///home/martypitt/dev/vyne-demos/films/taxi/src/vyne/internal-types.taxi:0.0.0",
            "contentHash": "79e033"
          }
        ],
        "typeDoc": "",
        "attributeNames": []
      }
    ],
    "anonymousTypes": {
      "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8": {
        "name": {
          "fullyQualifiedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
          "parameters": [],
          "name": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
          "parameterizedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
          "namespace": "",
          "longDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
          "shortDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8"
        },
        "attributes": {
          "id": {
            "type": {
              "fullyQualifiedName": "films.FilmId",
              "parameters": [],
              "name": "FilmId",
              "parameterizedName": "films.FilmId",
              "namespace": "films",
              "longDisplayName": "films.FilmId",
              "shortDisplayName": "FilmId"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.FilmId",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "title": {
            "type": {
              "fullyQualifiedName": "film.types.Title",
              "parameters": [],
              "name": "Title",
              "parameterizedName": "film.types.Title",
              "namespace": "film.types",
              "longDisplayName": "film.types.Title",
              "shortDisplayName": "Title"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "film.types.Title",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "provider": {
            "type": {
              "fullyQualifiedName": "films.StreamingProviderName",
              "parameters": [],
              "name": "StreamingProviderName",
              "parameterizedName": "films.StreamingProviderName",
              "namespace": "films",
              "longDisplayName": "films.StreamingProviderName",
              "shortDisplayName": "StreamingProviderName"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.StreamingProviderName",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "cost": {
            "type": {
              "fullyQualifiedName": "films.StreamingProviderPrice",
              "parameters": [],
              "name": "StreamingProviderPrice",
              "parameterizedName": "films.StreamingProviderPrice",
              "namespace": "films",
              "longDisplayName": "films.StreamingProviderPrice",
              "shortDisplayName": "StreamingProviderPrice"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.StreamingProviderPrice",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "reviewScore": {
            "type": {
              "fullyQualifiedName": "films.reviews.FilmReviewScore",
              "parameters": [],
              "name": "FilmReviewScore",
              "parameterizedName": "films.reviews.FilmReviewScore",
              "namespace": "films.reviews",
              "longDisplayName": "films.reviews.FilmReviewScore",
              "shortDisplayName": "FilmReviewScore"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.reviews.FilmReviewScore",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          },
          "reviewText": {
            "type": {
              "fullyQualifiedName": "films.reviews.ReviewText",
              "parameters": [],
              "name": "ReviewText",
              "parameterizedName": "films.reviews.ReviewText",
              "namespace": "films.reviews",
              "longDisplayName": "films.reviews.ReviewText",
              "shortDisplayName": "ReviewText"
            },
            "modifiers": [],
            "typeDoc": null,
            "defaultValue": null,
            "nullable": false,
            "typeDisplayName": "films.reviews.ReviewText",
            "metadata": [],
            "sourcedBy": null,
            "format": null
          }
        },
        "modifiers": [],
        "metadata": [],
        "aliasForType": null,
        "inheritsFrom": [],
        "enumValues": [],
        "sources": [
          {
            "name": "UnknownSource",
            "version": "0.0.0",
            "content": "{\n    id: FilmId\n    title : Title\n    // where can I watch this?\n    provider: StreamingProviderName\n    cost: StreamingProviderPrice\n    // Is it any good?\n    reviewScore: FilmReviewScore\n    reviewText: ReviewText\n}[]",
            "packageIdentifier": null,
            "packageQualifiedName": "UnknownSource",
            "id": "UnknownSource:0.0.0",
            "contentHash": "9cfc9c"
          }
        ],
        "typeParameters": [],
        "typeDoc": "",
        "paramaterizedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "isTypeAlias": false,
        "formatAndZoneOffset": {
          "patterns": [],
          "utcZoneOffsetInMinutes": null,
          "definesPattern": false,
          "isEmpty": true
        },
        "offset": null,
        "format": [],
        "hasFormat": false,
        "declaresFormat": true,
        "basePrimitiveTypeName": null,
        "hasExpression": false,
        "unformattedTypeName": null,
        "isParameterType": false,
        "isClosed": false,
        "isPrimitive": false,
        "longDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "fullyQualifiedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "memberQualifiedName": {
          "fullyQualifiedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
          "parameters": [],
          "name": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
          "parameterizedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
          "namespace": "",
          "longDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
          "shortDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8"
        },
        "underlyingTypeParameters": [],
        "isCollection": false,
        "isStream": false,
        "collectionType": null,
        "isScalar": false
      }
    }
  },
  "anonymousTypes": [
    {
      "name": {
        "fullyQualifiedName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
        "parameters": [],
        "name": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
        "parameterizedName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
        "namespace": "",
        "longDisplayName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
        "shortDisplayName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ"
      },
      "attributes": {
        "id": {
          "type": {
            "fullyQualifiedName": "films.FilmId",
            "parameters": [],
            "name": "FilmId",
            "parameterizedName": "films.FilmId",
            "namespace": "films",
            "longDisplayName": "films.FilmId",
            "shortDisplayName": "FilmId"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.FilmId",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "title": {
          "type": {
            "fullyQualifiedName": "film.types.Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "film.types.Title",
            "namespace": "film.types",
            "longDisplayName": "film.types.Title",
            "shortDisplayName": "Title"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "film.types.Title",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "provider": {
          "type": {
            "fullyQualifiedName": "films.StreamingProviderName",
            "parameters": [],
            "name": "StreamingProviderName",
            "parameterizedName": "films.StreamingProviderName",
            "namespace": "films",
            "longDisplayName": "films.StreamingProviderName",
            "shortDisplayName": "StreamingProviderName"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.StreamingProviderName",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "cost": {
          "type": {
            "fullyQualifiedName": "films.StreamingProviderPrice",
            "parameters": [],
            "name": "StreamingProviderPrice",
            "parameterizedName": "films.StreamingProviderPrice",
            "namespace": "films",
            "longDisplayName": "films.StreamingProviderPrice",
            "shortDisplayName": "StreamingProviderPrice"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.StreamingProviderPrice",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "reviewScore": {
          "type": {
            "fullyQualifiedName": "films.reviews.FilmReviewScore",
            "parameters": [],
            "name": "FilmReviewScore",
            "parameterizedName": "films.reviews.FilmReviewScore",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.FilmReviewScore",
            "shortDisplayName": "FilmReviewScore"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.reviews.FilmReviewScore",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "reviewText": {
          "type": {
            "fullyQualifiedName": "films.reviews.ReviewText",
            "parameters": [],
            "name": "ReviewText",
            "parameterizedName": "films.reviews.ReviewText",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.ReviewText",
            "shortDisplayName": "ReviewText"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.reviews.ReviewText",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        }
      },
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "UnknownSource",
          "version": "0.0.0",
          "content": "{\n    id: FilmId\n    title : Title\n    // where can I watch this?\n    provider: StreamingProviderName\n    cost: StreamingProviderPrice\n    // Is it any good?\n    reviewScore: FilmReviewScore\n    reviewText: ReviewText\n}[]",
          "packageIdentifier": null,
          "packageQualifiedName": "UnknownSource",
          "id": "UnknownSource:0.0.0",
          "contentHash": "9cfc9c"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "paramaterizedName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
      "isTypeAlias": false,
      "formatAndZoneOffset": {
        "patterns": [],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      },
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": true,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
      "fullyQualifiedName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
      "memberQualifiedName": {
        "fullyQualifiedName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
        "parameters": [],
        "name": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
        "parameterizedName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
        "namespace": "",
        "longDisplayName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ",
        "shortDisplayName": "AnonymousProjectedTypeVm61iKapVJ3cAICAxPbmvmVAZGQ"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "parameters": [],
        "name": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "parameterizedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "namespace": "",
        "longDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "shortDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8"
      },
      "attributes": {
        "id": {
          "type": {
            "fullyQualifiedName": "films.FilmId",
            "parameters": [],
            "name": "FilmId",
            "parameterizedName": "films.FilmId",
            "namespace": "films",
            "longDisplayName": "films.FilmId",
            "shortDisplayName": "FilmId"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.FilmId",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "title": {
          "type": {
            "fullyQualifiedName": "film.types.Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "film.types.Title",
            "namespace": "film.types",
            "longDisplayName": "film.types.Title",
            "shortDisplayName": "Title"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "film.types.Title",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "provider": {
          "type": {
            "fullyQualifiedName": "films.StreamingProviderName",
            "parameters": [],
            "name": "StreamingProviderName",
            "parameterizedName": "films.StreamingProviderName",
            "namespace": "films",
            "longDisplayName": "films.StreamingProviderName",
            "shortDisplayName": "StreamingProviderName"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.StreamingProviderName",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "cost": {
          "type": {
            "fullyQualifiedName": "films.StreamingProviderPrice",
            "parameters": [],
            "name": "StreamingProviderPrice",
            "parameterizedName": "films.StreamingProviderPrice",
            "namespace": "films",
            "longDisplayName": "films.StreamingProviderPrice",
            "shortDisplayName": "StreamingProviderPrice"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.StreamingProviderPrice",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "reviewScore": {
          "type": {
            "fullyQualifiedName": "films.reviews.FilmReviewScore",
            "parameters": [],
            "name": "FilmReviewScore",
            "parameterizedName": "films.reviews.FilmReviewScore",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.FilmReviewScore",
            "shortDisplayName": "FilmReviewScore"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.reviews.FilmReviewScore",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        },
        "reviewText": {
          "type": {
            "fullyQualifiedName": "films.reviews.ReviewText",
            "parameters": [],
            "name": "ReviewText",
            "parameterizedName": "films.reviews.ReviewText",
            "namespace": "films.reviews",
            "longDisplayName": "films.reviews.ReviewText",
            "shortDisplayName": "ReviewText"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "films.reviews.ReviewText",
          "metadata": [],
          "sourcedBy": null,
          "format": null
        }
      },
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "UnknownSource",
          "version": "0.0.0",
          "content": "{\n    id: FilmId\n    title : Title\n    // where can I watch this?\n    provider: StreamingProviderName\n    cost: StreamingProviderPrice\n    // Is it any good?\n    reviewScore: FilmReviewScore\n    reviewText: ReviewText\n}[]",
          "packageIdentifier": null,
          "packageQualifiedName": "UnknownSource",
          "id": "UnknownSource:0.0.0",
          "contentHash": "9cfc9c"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "paramaterizedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "isTypeAlias": false,
      "formatAndZoneOffset": {
        "patterns": [],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      },
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": true,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "fullyQualifiedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
      "memberQualifiedName": {
        "fullyQualifiedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "parameters": [],
        "name": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "parameterizedName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "namespace": "",
        "longDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8",
        "shortDisplayName": "AnonymousProjectedTypeY_aMauB1xUDScxpGubf1UzANOt8"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    }
  ]
}
