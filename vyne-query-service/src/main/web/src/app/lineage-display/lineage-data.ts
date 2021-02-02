export const LINEAGE_GRAPH = {
  'remoteCall': {
    'service': 'io.vyne.demos.rewards.balances.RewardsBalanceService',
    'addresss': 'http://192.168.5.73:9202/balances/4005-2003-2330-1002',
    'operation': 'getRewardsBalance',
    'responseTypeName': 'demo.RewardsAccountBalance',
    'method': 'GET',
    'requestBody': null,
    'resultCode': 200,
    'durationMs': 5,
    'response': {'cardNumber': '4005-2003-2330-1002', 'balance': 2300, 'currencyUnit': 'POINTS'},
    'operationQualifiedName': 'io.vyne.demos.rewards.balances.RewardsBalanceService@@getRewardsBalance'
  }, 'inputs': [{
    'parameterName': 'Unnamed', 'value': {
      'value': '4005-2003-2330-1002', 'source': {
        'remoteCall': {
          'service': 'io.vyne.demos.marketing.MarketingService',
          'addresss': 'http://192.168.5.73:9207/marketing/1',
          'operation': 'getMarketingDetailsForCustomer',
          'responseTypeName': 'io.vyne.demos.marketing.CustomerMarketingRecord',
          'method': 'GET',
          'requestBody': null,
          'resultCode': 200,
          'durationMs': 4,
          'response': {'id': 1, 'rewardsCardNumber': '4005-2003-2330-1002'},
          'operationQualifiedName': 'io.vyne.demos.marketing.MarketingService@@getMarketingDetailsForCustomer'
        },
        'inputs': [{
          'parameterName': 'Unnamed',
          'value': {
            'value': 1,
            'source': {
              'remoteCall': {
                'service': 'io.vyne.demos.rewards.CustomerService',
                'addresss': 'http://192.168.5.73:9201/customers/customers/email/jimmy@demo.com',
                'operation': 'getCustomerByEmail',
                'responseTypeName': 'demo.Customer',
                'method': 'GET',
                'requestBody': null,
                'resultCode': 200,
                'durationMs': 45,
                'response': {'id': 1, 'name': 'Jimmy', 'email': 'jimmy@demo.com', 'postcode': 'SW11'},
                'operationQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail'
              },
              'inputs': [{
                'parameterName': 'Unnamed',
                'value': {
                  'value': 'jimmy@demo.com',
                  'source': {'dataSourceName': 'Provided'},
                  'typeName': 'demo.CustomerEmailAddress'
                }
              }],
              'dataSourceName': 'Operation result'
            },
            'typeName': 'demo.CustomerId'
          }
        }],
        'dataSourceName': 'Operation result'
      }, 'typeName': 'demo.RewardsCardNumber'
    }
  }], 'dataSourceName': 'Operation result'
};


export const LINEAGE_GRAPH_WITH_EVALUATED_EXPRESSION = {
  'expressionTaxi': '(this.quantity / this.price)',
  'inputs': [{
    'value': 100,
    'source': {
      'remoteCall': {
        '@id': '5b441990-289c-4569-8ad2-6ba5c42e0dc6',
        'service': 'vyne.casks.OrderCaskService',
        'addresss': 'http://192.168.5.73:8800/api/cask/findAll/Order',
        'operation': 'findAll',
        'responseTypeName': 'lang.taxi.Array<Order>',
        'method': 'GET',
        'requestBody': null,
        'resultCode': 200,
        'durationMs': 21,
        'response': [{
          'quantity': 100,
          'price': 0.5,
          'caskmessageid': 'd550910d-5c99-41ca-947a-cd50381c53c0'
        }, {'quantity': 200, 'price': 0.0, 'caskmessageid': 'd550910d-5c99-41ca-947a-cd50381c53c0'}],
        'operationQualifiedName': 'vyne.casks.OrderCaskService@@findAll'
      }, 'inputs': [], 'dataSourceName': 'Operation result'
    },
    'typeName': 'OrderQuantity'
  }, {
    'value': 0.5,
    'source': {
      'remoteCall': '5b441990-289c-4569-8ad2-6ba5c42e0dc6',
      'inputs': [],
      'dataSourceName': 'Operation result'
    },
    'typeName': 'OrderPrice'
  }],
  'dataSourceName': 'Evaluated expression'
};

export const LINEAGE_GRAPH_WITH_FAILED_EXPRESSION = {
  'expressionTaxi': '(this.quantity / this.price)',
  'inputs': [{
    'value': 200,
    'source': {
      'remoteCall': {
        '@id': '5c05cc6f-1955-4733-a684-c57e2b52a284',
        'service': 'vyne.casks.OrderCaskService',
        'addresss': 'http://localhost:8800/api/cask/findAll/Order',
        'operation': 'findAll',
        'responseTypeName': 'lang.taxi.Array<Order>',
        'method': 'GET',
        'requestBody': null,
        'resultCode': 200,
        'durationMs': 334,
        'response': [{
          'quantity': 100,
          'price': 0.5,
          'caskmessageid': 'd550910d-5c99-41ca-947a-cd50381c53c0'
        }, {'quantity': 200, 'price': 0.0, 'caskmessageid': 'd550910d-5c99-41ca-947a-cd50381c53c0'}],
        'operationQualifiedName': 'vyne.casks.OrderCaskService@@findAll',
        'responseTypeDisplayName': 'Order[]'
      }, 'inputs': [], 'dataSourceName': 'Operation result'
    },
    'typeName': 'OrderQuantity'
  }, {
    'value': 0.0,
    'source': {
      'remoteCall': '5c05cc6f-1955-4733-a684-c57e2b52a284',
      'inputs': [],
      'dataSourceName': 'Operation result'
    },
    'typeName': 'OrderPrice'
  }],
  'errorMessage': '/ by zero',
  'dataSourceName': 'Failed evaluated expression'
};
