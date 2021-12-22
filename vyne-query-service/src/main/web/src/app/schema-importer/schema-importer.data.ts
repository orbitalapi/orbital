import {TableTaxiGenerationRequest} from '../db-connection-editor/db-importer.service';
import {SchemaSubmissionResult} from '../services/types.service';
import {Schema} from '../services/schema';

export const schemaWithNestedTypes = {
  'members' : [],
  'types': [{
    'name': {
      'fullyQualifiedName': 'lang.taxi.Boolean',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Boolean',
      'name': 'Boolean',
      'shortDisplayName': 'Boolean',
      'longDisplayName': 'lang.taxi.Boolean',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'Represents a value which is either `true` or `false`.',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Boolean',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Boolean',
      'name': 'Boolean',
      'shortDisplayName': 'Boolean',
      'longDisplayName': 'lang.taxi.Boolean',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Boolean',
    'longDisplayName': 'lang.taxi.Boolean',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Boolean',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Boolean',
      'name': 'Boolean',
      'shortDisplayName': 'Boolean',
      'longDisplayName': 'lang.taxi.Boolean',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'A collection of characters.',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.String',
    'longDisplayName': 'lang.taxi.String',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'name': 'Int',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'A signed integer - ie. a whole number (positive or negative), with no decimal places',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'name': 'Int',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Int',
    'longDisplayName': 'lang.taxi.Int',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'name': 'Int',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'name': 'Decimal',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'A signed decimal number - ie., a whole number with decimal places.',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'name': 'Decimal',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Decimal',
    'longDisplayName': 'lang.taxi.Decimal',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'name': 'Decimal',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Date',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Date',
      'name': 'Date',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'A date, without a time or timezone.',
    'isTypeAlias': false,
    'offset': null,
    'format': ['yyyy-MM-dd'],
    'hasFormat': true,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Date',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Date',
      'name': 'Date',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'lang.taxi.Date',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Date',
      'name': 'Date',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'namespace': 'lang.taxi'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Date',
    'longDisplayName': 'lang.taxi.Date(yyyy-MM-dd)',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Date',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Date',
      'name': 'Date',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Time',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Time',
      'name': 'Time',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'Time only, excluding the date part',
    'isTypeAlias': false,
    'offset': null,
    'format': ['HH:mm:ss'],
    'hasFormat': true,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Time',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Time',
      'name': 'Time',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'lang.taxi.Time',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Time',
      'name': 'Time',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'namespace': 'lang.taxi'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Time',
    'longDisplayName': 'lang.taxi.Time(HH:mm:ss)',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Time',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Time',
      'name': 'Time',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.DateTime',
      'parameters': [],
      'parameterizedName': 'lang.taxi.DateTime',
      'name': 'DateTime',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'A date and time, without a timezone.  Generally, favour using Instant which represents a point-in-time, as it has a timezone attached',
    'isTypeAlias': false,
    'offset': null,
    'format': ['yyyy-MM-dd\'T\'HH:mm:ss.SSS'],
    'hasFormat': true,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.DateTime',
      'parameters': [],
      'parameterizedName': 'lang.taxi.DateTime',
      'name': 'DateTime',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'lang.taxi.DateTime',
      'parameters': [],
      'parameterizedName': 'lang.taxi.DateTime',
      'name': 'DateTime',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'namespace': 'lang.taxi'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.DateTime',
    'longDisplayName': 'lang.taxi.DateTime(yyyy-MM-dd\'T\'HH:mm:ss.SSS)',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.DateTime',
      'parameters': [],
      'parameterizedName': 'lang.taxi.DateTime',
      'name': 'DateTime',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'name': 'Instant',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'A point in time, with date, time and timezone.  Follows ISO standard convention of yyyy-MM-dd\'T\'HH:mm:ss.SSSZ',
    'isTypeAlias': false,
    'offset': null,
    'format': ['yyyy-MM-dd\'T\'HH:mm:ss[.SSS]X'],
    'hasFormat': true,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'name': 'Instant',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'name': 'Instant',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Instant',
    'longDisplayName': 'lang.taxi.Instant(yyyy-MM-dd\'T\'HH:mm:ss[.SSS]X)',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'name': 'Instant',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'name': 'Any',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'Can be anything.  Try to avoid using \'Any\' as it\'s not descriptive - favour using a strongly typed approach instead',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'name': 'Any',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Any',
    'longDisplayName': 'lang.taxi.Any',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'name': 'Any',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Double',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Double',
      'name': 'Double',
      'shortDisplayName': 'Double',
      'longDisplayName': 'lang.taxi.Double',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'Represents a double-precision 64-bit IEEE 754 floating point number.',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Double',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Double',
      'name': 'Double',
      'shortDisplayName': 'Double',
      'longDisplayName': 'lang.taxi.Double',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Double',
    'longDisplayName': 'lang.taxi.Double',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Double',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Double',
      'name': 'Double',
      'shortDisplayName': 'Double',
      'longDisplayName': 'lang.taxi.Double',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Void',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Void',
      'name': 'Void',
      'shortDisplayName': 'Void',
      'longDisplayName': 'lang.taxi.Void',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'Nothing.  Represents the return value of operations that don\'t return anything.',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Void',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Void',
      'name': 'Void',
      'shortDisplayName': 'Void',
      'longDisplayName': 'lang.taxi.Void',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Void',
    'longDisplayName': 'lang.taxi.Void',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Void',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Void',
      'name': 'Void',
      'shortDisplayName': 'Void',
      'longDisplayName': 'lang.taxi.Void',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Array',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Array',
      'name': 'Array',
      'shortDisplayName': 'Array',
      'longDisplayName': 'lang.taxi.Array',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'A collection of things',
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
    'fullyQualifiedName': 'lang.taxi.Array',
    'longDisplayName': 'lang.taxi.Array',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Array',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Array',
      'name': 'Array',
      'shortDisplayName': 'Array',
      'longDisplayName': 'lang.taxi.Array',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': true,
    'isStream': false,
    'collectionType': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'name': 'Any',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'namespace': 'lang.taxi'
    },
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Stream',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Stream',
      'name': 'Stream',
      'shortDisplayName': 'Stream',
      'longDisplayName': 'lang.taxi.Stream',
      'namespace': 'lang.taxi'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'Native',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd509e4'
    }],
    'typeParameters': [],
    'typeDoc': 'Result of a service publishing sequence of events',
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
    'fullyQualifiedName': 'lang.taxi.Stream',
    'longDisplayName': 'lang.taxi.Stream',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Stream',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Stream',
      'name': 'Stream',
      'shortDisplayName': 'Stream',
      'longDisplayName': 'lang.taxi.Stream',
      'namespace': 'lang.taxi'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': true,
    'collectionType': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'name': 'Any',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'namespace': 'lang.taxi'
    },
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demo.LastName',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.LastName',
      'name': 'LastName',
      'shortDisplayName': 'LastName',
      'longDisplayName': 'io.vyne.demo.LastName',
      'namespace': 'io.vyne.demo'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    }],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'namespace io.vyne.demo {\n   type LastName inherits String\n}',
      'id': '<unknown>:0.0.0',
      'contentHash': 'd54ac0'
    }],
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
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.demo.LastName',
    'longDisplayName': 'io.vyne.demo.LastName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demo.LastName',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.LastName',
      'name': 'LastName',
      'shortDisplayName': 'LastName',
      'longDisplayName': 'io.vyne.demo.LastName',
      'namespace': 'io.vyne.demo'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demo.Country',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.Country',
      'name': 'Country',
      'shortDisplayName': 'Country',
      'longDisplayName': 'io.vyne.demo.Country',
      'namespace': 'io.vyne.demo'
    },
    'attributes': {},
    'modifiers': ['ENUM'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [{
      'name': 'NZ',
      'value': 'New Zealand',
      'synonyms': [],
      'typeDoc': 'Home sweet home'
    }, {
      'name': 'AUS',
      'value': 'Australia',
      'synonyms': [],
      'typeDoc': 'Ozzie'
    }, {
      'name': 'UK',
      'value': 'United Kingdom',
      'synonyms': [],
      'typeDoc': 'The queen lives here'
    }],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'enum Country {\n   [[ Home sweet home ]]\n   NZ("New Zealand"),\n   [[ Ozzie ]]\n   AUS("Australia"),\n   [[ The queen lives here ]]\n   UK("United Kingdom")\n}',
      'id': '<unknown>:0.0.0',
      'contentHash': '8a960e'
    }],
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
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.demo.Country',
    'longDisplayName': 'io.vyne.demo.Country',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demo.Country',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.Country',
      'name': 'Country',
      'shortDisplayName': 'Country',
      'longDisplayName': 'io.vyne.demo.Country',
      'namespace': 'io.vyne.demo'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demo.Address',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.Address',
      'name': 'Address',
      'shortDisplayName': 'Address',
      'longDisplayName': 'io.vyne.demo.Address',
      'namespace': 'io.vyne.demo'
    },
    'attributes': {
      'firstLine': {
        'type': {
          'fullyQualifiedName': 'io.vyne.demo.FirstLine',
          'parameters': [],
          'parameterizedName': 'io.vyne.demo.FirstLine',
          'name': 'FirstLine',
          'shortDisplayName': 'FirstLine',
          'longDisplayName': 'io.vyne.demo.FirstLine',
          'namespace': 'io.vyne.demo'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'io.vyne.demo.FirstLine',
        'metadata': [],
        'sourcedBy': null
      },
      'lastLine': {
        'type': {
          'fullyQualifiedName': 'io.vyne.demo.LastLine',
          'parameters': [],
          'parameterizedName': 'io.vyne.demo.LastLine',
          'name': 'LastLine',
          'shortDisplayName': 'LastLine',
          'longDisplayName': 'io.vyne.demo.LastLine',
          'namespace': 'io.vyne.demo'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'io.vyne.demo.LastLine',
        'metadata': [],
        'sourcedBy': null
      },
      'region': {
        'type': {
          'fullyQualifiedName': 'io.vyne.demo.io.vyne.demo.Address$Region',
          'parameters': [],
          'parameterizedName': 'io.vyne.demo.io.vyne.demo.Address$Region',
          'name': 'Address$Region',
          'shortDisplayName': 'Address$Region',
          'longDisplayName': 'io.vyne.demo.io.vyne.demo.Address$Region',
          'namespace': 'io.vyne.demo.io.vyne.demo'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'io.vyne.demo.io.vyne.demo.Address$Region',
        'metadata': [],
        'sourcedBy': null
      }
    },
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'import io.vyne.demo.io.vyne.demo.Address$Region\nnamespace io.vyne.demo {\n   model Address {\n      firstLine : FirstLine inherits String\n      lastLine : LastLine inherits String\n      region : {\n         country : Country\n         postCode : PostCode inherits String\n      }\n   }\n}',
      'id': '<unknown>:0.0.0',
      'contentHash': 'a88c05'
    }],
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
    'fullyQualifiedName': 'io.vyne.demo.Address',
    'longDisplayName': 'io.vyne.demo.Address',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demo.Address',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.Address',
      'name': 'Address',
      'shortDisplayName': 'Address',
      'longDisplayName': 'io.vyne.demo.Address',
      'namespace': 'io.vyne.demo'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demo.Person',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.Person',
      'name': 'Person',
      'shortDisplayName': 'Person',
      'longDisplayName': 'io.vyne.demo.Person',
      'namespace': 'io.vyne.demo'
    },
    'attributes': {
      'firstName': {
        'type': {
          'fullyQualifiedName': 'io.vyne.demo.FirstName',
          'parameters': [],
          'parameterizedName': 'io.vyne.demo.FirstName',
          'name': 'FirstName',
          'shortDisplayName': 'FirstName',
          'longDisplayName': 'io.vyne.demo.FirstName',
          'namespace': 'io.vyne.demo'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'io.vyne.demo.FirstName',
        'metadata': [],
        'sourcedBy': null
      },
      'lastName': {
        'type': {
          'fullyQualifiedName': 'io.vyne.demo.LastName',
          'parameters': [],
          'parameterizedName': 'io.vyne.demo.LastName',
          'name': 'LastName',
          'shortDisplayName': 'LastName',
          'longDisplayName': 'io.vyne.demo.LastName',
          'namespace': 'io.vyne.demo'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': true,
        'typeDisplayName': 'io.vyne.demo.LastName',
        'metadata': [],
        'sourcedBy': null
      },
      'address': {
        'type': {
          'fullyQualifiedName': 'io.vyne.demo.Address',
          'parameters': [],
          'parameterizedName': 'io.vyne.demo.Address',
          'name': 'Address',
          'shortDisplayName': 'Address',
          'longDisplayName': 'io.vyne.demo.Address',
          'namespace': 'io.vyne.demo'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'io.vyne.demo.Address',
        'metadata': [],
        'sourcedBy': null
      }
    },
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'namespace io.vyne.demo {\n   model Person {\n      firstName : FirstName inherits String\n      lastName : LastName?\n      address : Address\n   }\n}',
      'id': '<unknown>:0.0.0',
      'contentHash': '6331eb'
    }],
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
    'fullyQualifiedName': 'io.vyne.demo.Person',
    'longDisplayName': 'io.vyne.demo.Person',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demo.Person',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.Person',
      'name': 'Person',
      'shortDisplayName': 'Person',
      'longDisplayName': 'io.vyne.demo.Person',
      'namespace': 'io.vyne.demo'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demo.FirstLine',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.FirstLine',
      'name': 'FirstLine',
      'shortDisplayName': 'FirstLine',
      'longDisplayName': 'io.vyne.demo.FirstLine',
      'namespace': 'io.vyne.demo'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    }],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'FirstLine inherits String',
      'id': '<unknown>:0.0.0',
      'contentHash': '1060bb'
    }],
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
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.demo.FirstLine',
    'longDisplayName': 'io.vyne.demo.FirstLine',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demo.FirstLine',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.FirstLine',
      'name': 'FirstLine',
      'shortDisplayName': 'FirstLine',
      'longDisplayName': 'io.vyne.demo.FirstLine',
      'namespace': 'io.vyne.demo'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demo.LastLine',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.LastLine',
      'name': 'LastLine',
      'shortDisplayName': 'LastLine',
      'longDisplayName': 'io.vyne.demo.LastLine',
      'namespace': 'io.vyne.demo'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    }],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'LastLine inherits String',
      'id': '<unknown>:0.0.0',
      'contentHash': 'bc4ae8'
    }],
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
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.demo.LastLine',
    'longDisplayName': 'io.vyne.demo.LastLine',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demo.LastLine',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.LastLine',
      'name': 'LastLine',
      'shortDisplayName': 'LastLine',
      'longDisplayName': 'io.vyne.demo.LastLine',
      'namespace': 'io.vyne.demo'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demo.PostCode',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.PostCode',
      'name': 'PostCode',
      'shortDisplayName': 'PostCode',
      'longDisplayName': 'io.vyne.demo.PostCode',
      'namespace': 'io.vyne.demo'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    }],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'PostCode inherits String',
      'id': '<unknown>:0.0.0',
      'contentHash': '9ce07b'
    }],
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
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.demo.PostCode',
    'longDisplayName': 'io.vyne.demo.PostCode',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demo.PostCode',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.PostCode',
      'name': 'PostCode',
      'shortDisplayName': 'PostCode',
      'longDisplayName': 'io.vyne.demo.PostCode',
      'namespace': 'io.vyne.demo'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demo.io.vyne.demo.Address$Region',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.io.vyne.demo.Address$Region',
      'name': 'Address$Region',
      'shortDisplayName': 'Address$Region',
      'longDisplayName': 'io.vyne.demo.io.vyne.demo.Address$Region',
      'namespace': 'io.vyne.demo.io.vyne.demo'
    },
    'attributes': {
      'country': {
        'type': {
          'fullyQualifiedName': 'io.vyne.demo.Country',
          'parameters': [],
          'parameterizedName': 'io.vyne.demo.Country',
          'name': 'Country',
          'shortDisplayName': 'Country',
          'longDisplayName': 'io.vyne.demo.Country',
          'namespace': 'io.vyne.demo'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'io.vyne.demo.Country',
        'metadata': [],
        'sourcedBy': null
      },
      'postCode': {
        'type': {
          'fullyQualifiedName': 'io.vyne.demo.PostCode',
          'parameters': [],
          'parameterizedName': 'io.vyne.demo.PostCode',
          'name': 'PostCode',
          'shortDisplayName': 'PostCode',
          'longDisplayName': 'io.vyne.demo.PostCode',
          'namespace': 'io.vyne.demo'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'io.vyne.demo.PostCode',
        'metadata': [],
        'sourcedBy': null
      }
    },
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': '{\n      country : Country\n      postCode : PostCode inherits String\n   }',
      'id': '<unknown>:0.0.0',
      'contentHash': 'bc8b6b'
    }],
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
    'fullyQualifiedName': 'io.vyne.demo.io.vyne.demo.Address$Region',
    'longDisplayName': 'io.vyne.demo.io.vyne.demo.Address$Region',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demo.io.vyne.demo.Address$Region',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.io.vyne.demo.Address$Region',
      'name': 'Address$Region',
      'shortDisplayName': 'Address$Region',
      'longDisplayName': 'io.vyne.demo.io.vyne.demo.Address$Region',
      'namespace': 'io.vyne.demo.io.vyne.demo'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demo.FirstName',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.FirstName',
      'name': 'FirstName',
      'shortDisplayName': 'FirstName',
      'longDisplayName': 'io.vyne.demo.FirstName',
      'namespace': 'io.vyne.demo'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    }],
    'enumValues': [],
    'sources': [{
      'name': '<unknown>',
      'version': '0.0.0',
      'content': 'FirstName inherits String',
      'id': '<unknown>:0.0.0',
      'contentHash': '9bd55b'
    }],
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
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.demo.FirstName',
    'longDisplayName': 'io.vyne.demo.FirstName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demo.FirstName',
      'parameters': [],
      'parameterizedName': 'io.vyne.demo.FirstName',
      'name': 'FirstName',
      'shortDisplayName': 'FirstName',
      'longDisplayName': 'io.vyne.demo.FirstName',
      'namespace': 'io.vyne.demo'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }],
  'services': [],
  'policies': [],
  'dynamicMetadata': [],
  'metadataTypes': [],
  'operations': []
} as Schema;

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
} as SchemaSubmissionResult
