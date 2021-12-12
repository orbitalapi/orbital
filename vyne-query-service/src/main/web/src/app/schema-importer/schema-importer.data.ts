import {TableTaxiGenerationRequest} from '../db-connection-editor/db-importer.service';
import {TaxiSubmissionResult} from '../services/types.service';

export const importedSchema = {
  'types': [
    {
      'name': {
        'fullyQualifiedName': 'actor.ActorId',
        'parameters': [],
        'parameterizedName': 'actor.ActorId',
        'name': 'ActorId',
        'namespace': 'actor',
        'shortDisplayName': 'ActorId',
        'longDisplayName': 'actor.ActorId'
      },
      'attributes': {},
      'modifiers': [],
      'metadata': [],
      'aliasForType': null,
      'inheritsFrom': [
        {
          'fullyQualifiedName': 'lang.taxi.Int',
          'parameters': [],
          'parameterizedName': 'lang.taxi.Int',
          'name': 'Int',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'Int',
          'longDisplayName': 'lang.taxi.Int'
        }
      ],
      'enumValues': [],
      'sources': [
        {
          'name': 'actor/ActorId.taxi',
          'version': '0.0.0',
          'content': 'namespace actor {\n   type ActorId inherits Int\n}',
          'id': 'actor/ActorId.taxi:0.0.0',
          'contentHash': 'e92c49'
        }
      ],
      'typeParameters': [],
      'typeDoc': '',
      'isTypeAlias': false,
      'offset': null,
      'format': null,
      'hasFormat': false,
      'basePrimitiveTypeName': {
        'fullyQualifiedName': 'lang.taxi.Int',
        'parameters': [],
        'parameterizedName': 'lang.taxi.Int',
        'name': 'Int',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Int',
        'longDisplayName': 'lang.taxi.Int'
      },
      'hasExpression': false,
      'unformattedTypeName': null,
      'isParameterType': false,
      'isClosed': false,
      'isPrimitive': false,
      'fullyQualifiedName': 'actor.ActorId',
      'longDisplayName': 'actor.ActorId',
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.ActorId',
        'parameters': [],
        'parameterizedName': 'actor.ActorId',
        'name': 'ActorId',
        'namespace': 'actor',
        'shortDisplayName': 'ActorId',
        'longDisplayName': 'actor.ActorId'
      },
      'underlyingTypeParameters': [],
      'isCollection': false,
      'isStream': false,
      'collectionType': null,
      'isScalar': true
    },
    {
      'name': {
        'fullyQualifiedName': 'actor.FirstName',
        'parameters': [],
        'parameterizedName': 'actor.FirstName',
        'name': 'FirstName',
        'namespace': 'actor',
        'shortDisplayName': 'FirstName',
        'longDisplayName': 'actor.FirstName'
      },
      'attributes': {},
      'modifiers': [],
      'metadata': [],
      'aliasForType': null,
      'inheritsFrom': [
        {
          'fullyQualifiedName': 'lang.taxi.String',
          'parameters': [],
          'parameterizedName': 'lang.taxi.String',
          'name': 'String',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'String',
          'longDisplayName': 'lang.taxi.String'
        }
      ],
      'enumValues': [],
      'sources': [
        {
          'name': 'actor/FirstName.taxi',
          'version': '0.0.0',
          'content': 'namespace actor {\n   type FirstName inherits String\n}',
          'id': 'actor/FirstName.taxi:0.0.0',
          'contentHash': '35cc9e'
        }
      ],
      'typeParameters': [],
      'typeDoc': '',
      'isTypeAlias': false,
      'offset': null,
      'format': null,
      'hasFormat': false,
      'basePrimitiveTypeName': {
        'fullyQualifiedName': 'lang.taxi.String',
        'parameters': [],
        'parameterizedName': 'lang.taxi.String',
        'name': 'String',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'String',
        'longDisplayName': 'lang.taxi.String'
      },
      'hasExpression': false,
      'unformattedTypeName': null,
      'isParameterType': false,
      'isClosed': false,
      'isPrimitive': false,
      'fullyQualifiedName': 'actor.FirstName',
      'longDisplayName': 'actor.FirstName',
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.FirstName',
        'parameters': [],
        'parameterizedName': 'actor.FirstName',
        'name': 'FirstName',
        'namespace': 'actor',
        'shortDisplayName': 'FirstName',
        'longDisplayName': 'actor.FirstName'
      },
      'underlyingTypeParameters': [],
      'isCollection': false,
      'isStream': false,
      'collectionType': null,
      'isScalar': true
    },
    {
      'name': {
        'fullyQualifiedName': 'actor.LastName',
        'parameters': [],
        'parameterizedName': 'actor.LastName',
        'name': 'LastName',
        'namespace': 'actor',
        'shortDisplayName': 'LastName',
        'longDisplayName': 'actor.LastName'
      },
      'attributes': {},
      'modifiers': [],
      'metadata': [],
      'aliasForType': null,
      'inheritsFrom': [
        {
          'fullyQualifiedName': 'lang.taxi.String',
          'parameters': [],
          'parameterizedName': 'lang.taxi.String',
          'name': 'String',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'String',
          'longDisplayName': 'lang.taxi.String'
        }
      ],
      'enumValues': [],
      'sources': [
        {
          'name': 'actor/LastName.taxi',
          'version': '0.0.0',
          'content': 'namespace actor {\n   type LastName inherits String\n}',
          'id': 'actor/LastName.taxi:0.0.0',
          'contentHash': 'f0830f'
        }
      ],
      'typeParameters': [],
      'typeDoc': '',
      'isTypeAlias': false,
      'offset': null,
      'format': null,
      'hasFormat': false,
      'basePrimitiveTypeName': {
        'fullyQualifiedName': 'lang.taxi.String',
        'parameters': [],
        'parameterizedName': 'lang.taxi.String',
        'name': 'String',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'String',
        'longDisplayName': 'lang.taxi.String'
      },
      'hasExpression': false,
      'unformattedTypeName': null,
      'isParameterType': false,
      'isClosed': false,
      'isPrimitive': false,
      'fullyQualifiedName': 'actor.LastName',
      'longDisplayName': 'actor.LastName',
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.LastName',
        'parameters': [],
        'parameterizedName': 'actor.LastName',
        'name': 'LastName',
        'namespace': 'actor',
        'shortDisplayName': 'LastName',
        'longDisplayName': 'actor.LastName'
      },
      'underlyingTypeParameters': [],
      'isCollection': false,
      'isStream': false,
      'collectionType': null,
      'isScalar': true
    },
    {
      'name': {
        'fullyQualifiedName': 'actor.LastUpdate',
        'parameters': [],
        'parameterizedName': 'actor.LastUpdate',
        'name': 'LastUpdate',
        'namespace': 'actor',
        'shortDisplayName': 'LastUpdate',
        'longDisplayName': 'actor.LastUpdate'
      },
      'attributes': {},
      'modifiers': [],
      'metadata': [],
      'aliasForType': null,
      'inheritsFrom': [
        {
          'fullyQualifiedName': 'lang.taxi.Instant',
          'parameters': [],
          'parameterizedName': 'lang.taxi.Instant',
          'name': 'Instant',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'Instant',
          'longDisplayName': 'lang.taxi.Instant'
        }
      ],
      'enumValues': [],
      'sources': [
        {
          'name': 'actor/LastUpdate.taxi',
          'version': '0.0.0',
          'content': 'namespace actor {\n   type LastUpdate inherits Instant\n}',
          'id': 'actor/LastUpdate.taxi:0.0.0',
          'contentHash': '01bc86'
        }
      ],
      'typeParameters': [],
      'typeDoc': '',
      'isTypeAlias': false,
      'offset': null,
      'format': [
        'yyyy-MM-dd\'T\'HH:mm:ss[.SSS]X'
      ],
      'hasFormat': true,
      'basePrimitiveTypeName': {
        'fullyQualifiedName': 'lang.taxi.Instant',
        'parameters': [],
        'parameterizedName': 'lang.taxi.Instant',
        'name': 'Instant',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Instant',
        'longDisplayName': 'lang.taxi.Instant'
      },
      'hasExpression': false,
      'unformattedTypeName': {
        'fullyQualifiedName': 'actor.LastUpdate',
        'parameters': [],
        'parameterizedName': 'actor.LastUpdate',
        'name': 'LastUpdate',
        'namespace': 'actor',
        'shortDisplayName': 'LastUpdate',
        'longDisplayName': 'actor.LastUpdate'
      },
      'isParameterType': false,
      'isClosed': false,
      'isPrimitive': false,
      'fullyQualifiedName': 'actor.LastUpdate',
      'longDisplayName': 'actor.LastUpdate(yyyy-MM-dd\'T\'HH:mm:ss[.SSS]X)',
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.LastUpdate',
        'parameters': [],
        'parameterizedName': 'actor.LastUpdate',
        'name': 'LastUpdate',
        'namespace': 'actor',
        'shortDisplayName': 'LastUpdate',
        'longDisplayName': 'actor.LastUpdate'
      },
      'underlyingTypeParameters': [],
      'isCollection': false,
      'isStream': false,
      'collectionType': null,
      'isScalar': true
    },
    {
      'name': {
        'fullyQualifiedName': 'actor.Actor',
        'parameters': [],
        'parameterizedName': 'actor.Actor',
        'name': 'Actor',
        'namespace': 'actor',
        'shortDisplayName': 'Actor',
        'longDisplayName': 'actor.Actor'
      },
      'attributes': {
        'actor_id': {
          'type': {
            'fullyQualifiedName': 'actor.ActorId',
            'parameters': [],
            'parameterizedName': 'actor.ActorId',
            'name': 'ActorId',
            'namespace': 'actor',
            'shortDisplayName': 'ActorId',
            'longDisplayName': 'actor.ActorId'
          },
          'modifiers': [],
          'typeDoc': null,
          'defaultValue': null,
          'nullable': false,
          'typeDisplayName': 'actor.ActorId',
          'metadata': [
            {
              'name': {
                'fullyQualifiedName': 'Id',
                'parameters': [],
                'parameterizedName': 'Id',
                'name': 'Id',
                'namespace': '',
                'shortDisplayName': 'Id',
                'longDisplayName': 'Id'
              },
              'params': {}
            }
          ],
          'sourcedBy': null
        },
        'first_name': {
          'type': {
            'fullyQualifiedName': 'actor.FirstName',
            'parameters': [],
            'parameterizedName': 'actor.FirstName',
            'name': 'FirstName',
            'namespace': 'actor',
            'shortDisplayName': 'FirstName',
            'longDisplayName': 'actor.FirstName'
          },
          'modifiers': [],
          'typeDoc': null,
          'defaultValue': null,
          'nullable': false,
          'typeDisplayName': 'actor.FirstName',
          'metadata': [],
          'sourcedBy': null
        },
        'last_name': {
          'type': {
            'fullyQualifiedName': 'actor.LastName',
            'parameters': [],
            'parameterizedName': 'actor.LastName',
            'name': 'LastName',
            'namespace': 'actor',
            'shortDisplayName': 'LastName',
            'longDisplayName': 'actor.LastName'
          },
          'modifiers': [],
          'typeDoc': null,
          'defaultValue': null,
          'nullable': false,
          'typeDisplayName': 'actor.LastName',
          'metadata': [],
          'sourcedBy': null
        },
        'last_update': {
          'type': {
            'fullyQualifiedName': 'actor.LastUpdate',
            'parameters': [],
            'parameterizedName': 'actor.LastUpdate',
            'name': 'LastUpdate',
            'namespace': 'actor',
            'shortDisplayName': 'LastUpdate',
            'longDisplayName': 'actor.LastUpdate'
          },
          'modifiers': [],
          'typeDoc': null,
          'defaultValue': null,
          'nullable': false,
          'typeDisplayName': 'actor.LastUpdate',
          'metadata': [],
          'sourcedBy': null
        }
      },
      'modifiers': [],
      'metadata': [
        {
          'name': {
            'fullyQualifiedName': 'io.vyne.jdbc.Table',
            'parameters': [],
            'parameterizedName': 'io.vyne.jdbc.Table',
            'name': 'Table',
            'namespace': 'io.vyne.jdbc',
            'shortDisplayName': 'Table',
            'longDisplayName': 'io.vyne.jdbc.Table'
          },
          'params': {
            'table': 'actor',
            'schema': 'public',
            'connection': 'asfdf'
          }
        }
      ],
      'aliasForType': null,
      'inheritsFrom': [],
      'enumValues': [],
      'sources': [
        {
          'name': 'actor/Actor.taxi',
          'version': '0.0.0',
          'content': 'import io.vyne.jdbc.Table\nnamespace actor {\n   @io.vyne.jdbc.Table(table = "actor" , schema = "public" , connection = "asfdf")\n         model Actor {\n            @Id actor_id : ActorId\n            first_name : FirstName\n            last_name : LastName\n            last_update : LastUpdate\n         }\n}',
          'id': 'actor/Actor.taxi:0.0.0',
          'contentHash': '621410'
        }
      ],
      'typeParameters': [],
      'typeDoc': '',
      'isTypeAlias': false,
      'offset': null,
      'format': null,
      'hasFormat': false,
      'basePrimitiveTypeName': null,
      'hasExpression': false,
      'unformattedTypeName': null,
      'isParameterType': false,
      'isClosed': false,
      'isPrimitive': false,
      'fullyQualifiedName': 'actor.Actor',
      'longDisplayName': 'actor.Actor',
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.Actor',
        'parameters': [],
        'parameterizedName': 'actor.Actor',
        'name': 'Actor',
        'namespace': 'actor',
        'shortDisplayName': 'Actor',
        'longDisplayName': 'actor.Actor'
      },
      'underlyingTypeParameters': [],
      'isCollection': false,
      'isStream': false,
      'collectionType': null,
      'isScalar': false
    }
  ],
  'services': [
    {
      'name': {
        'fullyQualifiedName': 'actor.ActorService',
        'parameters': [],
        'parameterizedName': 'actor.ActorService',
        'name': 'ActorService',
        'namespace': 'actor',
        'shortDisplayName': 'ActorService',
        'longDisplayName': 'actor.ActorService'
      },
      'operations': [],
      'queryOperations': [
        {
          'qualifiedName': {
            'fullyQualifiedName': 'actor.ActorService@@actorQuery',
            'parameters': [],
            'parameterizedName': 'actor.ActorService@@actorQuery',
            'name': 'ActorService@@actorQuery',
            'namespace': 'actor',
            'shortDisplayName': 'actorQuery',
            'longDisplayName': 'actor.ActorService / actorQuery'
          },
          'parameters': [
            {
              'type': {
                'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
                'parameters': [],
                'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
                'name': 'VyneQlQuery',
                'namespace': 'vyne.vyneQl',
                'shortDisplayName': 'VyneQlQuery',
                'longDisplayName': 'vyne.vyneQl.VyneQlQuery'
              },
              'name': 'querySpec',
              'metadata': [],
              'constraints': []
            }
          ],
          'returnType': {
            'fullyQualifiedName': 'lang.taxi.Array',
            'parameters': [
              {
                'fullyQualifiedName': 'actor.Actor',
                'parameters': [],
                'parameterizedName': 'actor.Actor',
                'name': 'Actor',
                'namespace': 'actor',
                'shortDisplayName': 'Actor',
                'longDisplayName': 'actor.Actor'
              }
            ],
            'parameterizedName': 'lang.taxi.Array<actor.Actor>',
            'name': 'Array',
            'namespace': 'lang.taxi',
            'shortDisplayName': 'Actor[]',
            'longDisplayName': 'actor.Actor[]'
          },
          'metadata': [],
          'grammar': 'vyneQl',
          'capabilities': [
            'SUM',
            'COUNT',
            'AVG',
            'MIN',
            'MAX',
            {
              'supportedOperations': [
                'EQUAL',
                'NOT_EQUAL',
                'IN',
                'LIKE',
                'GREATER_THAN',
                'LESS_THAN',
                'GREATER_THAN_OR_EQUAL_TO',
                'LESS_THAN_OR_EQUAL_TO'
              ]
            }
          ],
          'typeDoc': null,
          'contract': {
            'returnType': {
              'fullyQualifiedName': 'lang.taxi.Array',
              'parameters': [
                {
                  'fullyQualifiedName': 'actor.Actor',
                  'parameters': [],
                  'parameterizedName': 'actor.Actor',
                  'name': 'Actor',
                  'namespace': 'actor',
                  'shortDisplayName': 'Actor',
                  'longDisplayName': 'actor.Actor'
                }
              ],
              'parameterizedName': 'lang.taxi.Array<actor.Actor>',
              'name': 'Array',
              'namespace': 'lang.taxi',
              'shortDisplayName': 'Actor[]',
              'longDisplayName': 'actor.Actor[]'
            },
            'constraints': []
          },
          'operationType': null,
          'hasFilterCapability': true,
          'supportedFilterOperations': [
            'EQUAL',
            'NOT_EQUAL',
            'IN',
            'LIKE',
            'GREATER_THAN',
            'LESS_THAN',
            'GREATER_THAN_OR_EQUAL_TO',
            'LESS_THAN_OR_EQUAL_TO'
          ],
          'name': 'actorQuery',
          'memberQualifiedName': {
            'fullyQualifiedName': 'actor.ActorService@@actorQuery',
            'parameters': [],
            'parameterizedName': 'actor.ActorService@@actorQuery',
            'name': 'ActorService@@actorQuery',
            'namespace': 'actor',
            'shortDisplayName': 'actorQuery',
            'longDisplayName': 'actor.ActorService / actorQuery'
          }
        }
      ],
      'metadata': [
        {
          'name': {
            'fullyQualifiedName': 'io.vyne.jdbc.DatabaseService',
            'parameters': [],
            'parameterizedName': 'io.vyne.jdbc.DatabaseService',
            'name': 'DatabaseService',
            'namespace': 'io.vyne.jdbc',
            'shortDisplayName': 'DatabaseService',
            'longDisplayName': 'io.vyne.jdbc.DatabaseService'
          },
          'params': {
            'connection': 'asfdf'
          }
        }
      ],
      'sourceCode': [
        {
          'name': 'actor/ActorService.taxi',
          'version': '0.0.0',
          'content': 'import vyne.vyneQl.VyneQlQuery\nnamespace actor {\n   @io.vyne.jdbc.DatabaseService(connection = "asfdf")\n         service ActorService {\n            vyneQl query actorQuery(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<actor.Actor> with capabilities {\n               sum,\n               count,\n               avg,\n               min,\n               max,\n               filter(==,!=,in,like,>,<,>=,<=)\n            }\n         }\n}',
          'id': 'actor/ActorService.taxi:0.0.0',
          'contentHash': '9e93b0'
        }
      ],
      'typeDoc': null,
      'lineage': null,
      'qualifiedName': 'actor.ActorService',
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.ActorService',
        'parameters': [],
        'parameterizedName': 'actor.ActorService',
        'name': 'ActorService',
        'namespace': 'actor',
        'shortDisplayName': 'ActorService',
        'longDisplayName': 'actor.ActorService'
      }
    }
  ],
  'messages': [],
  'taxi': 'namespace actor {\n   type ActorId inherits Int\n   \n   type FirstName inherits String\n   \n   type LastName inherits String\n   \n   type LastUpdate inherits Instant\n   \n   @io.vyne.jdbc.Table(table = "actor" , schema = "public" , connection = "asfdf")\n   model Actor {\n      @Id actor_id : ActorId\n      first_name : FirstName\n      last_name : LastName\n      last_update : LastUpdate\n   }\n   \n   @io.vyne.jdbc.DatabaseService(connection = "asfdf")\n   service ActorService {\n      vyneQl query actorQuery(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<actor.Actor> with capabilities {\n         sum,\n         count,\n         avg,\n         min,\n         max,\n         filter(==,!=,in,like,>,<,>=,<=)\n      }\n   }\n}'
} as TaxiSubmissionResult
