import { Schema } from '../services/schema';

export const FILMS_SCHEMA = {
  'types': [{
    'name': {
      'fullyQualifiedName': 'lang.taxi.Boolean',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Boolean',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Boolean',
      'longDisplayName': 'lang.taxi.Boolean',
      'name': 'Boolean'
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
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Boolean',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Boolean',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Boolean',
      'longDisplayName': 'lang.taxi.Boolean',
      'name': 'Boolean'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Boolean',
      'longDisplayName': 'lang.taxi.Boolean',
      'name': 'Boolean'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
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
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
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
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
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
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'name': 'Date'
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
    'declaresFormat': true,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Date',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Date',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'name': 'Date'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'lang.taxi.Date',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Date',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'name': 'Date'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'name': 'Date'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'name': 'Time'
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
    'declaresFormat': true,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Time',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Time',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'name': 'Time'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'lang.taxi.Time',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Time',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'name': 'Time'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'name': 'Time'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'name': 'DateTime'
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
    'declaresFormat': true,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.DateTime',
      'parameters': [],
      'parameterizedName': 'lang.taxi.DateTime',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'name': 'DateTime'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'lang.taxi.DateTime',
      'parameters': [],
      'parameterizedName': 'lang.taxi.DateTime',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'name': 'DateTime'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'name': 'DateTime'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'name': 'Instant'
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
    'declaresFormat': true,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'name': 'Instant'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'name': 'Instant'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'name': 'Instant'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'name': 'Any'
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
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'name': 'Any'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'name': 'Any'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Double',
      'longDisplayName': 'lang.taxi.Double',
      'name': 'Double'
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
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Double',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Double',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Double',
      'longDisplayName': 'lang.taxi.Double',
      'name': 'Double'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Double',
      'longDisplayName': 'lang.taxi.Double',
      'name': 'Double'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Void',
      'longDisplayName': 'lang.taxi.Void',
      'name': 'Void'
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
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Void',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Void',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Void',
      'longDisplayName': 'lang.taxi.Void',
      'name': 'Void'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Void',
      'longDisplayName': 'lang.taxi.Void',
      'name': 'Void'
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Array',
      'longDisplayName': 'lang.taxi.Array',
      'name': 'Array'
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
    'declaresFormat': false,
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Array',
      'longDisplayName': 'lang.taxi.Array',
      'name': 'Array'
    },
    'underlyingTypeParameters': [],
    'isCollection': true,
    'isStream': false,
    'collectionType': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'name': 'Any'
    },
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Stream',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Stream',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Stream',
      'longDisplayName': 'lang.taxi.Stream',
      'name': 'Stream'
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
    'declaresFormat': false,
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
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Stream',
      'longDisplayName': 'lang.taxi.Stream',
      'name': 'Stream'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': true,
    'collectionType': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'name': 'Any'
    },
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.Username',
      'parameters': [],
      'parameterizedName': 'io.vyne.Username',
      'namespace': 'io.vyne',
      'shortDisplayName': 'Username',
      'longDisplayName': 'io.vyne.Username',
      'name': 'Username'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'UserTypes',
      'version': '0.0.0',
      'content': 'namespace io.vyne {\n   type Username inherits String\n}',
      'id': 'UserTypes:0.0.0',
      'contentHash': '00a414'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.Username',
    'longDisplayName': 'io.vyne.Username',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.Username',
      'parameters': [],
      'parameterizedName': 'io.vyne.Username',
      'namespace': 'io.vyne',
      'shortDisplayName': 'Username',
      'longDisplayName': 'io.vyne.Username',
      'name': 'Username'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.jdbc.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.ConnectionName',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.jdbc.ConnectionName',
      'name': 'ConnectionName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'JdbcConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.jdbc {\n   type ConnectionName inherits String\n}',
      'id': 'JdbcConnectors:0.0.0',
      'contentHash': 'b0f9b0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.jdbc.ConnectionName',
    'longDisplayName': 'io.vyne.jdbc.ConnectionName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.jdbc.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.ConnectionName',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.jdbc.ConnectionName',
      'name': 'ConnectionName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.jdbc.DatabaseService',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.DatabaseService',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'DatabaseService',
      'longDisplayName': 'io.vyne.jdbc.DatabaseService',
      'name': 'DatabaseService'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'JdbcConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.jdbc {\n   annotation DatabaseService {\n         connection : ConnectionName\n      }\n}',
      'id': 'JdbcConnectors:0.0.0',
      'contentHash': '56bb57'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.jdbc.DatabaseService',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.DatabaseService',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'DatabaseService',
      'longDisplayName': 'io.vyne.jdbc.DatabaseService',
      'name': 'DatabaseService'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.jdbc.DatabaseService',
    'longDisplayName': 'io.vyne.jdbc.DatabaseService()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.jdbc.DatabaseService',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.DatabaseService',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'DatabaseService',
      'longDisplayName': 'io.vyne.jdbc.DatabaseService',
      'name': 'DatabaseService'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.jdbc.Table',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.Table',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'Table',
      'longDisplayName': 'io.vyne.jdbc.Table',
      'name': 'Table'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'JdbcConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.jdbc {\n   annotation Table {\n         connection : ConnectionName\n         table : TableName inherits String\n         schema: SchemaName inherits String\n      }\n}',
      'id': 'JdbcConnectors:0.0.0',
      'contentHash': 'e8ee48'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.jdbc.Table',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.Table',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'Table',
      'longDisplayName': 'io.vyne.jdbc.Table',
      'name': 'Table'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.jdbc.Table',
    'longDisplayName': 'io.vyne.jdbc.Table()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.jdbc.Table',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.Table',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'Table',
      'longDisplayName': 'io.vyne.jdbc.Table',
      'name': 'Table'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
      'parameters': [],
      'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
      'namespace': 'vyne.vyneQl',
      'shortDisplayName': 'VyneQlQuery',
      'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
      'name': 'VyneQlQuery'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'vyne/internal-types.taxi',
      'version': '0.0.0',
      'content': 'namespace vyne.vyneQl {\n   type VyneQlQuery inherits String\n}',
      'id': 'vyne/internal-types.taxi:0.0.0',
      'contentHash': '79e033'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
    'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
    'memberQualifiedName': {
      'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
      'parameters': [],
      'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
      'namespace': 'vyne.vyneQl',
      'shortDisplayName': 'VyneQlQuery',
      'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
      'name': 'VyneQlQuery'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.kafka.KafkaService',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.KafkaService',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'KafkaService',
      'longDisplayName': 'io.vyne.kafka.KafkaService',
      'name': 'KafkaService'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'vyne/internal-types.taxi',
      'version': '0.0.0',
      'content': 'namespace io.vyne.kafka {\n   annotation KafkaService {\n         connectionName : ConnectionName inherits String\n      }\n}',
      'id': 'vyne/internal-types.taxi:0.0.0',
      'contentHash': '6826ff'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.kafka.KafkaService',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.KafkaService',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'KafkaService',
      'longDisplayName': 'io.vyne.kafka.KafkaService',
      'name': 'KafkaService'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.kafka.KafkaService',
    'longDisplayName': 'io.vyne.kafka.KafkaService()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.kafka.KafkaService',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.KafkaService',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'KafkaService',
      'longDisplayName': 'io.vyne.kafka.KafkaService',
      'name': 'KafkaService'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.kafka.TopicOffset',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.TopicOffset',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'TopicOffset',
      'longDisplayName': 'io.vyne.kafka.TopicOffset',
      'name': 'TopicOffset'
    },
    'attributes': {},
    'modifiers': ['ENUM'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [{ 'name': 'earliest', 'value': 'earliest', 'synonyms': [], 'typeDoc': '' }, {
      'name': 'latest',
      'value': 'latest',
      'synonyms': [],
      'typeDoc': ''
    }, { 'name': 'none', 'value': 'none', 'synonyms': [], 'typeDoc': '' }],
    'sources': [{
      'name': 'vyne/internal-types.taxi',
      'version': '0.0.0',
      'content': 'namespace io.vyne.kafka {\n   enum TopicOffset {\n         earliest,\n         latest,\n         none\n      }\n}',
      'id': 'vyne/internal-types.taxi:0.0.0',
      'contentHash': 'd31705'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.kafka.TopicOffset',
    'longDisplayName': 'io.vyne.kafka.TopicOffset',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.kafka.TopicOffset',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.TopicOffset',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'TopicOffset',
      'longDisplayName': 'io.vyne.kafka.TopicOffset',
      'name': 'TopicOffset'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.kafka.KafkaOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.KafkaOperation',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'KafkaOperation',
      'longDisplayName': 'io.vyne.kafka.KafkaOperation',
      'name': 'KafkaOperation'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'vyne/internal-types.taxi',
      'version': '0.0.0',
      'content': 'namespace io.vyne.kafka {\n   annotation KafkaOperation {\n         topic : TopicName inherits String\n         offset : TopicOffset\n      }\n}',
      'id': 'vyne/internal-types.taxi:0.0.0',
      'contentHash': '890f9c'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.kafka.KafkaOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.KafkaOperation',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'KafkaOperation',
      'longDisplayName': 'io.vyne.kafka.KafkaOperation',
      'name': 'KafkaOperation'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.kafka.KafkaOperation',
    'longDisplayName': 'io.vyne.kafka.KafkaOperation()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.kafka.KafkaOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.KafkaOperation',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'KafkaOperation',
      'longDisplayName': 'io.vyne.kafka.KafkaOperation',
      'name': 'KafkaOperation'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.catalog.DataOwner',
      'parameters': [],
      'parameterizedName': 'io.vyne.catalog.DataOwner',
      'namespace': 'io.vyne.catalog',
      'shortDisplayName': 'DataOwner',
      'longDisplayName': 'io.vyne.catalog.DataOwner',
      'name': 'DataOwner'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'Catalog',
      'version': '0.0.0',
      'content': 'namespace io.vyne.catalog {\n   annotation DataOwner {\n         id : io.vyne.Username\n         name : String\n      }\n}',
      'id': 'Catalog:0.0.0',
      'contentHash': '2368b8'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.catalog.DataOwner',
      'parameters': [],
      'parameterizedName': 'io.vyne.catalog.DataOwner',
      'namespace': 'io.vyne.catalog',
      'shortDisplayName': 'DataOwner',
      'longDisplayName': 'io.vyne.catalog.DataOwner',
      'name': 'DataOwner'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.catalog.DataOwner',
    'longDisplayName': 'io.vyne.catalog.DataOwner()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.catalog.DataOwner',
      'parameters': [],
      'parameterizedName': 'io.vyne.catalog.DataOwner',
      'namespace': 'io.vyne.catalog',
      'shortDisplayName': 'DataOwner',
      'longDisplayName': 'io.vyne.catalog.DataOwner',
      'name': 'DataOwner'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.s3.S3Service',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.S3Service',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'S3Service',
      'longDisplayName': 'io.vyne.aws.s3.S3Service',
      'name': 'S3Service'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'AwsS3Connectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.s3 {\n   annotation S3Service {\n         connectionName : ConnectionName inherits String\n      }\n}',
      'id': 'AwsS3Connectors:0.0.0',
      'contentHash': 'c94220'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.aws.s3.S3Service',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.S3Service',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'S3Service',
      'longDisplayName': 'io.vyne.aws.s3.S3Service',
      'name': 'S3Service'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.s3.S3Service',
    'longDisplayName': 'io.vyne.aws.s3.S3Service()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.s3.S3Service',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.S3Service',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'S3Service',
      'longDisplayName': 'io.vyne.aws.s3.S3Service',
      'name': 'S3Service'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.s3.S3Operation',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.S3Operation',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'S3Operation',
      'longDisplayName': 'io.vyne.aws.s3.S3Operation',
      'name': 'S3Operation'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'AwsS3Connectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.s3 {\n   annotation S3Operation {\n         bucket : BucketName inherits String\n      }\n}',
      'id': 'AwsS3Connectors:0.0.0',
      'contentHash': '2d48b3'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.aws.s3.S3Operation',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.S3Operation',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'S3Operation',
      'longDisplayName': 'io.vyne.aws.s3.S3Operation',
      'name': 'S3Operation'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.s3.S3Operation',
    'longDisplayName': 'io.vyne.aws.s3.S3Operation()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.s3.S3Operation',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.S3Operation',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'S3Operation',
      'longDisplayName': 'io.vyne.aws.s3.S3Operation',
      'name': 'S3Operation'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.s3.S3EntryKey',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.S3EntryKey',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'S3EntryKey',
      'longDisplayName': 'io.vyne.aws.s3.S3EntryKey',
      'name': 'S3EntryKey'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AwsS3Connectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.s3 {\n   type S3EntryKey inherits String\n}',
      'id': 'AwsS3Connectors:0.0.0',
      'contentHash': 'b1a22b'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.s3.S3EntryKey',
    'longDisplayName': 'io.vyne.aws.s3.S3EntryKey',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.s3.S3EntryKey',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.S3EntryKey',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'S3EntryKey',
      'longDisplayName': 'io.vyne.aws.s3.S3EntryKey',
      'name': 'S3EntryKey'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.SqsService',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.SqsService',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'SqsService',
      'longDisplayName': 'io.vyne.aws.sqs.SqsService',
      'name': 'SqsService'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'AwsSqsConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.sqs {\n   annotation SqsService {\n         connectionName : ConnectionName inherits String\n      }\n}',
      'id': 'AwsSqsConnectors:0.0.0',
      'contentHash': 'bb0399'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.SqsService',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.SqsService',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'SqsService',
      'longDisplayName': 'io.vyne.aws.sqs.SqsService',
      'name': 'SqsService'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.sqs.SqsService',
    'longDisplayName': 'io.vyne.aws.sqs.SqsService()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.SqsService',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.SqsService',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'SqsService',
      'longDisplayName': 'io.vyne.aws.sqs.SqsService',
      'name': 'SqsService'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.SqsOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.SqsOperation',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'SqsOperation',
      'longDisplayName': 'io.vyne.aws.sqs.SqsOperation',
      'name': 'SqsOperation'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'AwsSqsConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.sqs {\n   annotation SqsOperation {\n         queue : QueueName inherits String\n      }\n}',
      'id': 'AwsSqsConnectors:0.0.0',
      'contentHash': 'e9ab78'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.SqsOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.SqsOperation',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'SqsOperation',
      'longDisplayName': 'io.vyne.aws.sqs.SqsOperation',
      'name': 'SqsOperation'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.sqs.SqsOperation',
    'longDisplayName': 'io.vyne.aws.sqs.SqsOperation()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.SqsOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.SqsOperation',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'SqsOperation',
      'longDisplayName': 'io.vyne.aws.sqs.SqsOperation',
      'name': 'SqsOperation'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.azure.store.BlobService',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.BlobService',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'BlobService',
      'longDisplayName': 'io.vyne.azure.store.BlobService',
      'name': 'BlobService'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'AzureStoreConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.azure.store {\n   annotation BlobService {\n         connectionName : ConnectionName inherits String\n      }\n}',
      'id': 'AzureStoreConnectors:0.0.0',
      'contentHash': '7804e6'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.azure.store.BlobService',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.BlobService',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'BlobService',
      'longDisplayName': 'io.vyne.azure.store.BlobService',
      'name': 'BlobService'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.azure.store.BlobService',
    'longDisplayName': 'io.vyne.azure.store.BlobService()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.azure.store.BlobService',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.BlobService',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'BlobService',
      'longDisplayName': 'io.vyne.azure.store.BlobService',
      'name': 'BlobService'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.AzureStoreOperation',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'AzureStoreOperation',
      'longDisplayName': 'io.vyne.azure.store.AzureStoreOperation',
      'name': 'AzureStoreOperation'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'AzureStoreConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.azure.store {\n   annotation AzureStoreOperation {\n         container : AzureStoreContainer inherits String\n      }\n}',
      'id': 'AzureStoreConnectors:0.0.0',
      'contentHash': '1305ac'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.AzureStoreOperation',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'AzureStoreOperation',
      'longDisplayName': 'io.vyne.azure.store.AzureStoreOperation',
      'name': 'AzureStoreOperation'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreOperation',
    'longDisplayName': 'io.vyne.azure.store.AzureStoreOperation()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.AzureStoreOperation',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'AzureStoreOperation',
      'longDisplayName': 'io.vyne.azure.store.AzureStoreOperation',
      'name': 'AzureStoreOperation'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreBlob',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.AzureStoreBlob',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'AzureStoreBlob',
      'longDisplayName': 'io.vyne.azure.store.AzureStoreBlob',
      'name': 'AzureStoreBlob'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AzureStoreConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.azure.store {\n   type AzureStoreBlob inherits String\n}',
      'id': 'AzureStoreConnectors:0.0.0',
      'contentHash': '30cf88'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreBlob',
    'longDisplayName': 'io.vyne.azure.store.AzureStoreBlob',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreBlob',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.AzureStoreBlob',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'AzureStoreBlob',
      'longDisplayName': 'io.vyne.azure.store.AzureStoreBlob',
      'name': 'AzureStoreBlob'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.AwsLambdaService',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.AwsLambdaService',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'AwsLambdaService',
      'longDisplayName': 'io.vyne.aws.lambda.AwsLambdaService',
      'name': 'AwsLambdaService'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'AwsLambdaConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.lambda {\n   annotation AwsLambdaService {\n         connectionName : ConnectionName inherits String\n      }\n}',
      'id': 'AwsLambdaConnectors:0.0.0',
      'contentHash': '4f9d53'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.AwsLambdaService',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.AwsLambdaService',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'AwsLambdaService',
      'longDisplayName': 'io.vyne.aws.lambda.AwsLambdaService',
      'name': 'AwsLambdaService'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.lambda.AwsLambdaService',
    'longDisplayName': 'io.vyne.aws.lambda.AwsLambdaService()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.AwsLambdaService',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.AwsLambdaService',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'AwsLambdaService',
      'longDisplayName': 'io.vyne.aws.lambda.AwsLambdaService',
      'name': 'AwsLambdaService'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.LambdaOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.LambdaOperation',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'LambdaOperation',
      'longDisplayName': 'io.vyne.aws.lambda.LambdaOperation',
      'name': 'LambdaOperation'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'AwsLambdaConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.lambda {\n   annotation LambdaOperation {\n         name : OperationName inherits String\n      }\n}',
      'id': 'AwsLambdaConnectors:0.0.0',
      'contentHash': '12a268'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.LambdaOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.LambdaOperation',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'LambdaOperation',
      'longDisplayName': 'io.vyne.aws.lambda.LambdaOperation',
      'name': 'LambdaOperation'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.lambda.LambdaOperation',
    'longDisplayName': 'io.vyne.aws.lambda.LambdaOperation()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.LambdaOperation',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.LambdaOperation',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'LambdaOperation',
      'longDisplayName': 'io.vyne.aws.lambda.LambdaOperation',
      'name': 'LambdaOperation'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.formats.Csv',
      'parameters': [],
      'parameterizedName': 'io.vyne.formats.Csv',
      'namespace': 'io.vyne.formats',
      'shortDisplayName': 'Csv',
      'longDisplayName': 'io.vyne.formats.Csv',
      'name': 'Csv'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'CsvFormat',
      'version': '0.0.0',
      'content': 'namespace io.vyne.formats {\n   annotation Csv {\n         delimiter : String?\n         firstRecordAsHeader : Boolean?\n         nullValue : String?\n         containsTrailingDelimiters : Boolean?\n         ignoreContentBefore : String?\n         withQuote: String?\n      }\n}',
      'id': 'CsvFormat:0.0.0',
      'contentHash': '296dc6'
    }],
    'typeParameters': [],
    'typeDoc': null,
    'isTypeAlias': false,
    'offset': null,
    'format': [],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'io.vyne.formats.Csv',
      'parameters': [],
      'parameterizedName': 'io.vyne.formats.Csv',
      'namespace': 'io.vyne.formats',
      'shortDisplayName': 'Csv',
      'longDisplayName': 'io.vyne.formats.Csv',
      'name': 'Csv'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.formats.Csv',
    'longDisplayName': 'io.vyne.formats.Csv()',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.formats.Csv',
      'parameters': [],
      'parameterizedName': 'io.vyne.formats.Csv',
      'namespace': 'io.vyne.formats',
      'shortDisplayName': 'Csv',
      'longDisplayName': 'io.vyne.formats.Csv',
      'name': 'Csv'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'actor.Actor',
      'parameters': [],
      'parameterizedName': 'actor.Actor',
      'namespace': 'actor',
      'shortDisplayName': 'Actor',
      'longDisplayName': 'actor.Actor',
      'name': 'Actor'
    },
    'attributes': {
      'actor_id': {
        'type': {
          'fullyQualifiedName': 'actor.types.ActorId',
          'parameters': [],
          'parameterizedName': 'actor.types.ActorId',
          'namespace': 'actor.types',
          'shortDisplayName': 'ActorId',
          'longDisplayName': 'actor.types.ActorId',
          'name': 'ActorId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'actor.types.ActorId',
        'metadata': [{
          'name': {
            'fullyQualifiedName': 'Id',
            'parameters': [],
            'parameterizedName': 'Id',
            'namespace': '',
            'shortDisplayName': 'Id',
            'longDisplayName': 'Id',
            'name': 'Id'
          }, 'params': {}
        }],
        'sourcedBy': null
      },
      'first_name': {
        'type': {
          'fullyQualifiedName': 'actor.types.FirstName',
          'parameters': [],
          'parameterizedName': 'actor.types.FirstName',
          'namespace': 'actor.types',
          'shortDisplayName': 'FirstName',
          'longDisplayName': 'actor.types.FirstName',
          'name': 'FirstName'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'actor.types.FirstName',
        'metadata': [],
        'sourcedBy': null
      },
      'last_name': {
        'type': {
          'fullyQualifiedName': 'actor.types.LastName',
          'parameters': [],
          'parameterizedName': 'actor.types.LastName',
          'namespace': 'actor.types',
          'shortDisplayName': 'LastName',
          'longDisplayName': 'actor.types.LastName',
          'name': 'LastName'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'actor.types.LastName',
        'metadata': [],
        'sourcedBy': null
      },
      'last_update': {
        'type': {
          'fullyQualifiedName': 'actor.types.LastUpdate',
          'parameters': [],
          'parameterizedName': 'actor.types.LastUpdate',
          'namespace': 'actor.types',
          'shortDisplayName': 'LastUpdate',
          'longDisplayName': 'actor.types.LastUpdate',
          'name': 'LastUpdate'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'actor.types.LastUpdate',
        'metadata': [],
        'sourcedBy': null
      }
    },
    'modifiers': [],
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'io.vyne.jdbc.Table',
        'parameters': [],
        'parameterizedName': 'io.vyne.jdbc.Table',
        'namespace': 'io.vyne.jdbc',
        'shortDisplayName': 'Table',
        'longDisplayName': 'io.vyne.jdbc.Table',
        'name': 'Table'
      }, 'params': { 'table': 'actor', 'schema': 'public', 'connection': 'films' }
    }],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'actor/Actor.taxi',
      'version': '0.0.0',
      'content': 'import actor.types.ActorId\nimport actor.types.FirstName\nimport actor.types.LastName\nimport actor.types.LastUpdate\nimport io.vyne.jdbc.Table\nnamespace actor {\n   @io.vyne.jdbc.Table(table = "actor" , schema = "public" , connection = "films")\n         model Actor {\n            @Id actor_id : actor.types.ActorId\n            first_name : actor.types.FirstName\n            last_name : actor.types.LastName\n            last_update : actor.types.LastUpdate\n         }\n}',
      'id': 'actor/Actor.taxi:0.0.0',
      'contentHash': 'd01534'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
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
      'namespace': 'actor',
      'shortDisplayName': 'Actor',
      'longDisplayName': 'actor.Actor',
      'name': 'Actor'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'actor.types.FirstName',
      'parameters': [],
      'parameterizedName': 'actor.types.FirstName',
      'namespace': 'actor.types',
      'shortDisplayName': 'FirstName',
      'longDisplayName': 'actor.types.FirstName',
      'name': 'FirstName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'actor/types/FirstName.taxi',
      'version': '0.0.0',
      'content': 'namespace actor.types {\n   type FirstName inherits String\n}',
      'id': 'actor/types/FirstName.taxi:0.0.0',
      'contentHash': '25a696'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'actor.types.FirstName',
    'longDisplayName': 'actor.types.FirstName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'actor.types.FirstName',
      'parameters': [],
      'parameterizedName': 'actor.types.FirstName',
      'namespace': 'actor.types',
      'shortDisplayName': 'FirstName',
      'longDisplayName': 'actor.types.FirstName',
      'name': 'FirstName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'actor.types.LastUpdate',
      'parameters': [],
      'parameterizedName': 'actor.types.LastUpdate',
      'namespace': 'actor.types',
      'shortDisplayName': 'LastUpdate',
      'longDisplayName': 'actor.types.LastUpdate',
      'name': 'LastUpdate'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'name': 'Instant'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'actor/types/LastUpdate.taxi',
      'version': '0.0.0',
      'content': 'namespace actor.types {\n   type LastUpdate inherits Instant\n}',
      'id': 'actor/types/LastUpdate.taxi:0.0.0',
      'contentHash': 'b1bce3'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': ['yyyy-MM-dd\'T\'HH:mm:ss[.SSS]X'],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'name': 'Instant'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'actor.types.LastUpdate',
      'parameters': [],
      'parameterizedName': 'actor.types.LastUpdate',
      'namespace': 'actor.types',
      'shortDisplayName': 'LastUpdate',
      'longDisplayName': 'actor.types.LastUpdate',
      'name': 'LastUpdate'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'actor.types.LastUpdate',
    'longDisplayName': 'actor.types.LastUpdate(yyyy-MM-dd\'T\'HH:mm:ss[.SSS]X)',
    'memberQualifiedName': {
      'fullyQualifiedName': 'actor.types.LastUpdate',
      'parameters': [],
      'parameterizedName': 'actor.types.LastUpdate',
      'namespace': 'actor.types',
      'shortDisplayName': 'LastUpdate',
      'longDisplayName': 'actor.types.LastUpdate',
      'name': 'LastUpdate'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'actor.types.LastName',
      'parameters': [],
      'parameterizedName': 'actor.types.LastName',
      'namespace': 'actor.types',
      'shortDisplayName': 'LastName',
      'longDisplayName': 'actor.types.LastName',
      'name': 'LastName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'actor/types/LastName.taxi',
      'version': '0.0.0',
      'content': 'namespace actor.types {\n   type LastName inherits String\n}',
      'id': 'actor/types/LastName.taxi:0.0.0',
      'contentHash': '50c8b7'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'actor.types.LastName',
    'longDisplayName': 'actor.types.LastName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'actor.types.LastName',
      'parameters': [],
      'parameterizedName': 'actor.types.LastName',
      'namespace': 'actor.types',
      'shortDisplayName': 'LastName',
      'longDisplayName': 'actor.types.LastName',
      'name': 'LastName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'actor.types.ActorId',
      'parameters': [],
      'parameterizedName': 'actor.types.ActorId',
      'namespace': 'actor.types',
      'shortDisplayName': 'ActorId',
      'longDisplayName': 'actor.types.ActorId',
      'name': 'ActorId'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'actor/types/ActorId.taxi',
      'version': '0.0.0',
      'content': 'namespace actor.types {\n   type ActorId inherits Int\n}',
      'id': 'actor/types/ActorId.taxi:0.0.0',
      'contentHash': 'e3a41d'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'actor.types.ActorId',
    'longDisplayName': 'actor.types.ActorId',
    'memberQualifiedName': {
      'fullyQualifiedName': 'actor.types.ActorId',
      'parameters': [],
      'parameterizedName': 'actor.types.ActorId',
      'namespace': 'actor.types',
      'shortDisplayName': 'ActorId',
      'longDisplayName': 'actor.types.ActorId',
      'name': 'ActorId'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
      'parameters': [],
      'parameterizedName': 'NewFilmReleaseAnnouncement',
      'namespace': '',
      'shortDisplayName': 'NewFilmReleaseAnnouncement',
      'longDisplayName': 'NewFilmReleaseAnnouncement',
      'name': 'NewFilmReleaseAnnouncement'
    },
    'attributes': {
      'filmId': {
        'type': {
          'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
          'parameters': [],
          'parameterizedName': 'demo.netflix.NetflixFilmId',
          'namespace': 'demo.netflix',
          'shortDisplayName': 'NetflixFilmId',
          'longDisplayName': 'demo.netflix.NetflixFilmId',
          'name': 'NetflixFilmId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': true,
        'typeDisplayName': 'demo.netflix.NetflixFilmId',
        'metadata': [{
          'name': {
            'fullyQualifiedName': 'lang.taxi.formats.ProtobufField',
            'parameters': [],
            'parameterizedName': 'lang.taxi.formats.ProtobufField',
            'namespace': 'lang.taxi.formats',
            'shortDisplayName': 'ProtobufField',
            'longDisplayName': 'lang.taxi.formats.ProtobufField',
            'name': 'ProtobufField'
          }, 'params': { 'tag': 1, 'protoType': 'int32' }
        }, {
          'name': {
            'fullyQualifiedName': 'Id',
            'parameters': [],
            'parameterizedName': 'Id',
            'namespace': '',
            'shortDisplayName': 'Id',
            'longDisplayName': 'Id',
            'name': 'Id'
          }, 'params': {}
        }],
        'sourcedBy': null
      },
      'announcement': {
        'type': {
          'fullyQualifiedName': 'lang.taxi.String',
          'parameters': [],
          'parameterizedName': 'lang.taxi.String',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'String',
          'longDisplayName': 'lang.taxi.String',
          'name': 'String'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': true,
        'typeDisplayName': 'lang.taxi.String',
        'metadata': [{
          'name': {
            'fullyQualifiedName': 'lang.taxi.formats.ProtobufField',
            'parameters': [],
            'parameterizedName': 'lang.taxi.formats.ProtobufField',
            'namespace': 'lang.taxi.formats',
            'shortDisplayName': 'ProtobufField',
            'longDisplayName': 'lang.taxi.formats.ProtobufField',
            'name': 'ProtobufField'
          }, 'params': { 'tag': 2, 'protoType': 'string' }
        }],
        'sourcedBy': null
      }
    },
    'modifiers': [],
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'lang.taxi.formats.ProtobufMessage',
        'parameters': [],
        'parameterizedName': 'lang.taxi.formats.ProtobufMessage',
        'namespace': 'lang.taxi.formats',
        'shortDisplayName': 'ProtobufMessage',
        'longDisplayName': 'lang.taxi.formats.ProtobufMessage',
        'name': 'ProtobufMessage'
      }, 'params': { 'packageName': '', 'messageName': 'NewFilmReleaseAnnouncement' }
    }],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'NewFilmReleaseAnnouncement.taxi',
      'version': '0.0.0',
      'content': 'import demo.netflix.NetflixFilmId\n@lang.taxi.formats.ProtobufMessage(packageName = "" , messageName = "NewFilmReleaseAnnouncement")\nmodel NewFilmReleaseAnnouncement {\n   @lang.taxi.formats.ProtobufField(tag = 1 , protoType = "int32") @Id filmId : NetflixFilmId?\n   @lang.taxi.formats.ProtobufField(tag = 2 , protoType = "string") announcement : String?\n}',
      'id': 'NewFilmReleaseAnnouncement.taxi:0.0.0',
      'contentHash': '7c672c'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
    'longDisplayName': 'NewFilmReleaseAnnouncement',
    'memberQualifiedName': {
      'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
      'parameters': [],
      'parameterizedName': 'NewFilmReleaseAnnouncement',
      'namespace': '',
      'shortDisplayName': 'NewFilmReleaseAnnouncement',
      'longDisplayName': 'NewFilmReleaseAnnouncement',
      'name': 'NewFilmReleaseAnnouncement'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'language.types.LanguageId',
      'parameters': [],
      'parameterizedName': 'language.types.LanguageId',
      'namespace': 'language.types',
      'shortDisplayName': 'LanguageId',
      'longDisplayName': 'language.types.LanguageId',
      'name': 'LanguageId'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'language/types/LanguageId.taxi',
      'version': '0.0.0',
      'content': 'namespace language.types {\n   type LanguageId inherits Int\n}',
      'id': 'language/types/LanguageId.taxi:0.0.0',
      'contentHash': 'abbc59'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'language.types.LanguageId',
    'longDisplayName': 'language.types.LanguageId',
    'memberQualifiedName': {
      'fullyQualifiedName': 'language.types.LanguageId',
      'parameters': [],
      'parameterizedName': 'language.types.LanguageId',
      'namespace': 'language.types',
      'shortDisplayName': 'LanguageId',
      'longDisplayName': 'language.types.LanguageId',
      'name': 'LanguageId'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.ReplacementCost',
      'parameters': [],
      'parameterizedName': 'film.types.ReplacementCost',
      'namespace': 'film.types',
      'shortDisplayName': 'ReplacementCost',
      'longDisplayName': 'film.types.ReplacementCost',
      'name': 'ReplacementCost'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/ReplacementCost.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type ReplacementCost inherits Decimal\n}',
      'id': 'film/types/ReplacementCost.taxi:0.0.0',
      'contentHash': 'f71714'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.ReplacementCost',
    'longDisplayName': 'film.types.ReplacementCost',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.ReplacementCost',
      'parameters': [],
      'parameterizedName': 'film.types.ReplacementCost',
      'namespace': 'film.types',
      'shortDisplayName': 'ReplacementCost',
      'longDisplayName': 'film.types.ReplacementCost',
      'name': 'ReplacementCost'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.LastUpdate',
      'parameters': [],
      'parameterizedName': 'film.types.LastUpdate',
      'namespace': 'film.types',
      'shortDisplayName': 'LastUpdate',
      'longDisplayName': 'film.types.LastUpdate',
      'name': 'LastUpdate'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'name': 'Instant'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/LastUpdate.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type LastUpdate inherits Instant\n}',
      'id': 'film/types/LastUpdate.taxi:0.0.0',
      'contentHash': 'a4f44f'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': ['yyyy-MM-dd\'T\'HH:mm:ss[.SSS]X'],
    'hasFormat': true,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'name': 'Instant'
    },
    'hasExpression': false,
    'unformattedTypeName': {
      'fullyQualifiedName': 'film.types.LastUpdate',
      'parameters': [],
      'parameterizedName': 'film.types.LastUpdate',
      'namespace': 'film.types',
      'shortDisplayName': 'LastUpdate',
      'longDisplayName': 'film.types.LastUpdate',
      'name': 'LastUpdate'
    },
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.LastUpdate',
    'longDisplayName': 'film.types.LastUpdate(yyyy-MM-dd\'T\'HH:mm:ss[.SSS]X)',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.LastUpdate',
      'parameters': [],
      'parameterizedName': 'film.types.LastUpdate',
      'namespace': 'film.types',
      'shortDisplayName': 'LastUpdate',
      'longDisplayName': 'film.types.LastUpdate',
      'name': 'LastUpdate'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.Title',
      'parameters': [],
      'parameterizedName': 'film.types.Title',
      'namespace': 'film.types',
      'shortDisplayName': 'Title',
      'longDisplayName': 'film.types.Title',
      'name': 'Title'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/Title.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type Title inherits String\n}',
      'id': 'film/types/Title.taxi:0.0.0',
      'contentHash': 'af88ac'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.Title',
    'longDisplayName': 'film.types.Title',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.Title',
      'parameters': [],
      'parameterizedName': 'film.types.Title',
      'namespace': 'film.types',
      'shortDisplayName': 'Title',
      'longDisplayName': 'film.types.Title',
      'name': 'Title'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.Fulltext',
      'parameters': [],
      'parameterizedName': 'film.types.Fulltext',
      'namespace': 'film.types',
      'shortDisplayName': 'Fulltext',
      'longDisplayName': 'film.types.Fulltext',
      'name': 'Fulltext'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'name': 'Any'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/Fulltext.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type Fulltext inherits Any\n}',
      'id': 'film/types/Fulltext.taxi:0.0.0',
      'contentHash': '15bfd0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'name': 'Any'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.Fulltext',
    'longDisplayName': 'film.types.Fulltext',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.Fulltext',
      'parameters': [],
      'parameterizedName': 'film.types.Fulltext',
      'namespace': 'film.types',
      'shortDisplayName': 'Fulltext',
      'longDisplayName': 'film.types.Fulltext',
      'name': 'Fulltext'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.Description',
      'parameters': [],
      'parameterizedName': 'film.types.Description',
      'namespace': 'film.types',
      'shortDisplayName': 'Description',
      'longDisplayName': 'film.types.Description',
      'name': 'Description'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/Description.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type Description inherits String\n}',
      'id': 'film/types/Description.taxi:0.0.0',
      'contentHash': 'ec3849'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.Description',
    'longDisplayName': 'film.types.Description',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.Description',
      'parameters': [],
      'parameterizedName': 'film.types.Description',
      'namespace': 'film.types',
      'shortDisplayName': 'Description',
      'longDisplayName': 'film.types.Description',
      'name': 'Description'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.ReleaseYear',
      'parameters': [],
      'parameterizedName': 'film.types.ReleaseYear',
      'namespace': 'film.types',
      'shortDisplayName': 'ReleaseYear',
      'longDisplayName': 'film.types.ReleaseYear',
      'name': 'ReleaseYear'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'name': 'Any'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/ReleaseYear.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type ReleaseYear inherits Any\n}',
      'id': 'film/types/ReleaseYear.taxi:0.0.0',
      'contentHash': '49725b'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'name': 'Any'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.ReleaseYear',
    'longDisplayName': 'film.types.ReleaseYear',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.ReleaseYear',
      'parameters': [],
      'parameterizedName': 'film.types.ReleaseYear',
      'namespace': 'film.types',
      'shortDisplayName': 'ReleaseYear',
      'longDisplayName': 'film.types.ReleaseYear',
      'name': 'ReleaseYear'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.SpecialFeatures',
      'parameters': [],
      'parameterizedName': 'film.types.SpecialFeatures',
      'namespace': 'film.types',
      'shortDisplayName': 'SpecialFeatures',
      'longDisplayName': 'film.types.SpecialFeatures',
      'name': 'SpecialFeatures'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/SpecialFeatures.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type SpecialFeatures inherits String\n}',
      'id': 'film/types/SpecialFeatures.taxi:0.0.0',
      'contentHash': '2c1071'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.SpecialFeatures',
    'longDisplayName': 'film.types.SpecialFeatures',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.SpecialFeatures',
      'parameters': [],
      'parameterizedName': 'film.types.SpecialFeatures',
      'namespace': 'film.types',
      'shortDisplayName': 'SpecialFeatures',
      'longDisplayName': 'film.types.SpecialFeatures',
      'name': 'SpecialFeatures'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.Length',
      'parameters': [],
      'parameterizedName': 'film.types.Length',
      'namespace': 'film.types',
      'shortDisplayName': 'Length',
      'longDisplayName': 'film.types.Length',
      'name': 'Length'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/Length.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type Length inherits Int\n}',
      'id': 'film/types/Length.taxi:0.0.0',
      'contentHash': '651a25'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.Length',
    'longDisplayName': 'film.types.Length',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.Length',
      'parameters': [],
      'parameterizedName': 'film.types.Length',
      'namespace': 'film.types',
      'shortDisplayName': 'Length',
      'longDisplayName': 'film.types.Length',
      'name': 'Length'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.RentalRate',
      'parameters': [],
      'parameterizedName': 'film.types.RentalRate',
      'namespace': 'film.types',
      'shortDisplayName': 'RentalRate',
      'longDisplayName': 'film.types.RentalRate',
      'name': 'RentalRate'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/RentalRate.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type RentalRate inherits Decimal\n}',
      'id': 'film/types/RentalRate.taxi:0.0.0',
      'contentHash': 'e1cc8c'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.RentalRate',
    'longDisplayName': 'film.types.RentalRate',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.RentalRate',
      'parameters': [],
      'parameterizedName': 'film.types.RentalRate',
      'namespace': 'film.types',
      'shortDisplayName': 'RentalRate',
      'longDisplayName': 'film.types.RentalRate',
      'name': 'RentalRate'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.Rating',
      'parameters': [],
      'parameterizedName': 'film.types.Rating',
      'namespace': 'film.types',
      'shortDisplayName': 'Rating',
      'longDisplayName': 'film.types.Rating',
      'name': 'Rating'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/Rating.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type Rating inherits String\n}',
      'id': 'film/types/Rating.taxi:0.0.0',
      'contentHash': '5f228f'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.Rating',
    'longDisplayName': 'film.types.Rating',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.Rating',
      'parameters': [],
      'parameterizedName': 'film.types.Rating',
      'namespace': 'film.types',
      'shortDisplayName': 'Rating',
      'longDisplayName': 'film.types.Rating',
      'name': 'Rating'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.types.RentalDuration',
      'parameters': [],
      'parameterizedName': 'film.types.RentalDuration',
      'namespace': 'film.types',
      'shortDisplayName': 'RentalDuration',
      'longDisplayName': 'film.types.RentalDuration',
      'name': 'RentalDuration'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/types/RentalDuration.taxi',
      'version': '0.0.0',
      'content': 'namespace film.types {\n   type RentalDuration inherits Int\n}',
      'id': 'film/types/RentalDuration.taxi:0.0.0',
      'contentHash': '584e8a'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.types.RentalDuration',
    'longDisplayName': 'film.types.RentalDuration',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.types.RentalDuration',
      'parameters': [],
      'parameterizedName': 'film.types.RentalDuration',
      'namespace': 'film.types',
      'shortDisplayName': 'RentalDuration',
      'longDisplayName': 'film.types.RentalDuration',
      'name': 'RentalDuration'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'film.Film',
      'parameters': [],
      'parameterizedName': 'film.Film',
      'namespace': 'film',
      'shortDisplayName': 'Film',
      'longDisplayName': 'film.Film',
      'name': 'Film'
    },
    'attributes': {
      'film_id': {
        'type': {
          'fullyQualifiedName': 'films.FilmId',
          'parameters': [],
          'parameterizedName': 'films.FilmId',
          'namespace': 'films',
          'shortDisplayName': 'FilmId',
          'longDisplayName': 'films.FilmId',
          'name': 'FilmId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'films.FilmId',
        'metadata': [{
          'name': {
            'fullyQualifiedName': 'Id',
            'parameters': [],
            'parameterizedName': 'Id',
            'namespace': '',
            'shortDisplayName': 'Id',
            'longDisplayName': 'Id',
            'name': 'Id'
          }, 'params': {}
        }],
        'sourcedBy': null
      },
      'title': {
        'type': {
          'fullyQualifiedName': 'film.types.Title',
          'parameters': [],
          'parameterizedName': 'film.types.Title',
          'namespace': 'film.types',
          'shortDisplayName': 'Title',
          'longDisplayName': 'film.types.Title',
          'name': 'Title'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'film.types.Title',
        'metadata': [],
        'sourcedBy': null
      },
      'description': {
        'type': {
          'fullyQualifiedName': 'film.types.Description',
          'parameters': [],
          'parameterizedName': 'film.types.Description',
          'namespace': 'film.types',
          'shortDisplayName': 'Description',
          'longDisplayName': 'film.types.Description',
          'name': 'Description'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': true,
        'typeDisplayName': 'film.types.Description',
        'metadata': [],
        'sourcedBy': null
      },
      'release_year': {
        'type': {
          'fullyQualifiedName': 'film.types.ReleaseYear',
          'parameters': [],
          'parameterizedName': 'film.types.ReleaseYear',
          'namespace': 'film.types',
          'shortDisplayName': 'ReleaseYear',
          'longDisplayName': 'film.types.ReleaseYear',
          'name': 'ReleaseYear'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': true,
        'typeDisplayName': 'film.types.ReleaseYear',
        'metadata': [],
        'sourcedBy': null
      },
      'language_id': {
        'type': {
          'fullyQualifiedName': 'language.types.LanguageId',
          'parameters': [],
          'parameterizedName': 'language.types.LanguageId',
          'namespace': 'language.types',
          'shortDisplayName': 'LanguageId',
          'longDisplayName': 'language.types.LanguageId',
          'name': 'LanguageId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'language.types.LanguageId',
        'metadata': [],
        'sourcedBy': null
      },
      'original_language_id': {
        'type': {
          'fullyQualifiedName': 'language.types.LanguageId',
          'parameters': [],
          'parameterizedName': 'language.types.LanguageId',
          'namespace': 'language.types',
          'shortDisplayName': 'LanguageId',
          'longDisplayName': 'language.types.LanguageId',
          'name': 'LanguageId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': true,
        'typeDisplayName': 'language.types.LanguageId',
        'metadata': [],
        'sourcedBy': null
      },
      'rental_duration': {
        'type': {
          'fullyQualifiedName': 'film.types.RentalDuration',
          'parameters': [],
          'parameterizedName': 'film.types.RentalDuration',
          'namespace': 'film.types',
          'shortDisplayName': 'RentalDuration',
          'longDisplayName': 'film.types.RentalDuration',
          'name': 'RentalDuration'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'film.types.RentalDuration',
        'metadata': [],
        'sourcedBy': null
      },
      'rental_rate': {
        'type': {
          'fullyQualifiedName': 'film.types.RentalRate',
          'parameters': [],
          'parameterizedName': 'film.types.RentalRate',
          'namespace': 'film.types',
          'shortDisplayName': 'RentalRate',
          'longDisplayName': 'film.types.RentalRate',
          'name': 'RentalRate'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'film.types.RentalRate',
        'metadata': [],
        'sourcedBy': null
      },
      'length': {
        'type': {
          'fullyQualifiedName': 'film.types.Length',
          'parameters': [],
          'parameterizedName': 'film.types.Length',
          'namespace': 'film.types',
          'shortDisplayName': 'Length',
          'longDisplayName': 'film.types.Length',
          'name': 'Length'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': true,
        'typeDisplayName': 'film.types.Length',
        'metadata': [],
        'sourcedBy': null
      },
      'replacement_cost': {
        'type': {
          'fullyQualifiedName': 'film.types.ReplacementCost',
          'parameters': [],
          'parameterizedName': 'film.types.ReplacementCost',
          'namespace': 'film.types',
          'shortDisplayName': 'ReplacementCost',
          'longDisplayName': 'film.types.ReplacementCost',
          'name': 'ReplacementCost'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'film.types.ReplacementCost',
        'metadata': [],
        'sourcedBy': null
      },
      'rating': {
        'type': {
          'fullyQualifiedName': 'film.types.Rating',
          'parameters': [],
          'parameterizedName': 'film.types.Rating',
          'namespace': 'film.types',
          'shortDisplayName': 'Rating',
          'longDisplayName': 'film.types.Rating',
          'name': 'Rating'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': true,
        'typeDisplayName': 'film.types.Rating',
        'metadata': [],
        'sourcedBy': null
      },
      'last_update': {
        'type': {
          'fullyQualifiedName': 'film.types.LastUpdate',
          'parameters': [],
          'parameterizedName': 'film.types.LastUpdate',
          'namespace': 'film.types',
          'shortDisplayName': 'LastUpdate',
          'longDisplayName': 'film.types.LastUpdate',
          'name': 'LastUpdate'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'film.types.LastUpdate',
        'metadata': [],
        'sourcedBy': null
      },
      'special_features': {
        'type': {
          'fullyQualifiedName': 'lang.taxi.Array',
          'parameters': [{
            'fullyQualifiedName': 'film.types.SpecialFeatures',
            'parameters': [],
            'parameterizedName': 'film.types.SpecialFeatures',
            'namespace': 'film.types',
            'shortDisplayName': 'SpecialFeatures',
            'longDisplayName': 'film.types.SpecialFeatures',
            'name': 'SpecialFeatures'
          }],
          'parameterizedName': 'lang.taxi.Array<film.types.SpecialFeatures>',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'SpecialFeatures[]',
          'longDisplayName': 'film.types.SpecialFeatures[]',
          'name': 'Array'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': true,
        'typeDisplayName': 'film.types.SpecialFeatures[]',
        'metadata': [],
        'sourcedBy': null
      },
      'fulltext': {
        'type': {
          'fullyQualifiedName': 'film.types.Fulltext',
          'parameters': [],
          'parameterizedName': 'film.types.Fulltext',
          'namespace': 'film.types',
          'shortDisplayName': 'Fulltext',
          'longDisplayName': 'film.types.Fulltext',
          'name': 'Fulltext'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'film.types.Fulltext',
        'metadata': [],
        'sourcedBy': null
      }
    },
    'modifiers': [],
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'io.vyne.jdbc.Table',
        'parameters': [],
        'parameterizedName': 'io.vyne.jdbc.Table',
        'namespace': 'io.vyne.jdbc',
        'shortDisplayName': 'Table',
        'longDisplayName': 'io.vyne.jdbc.Table',
        'name': 'Table'
      }, 'params': { 'table': 'film', 'schema': 'public', 'connection': 'films' }
    }],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'film/Film.taxi',
      'version': '0.0.0',
      'content': 'import films.FilmId\nimport film.types.Title\nimport film.types.Description\nimport film.types.ReleaseYear\nimport language.types.LanguageId\nimport language.types.LanguageId\nimport film.types.RentalDuration\nimport film.types.RentalRate\nimport film.types.Length\nimport film.types.ReplacementCost\nimport film.types.Rating\nimport film.types.LastUpdate\nimport film.types.Fulltext\nimport io.vyne.jdbc.Table\nimport film.types.SpecialFeatures\nnamespace film {\n   @io.vyne.jdbc.Table(table = "film" , schema = "public" , connection = "films")\n         model Film {\n            @Id film_id : films.FilmId\n            title : film.types.Title\n            description : film.types.Description?\n            release_year : film.types.ReleaseYear?\n            language_id : language.types.LanguageId\n            original_language_id : language.types.LanguageId?\n            rental_duration : film.types.RentalDuration\n            rental_rate : film.types.RentalRate\n            length : film.types.Length?\n            replacement_cost : film.types.ReplacementCost\n            rating : film.types.Rating?\n            last_update : film.types.LastUpdate\n            special_features : film.types.SpecialFeatures[]?\n            fulltext : film.types.Fulltext\n         }\n}',
      'id': 'film/Film.taxi:0.0.0',
      'contentHash': 'fc40ac'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'film.Film',
    'longDisplayName': 'film.Film',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.Film',
      'parameters': [],
      'parameterizedName': 'film.Film',
      'namespace': 'film',
      'shortDisplayName': 'Film',
      'longDisplayName': 'film.Film',
      'name': 'Film'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
      'parameters': [],
      'parameterizedName': 'demo.netflix.NetflixFilmId',
      'namespace': 'demo.netflix',
      'shortDisplayName': 'NetflixFilmId',
      'longDisplayName': 'demo.netflix.NetflixFilmId',
      'name': 'NetflixFilmId'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'film/netflix/netflix-types.taxi',
      'version': '0.0.0',
      'content': 'namespace demo.netflix {\n   type NetflixFilmId inherits Int\n}',
      'id': 'film/netflix/netflix-types.taxi:0.0.0',
      'contentHash': '086c9e'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
    'longDisplayName': 'demo.netflix.NetflixFilmId',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
      'parameters': [],
      'parameterizedName': 'demo.netflix.NetflixFilmId',
      'namespace': 'demo.netflix',
      'shortDisplayName': 'NetflixFilmId',
      'longDisplayName': 'demo.netflix.NetflixFilmId',
      'name': 'NetflixFilmId'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
      'parameters': [],
      'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
      'namespace': 'films.reviews',
      'shortDisplayName': 'SquashedTomatoesFilmId',
      'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
      'name': 'SquashedTomatoesFilmId'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'reviews/review-site.taxi',
      'version': '0.0.0',
      'content': 'namespace films.reviews {\n   type SquashedTomatoesFilmId inherits String\n}',
      'id': 'reviews/review-site.taxi:0.0.0',
      'contentHash': 'f01c20'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
    'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
    'memberQualifiedName': {
      'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
      'parameters': [],
      'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
      'namespace': 'films.reviews',
      'shortDisplayName': 'SquashedTomatoesFilmId',
      'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
      'name': 'SquashedTomatoesFilmId'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'films.reviews.FilmReviewScore',
      'parameters': [],
      'parameterizedName': 'films.reviews.FilmReviewScore',
      'namespace': 'films.reviews',
      'shortDisplayName': 'FilmReviewScore',
      'longDisplayName': 'films.reviews.FilmReviewScore',
      'name': 'FilmReviewScore'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'reviews/review-site.taxi',
      'version': '0.0.0',
      'content': 'namespace films.reviews {\n   type FilmReviewScore inherits Decimal\n}',
      'id': 'reviews/review-site.taxi:0.0.0',
      'contentHash': '883e32'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'films.reviews.FilmReviewScore',
    'longDisplayName': 'films.reviews.FilmReviewScore',
    'memberQualifiedName': {
      'fullyQualifiedName': 'films.reviews.FilmReviewScore',
      'parameters': [],
      'parameterizedName': 'films.reviews.FilmReviewScore',
      'namespace': 'films.reviews',
      'shortDisplayName': 'FilmReviewScore',
      'longDisplayName': 'films.reviews.FilmReviewScore',
      'name': 'FilmReviewScore'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'films.reviews.ReviewText',
      'parameters': [],
      'parameterizedName': 'films.reviews.ReviewText',
      'namespace': 'films.reviews',
      'shortDisplayName': 'ReviewText',
      'longDisplayName': 'films.reviews.ReviewText',
      'name': 'ReviewText'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'reviews/review-site.taxi',
      'version': '0.0.0',
      'content': 'namespace films.reviews {\n   type ReviewText inherits String\n}',
      'id': 'reviews/review-site.taxi:0.0.0',
      'contentHash': '1927dc'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'films.reviews.ReviewText',
    'longDisplayName': 'films.reviews.ReviewText',
    'memberQualifiedName': {
      'fullyQualifiedName': 'films.reviews.ReviewText',
      'parameters': [],
      'parameterizedName': 'films.reviews.ReviewText',
      'namespace': 'films.reviews',
      'shortDisplayName': 'ReviewText',
      'longDisplayName': 'films.reviews.ReviewText',
      'name': 'ReviewText'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'films.StreamingProviderName',
      'parameters': [],
      'parameterizedName': 'films.StreamingProviderName',
      'namespace': 'films',
      'shortDisplayName': 'StreamingProviderName',
      'longDisplayName': 'films.StreamingProviderName',
      'name': 'StreamingProviderName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'films.taxi',
      'version': '0.0.0',
      'content': 'namespace films {\n   type StreamingProviderName inherits String\n}',
      'id': 'films.taxi:0.0.0',
      'contentHash': '3454df'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'films.StreamingProviderName',
    'longDisplayName': 'films.StreamingProviderName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'films.StreamingProviderName',
      'parameters': [],
      'parameterizedName': 'films.StreamingProviderName',
      'namespace': 'films',
      'shortDisplayName': 'StreamingProviderName',
      'longDisplayName': 'films.StreamingProviderName',
      'name': 'StreamingProviderName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'films.StreamingProviderPrice',
      'parameters': [],
      'parameterizedName': 'films.StreamingProviderPrice',
      'namespace': 'films',
      'shortDisplayName': 'StreamingProviderPrice',
      'longDisplayName': 'films.StreamingProviderPrice',
      'name': 'StreamingProviderPrice'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'films.taxi',
      'version': '0.0.0',
      'content': 'namespace films {\n   type StreamingProviderPrice inherits Decimal\n}',
      'id': 'films.taxi:0.0.0',
      'contentHash': 'ec7078'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'name': 'Decimal'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'films.StreamingProviderPrice',
    'longDisplayName': 'films.StreamingProviderPrice',
    'memberQualifiedName': {
      'fullyQualifiedName': 'films.StreamingProviderPrice',
      'parameters': [],
      'parameterizedName': 'films.StreamingProviderPrice',
      'namespace': 'films',
      'shortDisplayName': 'StreamingProviderPrice',
      'longDisplayName': 'films.StreamingProviderPrice',
      'name': 'StreamingProviderPrice'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'films.FilmId',
      'parameters': [],
      'parameterizedName': 'films.FilmId',
      'namespace': 'films',
      'shortDisplayName': 'FilmId',
      'longDisplayName': 'films.FilmId',
      'name': 'FilmId'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'films.taxi',
      'version': '0.0.0',
      'content': 'namespace films {\n   type FilmId inherits Int\n}',
      'id': 'films.taxi:0.0.0',
      'contentHash': 'b5803c'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'parameterizedName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'name': 'Int'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'films.FilmId',
    'longDisplayName': 'films.FilmId',
    'memberQualifiedName': {
      'fullyQualifiedName': 'films.FilmId',
      'parameters': [],
      'parameterizedName': 'films.FilmId',
      'namespace': 'films',
      'shortDisplayName': 'FilmId',
      'longDisplayName': 'films.FilmId',
      'name': 'FilmId'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdResolution',
      'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
      'name': 'IdResolution'
    },
    'attributes': {
      'filmId': {
        'type': {
          'fullyQualifiedName': 'films.FilmId',
          'parameters': [],
          'parameterizedName': 'films.FilmId',
          'namespace': 'films',
          'shortDisplayName': 'FilmId',
          'longDisplayName': 'films.FilmId',
          'name': 'FilmId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'films.FilmId',
        'metadata': [],
        'sourcedBy': null
      },
      'netflixId': {
        'type': {
          'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
          'parameters': [],
          'parameterizedName': 'demo.netflix.NetflixFilmId',
          'namespace': 'demo.netflix',
          'shortDisplayName': 'NetflixFilmId',
          'longDisplayName': 'demo.netflix.NetflixFilmId',
          'name': 'NetflixFilmId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'demo.netflix.NetflixFilmId',
        'metadata': [],
        'sourcedBy': null
      },
      'squashedTomatoesFilmId': {
        'type': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'films.reviews.SquashedTomatoesFilmId',
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
      'name': 'id-resolution-service',
      'version': '0.0.0',
      'content': 'import films.FilmId\nimport demo.netflix.NetflixFilmId\nimport films.reviews.SquashedTomatoesFilmId\nnamespace io.vyne.films.idlookup {\n   model IdResolution {\n         filmId : films.FilmId\n         netflixId : demo.netflix.NetflixFilmId\n         squashedTomatoesFilmId : films.reviews.SquashedTomatoesFilmId\n      }\n}',
      'id': 'id-resolution-service:0.0.0',
      'contentHash': '683376'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
    'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdResolution',
      'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
      'name': 'IdResolution'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingProviderRequest',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingProviderRequest',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingProviderRequest',
      'longDisplayName': 'io.vyne.demos.films.StreamingProviderRequest',
      'name': 'StreamingProviderRequest'
    },
    'attributes': {
      'filmId': {
        'type': {
          'fullyQualifiedName': 'films.FilmId',
          'parameters': [],
          'parameterizedName': 'films.FilmId',
          'namespace': 'films',
          'shortDisplayName': 'FilmId',
          'longDisplayName': 'films.FilmId',
          'name': 'FilmId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'films.FilmId',
        'metadata': [],
        'sourcedBy': null
      }
    },
    'modifiers': ['PARAMETER_TYPE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'films-service',
      'version': '0.0.0',
      'content': 'import films.FilmId\nnamespace io.vyne.demos.films {\n   parameter model StreamingProviderRequest {\n         filmId : films.FilmId\n      }\n}',
      'id': 'films-service:0.0.0',
      'contentHash': '70cab2'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': true,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.demos.films.StreamingProviderRequest',
    'longDisplayName': 'io.vyne.demos.films.StreamingProviderRequest',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingProviderRequest',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingProviderRequest',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingProviderRequest',
      'longDisplayName': 'io.vyne.demos.films.StreamingProviderRequest',
      'name': 'StreamingProviderRequest'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingProvider',
      'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
      'name': 'StreamingProvider'
    },
    'attributes': {
      'name': {
        'type': {
          'fullyQualifiedName': 'films.StreamingProviderName',
          'parameters': [],
          'parameterizedName': 'films.StreamingProviderName',
          'namespace': 'films',
          'shortDisplayName': 'StreamingProviderName',
          'longDisplayName': 'films.StreamingProviderName',
          'name': 'StreamingProviderName'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'films.StreamingProviderName',
        'metadata': [],
        'sourcedBy': null
      },
      'pricePerMonth': {
        'type': {
          'fullyQualifiedName': 'films.StreamingProviderPrice',
          'parameters': [],
          'parameterizedName': 'films.StreamingProviderPrice',
          'namespace': 'films',
          'shortDisplayName': 'StreamingProviderPrice',
          'longDisplayName': 'films.StreamingProviderPrice',
          'name': 'StreamingProviderPrice'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'films.StreamingProviderPrice',
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
      'name': 'films-service',
      'version': '0.0.0',
      'content': 'import films.StreamingProviderName\nimport films.StreamingProviderPrice\nnamespace io.vyne.demos.films {\n   model StreamingProvider {\n         name : films.StreamingProviderName\n         pricePerMonth : films.StreamingProviderPrice\n      }\n}',
      'id': 'films-service:0.0.0',
      'contentHash': 'fa5db1'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
    'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingProvider',
      'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
      'name': 'StreamingProvider'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
      'parameters': [],
      'parameterizedName': 'io.vyne.reviews.FilmReview',
      'namespace': 'io.vyne.reviews',
      'shortDisplayName': 'FilmReview',
      'longDisplayName': 'io.vyne.reviews.FilmReview',
      'name': 'FilmReview'
    },
    'attributes': {
      'filmId': {
        'type': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'films.reviews.SquashedTomatoesFilmId',
        'metadata': [],
        'sourcedBy': null
      },
      'filmReview': {
        'type': {
          'fullyQualifiedName': 'films.reviews.ReviewText',
          'parameters': [],
          'parameterizedName': 'films.reviews.ReviewText',
          'namespace': 'films.reviews',
          'shortDisplayName': 'ReviewText',
          'longDisplayName': 'films.reviews.ReviewText',
          'name': 'ReviewText'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'films.reviews.ReviewText',
        'metadata': [],
        'sourcedBy': null
      },
      'score': {
        'type': {
          'fullyQualifiedName': 'films.reviews.FilmReviewScore',
          'parameters': [],
          'parameterizedName': 'films.reviews.FilmReviewScore',
          'namespace': 'films.reviews',
          'shortDisplayName': 'FilmReviewScore',
          'longDisplayName': 'films.reviews.FilmReviewScore',
          'name': 'FilmReviewScore'
        },
        'modifiers': [],
        'typeDoc': null,
        'defaultValue': null,
        'nullable': false,
        'typeDisplayName': 'films.reviews.FilmReviewScore',
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
      'name': 'squashed-tomatoes',
      'version': '0.0.0',
      'content': 'import films.reviews.SquashedTomatoesFilmId\nimport films.reviews.ReviewText\nimport films.reviews.FilmReviewScore\nnamespace io.vyne.reviews {\n   model FilmReview {\n         filmId : films.reviews.SquashedTomatoesFilmId\n         filmReview : films.reviews.ReviewText\n         score : films.reviews.FilmReviewScore\n      }\n}',
      'id': 'squashed-tomatoes:0.0.0',
      'contentHash': 'b7a47b'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': null,
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
    'longDisplayName': 'io.vyne.reviews.FilmReview',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
      'parameters': [],
      'parameterizedName': 'io.vyne.reviews.FilmReview',
      'namespace': 'io.vyne.reviews',
      'shortDisplayName': 'FilmReview',
      'longDisplayName': 'io.vyne.reviews.FilmReview',
      'name': 'FilmReview'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.jdbc.TableName',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.TableName',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'TableName',
      'longDisplayName': 'io.vyne.jdbc.TableName',
      'name': 'TableName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'JdbcConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.jdbc {\n   TableName inherits String\n}',
      'id': 'JdbcConnectors:0.0.0',
      'contentHash': 'df61fc'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.jdbc.TableName',
    'longDisplayName': 'io.vyne.jdbc.TableName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.jdbc.TableName',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.TableName',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'TableName',
      'longDisplayName': 'io.vyne.jdbc.TableName',
      'name': 'TableName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.jdbc.SchemaName',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.SchemaName',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'SchemaName',
      'longDisplayName': 'io.vyne.jdbc.SchemaName',
      'name': 'SchemaName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'JdbcConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.jdbc {\n   SchemaName inherits String\n}',
      'id': 'JdbcConnectors:0.0.0',
      'contentHash': '49e2fe'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.jdbc.SchemaName',
    'longDisplayName': 'io.vyne.jdbc.SchemaName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.jdbc.SchemaName',
      'parameters': [],
      'parameterizedName': 'io.vyne.jdbc.SchemaName',
      'namespace': 'io.vyne.jdbc',
      'shortDisplayName': 'SchemaName',
      'longDisplayName': 'io.vyne.jdbc.SchemaName',
      'name': 'SchemaName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.kafka.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.ConnectionName',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.kafka.ConnectionName',
      'name': 'ConnectionName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'vyne/internal-types.taxi',
      'version': '0.0.0',
      'content': 'namespace io.vyne.kafka {\n   ConnectionName inherits String\n}',
      'id': 'vyne/internal-types.taxi:0.0.0',
      'contentHash': '28e454'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.kafka.ConnectionName',
    'longDisplayName': 'io.vyne.kafka.ConnectionName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.kafka.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.ConnectionName',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.kafka.ConnectionName',
      'name': 'ConnectionName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.kafka.TopicName',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.TopicName',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'TopicName',
      'longDisplayName': 'io.vyne.kafka.TopicName',
      'name': 'TopicName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'vyne/internal-types.taxi',
      'version': '0.0.0',
      'content': 'namespace io.vyne.kafka {\n   TopicName inherits String\n}',
      'id': 'vyne/internal-types.taxi:0.0.0',
      'contentHash': 'c6ba94'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.kafka.TopicName',
    'longDisplayName': 'io.vyne.kafka.TopicName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.kafka.TopicName',
      'parameters': [],
      'parameterizedName': 'io.vyne.kafka.TopicName',
      'namespace': 'io.vyne.kafka',
      'shortDisplayName': 'TopicName',
      'longDisplayName': 'io.vyne.kafka.TopicName',
      'name': 'TopicName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.s3.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.ConnectionName',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.aws.s3.ConnectionName',
      'name': 'ConnectionName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AwsS3Connectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.s3 {\n   ConnectionName inherits String\n}',
      'id': 'AwsS3Connectors:0.0.0',
      'contentHash': 'e2d418'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.s3.ConnectionName',
    'longDisplayName': 'io.vyne.aws.s3.ConnectionName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.s3.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.ConnectionName',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.aws.s3.ConnectionName',
      'name': 'ConnectionName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.s3.BucketName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.BucketName',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'BucketName',
      'longDisplayName': 'io.vyne.aws.s3.BucketName',
      'name': 'BucketName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AwsS3Connectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.s3 {\n   BucketName inherits String\n}',
      'id': 'AwsS3Connectors:0.0.0',
      'contentHash': '7ac5e3'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.s3.BucketName',
    'longDisplayName': 'io.vyne.aws.s3.BucketName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.s3.BucketName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.s3.BucketName',
      'namespace': 'io.vyne.aws.s3',
      'shortDisplayName': 'BucketName',
      'longDisplayName': 'io.vyne.aws.s3.BucketName',
      'name': 'BucketName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.ConnectionName',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.aws.sqs.ConnectionName',
      'name': 'ConnectionName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AwsSqsConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.sqs {\n   ConnectionName inherits String\n}',
      'id': 'AwsSqsConnectors:0.0.0',
      'contentHash': '2997f6'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.sqs.ConnectionName',
    'longDisplayName': 'io.vyne.aws.sqs.ConnectionName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.ConnectionName',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.aws.sqs.ConnectionName',
      'name': 'ConnectionName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.QueueName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.QueueName',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'QueueName',
      'longDisplayName': 'io.vyne.aws.sqs.QueueName',
      'name': 'QueueName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AwsSqsConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.sqs {\n   QueueName inherits String\n}',
      'id': 'AwsSqsConnectors:0.0.0',
      'contentHash': 'e52a2b'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.sqs.QueueName',
    'longDisplayName': 'io.vyne.aws.sqs.QueueName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.sqs.QueueName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.sqs.QueueName',
      'namespace': 'io.vyne.aws.sqs',
      'shortDisplayName': 'QueueName',
      'longDisplayName': 'io.vyne.aws.sqs.QueueName',
      'name': 'QueueName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.azure.store.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.ConnectionName',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.azure.store.ConnectionName',
      'name': 'ConnectionName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AzureStoreConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.azure.store {\n   ConnectionName inherits String\n}',
      'id': 'AzureStoreConnectors:0.0.0',
      'contentHash': '93d191'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.azure.store.ConnectionName',
    'longDisplayName': 'io.vyne.azure.store.ConnectionName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.azure.store.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.ConnectionName',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.azure.store.ConnectionName',
      'name': 'ConnectionName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreContainer',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.AzureStoreContainer',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'AzureStoreContainer',
      'longDisplayName': 'io.vyne.azure.store.AzureStoreContainer',
      'name': 'AzureStoreContainer'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AzureStoreConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.azure.store {\n   AzureStoreContainer inherits String\n}',
      'id': 'AzureStoreConnectors:0.0.0',
      'contentHash': '1bb1cc'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreContainer',
    'longDisplayName': 'io.vyne.azure.store.AzureStoreContainer',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.azure.store.AzureStoreContainer',
      'parameters': [],
      'parameterizedName': 'io.vyne.azure.store.AzureStoreContainer',
      'namespace': 'io.vyne.azure.store',
      'shortDisplayName': 'AzureStoreContainer',
      'longDisplayName': 'io.vyne.azure.store.AzureStoreContainer',
      'name': 'AzureStoreContainer'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.ConnectionName',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.aws.lambda.ConnectionName',
      'name': 'ConnectionName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AwsLambdaConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.lambda {\n   ConnectionName inherits String\n}',
      'id': 'AwsLambdaConnectors:0.0.0',
      'contentHash': 'ba8822'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.lambda.ConnectionName',
    'longDisplayName': 'io.vyne.aws.lambda.ConnectionName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.ConnectionName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.ConnectionName',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'ConnectionName',
      'longDisplayName': 'io.vyne.aws.lambda.ConnectionName',
      'name': 'ConnectionName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.OperationName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.OperationName',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'OperationName',
      'longDisplayName': 'io.vyne.aws.lambda.OperationName',
      'name': 'OperationName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [{
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    }],
    'enumValues': [],
    'sources': [{
      'name': 'AwsLambdaConnectors',
      'version': '0.0.0',
      'content': 'namespace io.vyne.aws.lambda {\n   OperationName inherits String\n}',
      'id': 'AwsLambdaConnectors:0.0.0',
      'contentHash': 'f4ed37'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'offset': null,
    'format': null,
    'hasFormat': false,
    'declaresFormat': false,
    'basePrimitiveTypeName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'parameterizedName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'name': 'String'
    },
    'hasExpression': false,
    'unformattedTypeName': null,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'io.vyne.aws.lambda.OperationName',
    'longDisplayName': 'io.vyne.aws.lambda.OperationName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.aws.lambda.OperationName',
      'parameters': [],
      'parameterizedName': 'io.vyne.aws.lambda.OperationName',
      'namespace': 'io.vyne.aws.lambda',
      'shortDisplayName': 'OperationName',
      'longDisplayName': 'io.vyne.aws.lambda.OperationName',
      'name': 'OperationName'
    },
    'underlyingTypeParameters': [],
    'isCollection': false,
    'isStream': false,
    'collectionType': null,
    'isScalar': true
  }],
  'services': [{
    'name': {
      'fullyQualifiedName': 'actor.ActorService',
      'parameters': [],
      'parameterizedName': 'actor.ActorService',
      'namespace': 'actor',
      'shortDisplayName': 'ActorService',
      'longDisplayName': 'actor.ActorService',
      'name': 'ActorService'
    },
    'operations': [],
    'queryOperations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'actor.ActorService@@findManyActor',
        'parameters': [],
        'parameterizedName': 'actor.ActorService@@findManyActor',
        'namespace': 'actor',
        'shortDisplayName': 'findManyActor',
        'longDisplayName': 'actor.ActorService / findManyActor',
        'name': 'ActorService@@findManyActor'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        },
        'name': 'querySpec',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'actor.Actor',
          'parameters': [],
          'parameterizedName': 'actor.Actor',
          'namespace': 'actor',
          'shortDisplayName': 'Actor',
          'longDisplayName': 'actor.Actor',
          'name': 'Actor'
        }],
        'parameterizedName': 'lang.taxi.Array<actor.Actor>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Actor[]',
        'longDisplayName': 'actor.Actor[]',
        'name': 'Array'
      },
      'metadata': [],
      'grammar': 'vyneQl',
      'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
      'typeDoc': null,
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'lang.taxi.Array',
          'parameters': [{
            'fullyQualifiedName': 'actor.Actor',
            'parameters': [],
            'parameterizedName': 'actor.Actor',
            'namespace': 'actor',
            'shortDisplayName': 'Actor',
            'longDisplayName': 'actor.Actor',
            'name': 'Actor'
          }],
          'parameterizedName': 'lang.taxi.Array<actor.Actor>',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'Actor[]',
          'longDisplayName': 'actor.Actor[]',
          'name': 'Array'
        }, 'constraints': []
      },
      'operationType': null,
      'hasFilterCapability': true,
      'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
      'returnTypeName': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'actor.Actor',
          'parameters': [],
          'parameterizedName': 'actor.Actor',
          'namespace': 'actor',
          'shortDisplayName': 'Actor',
          'longDisplayName': 'actor.Actor',
          'name': 'Actor'
        }],
        'parameterizedName': 'lang.taxi.Array<actor.Actor>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Actor[]',
        'longDisplayName': 'actor.Actor[]',
        'name': 'Array'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.ActorService@@findManyActor',
        'parameters': [],
        'parameterizedName': 'actor.ActorService@@findManyActor',
        'namespace': 'actor',
        'shortDisplayName': 'findManyActor',
        'longDisplayName': 'actor.ActorService / findManyActor',
        'name': 'ActorService@@findManyActor'
      },
      'name': 'findManyActor'
    }, {
      'qualifiedName': {
        'fullyQualifiedName': 'actor.ActorService@@findOneActor',
        'parameters': [],
        'parameterizedName': 'actor.ActorService@@findOneActor',
        'namespace': 'actor',
        'shortDisplayName': 'findOneActor',
        'longDisplayName': 'actor.ActorService / findOneActor',
        'name': 'ActorService@@findOneActor'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        },
        'name': 'querySpec',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'actor.Actor',
        'parameters': [],
        'parameterizedName': 'actor.Actor',
        'namespace': 'actor',
        'shortDisplayName': 'Actor',
        'longDisplayName': 'actor.Actor',
        'name': 'Actor'
      },
      'metadata': [],
      'grammar': 'vyneQl',
      'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
      'typeDoc': null,
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'actor.Actor',
          'parameters': [],
          'parameterizedName': 'actor.Actor',
          'namespace': 'actor',
          'shortDisplayName': 'Actor',
          'longDisplayName': 'actor.Actor',
          'name': 'Actor'
        }, 'constraints': []
      },
      'operationType': null,
      'hasFilterCapability': true,
      'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
      'returnTypeName': {
        'fullyQualifiedName': 'actor.Actor',
        'parameters': [],
        'parameterizedName': 'actor.Actor',
        'namespace': 'actor',
        'shortDisplayName': 'Actor',
        'longDisplayName': 'actor.Actor',
        'name': 'Actor'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.ActorService@@findOneActor',
        'parameters': [],
        'parameterizedName': 'actor.ActorService@@findOneActor',
        'namespace': 'actor',
        'shortDisplayName': 'findOneActor',
        'longDisplayName': 'actor.ActorService / findOneActor',
        'name': 'ActorService@@findOneActor'
      },
      'name': 'findOneActor'
    }],
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'io.vyne.jdbc.DatabaseService',
        'parameters': [],
        'parameterizedName': 'io.vyne.jdbc.DatabaseService',
        'namespace': 'io.vyne.jdbc',
        'shortDisplayName': 'DatabaseService',
        'longDisplayName': 'io.vyne.jdbc.DatabaseService',
        'name': 'DatabaseService'
      }, 'params': { 'connection': 'films' }
    }],
    'sourceCode': [{
      'name': 'actor/ActorService.taxi',
      'version': '0.0.0',
      'content': 'import vyne.vyneQl.VyneQlQuery\nimport vyne.vyneQl.VyneQlQuery\nnamespace actor {\n   @io.vyne.jdbc.DatabaseService(connection = "films")\n         service ActorService {\n            vyneQl query findManyActor(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<actor.Actor> with capabilities {\n               sum,\n               count,\n               avg,\n               min,\n               max,\n               filter(==,!=,in,like,>,<,>=,<=)\n            }\n            vyneQl query findOneActor(querySpec: vyne.vyneQl.VyneQlQuery):actor.Actor with capabilities {\n               sum,\n               count,\n               avg,\n               min,\n               max,\n               filter(==,!=,in,like,>,<,>=,<=)\n            }\n         }\n}',
      'id': 'actor/ActorService.taxi:0.0.0',
      'contentHash': '550ed3'
    }],
    'typeDoc': null,
    'lineage': null,
    'serviceKind': 'Database',
    'remoteOperations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'actor.ActorService@@findManyActor',
        'parameters': [],
        'parameterizedName': 'actor.ActorService@@findManyActor',
        'namespace': 'actor',
        'shortDisplayName': 'findManyActor',
        'longDisplayName': 'actor.ActorService / findManyActor',
        'name': 'ActorService@@findManyActor'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        },
        'name': 'querySpec',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'actor.Actor',
          'parameters': [],
          'parameterizedName': 'actor.Actor',
          'namespace': 'actor',
          'shortDisplayName': 'Actor',
          'longDisplayName': 'actor.Actor',
          'name': 'Actor'
        }],
        'parameterizedName': 'lang.taxi.Array<actor.Actor>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Actor[]',
        'longDisplayName': 'actor.Actor[]',
        'name': 'Array'
      },
      'metadata': [],
      'grammar': 'vyneQl',
      'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
      'typeDoc': null,
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'lang.taxi.Array',
          'parameters': [{
            'fullyQualifiedName': 'actor.Actor',
            'parameters': [],
            'parameterizedName': 'actor.Actor',
            'namespace': 'actor',
            'shortDisplayName': 'Actor',
            'longDisplayName': 'actor.Actor',
            'name': 'Actor'
          }],
          'parameterizedName': 'lang.taxi.Array<actor.Actor>',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'Actor[]',
          'longDisplayName': 'actor.Actor[]',
          'name': 'Array'
        }, 'constraints': []
      },
      'operationType': null,
      'hasFilterCapability': true,
      'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
      'returnTypeName': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'actor.Actor',
          'parameters': [],
          'parameterizedName': 'actor.Actor',
          'namespace': 'actor',
          'shortDisplayName': 'Actor',
          'longDisplayName': 'actor.Actor',
          'name': 'Actor'
        }],
        'parameterizedName': 'lang.taxi.Array<actor.Actor>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Actor[]',
        'longDisplayName': 'actor.Actor[]',
        'name': 'Array'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.ActorService@@findManyActor',
        'parameters': [],
        'parameterizedName': 'actor.ActorService@@findManyActor',
        'namespace': 'actor',
        'shortDisplayName': 'findManyActor',
        'longDisplayName': 'actor.ActorService / findManyActor',
        'name': 'ActorService@@findManyActor'
      },
      'name': 'findManyActor'
    }, {
      'qualifiedName': {
        'fullyQualifiedName': 'actor.ActorService@@findOneActor',
        'parameters': [],
        'parameterizedName': 'actor.ActorService@@findOneActor',
        'namespace': 'actor',
        'shortDisplayName': 'findOneActor',
        'longDisplayName': 'actor.ActorService / findOneActor',
        'name': 'ActorService@@findOneActor'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        },
        'name': 'querySpec',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'actor.Actor',
        'parameters': [],
        'parameterizedName': 'actor.Actor',
        'namespace': 'actor',
        'shortDisplayName': 'Actor',
        'longDisplayName': 'actor.Actor',
        'name': 'Actor'
      },
      'metadata': [],
      'grammar': 'vyneQl',
      'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
      'typeDoc': null,
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'actor.Actor',
          'parameters': [],
          'parameterizedName': 'actor.Actor',
          'namespace': 'actor',
          'shortDisplayName': 'Actor',
          'longDisplayName': 'actor.Actor',
          'name': 'Actor'
        }, 'constraints': []
      },
      'operationType': null,
      'hasFilterCapability': true,
      'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
      'returnTypeName': {
        'fullyQualifiedName': 'actor.Actor',
        'parameters': [],
        'parameterizedName': 'actor.Actor',
        'namespace': 'actor',
        'shortDisplayName': 'Actor',
        'longDisplayName': 'actor.Actor',
        'name': 'Actor'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'actor.ActorService@@findOneActor',
        'parameters': [],
        'parameterizedName': 'actor.ActorService@@findOneActor',
        'namespace': 'actor',
        'shortDisplayName': 'findOneActor',
        'longDisplayName': 'actor.ActorService / findOneActor',
        'name': 'ActorService@@findOneActor'
      },
      'name': 'findOneActor'
    }],
    'qualifiedName': 'actor.ActorService',
    'memberQualifiedName': {
      'fullyQualifiedName': 'actor.ActorService',
      'parameters': [],
      'parameterizedName': 'actor.ActorService',
      'namespace': 'actor',
      'shortDisplayName': 'ActorService',
      'longDisplayName': 'actor.ActorService',
      'name': 'ActorService'
    }
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.films.announcements.KafkaService',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.announcements.KafkaService',
      'namespace': 'io.vyne.films.announcements',
      'shortDisplayName': 'KafkaService',
      'longDisplayName': 'io.vyne.films.announcements.KafkaService',
      'name': 'KafkaService'
    },
    'operations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
        'namespace': 'io.vyne.films.announcements',
        'shortDisplayName': 'consumeFromReleases',
        'longDisplayName': 'io.vyne.films.announcements.KafkaService / consumeFromReleases',
        'name': 'KafkaService@@consumeFromReleases'
      },
      'parameters': [],
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Stream',
        'parameters': [{
          'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
          'parameters': [],
          'parameterizedName': 'NewFilmReleaseAnnouncement',
          'namespace': '',
          'shortDisplayName': 'NewFilmReleaseAnnouncement',
          'longDisplayName': 'NewFilmReleaseAnnouncement',
          'name': 'NewFilmReleaseAnnouncement'
        }],
        'parameterizedName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Stream<NewFilmReleaseAnnouncement>',
        'longDisplayName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>',
        'name': 'Stream'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'io.vyne.kafka.KafkaOperation',
          'parameters': [],
          'parameterizedName': 'io.vyne.kafka.KafkaOperation',
          'namespace': 'io.vyne.kafka',
          'shortDisplayName': 'KafkaOperation',
          'longDisplayName': 'io.vyne.kafka.KafkaOperation',
          'name': 'KafkaOperation'
        }, 'params': { 'topic': 'releases', 'offset': 'latest' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'lang.taxi.Stream',
          'parameters': [{
            'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
            'parameters': [],
            'parameterizedName': 'NewFilmReleaseAnnouncement',
            'namespace': '',
            'shortDisplayName': 'NewFilmReleaseAnnouncement',
            'longDisplayName': 'NewFilmReleaseAnnouncement',
            'name': 'NewFilmReleaseAnnouncement'
          }],
          'parameterizedName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement>',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'Stream<NewFilmReleaseAnnouncement>',
          'longDisplayName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>',
          'name': 'Stream'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'lang.taxi.Stream',
        'parameters': [{
          'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
          'parameters': [],
          'parameterizedName': 'NewFilmReleaseAnnouncement',
          'namespace': '',
          'shortDisplayName': 'NewFilmReleaseAnnouncement',
          'longDisplayName': 'NewFilmReleaseAnnouncement',
          'name': 'NewFilmReleaseAnnouncement'
        }],
        'parameterizedName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Stream<NewFilmReleaseAnnouncement>',
        'longDisplayName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>',
        'name': 'Stream'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
        'namespace': 'io.vyne.films.announcements',
        'shortDisplayName': 'consumeFromReleases',
        'longDisplayName': 'io.vyne.films.announcements.KafkaService / consumeFromReleases',
        'name': 'KafkaService@@consumeFromReleases'
      },
      'name': 'consumeFromReleases'
    }],
    'queryOperations': [],
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'io.vyne.kafka.KafkaService',
        'parameters': [],
        'parameterizedName': 'io.vyne.kafka.KafkaService',
        'namespace': 'io.vyne.kafka',
        'shortDisplayName': 'KafkaService',
        'longDisplayName': 'io.vyne.kafka.KafkaService',
        'name': 'KafkaService'
      }, 'params': { 'connectionName': 'kafka' }
    }],
    'sourceCode': [{
      'name': 'io/vyne/films/announcements/KafkaService.taxi',
      'version': '0.0.0',
      'content': 'import io.vyne.kafka.KafkaOperation\nimport lang.taxi.Stream\nimport NewFilmReleaseAnnouncement\nnamespace io.vyne.films.announcements {\n   @io.vyne.kafka.KafkaService(connectionName = "kafka")\n         service KafkaService {\n            @io.vyne.kafka.KafkaOperation(topic = "releases" , offset = "latest")\n            operation consumeFromReleases(  ) : Stream<NewFilmReleaseAnnouncement>\n         }\n}',
      'id': 'io/vyne/films/announcements/KafkaService.taxi:0.0.0',
      'contentHash': '5c0e7d'
    }],
    'typeDoc': null,
    'lineage': null,
    'serviceKind': 'Kafka',
    'remoteOperations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
        'namespace': 'io.vyne.films.announcements',
        'shortDisplayName': 'consumeFromReleases',
        'longDisplayName': 'io.vyne.films.announcements.KafkaService / consumeFromReleases',
        'name': 'KafkaService@@consumeFromReleases'
      },
      'parameters': [],
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Stream',
        'parameters': [{
          'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
          'parameters': [],
          'parameterizedName': 'NewFilmReleaseAnnouncement',
          'namespace': '',
          'shortDisplayName': 'NewFilmReleaseAnnouncement',
          'longDisplayName': 'NewFilmReleaseAnnouncement',
          'name': 'NewFilmReleaseAnnouncement'
        }],
        'parameterizedName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Stream<NewFilmReleaseAnnouncement>',
        'longDisplayName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>',
        'name': 'Stream'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'io.vyne.kafka.KafkaOperation',
          'parameters': [],
          'parameterizedName': 'io.vyne.kafka.KafkaOperation',
          'namespace': 'io.vyne.kafka',
          'shortDisplayName': 'KafkaOperation',
          'longDisplayName': 'io.vyne.kafka.KafkaOperation',
          'name': 'KafkaOperation'
        }, 'params': { 'topic': 'releases', 'offset': 'latest' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'lang.taxi.Stream',
          'parameters': [{
            'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
            'parameters': [],
            'parameterizedName': 'NewFilmReleaseAnnouncement',
            'namespace': '',
            'shortDisplayName': 'NewFilmReleaseAnnouncement',
            'longDisplayName': 'NewFilmReleaseAnnouncement',
            'name': 'NewFilmReleaseAnnouncement'
          }],
          'parameterizedName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement>',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'Stream<NewFilmReleaseAnnouncement>',
          'longDisplayName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>',
          'name': 'Stream'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'lang.taxi.Stream',
        'parameters': [{
          'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
          'parameters': [],
          'parameterizedName': 'NewFilmReleaseAnnouncement',
          'namespace': '',
          'shortDisplayName': 'NewFilmReleaseAnnouncement',
          'longDisplayName': 'NewFilmReleaseAnnouncement',
          'name': 'NewFilmReleaseAnnouncement'
        }],
        'parameterizedName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Stream<NewFilmReleaseAnnouncement>',
        'longDisplayName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>',
        'name': 'Stream'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
        'namespace': 'io.vyne.films.announcements',
        'shortDisplayName': 'consumeFromReleases',
        'longDisplayName': 'io.vyne.films.announcements.KafkaService / consumeFromReleases',
        'name': 'KafkaService@@consumeFromReleases'
      },
      'name': 'consumeFromReleases'
    }],
    'qualifiedName': 'io.vyne.films.announcements.KafkaService',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.announcements.KafkaService',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.announcements.KafkaService',
      'namespace': 'io.vyne.films.announcements',
      'shortDisplayName': 'KafkaService',
      'longDisplayName': 'io.vyne.films.announcements.KafkaService',
      'name': 'KafkaService'
    }
  }, {
    'name': {
      'fullyQualifiedName': 'film.FilmDatabase',
      'parameters': [],
      'parameterizedName': 'film.FilmDatabase',
      'namespace': 'film',
      'shortDisplayName': 'FilmDatabase',
      'longDisplayName': 'film.FilmDatabase',
      'name': 'FilmDatabase'
    },
    'operations': [],
    'queryOperations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'film.FilmDatabase@@findManyFilm',
        'parameters': [],
        'parameterizedName': 'film.FilmDatabase@@findManyFilm',
        'namespace': 'film',
        'shortDisplayName': 'findManyFilm',
        'longDisplayName': 'film.FilmDatabase / findManyFilm',
        'name': 'FilmDatabase@@findManyFilm'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        },
        'name': 'querySpec',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'film.Film',
          'parameters': [],
          'parameterizedName': 'film.Film',
          'namespace': 'film',
          'shortDisplayName': 'Film',
          'longDisplayName': 'film.Film',
          'name': 'Film'
        }],
        'parameterizedName': 'lang.taxi.Array<film.Film>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Film[]',
        'longDisplayName': 'film.Film[]',
        'name': 'Array'
      },
      'metadata': [],
      'grammar': 'vyneQl',
      'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
      'typeDoc': null,
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'lang.taxi.Array',
          'parameters': [{
            'fullyQualifiedName': 'film.Film',
            'parameters': [],
            'parameterizedName': 'film.Film',
            'namespace': 'film',
            'shortDisplayName': 'Film',
            'longDisplayName': 'film.Film',
            'name': 'Film'
          }],
          'parameterizedName': 'lang.taxi.Array<film.Film>',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'Film[]',
          'longDisplayName': 'film.Film[]',
          'name': 'Array'
        }, 'constraints': []
      },
      'operationType': null,
      'hasFilterCapability': true,
      'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
      'returnTypeName': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'film.Film',
          'parameters': [],
          'parameterizedName': 'film.Film',
          'namespace': 'film',
          'shortDisplayName': 'Film',
          'longDisplayName': 'film.Film',
          'name': 'Film'
        }],
        'parameterizedName': 'lang.taxi.Array<film.Film>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Film[]',
        'longDisplayName': 'film.Film[]',
        'name': 'Array'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'film.FilmDatabase@@findManyFilm',
        'parameters': [],
        'parameterizedName': 'film.FilmDatabase@@findManyFilm',
        'namespace': 'film',
        'shortDisplayName': 'findManyFilm',
        'longDisplayName': 'film.FilmDatabase / findManyFilm',
        'name': 'FilmDatabase@@findManyFilm'
      },
      'name': 'findManyFilm'
    }, {
      'qualifiedName': {
        'fullyQualifiedName': 'film.FilmDatabase@@findOneFilm',
        'parameters': [],
        'parameterizedName': 'film.FilmDatabase@@findOneFilm',
        'namespace': 'film',
        'shortDisplayName': 'findOneFilm',
        'longDisplayName': 'film.FilmDatabase / findOneFilm',
        'name': 'FilmDatabase@@findOneFilm'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        },
        'name': 'querySpec',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'film.Film',
        'parameters': [],
        'parameterizedName': 'film.Film',
        'namespace': 'film',
        'shortDisplayName': 'Film',
        'longDisplayName': 'film.Film',
        'name': 'Film'
      },
      'metadata': [],
      'grammar': 'vyneQl',
      'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
      'typeDoc': null,
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'film.Film',
          'parameters': [],
          'parameterizedName': 'film.Film',
          'namespace': 'film',
          'shortDisplayName': 'Film',
          'longDisplayName': 'film.Film',
          'name': 'Film'
        }, 'constraints': []
      },
      'operationType': null,
      'hasFilterCapability': true,
      'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
      'returnTypeName': {
        'fullyQualifiedName': 'film.Film',
        'parameters': [],
        'parameterizedName': 'film.Film',
        'namespace': 'film',
        'shortDisplayName': 'Film',
        'longDisplayName': 'film.Film',
        'name': 'Film'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'film.FilmDatabase@@findOneFilm',
        'parameters': [],
        'parameterizedName': 'film.FilmDatabase@@findOneFilm',
        'namespace': 'film',
        'shortDisplayName': 'findOneFilm',
        'longDisplayName': 'film.FilmDatabase / findOneFilm',
        'name': 'FilmDatabase@@findOneFilm'
      },
      'name': 'findOneFilm'
    }],
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'io.vyne.jdbc.DatabaseService',
        'parameters': [],
        'parameterizedName': 'io.vyne.jdbc.DatabaseService',
        'namespace': 'io.vyne.jdbc',
        'shortDisplayName': 'DatabaseService',
        'longDisplayName': 'io.vyne.jdbc.DatabaseService',
        'name': 'DatabaseService'
      }, 'params': { 'connection': 'films' }
    }],
    'sourceCode': [{
      'name': 'film/FilmService.taxi',
      'version': '0.0.0',
      'content': 'import vyne.vyneQl.VyneQlQuery\nimport vyne.vyneQl.VyneQlQuery\nnamespace film {\n   @io.vyne.jdbc.DatabaseService(connection = "films")\n         service FilmDatabase {\n            vyneQl query findManyFilm(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<film.Film> with capabilities {\n               sum,\n               count,\n               avg,\n               min,\n               max,\n               filter(==,!=,in,like,>,<,>=,<=)\n            }\n            vyneQl query findOneFilm(querySpec: vyne.vyneQl.VyneQlQuery):film.Film with capabilities {\n               sum,\n               count,\n               avg,\n               min,\n               max,\n               filter(==,!=,in,like,>,<,>=,<=)\n            }\n         }\n}',
      'id': 'film/FilmService.taxi:0.0.0',
      'contentHash': 'b884ef'
    }],
    'typeDoc': null,
    'lineage': null,
    'serviceKind': 'Database',
    'remoteOperations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'film.FilmDatabase@@findManyFilm',
        'parameters': [],
        'parameterizedName': 'film.FilmDatabase@@findManyFilm',
        'namespace': 'film',
        'shortDisplayName': 'findManyFilm',
        'longDisplayName': 'film.FilmDatabase / findManyFilm',
        'name': 'FilmDatabase@@findManyFilm'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        },
        'name': 'querySpec',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'film.Film',
          'parameters': [],
          'parameterizedName': 'film.Film',
          'namespace': 'film',
          'shortDisplayName': 'Film',
          'longDisplayName': 'film.Film',
          'name': 'Film'
        }],
        'parameterizedName': 'lang.taxi.Array<film.Film>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Film[]',
        'longDisplayName': 'film.Film[]',
        'name': 'Array'
      },
      'metadata': [],
      'grammar': 'vyneQl',
      'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
      'typeDoc': null,
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'lang.taxi.Array',
          'parameters': [{
            'fullyQualifiedName': 'film.Film',
            'parameters': [],
            'parameterizedName': 'film.Film',
            'namespace': 'film',
            'shortDisplayName': 'Film',
            'longDisplayName': 'film.Film',
            'name': 'Film'
          }],
          'parameterizedName': 'lang.taxi.Array<film.Film>',
          'namespace': 'lang.taxi',
          'shortDisplayName': 'Film[]',
          'longDisplayName': 'film.Film[]',
          'name': 'Array'
        }, 'constraints': []
      },
      'operationType': null,
      'hasFilterCapability': true,
      'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
      'returnTypeName': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'film.Film',
          'parameters': [],
          'parameterizedName': 'film.Film',
          'namespace': 'film',
          'shortDisplayName': 'Film',
          'longDisplayName': 'film.Film',
          'name': 'Film'
        }],
        'parameterizedName': 'lang.taxi.Array<film.Film>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Film[]',
        'longDisplayName': 'film.Film[]',
        'name': 'Array'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'film.FilmDatabase@@findManyFilm',
        'parameters': [],
        'parameterizedName': 'film.FilmDatabase@@findManyFilm',
        'namespace': 'film',
        'shortDisplayName': 'findManyFilm',
        'longDisplayName': 'film.FilmDatabase / findManyFilm',
        'name': 'FilmDatabase@@findManyFilm'
      },
      'name': 'findManyFilm'
    }, {
      'qualifiedName': {
        'fullyQualifiedName': 'film.FilmDatabase@@findOneFilm',
        'parameters': [],
        'parameterizedName': 'film.FilmDatabase@@findOneFilm',
        'namespace': 'film',
        'shortDisplayName': 'findOneFilm',
        'longDisplayName': 'film.FilmDatabase / findOneFilm',
        'name': 'FilmDatabase@@findOneFilm'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        },
        'name': 'querySpec',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
          'parameters': [],
          'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
          'namespace': 'vyne.vyneQl',
          'shortDisplayName': 'VyneQlQuery',
          'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
          'name': 'VyneQlQuery'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'film.Film',
        'parameters': [],
        'parameterizedName': 'film.Film',
        'namespace': 'film',
        'shortDisplayName': 'Film',
        'longDisplayName': 'film.Film',
        'name': 'Film'
      },
      'metadata': [],
      'grammar': 'vyneQl',
      'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
      'typeDoc': null,
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'film.Film',
          'parameters': [],
          'parameterizedName': 'film.Film',
          'namespace': 'film',
          'shortDisplayName': 'Film',
          'longDisplayName': 'film.Film',
          'name': 'Film'
        }, 'constraints': []
      },
      'operationType': null,
      'hasFilterCapability': true,
      'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
      'returnTypeName': {
        'fullyQualifiedName': 'film.Film',
        'parameters': [],
        'parameterizedName': 'film.Film',
        'namespace': 'film',
        'shortDisplayName': 'Film',
        'longDisplayName': 'film.Film',
        'name': 'Film'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'film.FilmDatabase@@findOneFilm',
        'parameters': [],
        'parameterizedName': 'film.FilmDatabase@@findOneFilm',
        'namespace': 'film',
        'shortDisplayName': 'findOneFilm',
        'longDisplayName': 'film.FilmDatabase / findOneFilm',
        'name': 'FilmDatabase@@findOneFilm'
      },
      'name': 'findOneFilm'
    }],
    'qualifiedName': 'film.FilmDatabase',
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.FilmDatabase',
      'parameters': [],
      'parameterizedName': 'film.FilmDatabase',
      'namespace': 'film',
      'shortDisplayName': 'FilmDatabase',
      'longDisplayName': 'film.FilmDatabase',
      'name': 'FilmDatabase'
    }
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdLookupService',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdLookupService',
      'longDisplayName': 'io.vyne.films.idlookup.IdLookupService',
      'name': 'IdLookupService'
    },
    'operations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromSquashedTomatoesId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId',
        'name': 'IdLookupService@@lookupFromSquashedTomatoesId'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        },
        'name': 'id',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        },
        'params': {
          'method': 'GET',
          'url': 'http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}'
        }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
          'parameters': [],
          'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
          'namespace': 'io.vyne.films.idlookup',
          'shortDisplayName': 'IdResolution',
          'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
          'name': 'IdResolution'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromSquashedTomatoesId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId',
        'name': 'IdLookupService@@lookupFromSquashedTomatoesId'
      },
      'name': 'lookupFromSquashedTomatoesId'
    }, {
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromInternalFilmId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId',
        'name': 'IdLookupService@@lookupFromInternalFilmId'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'films.FilmId',
          'parameters': [],
          'parameterizedName': 'films.FilmId',
          'namespace': 'films',
          'shortDisplayName': 'FilmId',
          'longDisplayName': 'films.FilmId',
          'name': 'FilmId'
        },
        'name': 'id',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'films.FilmId',
          'parameters': [],
          'parameterizedName': 'films.FilmId',
          'namespace': 'films',
          'shortDisplayName': 'FilmId',
          'longDisplayName': 'films.FilmId',
          'name': 'FilmId'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        }, 'params': { 'method': 'GET', 'url': 'http://localhost:9986/ids/internal/{films.FilmId}' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
          'parameters': [],
          'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
          'namespace': 'io.vyne.films.idlookup',
          'shortDisplayName': 'IdResolution',
          'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
          'name': 'IdResolution'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromInternalFilmId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId',
        'name': 'IdLookupService@@lookupFromInternalFilmId'
      },
      'name': 'lookupFromInternalFilmId'
    }, {
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromNetflixFilmId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId',
        'name': 'IdLookupService@@lookupFromNetflixFilmId'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
          'parameters': [],
          'parameterizedName': 'demo.netflix.NetflixFilmId',
          'namespace': 'demo.netflix',
          'shortDisplayName': 'NetflixFilmId',
          'longDisplayName': 'demo.netflix.NetflixFilmId',
          'name': 'NetflixFilmId'
        },
        'name': 'id',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
          'parameters': [],
          'parameterizedName': 'demo.netflix.NetflixFilmId',
          'namespace': 'demo.netflix',
          'shortDisplayName': 'NetflixFilmId',
          'longDisplayName': 'demo.netflix.NetflixFilmId',
          'name': 'NetflixFilmId'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        }, 'params': { 'method': 'GET', 'url': 'http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
          'parameters': [],
          'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
          'namespace': 'io.vyne.films.idlookup',
          'shortDisplayName': 'IdResolution',
          'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
          'name': 'IdResolution'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromNetflixFilmId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId',
        'name': 'IdLookupService@@lookupFromNetflixFilmId'
      },
      'name': 'lookupFromNetflixFilmId'
    }],
    'queryOperations': [],
    'metadata': [],
    'sourceCode': [{
      'name': 'id-resolution-service',
      'version': '0.0.0',
      'content': 'import films.reviews.SquashedTomatoesFilmId\nimport films.FilmId\nimport demo.netflix.NetflixFilmId\nnamespace io.vyne.films.idlookup {\n   service IdLookupService {\n         @HttpOperation(method = "GET" , url = "http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}")\n         operation lookupFromSquashedTomatoesId(  id : films.reviews.SquashedTomatoesFilmId ) : IdResolution\n         @HttpOperation(method = "GET" , url = "http://localhost:9986/ids/internal/{films.FilmId}")\n         operation lookupFromInternalFilmId(  id : films.FilmId ) : IdResolution\n         @HttpOperation(method = "GET" , url = "http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}")\n         operation lookupFromNetflixFilmId(  id : demo.netflix.NetflixFilmId ) : IdResolution\n      }\n}',
      'id': 'id-resolution-service:0.0.0',
      'contentHash': '6e6080'
    }],
    'typeDoc': null,
    'lineage': null,
    'serviceKind': 'API',
    'remoteOperations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromSquashedTomatoesId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId',
        'name': 'IdLookupService@@lookupFromSquashedTomatoesId'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        },
        'name': 'id',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        },
        'params': {
          'method': 'GET',
          'url': 'http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}'
        }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
          'parameters': [],
          'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
          'namespace': 'io.vyne.films.idlookup',
          'shortDisplayName': 'IdResolution',
          'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
          'name': 'IdResolution'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromSquashedTomatoesId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId',
        'name': 'IdLookupService@@lookupFromSquashedTomatoesId'
      },
      'name': 'lookupFromSquashedTomatoesId'
    }, {
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromInternalFilmId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId',
        'name': 'IdLookupService@@lookupFromInternalFilmId'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'films.FilmId',
          'parameters': [],
          'parameterizedName': 'films.FilmId',
          'namespace': 'films',
          'shortDisplayName': 'FilmId',
          'longDisplayName': 'films.FilmId',
          'name': 'FilmId'
        },
        'name': 'id',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'films.FilmId',
          'parameters': [],
          'parameterizedName': 'films.FilmId',
          'namespace': 'films',
          'shortDisplayName': 'FilmId',
          'longDisplayName': 'films.FilmId',
          'name': 'FilmId'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        }, 'params': { 'method': 'GET', 'url': 'http://localhost:9986/ids/internal/{films.FilmId}' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
          'parameters': [],
          'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
          'namespace': 'io.vyne.films.idlookup',
          'shortDisplayName': 'IdResolution',
          'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
          'name': 'IdResolution'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromInternalFilmId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId',
        'name': 'IdLookupService@@lookupFromInternalFilmId'
      },
      'name': 'lookupFromInternalFilmId'
    }, {
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromNetflixFilmId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId',
        'name': 'IdLookupService@@lookupFromNetflixFilmId'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
          'parameters': [],
          'parameterizedName': 'demo.netflix.NetflixFilmId',
          'namespace': 'demo.netflix',
          'shortDisplayName': 'NetflixFilmId',
          'longDisplayName': 'demo.netflix.NetflixFilmId',
          'name': 'NetflixFilmId'
        },
        'name': 'id',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
          'parameters': [],
          'parameterizedName': 'demo.netflix.NetflixFilmId',
          'namespace': 'demo.netflix',
          'shortDisplayName': 'NetflixFilmId',
          'longDisplayName': 'demo.netflix.NetflixFilmId',
          'name': 'NetflixFilmId'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        }, 'params': { 'method': 'GET', 'url': 'http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
          'parameters': [],
          'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
          'namespace': 'io.vyne.films.idlookup',
          'shortDisplayName': 'IdResolution',
          'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
          'name': 'IdResolution'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'lookupFromNetflixFilmId',
        'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId',
        'name': 'IdLookupService@@lookupFromNetflixFilmId'
      },
      'name': 'lookupFromNetflixFilmId'
    }],
    'qualifiedName': 'io.vyne.films.idlookup.IdLookupService',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdLookupService',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdLookupService',
      'longDisplayName': 'io.vyne.films.idlookup.IdLookupService',
      'name': 'IdLookupService'
    }
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingMoviesProvider',
      'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider',
      'name': 'StreamingMoviesProvider'
    },
    'operations': [
      {
        'qualifiedName': {
          'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
          'parameters': [],
          'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
          'namespace': 'io.vyne.demos.films',
          'shortDisplayName': 'getStreamingProvidersForFilm',
          'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
          'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
        },
        'parameters': [{
          'type': {
            'fullyQualifiedName': 'io.vyne.demos.films.StreamingProviderRequest',
            'parameters': [],
            'parameterizedName': 'io.vyne.demos.films.StreamingProviderRequest',
            'namespace': 'io.vyne.demos.films',
            'shortDisplayName': 'StreamingProviderRequest',
            'longDisplayName': 'io.vyne.demos.films.StreamingProviderRequest',
            'name': 'StreamingProviderRequest'
          },
          'name': 'request',
          'metadata': [{
            'name': {
              'fullyQualifiedName': 'RequestBody',
              'parameters': [],
              'parameterizedName': 'RequestBody',
              'namespace': '',
              'shortDisplayName': 'RequestBody',
              'longDisplayName': 'RequestBody',
              'name': 'RequestBody'
            }, 'params': {}
          }],
          'constraints': [],
          'typeName': {
            'fullyQualifiedName': 'io.vyne.demos.films.StreamingProviderRequest',
            'parameters': [],
            'parameterizedName': 'io.vyne.demos.films.StreamingProviderRequest',
            'namespace': 'io.vyne.demos.films',
            'shortDisplayName': 'StreamingProviderRequest',
            'longDisplayName': 'io.vyne.demos.films.StreamingProviderRequest',
            'name': 'StreamingProviderRequest'
          }
        }],
        'returnType': {
          'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
          'parameters': [],
          'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
          'namespace': 'io.vyne.demos.films',
          'shortDisplayName': 'StreamingProvider',
          'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
          'name': 'StreamingProvider'
        },
        'operationType': null,
        'metadata': [{
          'name': {
            'fullyQualifiedName': 'HttpOperation',
            'parameters': [],
            'parameterizedName': 'HttpOperation',
            'namespace': '',
            'shortDisplayName': 'HttpOperation',
            'longDisplayName': 'HttpOperation',
            'name': 'HttpOperation'
          }, 'params': { 'method': 'POST', 'url': 'http://localhost:9981/films/streamingServices' }
        }],
        'contract': {
          'returnType': {
            'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
            'parameters': [],
            'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
            'namespace': 'io.vyne.demos.films',
            'shortDisplayName': 'StreamingProvider',
            'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
            'name': 'StreamingProvider'
          }, 'constraints': []
        },
        'typeDoc': null,
        'returnTypeName': {
          'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
          'parameters': [],
          'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
          'namespace': 'io.vyne.demos.films',
          'shortDisplayName': 'StreamingProvider',
          'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
          'name': 'StreamingProvider'
        },
        'memberQualifiedName': {
          'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
          'parameters': [],
          'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
          'namespace': 'io.vyne.demos.films',
          'shortDisplayName': 'getStreamingProvidersForFilm',
          'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
          'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
        },
        'name': 'getStreamingProvidersForFilm'
      }],
    'queryOperations': [],
    'metadata': [],
    'sourceCode': [{
      'name': 'films-service',
      'version': '0.0.0',
      'content': 'import films.FilmId\nnamespace io.vyne.demos.films {\n   service StreamingMoviesProvider {\n         @HttpOperation(method = "GET" , url = "http://localhost:9981/films/{films.FilmId}/streamingProviders")\n         operation getStreamingProvidersForFilm(  filmId : films.FilmId ) : StreamingProvider\n         @HttpOperation(method = "POST" , url = "http://localhost:9981/films/streamingServices")\n         operation getStreamingProvidersForFilm( @RequestBody request : StreamingProviderRequest ) : StreamingProvider\n      }\n}',
      'id': 'films-service:0.0.0',
      'contentHash': '9243b0'
    }],
    'typeDoc': null,
    'lineage': null,
    'serviceKind': 'API',
    'remoteOperations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'getStreamingProvidersForFilm',
        'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
        'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'films.FilmId',
          'parameters': [],
          'parameterizedName': 'films.FilmId',
          'namespace': 'films',
          'shortDisplayName': 'FilmId',
          'longDisplayName': 'films.FilmId',
          'name': 'FilmId'
        },
        'name': 'filmId',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'films.FilmId',
          'parameters': [],
          'parameterizedName': 'films.FilmId',
          'namespace': 'films',
          'shortDisplayName': 'FilmId',
          'longDisplayName': 'films.FilmId',
          'name': 'FilmId'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'StreamingProvider',
        'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
        'name': 'StreamingProvider'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        }, 'params': { 'method': 'GET', 'url': 'http://localhost:9981/films/{films.FilmId}/streamingProviders' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
          'parameters': [],
          'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
          'namespace': 'io.vyne.demos.films',
          'shortDisplayName': 'StreamingProvider',
          'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
          'name': 'StreamingProvider'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'StreamingProvider',
        'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
        'name': 'StreamingProvider'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'getStreamingProvidersForFilm',
        'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
        'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
      },
      'name': 'getStreamingProvidersForFilm'
    }, {
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'getStreamingProvidersForFilm',
        'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
        'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'io.vyne.demos.films.StreamingProviderRequest',
          'parameters': [],
          'parameterizedName': 'io.vyne.demos.films.StreamingProviderRequest',
          'namespace': 'io.vyne.demos.films',
          'shortDisplayName': 'StreamingProviderRequest',
          'longDisplayName': 'io.vyne.demos.films.StreamingProviderRequest',
          'name': 'StreamingProviderRequest'
        },
        'name': 'request',
        'metadata': [{
          'name': {
            'fullyQualifiedName': 'RequestBody',
            'parameters': [],
            'parameterizedName': 'RequestBody',
            'namespace': '',
            'shortDisplayName': 'RequestBody',
            'longDisplayName': 'RequestBody',
            'name': 'RequestBody'
          }, 'params': {}
        }],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'io.vyne.demos.films.StreamingProviderRequest',
          'parameters': [],
          'parameterizedName': 'io.vyne.demos.films.StreamingProviderRequest',
          'namespace': 'io.vyne.demos.films',
          'shortDisplayName': 'StreamingProviderRequest',
          'longDisplayName': 'io.vyne.demos.films.StreamingProviderRequest',
          'name': 'StreamingProviderRequest'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'StreamingProvider',
        'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
        'name': 'StreamingProvider'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        }, 'params': { 'method': 'POST', 'url': 'http://localhost:9981/films/streamingServices' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
          'parameters': [],
          'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
          'namespace': 'io.vyne.demos.films',
          'shortDisplayName': 'StreamingProvider',
          'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
          'name': 'StreamingProvider'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'StreamingProvider',
        'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
        'name': 'StreamingProvider'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'getStreamingProvidersForFilm',
        'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
        'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
      },
      'name': 'getStreamingProvidersForFilm'
    }],
    'qualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingMoviesProvider',
      'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider',
      'name': 'StreamingMoviesProvider'
    }
  }, {
    'name': {
      'fullyQualifiedName': 'io.vyne.reviews.ReviewsService',
      'parameters': [],
      'parameterizedName': 'io.vyne.reviews.ReviewsService',
      'namespace': 'io.vyne.reviews',
      'shortDisplayName': 'ReviewsService',
      'longDisplayName': 'io.vyne.reviews.ReviewsService',
      'name': 'ReviewsService'
    },
    'operations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.reviews.ReviewsService@@getReview',
        'parameters': [],
        'parameterizedName': 'io.vyne.reviews.ReviewsService@@getReview',
        'namespace': 'io.vyne.reviews',
        'shortDisplayName': 'getReview',
        'longDisplayName': 'io.vyne.reviews.ReviewsService / getReview',
        'name': 'ReviewsService@@getReview'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        },
        'name': 'filmId',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
        'parameters': [],
        'parameterizedName': 'io.vyne.reviews.FilmReview',
        'namespace': 'io.vyne.reviews',
        'shortDisplayName': 'FilmReview',
        'longDisplayName': 'io.vyne.reviews.FilmReview',
        'name': 'FilmReview'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        }, 'params': { 'method': 'GET', 'url': 'http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
          'parameters': [],
          'parameterizedName': 'io.vyne.reviews.FilmReview',
          'namespace': 'io.vyne.reviews',
          'shortDisplayName': 'FilmReview',
          'longDisplayName': 'io.vyne.reviews.FilmReview',
          'name': 'FilmReview'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
        'parameters': [],
        'parameterizedName': 'io.vyne.reviews.FilmReview',
        'namespace': 'io.vyne.reviews',
        'shortDisplayName': 'FilmReview',
        'longDisplayName': 'io.vyne.reviews.FilmReview',
        'name': 'FilmReview'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.reviews.ReviewsService@@getReview',
        'parameters': [],
        'parameterizedName': 'io.vyne.reviews.ReviewsService@@getReview',
        'namespace': 'io.vyne.reviews',
        'shortDisplayName': 'getReview',
        'longDisplayName': 'io.vyne.reviews.ReviewsService / getReview',
        'name': 'ReviewsService@@getReview'
      },
      'name': 'getReview'
    }],
    'queryOperations': [],
    'metadata': [],
    'sourceCode': [{
      'name': 'squashed-tomatoes',
      'version': '0.0.0',
      'content': 'import films.reviews.SquashedTomatoesFilmId\nnamespace io.vyne.reviews {\n   service ReviewsService {\n         @HttpOperation(method = "GET" , url = "http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}")\n         operation getReview(  filmId : films.reviews.SquashedTomatoesFilmId ) : FilmReview\n      }\n}',
      'id': 'squashed-tomatoes:0.0.0',
      'contentHash': '0945a0'
    }],
    'typeDoc': null,
    'lineage': null,
    'serviceKind': 'API',
    'remoteOperations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.reviews.ReviewsService@@getReview',
        'parameters': [],
        'parameterizedName': 'io.vyne.reviews.ReviewsService@@getReview',
        'namespace': 'io.vyne.reviews',
        'shortDisplayName': 'getReview',
        'longDisplayName': 'io.vyne.reviews.ReviewsService / getReview',
        'name': 'ReviewsService@@getReview'
      },
      'parameters': [{
        'type': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        },
        'name': 'filmId',
        'metadata': [],
        'constraints': [],
        'typeName': {
          'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
          'parameters': [],
          'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
          'namespace': 'films.reviews',
          'shortDisplayName': 'SquashedTomatoesFilmId',
          'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
          'name': 'SquashedTomatoesFilmId'
        }
      }],
      'returnType': {
        'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
        'parameters': [],
        'parameterizedName': 'io.vyne.reviews.FilmReview',
        'namespace': 'io.vyne.reviews',
        'shortDisplayName': 'FilmReview',
        'longDisplayName': 'io.vyne.reviews.FilmReview',
        'name': 'FilmReview'
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'parameterizedName': 'HttpOperation',
          'namespace': '',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'name': 'HttpOperation'
        }, 'params': { 'method': 'GET', 'url': 'http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}' }
      }],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
          'parameters': [],
          'parameterizedName': 'io.vyne.reviews.FilmReview',
          'namespace': 'io.vyne.reviews',
          'shortDisplayName': 'FilmReview',
          'longDisplayName': 'io.vyne.reviews.FilmReview',
          'name': 'FilmReview'
        }, 'constraints': []
      },
      'typeDoc': null,
      'returnTypeName': {
        'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
        'parameters': [],
        'parameterizedName': 'io.vyne.reviews.FilmReview',
        'namespace': 'io.vyne.reviews',
        'shortDisplayName': 'FilmReview',
        'longDisplayName': 'io.vyne.reviews.FilmReview',
        'name': 'FilmReview'
      },
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.reviews.ReviewsService@@getReview',
        'parameters': [],
        'parameterizedName': 'io.vyne.reviews.ReviewsService@@getReview',
        'namespace': 'io.vyne.reviews',
        'shortDisplayName': 'getReview',
        'longDisplayName': 'io.vyne.reviews.ReviewsService / getReview',
        'name': 'ReviewsService@@getReview'
      },
      'name': 'getReview'
    }],
    'qualifiedName': 'io.vyne.reviews.ReviewsService',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.reviews.ReviewsService',
      'parameters': [],
      'parameterizedName': 'io.vyne.reviews.ReviewsService',
      'namespace': 'io.vyne.reviews',
      'shortDisplayName': 'ReviewsService',
      'longDisplayName': 'io.vyne.reviews.ReviewsService',
      'name': 'ReviewsService'
    }
  }],
  'dynamicMetadata': [{
    'fullyQualifiedName': 'HttpOperation',
    'parameters': [],
    'parameterizedName': 'HttpOperation',
    'namespace': '',
    'shortDisplayName': 'HttpOperation',
    'longDisplayName': 'HttpOperation',
    'name': 'HttpOperation'
  }, {
    'fullyQualifiedName': 'Id',
    'parameters': [],
    'parameterizedName': 'Id',
    'namespace': '',
    'shortDisplayName': 'Id',
    'longDisplayName': 'Id',
    'name': 'Id'
  }, {
    'fullyQualifiedName': 'lang.taxi.formats.ProtobufMessage',
    'parameters': [],
    'parameterizedName': 'lang.taxi.formats.ProtobufMessage',
    'namespace': 'lang.taxi.formats',
    'shortDisplayName': 'ProtobufMessage',
    'longDisplayName': 'lang.taxi.formats.ProtobufMessage',
    'name': 'ProtobufMessage'
  }, {
    'fullyQualifiedName': 'lang.taxi.formats.ProtobufField',
    'parameters': [],
    'parameterizedName': 'lang.taxi.formats.ProtobufField',
    'namespace': 'lang.taxi.formats',
    'shortDisplayName': 'ProtobufField',
    'longDisplayName': 'lang.taxi.formats.ProtobufField',
    'name': 'ProtobufField'
  }],
  'metadataTypes': [],
  'operations': [{
    'qualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
      'namespace': 'io.vyne.films.announcements',
      'shortDisplayName': 'consumeFromReleases',
      'longDisplayName': 'io.vyne.films.announcements.KafkaService / consumeFromReleases',
      'name': 'KafkaService@@consumeFromReleases'
    },
    'parameters': [],
    'returnType': {
      'fullyQualifiedName': 'lang.taxi.Stream',
      'parameters': [{
        'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
        'parameters': [],
        'parameterizedName': 'NewFilmReleaseAnnouncement',
        'namespace': '',
        'shortDisplayName': 'NewFilmReleaseAnnouncement',
        'longDisplayName': 'NewFilmReleaseAnnouncement',
        'name': 'NewFilmReleaseAnnouncement'
      }],
      'parameterizedName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement>',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Stream<NewFilmReleaseAnnouncement>',
      'longDisplayName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>',
      'name': 'Stream'
    },
    'operationType': null,
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'io.vyne.kafka.KafkaOperation',
        'parameters': [],
        'parameterizedName': 'io.vyne.kafka.KafkaOperation',
        'namespace': 'io.vyne.kafka',
        'shortDisplayName': 'KafkaOperation',
        'longDisplayName': 'io.vyne.kafka.KafkaOperation',
        'name': 'KafkaOperation'
      }, 'params': { 'topic': 'releases', 'offset': 'latest' }
    }],
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Stream',
        'parameters': [{
          'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
          'parameters': [],
          'parameterizedName': 'NewFilmReleaseAnnouncement',
          'namespace': '',
          'shortDisplayName': 'NewFilmReleaseAnnouncement',
          'longDisplayName': 'NewFilmReleaseAnnouncement',
          'name': 'NewFilmReleaseAnnouncement'
        }],
        'parameterizedName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Stream<NewFilmReleaseAnnouncement>',
        'longDisplayName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>',
        'name': 'Stream'
      }, 'constraints': []
    },
    'typeDoc': null,
    'returnTypeName': {
      'fullyQualifiedName': 'lang.taxi.Stream',
      'parameters': [{
        'fullyQualifiedName': 'NewFilmReleaseAnnouncement',
        'parameters': [],
        'parameterizedName': 'NewFilmReleaseAnnouncement',
        'namespace': '',
        'shortDisplayName': 'NewFilmReleaseAnnouncement',
        'longDisplayName': 'NewFilmReleaseAnnouncement',
        'name': 'NewFilmReleaseAnnouncement'
      }],
      'parameterizedName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement>',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Stream<NewFilmReleaseAnnouncement>',
      'longDisplayName': 'lang.taxi.Stream<NewFilmReleaseAnnouncement><NewFilmReleaseAnnouncement>',
      'name': 'Stream'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.announcements.KafkaService@@consumeFromReleases',
      'namespace': 'io.vyne.films.announcements',
      'shortDisplayName': 'consumeFromReleases',
      'longDisplayName': 'io.vyne.films.announcements.KafkaService / consumeFromReleases',
      'name': 'KafkaService@@consumeFromReleases'
    },
    'name': 'consumeFromReleases'
  }, {
    'qualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'lookupFromSquashedTomatoesId',
      'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId',
      'name': 'IdLookupService@@lookupFromSquashedTomatoesId'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
        'parameters': [],
        'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
        'namespace': 'films.reviews',
        'shortDisplayName': 'SquashedTomatoesFilmId',
        'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
        'name': 'SquashedTomatoesFilmId'
      },
      'name': 'id',
      'metadata': [],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
        'parameters': [],
        'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
        'namespace': 'films.reviews',
        'shortDisplayName': 'SquashedTomatoesFilmId',
        'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
        'name': 'SquashedTomatoesFilmId'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdResolution',
      'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
      'name': 'IdResolution'
    },
    'operationType': null,
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'HttpOperation',
        'parameters': [],
        'parameterizedName': 'HttpOperation',
        'namespace': '',
        'shortDisplayName': 'HttpOperation',
        'longDisplayName': 'HttpOperation',
        'name': 'HttpOperation'
      },
      'params': { 'method': 'GET', 'url': 'http://localhost:9986/ids/squashed/{films.reviews.SquashedTomatoesFilmId}' }
    }],
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      }, 'constraints': []
    },
    'typeDoc': null,
    'returnTypeName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdResolution',
      'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
      'name': 'IdResolution'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromSquashedTomatoesId',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'lookupFromSquashedTomatoesId',
      'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromSquashedTomatoesId',
      'name': 'IdLookupService@@lookupFromSquashedTomatoesId'
    },
    'name': 'lookupFromSquashedTomatoesId'
  }, {
    'qualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'lookupFromInternalFilmId',
      'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId',
      'name': 'IdLookupService@@lookupFromInternalFilmId'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'films.FilmId',
        'parameters': [],
        'parameterizedName': 'films.FilmId',
        'namespace': 'films',
        'shortDisplayName': 'FilmId',
        'longDisplayName': 'films.FilmId',
        'name': 'FilmId'
      },
      'name': 'id',
      'metadata': [],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'films.FilmId',
        'parameters': [],
        'parameterizedName': 'films.FilmId',
        'namespace': 'films',
        'shortDisplayName': 'FilmId',
        'longDisplayName': 'films.FilmId',
        'name': 'FilmId'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdResolution',
      'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
      'name': 'IdResolution'
    },
    'operationType': null,
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'HttpOperation',
        'parameters': [],
        'parameterizedName': 'HttpOperation',
        'namespace': '',
        'shortDisplayName': 'HttpOperation',
        'longDisplayName': 'HttpOperation',
        'name': 'HttpOperation'
      }, 'params': { 'method': 'GET', 'url': 'http://localhost:9986/ids/internal/{films.FilmId}' }
    }],
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      }, 'constraints': []
    },
    'typeDoc': null,
    'returnTypeName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdResolution',
      'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
      'name': 'IdResolution'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromInternalFilmId',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'lookupFromInternalFilmId',
      'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromInternalFilmId',
      'name': 'IdLookupService@@lookupFromInternalFilmId'
    },
    'name': 'lookupFromInternalFilmId'
  }, {
    'qualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'lookupFromNetflixFilmId',
      'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId',
      'name': 'IdLookupService@@lookupFromNetflixFilmId'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
        'parameters': [],
        'parameterizedName': 'demo.netflix.NetflixFilmId',
        'namespace': 'demo.netflix',
        'shortDisplayName': 'NetflixFilmId',
        'longDisplayName': 'demo.netflix.NetflixFilmId',
        'name': 'NetflixFilmId'
      },
      'name': 'id',
      'metadata': [],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'demo.netflix.NetflixFilmId',
        'parameters': [],
        'parameterizedName': 'demo.netflix.NetflixFilmId',
        'namespace': 'demo.netflix',
        'shortDisplayName': 'NetflixFilmId',
        'longDisplayName': 'demo.netflix.NetflixFilmId',
        'name': 'NetflixFilmId'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdResolution',
      'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
      'name': 'IdResolution'
    },
    'operationType': null,
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'HttpOperation',
        'parameters': [],
        'parameterizedName': 'HttpOperation',
        'namespace': '',
        'shortDisplayName': 'HttpOperation',
        'longDisplayName': 'HttpOperation',
        'name': 'HttpOperation'
      }, 'params': { 'method': 'GET', 'url': 'http://localhost:9986/ids/netflix/{demo.netflix.NetflixFilmId}' }
    }],
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
        'parameters': [],
        'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
        'namespace': 'io.vyne.films.idlookup',
        'shortDisplayName': 'IdResolution',
        'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
        'name': 'IdResolution'
      }, 'constraints': []
    },
    'typeDoc': null,
    'returnTypeName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdResolution',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdResolution',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'IdResolution',
      'longDisplayName': 'io.vyne.films.idlookup.IdResolution',
      'name': 'IdResolution'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
      'parameters': [],
      'parameterizedName': 'io.vyne.films.idlookup.IdLookupService@@lookupFromNetflixFilmId',
      'namespace': 'io.vyne.films.idlookup',
      'shortDisplayName': 'lookupFromNetflixFilmId',
      'longDisplayName': 'io.vyne.films.idlookup.IdLookupService / lookupFromNetflixFilmId',
      'name': 'IdLookupService@@lookupFromNetflixFilmId'
    },
    'name': 'lookupFromNetflixFilmId'
  }, {
    'qualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'getStreamingProvidersForFilm',
      'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
      'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'films.FilmId',
        'parameters': [],
        'parameterizedName': 'films.FilmId',
        'namespace': 'films',
        'shortDisplayName': 'FilmId',
        'longDisplayName': 'films.FilmId',
        'name': 'FilmId'
      },
      'name': 'filmId',
      'metadata': [],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'films.FilmId',
        'parameters': [],
        'parameterizedName': 'films.FilmId',
        'namespace': 'films',
        'shortDisplayName': 'FilmId',
        'longDisplayName': 'films.FilmId',
        'name': 'FilmId'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingProvider',
      'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
      'name': 'StreamingProvider'
    },
    'operationType': null,
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'HttpOperation',
        'parameters': [],
        'parameterizedName': 'HttpOperation',
        'namespace': '',
        'shortDisplayName': 'HttpOperation',
        'longDisplayName': 'HttpOperation',
        'name': 'HttpOperation'
      }, 'params': { 'method': 'GET', 'url': 'http://localhost:9981/films/{films.FilmId}/streamingProviders' }
    }],
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'StreamingProvider',
        'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
        'name': 'StreamingProvider'
      }, 'constraints': []
    },
    'typeDoc': null,
    'returnTypeName': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingProvider',
      'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
      'name': 'StreamingProvider'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'getStreamingProvidersForFilm',
      'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
      'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
    },
    'name': 'getStreamingProvidersForFilm'
  }, {
    'qualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'getStreamingProvidersForFilm',
      'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
      'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingProviderRequest',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingProviderRequest',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'StreamingProviderRequest',
        'longDisplayName': 'io.vyne.demos.films.StreamingProviderRequest',
        'name': 'StreamingProviderRequest'
      },
      'name': 'request',
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'RequestBody',
          'parameters': [],
          'parameterizedName': 'RequestBody',
          'namespace': '',
          'shortDisplayName': 'RequestBody',
          'longDisplayName': 'RequestBody',
          'name': 'RequestBody'
        }, 'params': {}
      }],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingProviderRequest',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingProviderRequest',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'StreamingProviderRequest',
        'longDisplayName': 'io.vyne.demos.films.StreamingProviderRequest',
        'name': 'StreamingProviderRequest'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingProvider',
      'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
      'name': 'StreamingProvider'
    },
    'operationType': null,
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'HttpOperation',
        'parameters': [],
        'parameterizedName': 'HttpOperation',
        'namespace': '',
        'shortDisplayName': 'HttpOperation',
        'longDisplayName': 'HttpOperation',
        'name': 'HttpOperation'
      }, 'params': { 'method': 'POST', 'url': 'http://localhost:9981/films/streamingServices' }
    }],
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
        'parameters': [],
        'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
        'namespace': 'io.vyne.demos.films',
        'shortDisplayName': 'StreamingProvider',
        'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
        'name': 'StreamingProvider'
      }, 'constraints': []
    },
    'typeDoc': null,
    'returnTypeName': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingProvider',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingProvider',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'StreamingProvider',
      'longDisplayName': 'io.vyne.demos.films.StreamingProvider',
      'name': 'StreamingProvider'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
      'parameters': [],
      'parameterizedName': 'io.vyne.demos.films.StreamingMoviesProvider@@getStreamingProvidersForFilm',
      'namespace': 'io.vyne.demos.films',
      'shortDisplayName': 'getStreamingProvidersForFilm',
      'longDisplayName': 'io.vyne.demos.films.StreamingMoviesProvider / getStreamingProvidersForFilm',
      'name': 'StreamingMoviesProvider@@getStreamingProvidersForFilm'
    },
    'name': 'getStreamingProvidersForFilm'
  }, {
    'qualifiedName': {
      'fullyQualifiedName': 'io.vyne.reviews.ReviewsService@@getReview',
      'parameters': [],
      'parameterizedName': 'io.vyne.reviews.ReviewsService@@getReview',
      'namespace': 'io.vyne.reviews',
      'shortDisplayName': 'getReview',
      'longDisplayName': 'io.vyne.reviews.ReviewsService / getReview',
      'name': 'ReviewsService@@getReview'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
        'parameters': [],
        'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
        'namespace': 'films.reviews',
        'shortDisplayName': 'SquashedTomatoesFilmId',
        'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
        'name': 'SquashedTomatoesFilmId'
      },
      'name': 'filmId',
      'metadata': [],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'films.reviews.SquashedTomatoesFilmId',
        'parameters': [],
        'parameterizedName': 'films.reviews.SquashedTomatoesFilmId',
        'namespace': 'films.reviews',
        'shortDisplayName': 'SquashedTomatoesFilmId',
        'longDisplayName': 'films.reviews.SquashedTomatoesFilmId',
        'name': 'SquashedTomatoesFilmId'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
      'parameters': [],
      'parameterizedName': 'io.vyne.reviews.FilmReview',
      'namespace': 'io.vyne.reviews',
      'shortDisplayName': 'FilmReview',
      'longDisplayName': 'io.vyne.reviews.FilmReview',
      'name': 'FilmReview'
    },
    'operationType': null,
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'HttpOperation',
        'parameters': [],
        'parameterizedName': 'HttpOperation',
        'namespace': '',
        'shortDisplayName': 'HttpOperation',
        'longDisplayName': 'HttpOperation',
        'name': 'HttpOperation'
      }, 'params': { 'method': 'GET', 'url': 'http://localhost:9985/reviews/{films.reviews.SquashedTomatoesFilmId}' }
    }],
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
        'parameters': [],
        'parameterizedName': 'io.vyne.reviews.FilmReview',
        'namespace': 'io.vyne.reviews',
        'shortDisplayName': 'FilmReview',
        'longDisplayName': 'io.vyne.reviews.FilmReview',
        'name': 'FilmReview'
      }, 'constraints': []
    },
    'typeDoc': null,
    'returnTypeName': {
      'fullyQualifiedName': 'io.vyne.reviews.FilmReview',
      'parameters': [],
      'parameterizedName': 'io.vyne.reviews.FilmReview',
      'namespace': 'io.vyne.reviews',
      'shortDisplayName': 'FilmReview',
      'longDisplayName': 'io.vyne.reviews.FilmReview',
      'name': 'FilmReview'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.reviews.ReviewsService@@getReview',
      'parameters': [],
      'parameterizedName': 'io.vyne.reviews.ReviewsService@@getReview',
      'namespace': 'io.vyne.reviews',
      'shortDisplayName': 'getReview',
      'longDisplayName': 'io.vyne.reviews.ReviewsService / getReview',
      'name': 'ReviewsService@@getReview'
    },
    'name': 'getReview'
  }],
  'queryOperations': [{
    'qualifiedName': {
      'fullyQualifiedName': 'actor.ActorService@@findManyActor',
      'parameters': [],
      'parameterizedName': 'actor.ActorService@@findManyActor',
      'namespace': 'actor',
      'shortDisplayName': 'findManyActor',
      'longDisplayName': 'actor.ActorService / findManyActor',
      'name': 'ActorService@@findManyActor'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
        'parameters': [],
        'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
        'namespace': 'vyne.vyneQl',
        'shortDisplayName': 'VyneQlQuery',
        'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
        'name': 'VyneQlQuery'
      },
      'name': 'querySpec',
      'metadata': [],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
        'parameters': [],
        'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
        'namespace': 'vyne.vyneQl',
        'shortDisplayName': 'VyneQlQuery',
        'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
        'name': 'VyneQlQuery'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'lang.taxi.Array',
      'parameters': [{
        'fullyQualifiedName': 'actor.Actor',
        'parameters': [],
        'parameterizedName': 'actor.Actor',
        'namespace': 'actor',
        'shortDisplayName': 'Actor',
        'longDisplayName': 'actor.Actor',
        'name': 'Actor'
      }],
      'parameterizedName': 'lang.taxi.Array<actor.Actor>',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Actor[]',
      'longDisplayName': 'actor.Actor[]',
      'name': 'Array'
    },
    'metadata': [],
    'grammar': 'vyneQl',
    'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
    'typeDoc': null,
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'actor.Actor',
          'parameters': [],
          'parameterizedName': 'actor.Actor',
          'namespace': 'actor',
          'shortDisplayName': 'Actor',
          'longDisplayName': 'actor.Actor',
          'name': 'Actor'
        }],
        'parameterizedName': 'lang.taxi.Array<actor.Actor>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Actor[]',
        'longDisplayName': 'actor.Actor[]',
        'name': 'Array'
      }, 'constraints': []
    },
    'operationType': null,
    'hasFilterCapability': true,
    'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
    'returnTypeName': {
      'fullyQualifiedName': 'lang.taxi.Array',
      'parameters': [{
        'fullyQualifiedName': 'actor.Actor',
        'parameters': [],
        'parameterizedName': 'actor.Actor',
        'namespace': 'actor',
        'shortDisplayName': 'Actor',
        'longDisplayName': 'actor.Actor',
        'name': 'Actor'
      }],
      'parameterizedName': 'lang.taxi.Array<actor.Actor>',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Actor[]',
      'longDisplayName': 'actor.Actor[]',
      'name': 'Array'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'actor.ActorService@@findManyActor',
      'parameters': [],
      'parameterizedName': 'actor.ActorService@@findManyActor',
      'namespace': 'actor',
      'shortDisplayName': 'findManyActor',
      'longDisplayName': 'actor.ActorService / findManyActor',
      'name': 'ActorService@@findManyActor'
    },
    'name': 'findManyActor'
  }, {
    'qualifiedName': {
      'fullyQualifiedName': 'actor.ActorService@@findOneActor',
      'parameters': [],
      'parameterizedName': 'actor.ActorService@@findOneActor',
      'namespace': 'actor',
      'shortDisplayName': 'findOneActor',
      'longDisplayName': 'actor.ActorService / findOneActor',
      'name': 'ActorService@@findOneActor'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
        'parameters': [],
        'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
        'namespace': 'vyne.vyneQl',
        'shortDisplayName': 'VyneQlQuery',
        'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
        'name': 'VyneQlQuery'
      },
      'name': 'querySpec',
      'metadata': [],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
        'parameters': [],
        'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
        'namespace': 'vyne.vyneQl',
        'shortDisplayName': 'VyneQlQuery',
        'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
        'name': 'VyneQlQuery'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'actor.Actor',
      'parameters': [],
      'parameterizedName': 'actor.Actor',
      'namespace': 'actor',
      'shortDisplayName': 'Actor',
      'longDisplayName': 'actor.Actor',
      'name': 'Actor'
    },
    'metadata': [],
    'grammar': 'vyneQl',
    'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
    'typeDoc': null,
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'actor.Actor',
        'parameters': [],
        'parameterizedName': 'actor.Actor',
        'namespace': 'actor',
        'shortDisplayName': 'Actor',
        'longDisplayName': 'actor.Actor',
        'name': 'Actor'
      }, 'constraints': []
    },
    'operationType': null,
    'hasFilterCapability': true,
    'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
    'returnTypeName': {
      'fullyQualifiedName': 'actor.Actor',
      'parameters': [],
      'parameterizedName': 'actor.Actor',
      'namespace': 'actor',
      'shortDisplayName': 'Actor',
      'longDisplayName': 'actor.Actor',
      'name': 'Actor'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'actor.ActorService@@findOneActor',
      'parameters': [],
      'parameterizedName': 'actor.ActorService@@findOneActor',
      'namespace': 'actor',
      'shortDisplayName': 'findOneActor',
      'longDisplayName': 'actor.ActorService / findOneActor',
      'name': 'ActorService@@findOneActor'
    },
    'name': 'findOneActor'
  }, {
    'qualifiedName': {
      'fullyQualifiedName': 'film.FilmDatabase@@findManyFilm',
      'parameters': [],
      'parameterizedName': 'film.FilmDatabase@@findManyFilm',
      'namespace': 'film',
      'shortDisplayName': 'findManyFilm',
      'longDisplayName': 'film.FilmDatabase / findManyFilm',
      'name': 'FilmDatabase@@findManyFilm'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
        'parameters': [],
        'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
        'namespace': 'vyne.vyneQl',
        'shortDisplayName': 'VyneQlQuery',
        'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
        'name': 'VyneQlQuery'
      },
      'name': 'querySpec',
      'metadata': [],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
        'parameters': [],
        'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
        'namespace': 'vyne.vyneQl',
        'shortDisplayName': 'VyneQlQuery',
        'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
        'name': 'VyneQlQuery'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'lang.taxi.Array',
      'parameters': [{
        'fullyQualifiedName': 'film.Film',
        'parameters': [],
        'parameterizedName': 'film.Film',
        'namespace': 'film',
        'shortDisplayName': 'Film',
        'longDisplayName': 'film.Film',
        'name': 'Film'
      }],
      'parameterizedName': 'lang.taxi.Array<film.Film>',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Film[]',
      'longDisplayName': 'film.Film[]',
      'name': 'Array'
    },
    'metadata': [],
    'grammar': 'vyneQl',
    'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
    'typeDoc': null,
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [{
          'fullyQualifiedName': 'film.Film',
          'parameters': [],
          'parameterizedName': 'film.Film',
          'namespace': 'film',
          'shortDisplayName': 'Film',
          'longDisplayName': 'film.Film',
          'name': 'Film'
        }],
        'parameterizedName': 'lang.taxi.Array<film.Film>',
        'namespace': 'lang.taxi',
        'shortDisplayName': 'Film[]',
        'longDisplayName': 'film.Film[]',
        'name': 'Array'
      }, 'constraints': []
    },
    'operationType': null,
    'hasFilterCapability': true,
    'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
    'returnTypeName': {
      'fullyQualifiedName': 'lang.taxi.Array',
      'parameters': [{
        'fullyQualifiedName': 'film.Film',
        'parameters': [],
        'parameterizedName': 'film.Film',
        'namespace': 'film',
        'shortDisplayName': 'Film',
        'longDisplayName': 'film.Film',
        'name': 'Film'
      }],
      'parameterizedName': 'lang.taxi.Array<film.Film>',
      'namespace': 'lang.taxi',
      'shortDisplayName': 'Film[]',
      'longDisplayName': 'film.Film[]',
      'name': 'Array'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.FilmDatabase@@findManyFilm',
      'parameters': [],
      'parameterizedName': 'film.FilmDatabase@@findManyFilm',
      'namespace': 'film',
      'shortDisplayName': 'findManyFilm',
      'longDisplayName': 'film.FilmDatabase / findManyFilm',
      'name': 'FilmDatabase@@findManyFilm'
    },
    'name': 'findManyFilm'
  }, {
    'qualifiedName': {
      'fullyQualifiedName': 'film.FilmDatabase@@findOneFilm',
      'parameters': [],
      'parameterizedName': 'film.FilmDatabase@@findOneFilm',
      'namespace': 'film',
      'shortDisplayName': 'findOneFilm',
      'longDisplayName': 'film.FilmDatabase / findOneFilm',
      'name': 'FilmDatabase@@findOneFilm'
    },
    'parameters': [{
      'type': {
        'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
        'parameters': [],
        'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
        'namespace': 'vyne.vyneQl',
        'shortDisplayName': 'VyneQlQuery',
        'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
        'name': 'VyneQlQuery'
      },
      'name': 'querySpec',
      'metadata': [],
      'constraints': [],
      'typeName': {
        'fullyQualifiedName': 'vyne.vyneQl.VyneQlQuery',
        'parameters': [],
        'parameterizedName': 'vyne.vyneQl.VyneQlQuery',
        'namespace': 'vyne.vyneQl',
        'shortDisplayName': 'VyneQlQuery',
        'longDisplayName': 'vyne.vyneQl.VyneQlQuery',
        'name': 'VyneQlQuery'
      }
    }],
    'returnType': {
      'fullyQualifiedName': 'film.Film',
      'parameters': [],
      'parameterizedName': 'film.Film',
      'namespace': 'film',
      'shortDisplayName': 'Film',
      'longDisplayName': 'film.Film',
      'name': 'Film'
    },
    'metadata': [],
    'grammar': 'vyneQl',
    'capabilities': ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', { 'supportedOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'] }],
    'typeDoc': null,
    'contract': {
      'returnType': {
        'fullyQualifiedName': 'film.Film',
        'parameters': [],
        'parameterizedName': 'film.Film',
        'namespace': 'film',
        'shortDisplayName': 'Film',
        'longDisplayName': 'film.Film',
        'name': 'Film'
      }, 'constraints': []
    },
    'operationType': null,
    'hasFilterCapability': true,
    'supportedFilterOperations': ['EQUAL', 'NOT_EQUAL', 'IN', 'LIKE', 'GREATER_THAN', 'LESS_THAN', 'GREATER_THAN_OR_EQUAL_TO', 'LESS_THAN_OR_EQUAL_TO'],
    'returnTypeName': {
      'fullyQualifiedName': 'film.Film',
      'parameters': [],
      'parameterizedName': 'film.Film',
      'namespace': 'film',
      'shortDisplayName': 'Film',
      'longDisplayName': 'film.Film',
      'name': 'Film'
    },
    'memberQualifiedName': {
      'fullyQualifiedName': 'film.FilmDatabase@@findOneFilm',
      'parameters': [],
      'parameterizedName': 'film.FilmDatabase@@findOneFilm',
      'namespace': 'film',
      'shortDisplayName': 'findOneFilm',
      'longDisplayName': 'film.FilmDatabase / findOneFilm',
      'name': 'FilmDatabase@@findOneFilm'
    },
    'name': 'findOneFilm'
  }]
} as any as Schema