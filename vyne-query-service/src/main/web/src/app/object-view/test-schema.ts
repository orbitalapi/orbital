/* tslint:disable:max-line-length */
export const testSchema = {
  'services': [{
    'name': {
      'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService',
      'parameters': [],
      'name': 'CustomerService',
      'shortDisplayName': 'CustomerService',
      'longDisplayName': 'io.vyne.demos.rewards.CustomerService',
      'namespace': 'io.vyne.demos.rewards',
      'parameterizedName': 'io.vyne.demos.rewards.CustomerService'
    },
    'operations': [{
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'parameters': [],
        'name': 'CustomerService@@getCustomerByEmail',
        'shortDisplayName': 'CustomerService@@getCustomerByEmail',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail'
      },
      'parameters': [{
        'type': {
          'name': {
            'fullyQualifiedName': 'demo.CustomerEmailAddress',
            'parameters': [],
            'name': 'CustomerEmailAddress',
            'shortDisplayName': 'CustomerEmailAddress',
            'longDisplayName': 'demo.CustomerEmailAddress',
            'namespace': 'demo',
            'parameterizedName': 'demo.CustomerEmailAddress'
          },
          'attributes': {},
          'modifiers': [],
          'metadata': [],
          'aliasForType': {
            'fullyQualifiedName': 'lang.taxi.String',
            'parameters': [],
            'name': 'String',
            'shortDisplayName': 'String',
            'longDisplayName': 'lang.taxi.String',
            'namespace': 'lang.taxi',
            'parameterizedName': 'lang.taxi.String'
          },
          'inheritsFrom': [],
          'enumValues': [],
          'sources': [{
            'name': 'customer-service',
            'version': '0.0.0',
            'content': 'type alias CustomerEmailAddress as String',
            'id': 'customer-service:0.0.0'
          }],
          'typeParameters': [],
          'typeDoc': '',
          'isTypeAlias': true,
          'format': null,
          'hasFormat': false,
          'isParameterType': false,
          'isClosed': false,
          'isPrimitive': false,
          'fullyQualifiedName': 'demo.CustomerEmailAddress',
          'memberQualifiedName': {
            'fullyQualifiedName': 'demo.CustomerEmailAddress',
            'parameters': [],
            'name': 'CustomerEmailAddress',
            'shortDisplayName': 'CustomerEmailAddress',
            'longDisplayName': 'demo.CustomerEmailAddress',
            'namespace': 'demo',
            'parameterizedName': 'demo.CustomerEmailAddress'
          },
          'isCollection': false,
          'underlyingTypeParameters': [],
          'collectionType': null,
          'isScalar': true
        }, 'name': null, 'metadata': [], 'constraints': []
      }],
      'returnType': {
        'name': {
          'fullyQualifiedName': 'demo.Customer',
          'parameters': [],
          'name': 'Customer',
          'shortDisplayName': 'Customer',
          'longDisplayName': 'demo.Customer',
          'namespace': 'demo',
          'parameterizedName': 'demo.Customer'
        },
        'attributes': {
          'email': {
            'type': {
              'fullyQualifiedName': 'demo.CustomerEmailAddress',
              'parameters': [],
              'name': 'CustomerEmailAddress',
              'shortDisplayName': 'CustomerEmailAddress',
              'longDisplayName': 'demo.CustomerEmailAddress',
              'namespace': 'demo',
              'parameterizedName': 'demo.CustomerEmailAddress'
            }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
          },
          'id': {
            'type': {
              'fullyQualifiedName': 'demo.CustomerId',
              'parameters': [],
              'name': 'CustomerId',
              'shortDisplayName': 'CustomerId',
              'longDisplayName': 'demo.CustomerId',
              'namespace': 'demo',
              'parameterizedName': 'demo.CustomerId'
            }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
          },
          'name': {
            'type': {
              'fullyQualifiedName': 'demo.CustomerName',
              'parameters': [],
              'name': 'CustomerName',
              'shortDisplayName': 'CustomerName',
              'longDisplayName': 'demo.CustomerName',
              'namespace': 'demo',
              'parameterizedName': 'demo.CustomerName'
            }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
          },
          'postcode': {
            'type': {
              'fullyQualifiedName': 'demo.Postcode',
              'parameters': [],
              'name': 'Postcode',
              'shortDisplayName': 'Postcode',
              'longDisplayName': 'demo.Postcode',
              'namespace': 'demo',
              'parameterizedName': 'demo.Postcode'
            }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
          }
        },
        'modifiers': [],
        'metadata': [],
        'aliasForType': null,
        'inheritsFrom': [],
        'enumValues': [],
        'sources': [{
          'name': 'customer-service',
          'version': '0.0.0',
          'content': 'type Customer {\n      email : CustomerEmailAddress\n      id : CustomerId\n      name : CustomerName\n      postcode : Postcode\n   }',
          'id': 'customer-service:0.0.0'
        }],
        'typeParameters': [],
        'typeDoc': '',
        'isTypeAlias': false,
        'format': null,
        'hasFormat': false,
        'isParameterType': false,
        'isClosed': false,
        'isPrimitive': false,
        'fullyQualifiedName': 'demo.Customer',
        'memberQualifiedName': {
          'fullyQualifiedName': 'demo.Customer',
          'parameters': [],
          'name': 'Customer',
          'shortDisplayName': 'Customer',
          'longDisplayName': 'demo.Customer',
          'namespace': 'demo',
          'parameterizedName': 'demo.Customer'
        },
        'isCollection': false,
        'underlyingTypeParameters': [],
        'collectionType': null,
        'isScalar': false
      },
      'operationType': null,
      'metadata': [{
        'name': {
          'fullyQualifiedName': 'HttpOperation',
          'parameters': [],
          'name': 'HttpOperation',
          'shortDisplayName': 'HttpOperation',
          'longDisplayName': 'HttpOperation',
          'namespace': '',
          'parameterizedName': 'HttpOperation'
        }, 'params': {'method': 'GET', 'url': '/customers/email/{demo.CustomerEmailAddress}'}
      }],
      'contract': {
        'returnType': {
          'name': {
            'fullyQualifiedName': 'demo.Customer',
            'parameters': [],
            'name': 'Customer',
            'shortDisplayName': 'Customer',
            'longDisplayName': 'demo.Customer',
            'namespace': 'demo',
            'parameterizedName': 'demo.Customer'
          },
          'attributes': {
            'email': {
              'type': {
                'fullyQualifiedName': 'demo.CustomerEmailAddress',
                'parameters': [],
                'name': 'CustomerEmailAddress',
                'shortDisplayName': 'CustomerEmailAddress',
                'longDisplayName': 'demo.CustomerEmailAddress',
                'namespace': 'demo',
                'parameterizedName': 'demo.CustomerEmailAddress'
              }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
            },
            'id': {
              'type': {
                'fullyQualifiedName': 'demo.CustomerId',
                'parameters': [],
                'name': 'CustomerId',
                'shortDisplayName': 'CustomerId',
                'longDisplayName': 'demo.CustomerId',
                'namespace': 'demo',
                'parameterizedName': 'demo.CustomerId'
              }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
            },
            'name': {
              'type': {
                'fullyQualifiedName': 'demo.CustomerName',
                'parameters': [],
                'name': 'CustomerName',
                'shortDisplayName': 'CustomerName',
                'longDisplayName': 'demo.CustomerName',
                'namespace': 'demo',
                'parameterizedName': 'demo.CustomerName'
              }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
            },
            'postcode': {
              'type': {
                'fullyQualifiedName': 'demo.Postcode',
                'parameters': [],
                'name': 'Postcode',
                'shortDisplayName': 'Postcode',
                'longDisplayName': 'demo.Postcode',
                'namespace': 'demo',
                'parameterizedName': 'demo.Postcode'
              }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
            }
          },
          'modifiers': [],
          'metadata': [],
          'aliasForType': null,
          'inheritsFrom': [],
          'enumValues': [],
          'sources': [{
            'name': 'customer-service',
            'version': '0.0.0',
            'content': 'type Customer {\n      email : CustomerEmailAddress\n      id : CustomerId\n      name : CustomerName\n      postcode : Postcode\n   }',
            'id': 'customer-service:0.0.0'
          }],
          'typeParameters': [],
          'typeDoc': '',
          'isTypeAlias': false,
          'format': null,
          'hasFormat': false,
          'isParameterType': false,
          'isClosed': false,
          'isPrimitive': false,
          'fullyQualifiedName': 'demo.Customer',
          'memberQualifiedName': {
            'fullyQualifiedName': 'demo.Customer',
            'parameters': [],
            'name': 'Customer',
            'shortDisplayName': 'Customer',
            'longDisplayName': 'demo.Customer',
            'namespace': 'demo',
            'parameterizedName': 'demo.Customer'
          },
          'isCollection': false,
          'underlyingTypeParameters': [],
          'collectionType': null,
          'isScalar': false
        }, 'constraints': []
      },
      'sources': [{
        'name': 'customer-service',
        'version': '0.0.0',
        'content': '@HttpOperation(method = "GET" , url = "/customers/email/{demo.CustomerEmailAddress}")\n      operation getCustomerByEmail(  demo.CustomerEmailAddress ) : demo.Customer',
        'id': 'customer-service:0.0.0'
      }],
      'name': 'getCustomerByEmail',
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'parameters': [],
        'name': 'CustomerService@@getCustomerByEmail',
        'shortDisplayName': 'CustomerService@@getCustomerByEmail',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail'
      }
    }],
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'ServiceDiscoveryClient',
        'parameters': [],
        'name': 'ServiceDiscoveryClient',
        'shortDisplayName': 'ServiceDiscoveryClient',
        'longDisplayName': 'ServiceDiscoveryClient',
        'namespace': '',
        'parameterizedName': 'ServiceDiscoveryClient'
      }, 'params': {'serviceName': 'customer-service'}
    }],
    'sourceCode': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': '@ServiceDiscoveryClient(serviceName = "customer-service")\n   service CustomerService {\n      @HttpOperation(method = "GET" , url = "/customers/email/{demo.CustomerEmailAddress}")\n      operation getCustomerByEmail(  demo.CustomerEmailAddress ) : demo.Customer\n   }',
      'id': 'customer-service:0.0.0'
    }],
    'qualifiedName': 'io.vyne.demos.rewards.CustomerService',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService',
      'parameters': [],
      'name': 'CustomerService',
      'shortDisplayName': 'CustomerService',
      'longDisplayName': 'io.vyne.demos.rewards.CustomerService',
      'namespace': 'io.vyne.demos.rewards',
      'parameterizedName': 'io.vyne.demos.rewards.CustomerService'
    }
  }],
  'types': [{
    'name': {
      'fullyQualifiedName': 'lang.taxi.Boolean',
      'parameters': [],
      'name': 'Boolean',
      'shortDisplayName': 'Boolean',
      'longDisplayName': 'lang.taxi.Boolean',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Boolean'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'Represents a value which is either `true` or `false`.',
    'isTypeAlias': false,
    'format': null,
    'basePrimitiveTypeName' : 'lang.taxi.Boolean',
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Boolean',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Boolean',
      'parameters': [],
      'name': 'Boolean',
      'shortDisplayName': 'Boolean',
      'longDisplayName': 'lang.taxi.Boolean',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Boolean'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.String'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'A collection of characters.',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.String',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.String'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'name': 'Int',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Int'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'A signed integer - ie. a whole number (positive or negative), with no decimal places',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Int',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'name': 'Int',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Int'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'name': 'Decimal',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Decimal'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'A signed decimal number - ie., a whole number with decimal places.',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Decimal',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'name': 'Decimal',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Decimal'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Date',
      'parameters': [],
      'name': 'Date',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Date'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'A date, without a time or timezone.',
    'isTypeAlias': false,
    'format': 'yyyy-MM-dd',
    'hasFormat': true,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Date',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Date',
      'parameters': [],
      'name': 'Date',
      'shortDisplayName': 'Date',
      'longDisplayName': 'lang.taxi.Date',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Date'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Time',
      'parameters': [],
      'name': 'Time',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Time'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'Time only, excluding the date part',
    'isTypeAlias': false,
    'format': 'HH:mm:ss',
    'hasFormat': true,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Time',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Time',
      'parameters': [],
      'name': 'Time',
      'shortDisplayName': 'Time',
      'longDisplayName': 'lang.taxi.Time',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Time'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.DateTime',
      'parameters': [],
      'name': 'DateTime',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.DateTime'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'A date and time, without a timezone.  Generally, favour using Instant which represents a point-in-time, as it has a timezone attached',
    'isTypeAlias': false,
    'format': 'yyyy-MM-dd\'T\'HH:mm:ss.SSS',
    'hasFormat': true,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.DateTime',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.DateTime',
      'parameters': [],
      'name': 'DateTime',
      'shortDisplayName': 'DateTime',
      'longDisplayName': 'lang.taxi.DateTime',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.DateTime'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'name': 'Instant',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Instant'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'A point in time, with date, time and timezone.  Follows ISO standard convention of yyyy-MM-dd\'T\'HH:mm:ss.SSSZ',
    'isTypeAlias': false,
    'format': 'yyyy-MM-dd\'T\'HH:mm:ss[.SSS]X',
    'hasFormat': true,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Instant',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Instant',
      'parameters': [],
      'name': 'Instant',
      'shortDisplayName': 'Instant',
      'longDisplayName': 'lang.taxi.Instant',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Instant'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Array',
      'parameters': [],
      'name': 'Array',
      'shortDisplayName': 'Array',
      'longDisplayName': 'lang.taxi.Array',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Array'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'A collection of things',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Array',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Array',
      'parameters': [],
      'name': 'Array',
      'shortDisplayName': 'Array',
      'longDisplayName': 'lang.taxi.Array',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Array'
    },
    'isCollection': true,
    'underlyingTypeParameters': [],
    'collectionType': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'name': 'Any',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Any'
    },
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'name': 'Any',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Any'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'Can be anything.  Try to avoid using \'Any\' as it\'s not descriptive - favour using a strongly typed approach instead',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Any',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Any',
      'parameters': [],
      'name': 'Any',
      'shortDisplayName': 'Any',
      'longDisplayName': 'lang.taxi.Any',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Any'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Double',
      'parameters': [],
      'name': 'Double',
      'shortDisplayName': 'Double',
      'longDisplayName': 'lang.taxi.Double',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Double'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'Represents a double-precision 64-bit IEEE 754 floating point number.',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Double',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Double',
      'parameters': [],
      'name': 'Double',
      'shortDisplayName': 'Double',
      'longDisplayName': 'lang.taxi.Double',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Double'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'lang.taxi.Void',
      'parameters': [],
      'name': 'Void',
      'shortDisplayName': 'Void',
      'longDisplayName': 'lang.taxi.Void',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Void'
    },
    'attributes': {},
    'modifiers': ['PRIMITIVE'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{'name': '<unknown>', 'version': '0.0.0', 'content': 'Native', 'id': '<unknown>:0.0.0'}],
    'typeParameters': [],
    'typeDoc': 'Nothing.  Represents the return value of operations that don\'t return anything.',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': true,
    'fullyQualifiedName': 'lang.taxi.Void',
    'memberQualifiedName': {
      'fullyQualifiedName': 'lang.taxi.Void',
      'parameters': [],
      'name': 'Void',
      'shortDisplayName': 'Void',
      'longDisplayName': 'lang.taxi.Void',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Void'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'demo.Customer',
      'parameters': [],
      'name': 'Customer',
      'shortDisplayName': 'Customer',
      'longDisplayName': 'demo.Customer',
      'namespace': 'demo',
      'parameterizedName': 'demo.Customer'
    },
    'attributes': {
      'email': {
        'type': {
          'fullyQualifiedName': 'demo.CustomerEmailAddress',
          'parameters': [],
          'name': 'CustomerEmailAddress',
          'shortDisplayName': 'CustomerEmailAddress',
          'longDisplayName': 'demo.CustomerEmailAddress',
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerEmailAddress'
        }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
      },
      'id': {
        'type': {
          'fullyQualifiedName': 'demo.CustomerId',
          'parameters': [],
          'name': 'CustomerId',
          'shortDisplayName': 'CustomerId',
          'longDisplayName': 'demo.CustomerId',
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerId'
        }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
      },
      'name': {
        'type': {
          'fullyQualifiedName': 'demo.CustomerName',
          'parameters': [],
          'name': 'CustomerName',
          'shortDisplayName': 'CustomerName',
          'longDisplayName': 'demo.CustomerName',
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerName'
        }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
      },
      'postcode': {
        'type': {
          'fullyQualifiedName': 'demo.Postcode',
          'parameters': [],
          'name': 'Postcode',
          'shortDisplayName': 'Postcode',
          'longDisplayName': 'demo.Postcode',
          'namespace': 'demo',
          'parameterizedName': 'demo.Postcode'
        }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
      }
    },
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'type Customer {\n      email : CustomerEmailAddress\n      id : CustomerId\n      name : CustomerName\n      postcode : Postcode\n   }',
      'id': 'customer-service:0.0.0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.Customer',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.Customer',
      'parameters': [],
      'name': 'Customer',
      'shortDisplayName': 'Customer',
      'longDisplayName': 'demo.Customer',
      'namespace': 'demo',
      'parameterizedName': 'demo.Customer'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'demo.CustomerEmailAddress',
      'parameters': [],
      'name': 'CustomerEmailAddress',
      'shortDisplayName': 'CustomerEmailAddress',
      'longDisplayName': 'demo.CustomerEmailAddress',
      'namespace': 'demo',
      'parameterizedName': 'demo.CustomerEmailAddress'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.String'
    },
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'type alias CustomerEmailAddress as String',
      'id': 'customer-service:0.0.0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': true,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.CustomerEmailAddress',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.CustomerEmailAddress',
      'parameters': [],
      'name': 'CustomerEmailAddress',
      'shortDisplayName': 'CustomerEmailAddress',
      'longDisplayName': 'demo.CustomerEmailAddress',
      'namespace': 'demo',
      'parameterizedName': 'demo.CustomerEmailAddress'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'demo.CustomerId',
      'parameters': [],
      'name': 'CustomerId',
      'shortDisplayName': 'CustomerId',
      'longDisplayName': 'demo.CustomerId',
      'namespace': 'demo',
      'parameterizedName': 'demo.CustomerId'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': {
      'fullyQualifiedName': 'lang.taxi.Int',
      'parameters': [],
      'name': 'Int',
      'shortDisplayName': 'Int',
      'longDisplayName': 'lang.taxi.Int',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Int'
    },
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'type alias CustomerId as Int',
      'id': 'customer-service:0.0.0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': true,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.CustomerId',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.CustomerId',
      'parameters': [],
      'name': 'CustomerId',
      'shortDisplayName': 'CustomerId',
      'longDisplayName': 'demo.CustomerId',
      'namespace': 'demo',
      'parameterizedName': 'demo.CustomerId'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'demo.CustomerName',
      'parameters': [],
      'name': 'CustomerName',
      'shortDisplayName': 'CustomerName',
      'longDisplayName': 'demo.CustomerName',
      'namespace': 'demo',
      'parameterizedName': 'demo.CustomerName'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.String'
    },
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'type alias CustomerName as String',
      'id': 'customer-service:0.0.0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': true,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.CustomerName',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.CustomerName',
      'parameters': [],
      'name': 'CustomerName',
      'shortDisplayName': 'CustomerName',
      'longDisplayName': 'demo.CustomerName',
      'namespace': 'demo',
      'parameterizedName': 'demo.CustomerName'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'demo.Postcode',
      'parameters': [],
      'name': 'Postcode',
      'shortDisplayName': 'Postcode',
      'longDisplayName': 'demo.Postcode',
      'namespace': 'demo',
      'parameterizedName': 'demo.Postcode'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.String'
    },
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'type alias Postcode as String',
      'id': 'customer-service:0.0.0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': true,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.Postcode',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.Postcode',
      'parameters': [],
      'name': 'Postcode',
      'shortDisplayName': 'Postcode',
      'longDisplayName': 'demo.Postcode',
      'namespace': 'demo',
      'parameterizedName': 'demo.Postcode'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'demo.CurrencyUnit',
      'parameters': [],
      'name': 'CurrencyUnit',
      'shortDisplayName': 'CurrencyUnit',
      'longDisplayName': 'demo.CurrencyUnit',
      'namespace': 'demo',
      'parameterizedName': 'demo.CurrencyUnit'
    },
    'attributes': {},
    'modifiers': ['ENUM'],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': ['POINTS', 'GBP'],
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'enum CurrencyUnit {\n      POINTS,\n      GBP\n   }',
      'id': 'customer-service:0.0.0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.CurrencyUnit',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.CurrencyUnit',
      'parameters': [],
      'name': 'CurrencyUnit',
      'shortDisplayName': 'CurrencyUnit',
      'longDisplayName': 'demo.CurrencyUnit',
      'namespace': 'demo',
      'parameterizedName': 'demo.CurrencyUnit'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'demo.RewardsAccountBalance',
      'parameters': [],
      'name': 'RewardsAccountBalance',
      'shortDisplayName': 'RewardsAccountBalance',
      'longDisplayName': 'demo.RewardsAccountBalance',
      'namespace': 'demo',
      'parameterizedName': 'demo.RewardsAccountBalance'
    },
    'attributes': {
      'balance': {
        'type': {
          'fullyQualifiedName': 'demo.RewardsBalance',
          'parameters': [],
          'name': 'RewardsBalance',
          'shortDisplayName': 'RewardsBalance',
          'longDisplayName': 'demo.RewardsBalance',
          'namespace': 'demo',
          'parameterizedName': 'demo.RewardsBalance'
        }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
      },
      'cardNumber': {
        'type': {
          'fullyQualifiedName': 'demo.RewardsCardNumber',
          'parameters': [],
          'name': 'RewardsCardNumber',
          'shortDisplayName': 'RewardsCardNumber',
          'longDisplayName': 'demo.RewardsCardNumber',
          'namespace': 'demo',
          'parameterizedName': 'demo.RewardsCardNumber'
        }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
      },
      'currencyUnit': {
        'type': {
          'fullyQualifiedName': 'demo.CurrencyUnit',
          'parameters': [],
          'name': 'CurrencyUnit',
          'shortDisplayName': 'CurrencyUnit',
          'longDisplayName': 'demo.CurrencyUnit',
          'namespace': 'demo',
          'parameterizedName': 'demo.CurrencyUnit'
        }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
      }
    },
    'modifiers': [],
    'metadata': [],
    'aliasForType': null,
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'type RewardsAccountBalance {\n      balance : RewardsBalance\n      cardNumber : RewardsCardNumber\n      currencyUnit : CurrencyUnit\n   }',
      'id': 'customer-service:0.0.0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': false,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.RewardsAccountBalance',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.RewardsAccountBalance',
      'parameters': [],
      'name': 'RewardsAccountBalance',
      'shortDisplayName': 'RewardsAccountBalance',
      'longDisplayName': 'demo.RewardsAccountBalance',
      'namespace': 'demo',
      'parameterizedName': 'demo.RewardsAccountBalance'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': false
  }, {
    'name': {
      'fullyQualifiedName': 'demo.RewardsBalance',
      'parameters': [],
      'name': 'RewardsBalance',
      'shortDisplayName': 'RewardsBalance',
      'longDisplayName': 'demo.RewardsBalance',
      'namespace': 'demo',
      'parameterizedName': 'demo.RewardsBalance'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': {
      'fullyQualifiedName': 'lang.taxi.Decimal',
      'parameters': [],
      'name': 'Decimal',
      'shortDisplayName': 'Decimal',
      'longDisplayName': 'lang.taxi.Decimal',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.Decimal'
    },
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'type alias RewardsBalance as Decimal',
      'id': 'customer-service:0.0.0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': true,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.RewardsBalance',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.RewardsBalance',
      'parameters': [],
      'name': 'RewardsBalance',
      'shortDisplayName': 'RewardsBalance',
      'longDisplayName': 'demo.RewardsBalance',
      'namespace': 'demo',
      'parameterizedName': 'demo.RewardsBalance'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }, {
    'name': {
      'fullyQualifiedName': 'demo.RewardsCardNumber',
      'parameters': [],
      'name': 'RewardsCardNumber',
      'shortDisplayName': 'RewardsCardNumber',
      'longDisplayName': 'demo.RewardsCardNumber',
      'namespace': 'demo',
      'parameterizedName': 'demo.RewardsCardNumber'
    },
    'attributes': {},
    'modifiers': [],
    'metadata': [],
    'aliasForType': {
      'fullyQualifiedName': 'lang.taxi.String',
      'parameters': [],
      'name': 'String',
      'shortDisplayName': 'String',
      'longDisplayName': 'lang.taxi.String',
      'namespace': 'lang.taxi',
      'parameterizedName': 'lang.taxi.String'
    },
    'inheritsFrom': [],
    'enumValues': [],
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'type alias RewardsCardNumber as String',
      'id': 'customer-service:0.0.0'
    }],
    'typeParameters': [],
    'typeDoc': '',
    'isTypeAlias': true,
    'format': null,
    'hasFormat': false,
    'isParameterType': false,
    'isClosed': false,
    'isPrimitive': false,
    'fullyQualifiedName': 'demo.RewardsCardNumber',
    'memberQualifiedName': {
      'fullyQualifiedName': 'demo.RewardsCardNumber',
      'parameters': [],
      'name': 'RewardsCardNumber',
      'shortDisplayName': 'RewardsCardNumber',
      'longDisplayName': 'demo.RewardsCardNumber',
      'namespace': 'demo',
      'parameterizedName': 'demo.RewardsCardNumber'
    },
    'isCollection': false,
    'underlyingTypeParameters': [],
    'collectionType': null,
    'isScalar': true
  }],
  'operations': [{
    'qualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
      'parameters': [],
      'name': 'CustomerService@@getCustomerByEmail',
      'shortDisplayName': 'CustomerService@@getCustomerByEmail',
      'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
      'namespace': 'io.vyne.demos.rewards',
      'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail'
    },
    'parameters': [{
      'type': {
        'name': {
          'fullyQualifiedName': 'demo.CustomerEmailAddress',
          'parameters': [],
          'name': 'CustomerEmailAddress',
          'shortDisplayName': 'CustomerEmailAddress',
          'longDisplayName': 'demo.CustomerEmailAddress',
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerEmailAddress'
        },
        'attributes': {},
        'modifiers': [],
        'metadata': [],
        'aliasForType': {
          'fullyQualifiedName': 'lang.taxi.String',
          'parameters': [],
          'name': 'String',
          'shortDisplayName': 'String',
          'longDisplayName': 'lang.taxi.String',
          'namespace': 'lang.taxi',
          'parameterizedName': 'lang.taxi.String'
        },
        'inheritsFrom': [],
        'enumValues': [],
        'sources': [{
          'name': 'customer-service',
          'version': '0.0.0',
          'content': 'type alias CustomerEmailAddress as String',
          'id': 'customer-service:0.0.0'
        }],
        'typeParameters': [],
        'typeDoc': '',
        'isTypeAlias': true,
        'format': null,
        'hasFormat': false,
        'isParameterType': false,
        'isClosed': false,
        'isPrimitive': false,
        'fullyQualifiedName': 'demo.CustomerEmailAddress',
        'memberQualifiedName': {
          'fullyQualifiedName': 'demo.CustomerEmailAddress',
          'parameters': [],
          'name': 'CustomerEmailAddress',
          'shortDisplayName': 'CustomerEmailAddress',
          'longDisplayName': 'demo.CustomerEmailAddress',
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerEmailAddress'
        },
        'isCollection': false,
        'underlyingTypeParameters': [],
        'collectionType': null,
        'isScalar': true
      }, 'name': null, 'metadata': [], 'constraints': []
    }],
    'returnType': {
      'name': {
        'fullyQualifiedName': 'demo.Customer',
        'parameters': [],
        'name': 'Customer',
        'shortDisplayName': 'Customer',
        'longDisplayName': 'demo.Customer',
        'namespace': 'demo',
        'parameterizedName': 'demo.Customer'
      },
      'attributes': {
        'email': {
          'type': {
            'fullyQualifiedName': 'demo.CustomerEmailAddress',
            'parameters': [],
            'name': 'CustomerEmailAddress',
            'shortDisplayName': 'CustomerEmailAddress',
            'longDisplayName': 'demo.CustomerEmailAddress',
            'namespace': 'demo',
            'parameterizedName': 'demo.CustomerEmailAddress'
          }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
        },
        'id': {
          'type': {
            'fullyQualifiedName': 'demo.CustomerId',
            'parameters': [],
            'name': 'CustomerId',
            'shortDisplayName': 'CustomerId',
            'longDisplayName': 'demo.CustomerId',
            'namespace': 'demo',
            'parameterizedName': 'demo.CustomerId'
          }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
        },
        'name': {
          'type': {
            'fullyQualifiedName': 'demo.CustomerName',
            'parameters': [],
            'name': 'CustomerName',
            'shortDisplayName': 'CustomerName',
            'longDisplayName': 'demo.CustomerName',
            'namespace': 'demo',
            'parameterizedName': 'demo.CustomerName'
          }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
        },
        'postcode': {
          'type': {
            'fullyQualifiedName': 'demo.Postcode',
            'parameters': [],
            'name': 'Postcode',
            'shortDisplayName': 'Postcode',
            'longDisplayName': 'demo.Postcode',
            'namespace': 'demo',
            'parameterizedName': 'demo.Postcode'
          }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
        }
      },
      'modifiers': [],
      'metadata': [],
      'aliasForType': null,
      'inheritsFrom': [],
      'enumValues': [],
      'sources': [{
        'name': 'customer-service',
        'version': '0.0.0',
        'content': 'type Customer {\n      email : CustomerEmailAddress\n      id : CustomerId\n      name : CustomerName\n      postcode : Postcode\n   }',
        'id': 'customer-service:0.0.0'
      }],
      'typeParameters': [],
      'typeDoc': '',
      'isTypeAlias': false,
      'format': null,
      'hasFormat': false,
      'isParameterType': false,
      'isClosed': false,
      'isPrimitive': false,
      'fullyQualifiedName': 'demo.Customer',
      'memberQualifiedName': {
        'fullyQualifiedName': 'demo.Customer',
        'parameters': [],
        'name': 'Customer',
        'shortDisplayName': 'Customer',
        'longDisplayName': 'demo.Customer',
        'namespace': 'demo',
        'parameterizedName': 'demo.Customer'
      },
      'isCollection': false,
      'underlyingTypeParameters': [],
      'collectionType': null,
      'isScalar': false
    },
    'operationType': null,
    'metadata': [{
      'name': {
        'fullyQualifiedName': 'HttpOperation',
        'parameters': [],
        'name': 'HttpOperation',
        'shortDisplayName': 'HttpOperation',
        'longDisplayName': 'HttpOperation',
        'namespace': '',
        'parameterizedName': 'HttpOperation'
      }, 'params': {'method': 'GET', 'url': '/customers/email/{demo.CustomerEmailAddress}'}
    }],
    'contract': {
      'returnType': {
        'name': {
          'fullyQualifiedName': 'demo.Customer',
          'parameters': [],
          'name': 'Customer',
          'shortDisplayName': 'Customer',
          'longDisplayName': 'demo.Customer',
          'namespace': 'demo',
          'parameterizedName': 'demo.Customer'
        },
        'attributes': {
          'email': {
            'type': {
              'fullyQualifiedName': 'demo.CustomerEmailAddress',
              'parameters': [],
              'name': 'CustomerEmailAddress',
              'shortDisplayName': 'CustomerEmailAddress',
              'longDisplayName': 'demo.CustomerEmailAddress',
              'namespace': 'demo',
              'parameterizedName': 'demo.CustomerEmailAddress'
            }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
          },
          'id': {
            'type': {
              'fullyQualifiedName': 'demo.CustomerId',
              'parameters': [],
              'name': 'CustomerId',
              'shortDisplayName': 'CustomerId',
              'longDisplayName': 'demo.CustomerId',
              'namespace': 'demo',
              'parameterizedName': 'demo.CustomerId'
            }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
          },
          'name': {
            'type': {
              'fullyQualifiedName': 'demo.CustomerName',
              'parameters': [],
              'name': 'CustomerName',
              'shortDisplayName': 'CustomerName',
              'longDisplayName': 'demo.CustomerName',
              'namespace': 'demo',
              'parameterizedName': 'demo.CustomerName'
            }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
          },
          'postcode': {
            'type': {
              'fullyQualifiedName': 'demo.Postcode',
              'parameters': [],
              'name': 'Postcode',
              'shortDisplayName': 'Postcode',
              'longDisplayName': 'demo.Postcode',
              'namespace': 'demo',
              'parameterizedName': 'demo.Postcode'
            }, 'modifiers': [], 'accessor': null, 'readCondition': null, 'typeDoc': null, 'constraints': []
          }
        },
        'modifiers': [],
        'metadata': [],
        'aliasForType': null,
        'inheritsFrom': [],
        'enumValues': [],
        'sources': [{
          'name': 'customer-service',
          'version': '0.0.0',
          'content': 'type Customer {\n      email : CustomerEmailAddress\n      id : CustomerId\n      name : CustomerName\n      postcode : Postcode\n   }',
          'id': 'customer-service:0.0.0'
        }],
        'typeParameters': [],
        'typeDoc': '',
        'isTypeAlias': false,
        'format': null,
        'hasFormat': false,
        'isParameterType': false,
        'isClosed': false,
        'isPrimitive': false,
        'fullyQualifiedName': 'demo.Customer',
        'memberQualifiedName': {
          'fullyQualifiedName': 'demo.Customer',
          'parameters': [],
          'name': 'Customer',
          'shortDisplayName': 'Customer',
          'longDisplayName': 'demo.Customer',
          'namespace': 'demo',
          'parameterizedName': 'demo.Customer'
        },
        'isCollection': false,
        'underlyingTypeParameters': [],
        'collectionType': null,
        'isScalar': false
      }, 'constraints': []
    },
    'sources': [{
      'name': 'customer-service',
      'version': '0.0.0',
      'content': '@HttpOperation(method = "GET" , url = "/customers/email/{demo.CustomerEmailAddress}")\n      operation getCustomerByEmail(  demo.CustomerEmailAddress ) : demo.Customer',
      'id': 'customer-service:0.0.0'
    }],
    'name': 'getCustomerByEmail',
    'memberQualifiedName': {
      'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
      'parameters': [],
      'name': 'CustomerService@@getCustomerByEmail',
      'shortDisplayName': 'CustomerService@@getCustomerByEmail',
      'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
      'namespace': 'io.vyne.demos.rewards',
      'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail'
    }
  }]
};
