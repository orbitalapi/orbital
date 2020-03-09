export const testSchema = {
  'services': [],
  'operations': [],
  'types': [
    {
      'name': {
        'fullyQualifiedName': 'demo.CurrencyUnit',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.CurrencyUnit',
        'name': 'CurrencyUnit'
      },
      'attributes': {},
      'modifiers': [
        'ENUM'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [
        'POINTS',
        'GBP'
      ],
      'sources': [
        {
          'origin': 'customer-service:0.1.0',
          'language': 'Taxi',
          'content': 'enum CurrencyUnit {\n      POINTS,\n      GBP\n   }'
        }
      ],
      'typeParameters': [],
      'typeDoc': '',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'demo.CurrencyUnit',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.CurrencyUnit',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.CurrencyUnit',
        'name': 'CurrencyUnit'
      },
      'primitive': false
    },
    {
      'name': {
        'fullyQualifiedName': 'demo.Customer',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.Customer',
        'name': 'Customer'
      },
      'attributes': {
        'email': {
          'type': {
            'name': {
              'fullyQualifiedName': 'demo.CustomerEmailAddress',
              'parameters': [],
              'namespace': 'demo',
              'parameterizedName': 'demo.CustomerEmailAddress',
              'name': 'CustomerEmailAddress'
            },
            'collection': false,
            'fullyQualifiedName': 'demo.CustomerEmailAddress'
          },
          'modifiers': [],
          'accessor': null,
          'readCondition': null,
          'constraints': []
        },
        'id': {
          'type': {
            'name': {
              'fullyQualifiedName': 'demo.CustomerId',
              'parameters': [],
              'namespace': 'demo',
              'parameterizedName': 'demo.CustomerId',
              'name': 'CustomerId'
            },
            'collection': false,
            'fullyQualifiedName': 'demo.CustomerId'
          },
          'modifiers': [],
          'accessor': null,
          'readCondition': null,
          'constraints': []
        },
        'name': {
          'type': {
            'name': {
              'fullyQualifiedName': 'demo.CustomerName',
              'parameters': [],
              'namespace': 'demo',
              'parameterizedName': 'demo.CustomerName',
              'name': 'CustomerName'
            },
            'collection': false,
            'fullyQualifiedName': 'demo.CustomerName'
          },
          'modifiers': [],
          'accessor': null,
          'readCondition': null,
          'constraints': []
        },
        'postcode': {
          'type': {
            'name': {
              'fullyQualifiedName': 'demo.Postcode',
              'parameters': [],
              'namespace': 'demo',
              'parameterizedName': 'demo.Postcode',
              'name': 'Postcode'
            },
            'collection': false,
            'fullyQualifiedName': 'demo.Postcode'
          },
          'modifiers': [],
          'accessor': null,
          'readCondition': null,
          'constraints': []
        },
        'balance': {
          'type': {
            'name': {
              'fullyQualifiedName': 'demo.RewardsAccountBalance',
              'parameters': [],
              'namespace': 'demo',
              'parameterizedName': 'demo.RewardsAccountBalance',
              'name': 'Postcode'
            },
            'collection': false,
            'fullyQualifiedName': 'demo.RewardsAccountBalance'
          },
          'modifiers': [],
          'accessor': null,
          'readCondition': null,
          'constraints': []
        }
      },
      'modifiers': [],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'customer-service:0.1.0',
          'language': 'Taxi',
          'content': 'type Customer {\n      email : CustomerEmailAddress\n      id : CustomerId\n      name : CustomerName\n      postcode : Postcode\n   }'
        }
      ],
      'typeParameters': [],
      'typeDoc': '',
      'isTypeAlias': false,
      'isScalar': false,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': false,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'demo.Customer',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.Customer',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.Customer',
        'name': 'Customer'
      },
      'primitive': false
    },
    {
      'name': {
        'fullyQualifiedName': 'demo.CustomerEmailAddress',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerEmailAddress',
        'name': 'CustomerEmailAddress'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': {
        'fullyQualifiedName': 'lang.taxi.String',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.String',
        'name': 'String'
      },
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'customer-service:0.1.0',
          'language': 'Taxi',
          'content': 'type alias CustomerEmailAddress as String'
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A collection of characters.',
      'isTypeAlias': true,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': true,
      'closed': false,
      'fullyQualifiedName': 'demo.CustomerEmailAddress',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.CustomerEmailAddress',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerEmailAddress',
        'name': 'CustomerEmailAddress'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'demo.CustomerId',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerId',
        'name': 'CustomerId'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': {
        'fullyQualifiedName': 'lang.taxi.Int',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Int',
        'name': 'Int'
      },
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'customer-service:0.1.0',
          'language': 'Taxi',
          'content': 'type alias CustomerId as Int'
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A signed integer - ie. a whole number (positive or negative), with no decimal places',
      'isTypeAlias': true,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': true,
      'closed': false,
      'fullyQualifiedName': 'demo.CustomerId',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.CustomerId',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerId',
        'name': 'CustomerId'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'demo.CustomerName',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerName',
        'name': 'CustomerName'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': {
        'fullyQualifiedName': 'lang.taxi.String',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.String',
        'name': 'String'
      },
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'customer-service:0.1.0',
          'language': 'Taxi',
          'content': 'type alias CustomerName as String'
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A collection of characters.',
      'isTypeAlias': true,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': true,
      'closed': false,
      'fullyQualifiedName': 'demo.CustomerName',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.CustomerName',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerName',
        'name': 'CustomerName'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'demo.Postcode',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.Postcode',
        'name': 'Postcode'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': {
        'fullyQualifiedName': 'lang.taxi.String',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.String',
        'name': 'String'
      },
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'customer-service:0.1.0',
          'language': 'Taxi',
          'content': 'type alias Postcode as String'
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A collection of characters.',
      'isTypeAlias': true,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': true,
      'closed': false,
      'fullyQualifiedName': 'demo.Postcode',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.Postcode',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.Postcode',
        'name': 'Postcode'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'demo.RewardsAccountBalance',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.RewardsAccountBalance',
        'name': 'RewardsAccountBalance'
      },
      'attributes': {
        'balance': {
          'type': {
            'name': {
              'fullyQualifiedName': 'demo.RewardsBalance',
              'parameters': [],
              'namespace': 'demo',
              'parameterizedName': 'demo.RewardsBalance',
              'name': 'RewardsBalance'
            },
            'collection': false,
            'fullyQualifiedName': 'demo.RewardsBalance'
          },
          'modifiers': [],
          'accessor': null,
          'readCondition': null,
          'constraints': []
        },
        'cardNumber': {
          'type': {
            'name': {
              'fullyQualifiedName': 'demo.RewardsCardNumber',
              'parameters': [],
              'namespace': 'demo',
              'parameterizedName': 'demo.RewardsCardNumber',
              'name': 'RewardsCardNumber'
            },
            'collection': false,
            'fullyQualifiedName': 'demo.RewardsCardNumber'
          },
          'modifiers': [],
          'accessor': null,
          'readCondition': null,
          'constraints': []
        },
        'currencyUnit': {
          'type': {
            'name': {
              'fullyQualifiedName': 'demo.CurrencyUnit',
              'parameters': [],
              'namespace': 'demo',
              'parameterizedName': 'demo.CurrencyUnit',
              'name': 'CurrencyUnit'
            },
            'collection': false,
            'fullyQualifiedName': 'demo.CurrencyUnit'
          },
          'modifiers': [],
          'accessor': null,
          'readCondition': null,
          'constraints': []
        }
      },
      'modifiers': [],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'customer-service:0.1.0',
          'language': 'Taxi',
          'content': 'type RewardsAccountBalance {\n      balance : RewardsBalance\n      cardNumber : RewardsCardNumber\n      currencyUnit : CurrencyUnit\n   }'
        }
      ],
      'typeParameters': [],
      'typeDoc': '',
      'isTypeAlias': false,
      'isScalar': false,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': false,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'demo.RewardsAccountBalance',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.RewardsAccountBalance',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.RewardsAccountBalance',
        'name': 'RewardsAccountBalance'
      },
      'primitive': false
    },
    {
      'name': {
        'fullyQualifiedName': 'demo.RewardsBalance',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.RewardsBalance',
        'name': 'RewardsBalance'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': {
        'fullyQualifiedName': 'lang.taxi.Decimal',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Decimal',
        'name': 'Decimal'
      },
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'customer-service:0.1.0',
          'language': 'Taxi',
          'content': 'type alias RewardsBalance as Decimal'
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A signed decimal number - ie., a whole number with decimal places.',
      'isTypeAlias': true,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': true,
      'closed': false,
      'fullyQualifiedName': 'demo.RewardsBalance',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.RewardsBalance',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.RewardsBalance',
        'name': 'RewardsBalance'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'demo.RewardsCardNumber',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.RewardsCardNumber',
        'name': 'RewardsCardNumber'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': {
        'fullyQualifiedName': 'lang.taxi.String',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.String',
        'name': 'String'
      },
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'customer-service:0.1.0',
          'language': 'Taxi',
          'content': 'type alias RewardsCardNumber as String'
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A collection of characters.',
      'isTypeAlias': true,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': true,
      'closed': false,
      'fullyQualifiedName': 'demo.RewardsCardNumber',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.RewardsCardNumber',
        'parameters': [],
        'namespace': 'demo',
        'parameterizedName': 'demo.RewardsCardNumber',
        'name': 'RewardsCardNumber'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Any',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Any',
        'name': 'Any'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'Can be anything.  Try to avoid using \'Any\' as it\'s not descriptive - favour using a strongly typed approach instead',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Any',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Any',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Any',
        'name': 'Any'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Array',
        'name': 'Array'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A collection of things',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Array',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Array',
        'name': 'Array'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Boolean',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Boolean',
        'name': 'Boolean'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'Represents a value which is either `true` or `false`.',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Boolean',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Boolean',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Boolean',
        'name': 'Boolean'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Date',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Date',
        'name': 'Date'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A date, without a time or timezone.',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Date',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Date',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Date',
        'name': 'Date'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.DateTime',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.DateTime',
        'name': 'DateTime'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A date and time, without a timezone.  Generally, favour using Instant which represents a point-in-time, as it has a timezone attached',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.DateTime',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.DateTime',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.DateTime',
        'name': 'DateTime'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Decimal',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Decimal',
        'name': 'Decimal'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A signed decimal number - ie., a whole number with decimal places.',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Decimal',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Decimal',
        'name': 'Decimal'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Double',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Double',
        'name': 'Double'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'Represents a double-precision 64-bit IEEE 754 floating point number.',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Double',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Double',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Double',
        'name': 'Double'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Instant',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Instant',
        'name': 'Instant'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A point in time, with date, time and timezone.  Follows ISO standard convention of YYYY-mm-yyThh:dd:ssZ',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Instant',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Instant',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Instant',
        'name': 'Instant'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Int',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Int',
        'name': 'Int'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A signed integer - ie. a whole number (positive or negative), with no decimal places',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Int',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Int',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Int',
        'name': 'Int'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.String',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.String',
        'name': 'String'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'A collection of characters.',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.String',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.String',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.String',
        'name': 'String'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Time',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Time',
        'name': 'Time'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'Time only, excluding the date part',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Time',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Time',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Time',
        'name': 'Time'
      },
      'primitive': true
    },
    {
      'name': {
        'fullyQualifiedName': 'lang.taxi.Void',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Void',
        'name': 'Void'
      },
      'attributes': {},
      'modifiers': [
        'PRIMITIVE'
      ],
      'aliasForType': null,
      'inherits': [],
      'enumValues': [],
      'sources': [
        {
          'origin': 'Native',
          'language': 'Taxi',
          'content': ''
        }
      ],
      'typeParameters': [],
      'typeDoc': 'Nothing.  Represents the return value of operations that don\'t return anything.',
      'isTypeAlias': false,
      'isScalar': true,
      'isParameterType': false,
      'isClosed': false,
      'inheritanceGraph': [],
      'parameterType': false,
      'scalar': true,
      'typeAlias': false,
      'closed': false,
      'fullyQualifiedName': 'lang.taxi.Void',
      'memberQualifiedName': {
        'fullyQualifiedName': 'lang.taxi.Void',
        'parameters': [],
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Void',
        'name': 'Void'
      },
      'primitive': true
    }
  ],
  'typeCache': {}
};
