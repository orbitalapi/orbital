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
