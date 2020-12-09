import {Service} from '../services/schema';

export const service: Service = {
  'name': {
    'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService',
    'parameters': [],
    'name': 'CustomerService',
    'namespace': 'io.vyne.demos.rewards',
    'parameterizedName': 'io.vyne.demos.rewards.CustomerService',
    'longDisplayName': 'io.vyne.demos.rewards.CustomerService',
    'shortDisplayName': 'CustomerService'
  },
  typeDoc: 'Provides basic access to read data about Customers',
  'operations': [
    {
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerIdByEmail',
        'parameters': [],
        'name': 'CustomerService@@getCustomerIdByEmail',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerIdByEmail',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomerIdByEmail',
        'shortDisplayName': 'CustomerService@@getCustomerIdByEmail'
      },
      'parameters': [
        {
          'type': {
            'fullyQualifiedName': 'demo.CustomerEmailAddress',
            'parameters': [],
            'name': 'CustomerEmailAddress',
            'namespace': 'demo',
            'parameterizedName': 'demo.CustomerEmailAddress',
            'longDisplayName': 'demo.CustomerEmailAddress',
            'shortDisplayName': 'CustomerEmailAddress'
          },
          'name': 'customerEmail',
          'metadata': [],
          'constraints': []
        }
      ],
      'returnType': {
        'fullyQualifiedName': 'demo.CustomerId',
        'parameters': [],
        'name': 'CustomerId',
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerId',
        'longDisplayName': 'demo.CustomerId',
        'shortDisplayName': 'CustomerId'
      },
      'operationType': null,
      'metadata': [
        {
          'name': {
            'fullyQualifiedName': 'HttpOperation',
            'parameters': [],
            'name': 'HttpOperation',
            'namespace': '',
            'parameterizedName': 'HttpOperation',
            'longDisplayName': 'HttpOperation',
            'shortDisplayName': 'HttpOperation'
          },
          'params': {
            'method': 'GET',
            'url': '/customers/customers/{demo.CustomerEmailAddress}/id'
          }
        }
      ],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'demo.CustomerId',
          'parameters': [],
          'name': 'CustomerId',
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerId',
          'longDisplayName': 'demo.CustomerId',
          'shortDisplayName': 'CustomerId'
        },
        'constraints': []
      },
      'typeDoc': null,
      'name': 'getCustomerIdByEmail',
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerIdByEmail',
        'parameters': [],
        'name': 'CustomerService@@getCustomerIdByEmail',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerIdByEmail',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomerIdByEmail',
        'shortDisplayName': 'CustomerService@@getCustomerIdByEmail'
      }
    },
    {
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomer',
        'parameters': [],
        'name': 'CustomerService@@getCustomer',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomer',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomer',
        'shortDisplayName': 'CustomerService@@getCustomer'
      },
      'parameters': [
        {
          'type': {
            'fullyQualifiedName': 'demo.CustomerId',
            'parameters': [],
            'name': 'CustomerId',
            'namespace': 'demo',
            'parameterizedName': 'demo.CustomerId',
            'longDisplayName': 'demo.CustomerId',
            'shortDisplayName': 'CustomerId'
          },
          'name': 'customerId',
          'metadata': [],
          'constraints': []
        }
      ],
      'returnType': {
        'fullyQualifiedName': 'demo.Customer',
        'parameters': [],
        'name': 'Customer',
        'namespace': 'demo',
        'parameterizedName': 'demo.Customer',
        'longDisplayName': 'demo.Customer',
        'shortDisplayName': 'Customer'
      },
      'operationType': null,
      'metadata': [
        {
          'name': {
            'fullyQualifiedName': 'HttpOperation',
            'parameters': [],
            'name': 'HttpOperation',
            'namespace': '',
            'parameterizedName': 'HttpOperation',
            'longDisplayName': 'HttpOperation',
            'shortDisplayName': 'HttpOperation'
          },
          'params': {
            'method': 'POST',
            'url': '/customers/customers/{demo.CustomerId}'
          }
        }
      ],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'demo.Customer',
          'parameters': [],
          'name': 'Customer',
          'namespace': 'demo',
          'parameterizedName': 'demo.Customer',
          'longDisplayName': 'demo.Customer',
          'shortDisplayName': 'Customer'
        },
        'constraints': []
      },
      'typeDoc': null,
      'name': 'getCustomer',
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomer',
        'parameters': [],
        'name': 'CustomerService@@getCustomer',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomer',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomer',
        'shortDisplayName': 'CustomerService@@getCustomer'
      }
    },
    {
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomers',
        'parameters': [],
        'name': 'CustomerService@@getCustomers',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomers',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomers',
        'shortDisplayName': 'CustomerService@@getCustomers'
      },
      'parameters': [],
      'returnType': {
        'fullyQualifiedName': 'lang.taxi.Array',
        'parameters': [
          {
            'fullyQualifiedName': 'demo.Customer',
            'parameters': [],
            'name': 'Customer',
            'namespace': 'demo',
            'parameterizedName': 'demo.Customer',
            'longDisplayName': 'demo.Customer',
            'shortDisplayName': 'Customer'
          }
        ],
        'name': 'Array',
        'namespace': 'lang.taxi',
        'parameterizedName': 'lang.taxi.Array<demo.Customer>',
        'longDisplayName': 'demo.Customer[]',
        'shortDisplayName': 'Customer[]'
      },
      'operationType': null,
      'metadata': [
        {
          'name': {
            'fullyQualifiedName': 'HttpOperation',
            'parameters': [],
            'name': 'HttpOperation',
            'namespace': '',
            'parameterizedName': 'HttpOperation',
            'longDisplayName': 'HttpOperation',
            'shortDisplayName': 'HttpOperation'
          },
          'params': {
            'method': 'PUT',
            'url': '/customers/customers'
          }
        }
      ],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'lang.taxi.Array',
          'parameters': [
            {
              'fullyQualifiedName': 'demo.Customer',
              'parameters': [],
              'name': 'Customer',
              'namespace': 'demo',
              'parameterizedName': 'demo.Customer',
              'longDisplayName': 'demo.Customer',
              'shortDisplayName': 'Customer'
            }
          ],
          'name': 'Array',
          'namespace': 'lang.taxi',
          'parameterizedName': 'lang.taxi.Array<demo.Customer>',
          'longDisplayName': 'demo.Customer[]',
          'shortDisplayName': 'Customer[]'
        },
        'constraints': []
      },
      'typeDoc': null,
      'name': 'getCustomers',
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomers',
        'parameters': [],
        'name': 'CustomerService@@getCustomers',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomers',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomers',
        'shortDisplayName': 'CustomerService@@getCustomers'
      }
    },
    {
      'qualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'parameters': [],
        'name': 'CustomerService@@getCustomerByEmail',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'shortDisplayName': 'CustomerService@@getCustomerByEmail'
      },
      'parameters': [
        {
          'type': {
            'fullyQualifiedName': 'demo.CustomerEmailAddress',
            'parameters': [],
            'name': 'CustomerEmailAddress',
            'namespace': 'demo',
            'parameterizedName': 'demo.CustomerEmailAddress',
            'longDisplayName': 'demo.CustomerEmailAddress',
            'shortDisplayName': 'CustomerEmailAddress'
          },
          'name': 'customerEmail',
          'metadata': [],
          'constraints': []
        }
      ],
      'returnType': {
        'fullyQualifiedName': 'demo.Customer',
        'parameters': [],
        'name': 'Customer',
        'namespace': 'demo',
        'parameterizedName': 'demo.Customer',
        'longDisplayName': 'demo.Customer',
        'shortDisplayName': 'Customer'
      },
      'operationType': null,
      'metadata': [
        {
          'name': {
            'fullyQualifiedName': 'HttpOperation',
            'parameters': [],
            'name': 'HttpOperation',
            'namespace': '',
            'parameterizedName': 'HttpOperation',
            'longDisplayName': 'HttpOperation',
            'shortDisplayName': 'HttpOperation'
          },
          'params': {
            'method': 'DELETE',
            'url': '/customers/customers/email/{demo.CustomerEmailAddress}'
          }
        }
      ],
      'contract': {
        'returnType': {
          'fullyQualifiedName': 'demo.Customer',
          'parameters': [],
          'name': 'Customer',
          'namespace': 'demo',
          'parameterizedName': 'demo.Customer',
          'longDisplayName': 'demo.Customer',
          'shortDisplayName': 'Customer'
        },
        'constraints': []
      },
      'typeDoc': null,
      'name': 'getCustomerByEmail',
      'memberQualifiedName': {
        'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'parameters': [],
        'name': 'CustomerService@@getCustomerByEmail',
        'namespace': 'io.vyne.demos.rewards',
        'parameterizedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'longDisplayName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'shortDisplayName': 'CustomerService@@getCustomerByEmail'
      }
    }
  ],
  'queryOperations': [],
  'metadata': [
    {
      'name': {
        'fullyQualifiedName': 'ServiceDiscoveryClient',
        'parameters': [],
        'name': 'ServiceDiscoveryClient',
        'namespace': '',
        'parameterizedName': 'ServiceDiscoveryClient',
        'longDisplayName': 'ServiceDiscoveryClient',
        'shortDisplayName': 'ServiceDiscoveryClient'
      },
      'params': {
        'serviceName': 'customer-service'
      }
    }
  ],
  'qualifiedName': 'io.vyne.demos.rewards.CustomerService',
  'memberQualifiedName': {
    'fullyQualifiedName': 'io.vyne.demos.rewards.CustomerService',
    'parameters': [],
    'name': 'CustomerService',
    'namespace': 'io.vyne.demos.rewards',
    'parameterizedName': 'io.vyne.demos.rewards.CustomerService',
    'longDisplayName': 'io.vyne.demos.rewards.CustomerService',
    'shortDisplayName': 'CustomerService'
  }
};
