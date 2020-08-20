import {LineageGraph} from '../services/query.service';

export const LINEAGE_GRAPH = {
  '0': {
    'inputs': [
      {
        'parameterName': 'Unnamed',
        'value': {
          'value': '4005-2003-2330-1002',
          'source': {
            'dataSourceIndex': 1
          },
          'typeName': 'demo.RewardsCardNumber'
        }
      }
    ],
    'name': 'Operation result',
    'remoteCall': {
      'service': 'io.vyne.demos.rewards.balances.RewardsBalanceService',
      'addresss': 'http://192.168.5.73:9202/balances/4005-2003-2330-1002',
      'operation': 'getRewardsBalance',
      'responseTypeName': 'demo.RewardsAccountBalance',
      'method': 'GET',
      'requestBody': null,
      'resultCode': 200,
      'durationMs': 6,
      'response': {
        'cardNumber': '4005-2003-2330-1002',
        'balance': 2300,
        'currencyUnit': 'POINTS'
      },
      'operationQualifiedName': 'io.vyne.demos.rewards.balances.RewardsBalanceService@@getRewardsBalance'
    }
  },
  '1': {
    'inputs': [
      {
        'parameterName': 'Unnamed',
        'value': {
          'value': 1,
          'source': {
            'dataSourceIndex': 2
          },
          'typeName': 'demo.CustomerId'
        }
      }
    ],
    'name': 'Operation result',
    'remoteCall': {
      'service': 'io.vyne.demos.marketing.MarketingService',
      'addresss': 'http://192.168.5.73:9207/marketing/1',
      'operation': 'getMarketingDetailsForCustomer',
      'responseTypeName': 'io.vyne.demos.marketing.CustomerMarketingRecord',
      'method': 'GET',
      'requestBody': null,
      'resultCode': 200,
      'durationMs': 6,
      'response': {
        'id': 1,
        'rewardsCardNumber': '4005-2003-2330-1002'
      },
      'operationQualifiedName': 'io.vyne.demos.marketing.MarketingService@@getMarketingDetailsForCustomer'
    }
  },
  '2': {
    'inputs': [
      {
        'parameterName': 'Unnamed',
        'value': {
          'value': 'jimmy@demo.com',
          'source': {
            'dataSourceIndex': 3
          },
          'typeName': 'demo.CustomerEmailAddress'
        }
      }
    ],
    'name': 'Operation result',
    'remoteCall': {
      'service': 'io.vyne.demos.rewards.CustomerService',
      'addresss': 'http://192.168.5.73:9201/customers/email/jimmy@demo.com',
      'operation': 'getCustomerByEmail',
      'responseTypeName': 'demo.Customer',
      'method': 'GET',
      'requestBody': null,
      'resultCode': 200,
      'durationMs': 6,
      'response': {
        'id': 1,
        'name': 'Jimmy',
        'email': 'jimmy@demo.com',
        'postcode': 'SW11'
      },
      'operationQualifiedName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail'
    }
  },
  '3': {
    'name': 'Provided'
  }
};
