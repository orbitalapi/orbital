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

export const LINEAGE_GRAPH_WITH_COMPLEX_EXPRESSION = {
    'expressionTaxi': 'FundTotalEsgScore / TotalPortfolioSize',
    'inputs': [
      {
        'value': 569.597,
        'source': {
          'expressionTaxi': 'by taxi.stdlib.sum( FundHoldingWithScore[],(FundHoldingWithScore) -> FundHoldingPercentage * WeightedAverageScore )',
          'inputs': [
            {
              'value': 173.873,
              'source': {
                'expressionTaxi': 'FundHoldingPercentage * WeightedAverageScore',
                'inputs': [
                  {
                    'value': 29.47,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                        'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/holdings/LU0323240373',
                        'operation': 'getHoldings',
                        'responseTypeName': 'msci.IsinFundHoldings',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 4,
                        'timestamp': '2021-10-12T08:15:53.420409Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                        'responseTypeDisplayName': 'IsinFundHoldings',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'msci.FundIsin',
                            'value': 'LU0323240373',
                            'dataSourceId': 'Provided'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
                    'hashCodeWithDataSource': -2046081489
                  },
                  {
                    'value': 5.9,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': '8c4f9434-8e11-4a63-bbb3-ef089236b0b0',
                        'responseId': 'c41a1477-f594-4a98-9139-e6dbd1200bd9',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/metrics/NY2460100003',
                        'operation': 'getMetrics',
                        'responseTypeName': 'msci.RatingsReportJson',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 4,
                        'timestamp': '2021-10-12T08:15:53.431714Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getMetrics',
                        'responseTypeDisplayName': 'RatingsReportJson',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'financial.core.Isin',
                            'value': 'NY2460100003',
                            'dataSourceId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': '8c4f9434-8e11-4a63-bbb3-ef089236b0b0',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'msci.WeightedAverageScore',
                    'hashCodeWithDataSource': 346926106
                  }
                ],
                'id': 'b25c11f9-a796-4e48-be87-9c4146495a85',
                'failedAttempts': [],
                'dataSourceName': 'Evaluated expression'
              },
              'typeName': 'lang.taxi.Decimal',
              'hashCodeWithDataSource': 728641882
            },
            {
              'value': 20.76,
              'source': {
                'expressionTaxi': 'FundHoldingPercentage * WeightedAverageScore',
                'inputs': [
                  {
                    'value': 3.46,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                        'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/holdings/LU0323240373',
                        'operation': 'getHoldings',
                        'responseTypeName': 'msci.IsinFundHoldings',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 4,
                        'timestamp': '2021-10-12T08:15:53.420409Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                        'responseTypeDisplayName': 'IsinFundHoldings',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'msci.FundIsin',
                            'value': 'LU0323240373',
                            'dataSourceId': 'Provided'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
                    'hashCodeWithDataSource': -2049064836
                  },
                  {
                    'value': 6,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': 'a241ec59-cf5e-48cd-9356-ac7e76181289',
                        'responseId': '74c50a92-d2ce-40f5-a355-50b398f62697',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/metrics/NY2460100014',
                        'operation': 'getMetrics',
                        'responseTypeName': 'msci.RatingsReportJson',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 4,
                        'timestamp': '2021-10-12T08:15:53.468143Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getMetrics',
                        'responseTypeDisplayName': 'RatingsReportJson',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'financial.core.Isin',
                            'value': 'NY2460100014',
                            'dataSourceId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': 'a241ec59-cf5e-48cd-9356-ac7e76181289',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'msci.WeightedAverageScore',
                    'hashCodeWithDataSource': -1811293646
                  }
                ],
                'id': '034cd821-190a-4a5b-b1ec-9a3c0cf9bc06',
                'failedAttempts': [],
                'dataSourceName': 'Evaluated expression'
              },
              'typeName': 'lang.taxi.Decimal',
              'hashCodeWithDataSource': -489755511
            },
            {
              'value': 9.072,
              'source': {
                'expressionTaxi': 'FundHoldingPercentage * WeightedAverageScore',
                'inputs': [
                  {
                    'value': 1.62,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                        'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/holdings/LU0323240373',
                        'operation': 'getHoldings',
                        'responseTypeName': 'msci.IsinFundHoldings',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 4,
                        'timestamp': '2021-10-12T08:15:53.420409Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                        'responseTypeDisplayName': 'IsinFundHoldings',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'msci.FundIsin',
                            'value': 'LU0323240373',
                            'dataSourceId': 'Provided'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
                    'hashCodeWithDataSource': -2049275884
                  },
                  {
                    'value': 5.6,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': '961bd45d-2823-4c62-b508-6cc0188bf9ac',
                        'responseId': 'c16b8a8c-5a14-4b29-9cae-54d6e08e09d4',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/metrics/NY2460100024',
                        'operation': 'getMetrics',
                        'responseTypeName': 'msci.RatingsReportJson',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 4,
                        'timestamp': '2021-10-12T08:15:53.500448Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getMetrics',
                        'responseTypeDisplayName': 'RatingsReportJson',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'financial.core.Isin',
                            'value': 'NY2460100024',
                            'dataSourceId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': '961bd45d-2823-4c62-b508-6cc0188bf9ac',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'msci.WeightedAverageScore',
                    'hashCodeWithDataSource': 922665674
                  }
                ],
                'id': '03db508c-a362-43a3-a7c1-2355c16accd0',
                'failedAttempts': [],
                'dataSourceName': 'Evaluated expression'
              },
              'typeName': 'lang.taxi.Decimal',
              'hashCodeWithDataSource': 500500929
            },
            {
              'value': 8.164,
              'source': {
                'expressionTaxi': 'FundHoldingPercentage * WeightedAverageScore',
                'inputs': [
                  {
                    'value': 1.57,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                        'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/holdings/LU0323240373',
                        'operation': 'getHoldings',
                        'responseTypeName': 'msci.IsinFundHoldings',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 4,
                        'timestamp': '2021-10-12T08:15:53.420409Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                        'responseTypeDisplayName': 'IsinFundHoldings',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'msci.FundIsin',
                            'value': 'LU0323240373',
                            'dataSourceId': 'Provided'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
                    'hashCodeWithDataSource': -2049281619
                  },
                  {
                    'value': 5.2,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': 'f2832618-74fe-4472-8ca0-f5fab920ce8c',
                        'responseId': '48913e59-4eba-4003-9021-11bfa4c2c206',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/metrics/NY2460100082',
                        'operation': 'getMetrics',
                        'responseTypeName': 'msci.RatingsReportJson',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 3,
                        'timestamp': '2021-10-12T08:15:53.533821Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getMetrics',
                        'responseTypeDisplayName': 'RatingsReportJson',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'financial.core.Isin',
                            'value': 'NY2460100082',
                            'dataSourceId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': 'f2832618-74fe-4472-8ca0-f5fab920ce8c',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'msci.WeightedAverageScore',
                    'hashCodeWithDataSource': 320115680
                  }
                ],
                'id': '4478f3e6-e567-4229-885e-7cebaa603f1a',
                'failedAttempts': [],
                'dataSourceName': 'Evaluated expression'
              },
              'typeName': 'lang.taxi.Decimal',
              'hashCodeWithDataSource': -363496773
            },
            {
              'value': 357.728,
              'source': {
                'expressionTaxi': 'FundHoldingPercentage * WeightedAverageScore',
                'inputs': [
                  {
                    'value': 63.88,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                        'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/holdings/LU0323240373',
                        'operation': 'getHoldings',
                        'responseTypeName': 'msci.IsinFundHoldings',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 4,
                        'timestamp': '2021-10-12T08:15:53.420409Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                        'responseTypeDisplayName': 'IsinFundHoldings',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'msci.FundIsin',
                            'value': 'LU0323240373',
                            'dataSourceId': 'Provided'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
                    'hashCodeWithDataSource': -2042134662
                  },
                  {
                    'value': 5.6,
                    'source': {
                      'remoteCall': {
                        'remoteCallId': '7b8c4eb3-39c5-4134-961a-2bdc8d411644',
                        'responseId': 'fc341ef9-304d-4749-b0b1-494e527af8c5',
                        'service': 'msci.MsciDataService',
                        'address': 'http://localhost:9985/metrics/NY2460100134',
                        'operation': 'getMetrics',
                        'responseTypeName': 'msci.RatingsReportJson',
                        'method': 'GET',
                        'requestBody': null,
                        'resultCode': 200,
                        'durationMs': 3,
                        'timestamp': '2021-10-12T08:15:53.565827Z',
                        'responseMessageType': 'FULL',
                        'operationQualifiedName': 'msci.MsciDataService@@getMetrics',
                        'responseTypeDisplayName': 'RatingsReportJson',
                        'serviceDisplayName': 'MsciDataService'
                      },
                      'inputs': [
                        {
                          'parameterName': 'isin',
                          'value': {
                            'typeName': 'financial.core.Isin',
                            'value': 'NY2460100134',
                            'dataSourceId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac'
                          }
                        }
                      ],
                      'failedAttempts': [],
                      'id': '7b8c4eb3-39c5-4134-961a-2bdc8d411644',
                      'dataSourceName': 'Operation result'
                    },
                    'typeName': 'msci.WeightedAverageScore',
                    'hashCodeWithDataSource': -1239778033
                  }
                ],
                'id': '535e9836-45a4-4616-86e2-a50376bb206f',
                'failedAttempts': [],
                'dataSourceName': 'Evaluated expression'
              },
              'typeName': 'lang.taxi.Decimal',
              'hashCodeWithDataSource': 1818070735
            }
          ],
          'id': '43cdc47e-c0a4-42dd-82d5-b35b3daeec51',
          'failedAttempts': [],
          'dataSourceName': 'Evaluated expression'
        },
        'typeName': 'nyaya.esg.core.data.FundTotalEsgScore',
        'hashCodeWithDataSource': 2102558834
      },
      {
        'value': 100.00,
        'source': {
          'expressionTaxi': 'by taxi.stdlib.sum( FundHoldingWithScore[],(FundHoldingWithScore) -> FundHoldingPercentage )',
          'inputs': [
            {
              'value': 29.47,
              'source': {
                'remoteCall': {
                  'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                  'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                  'service': 'msci.MsciDataService',
                  'address': 'http://localhost:9985/holdings/LU0323240373',
                  'operation': 'getHoldings',
                  'responseTypeName': 'msci.IsinFundHoldings',
                  'method': 'GET',
                  'requestBody': null,
                  'resultCode': 200,
                  'durationMs': 4,
                  'timestamp': '2021-10-12T08:15:53.420409Z',
                  'responseMessageType': 'FULL',
                  'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                  'responseTypeDisplayName': 'IsinFundHoldings',
                  'serviceDisplayName': 'MsciDataService'
                },
                'inputs': [
                  {
                    'parameterName': 'isin',
                    'value': {
                      'typeName': 'msci.FundIsin',
                      'value': 'LU0323240373',
                      'dataSourceId': 'Provided'
                    }
                  }
                ],
                'failedAttempts': [],
                'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                'dataSourceName': 'Operation result'
              },
              'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
              'hashCodeWithDataSource': -2046081489
            },
            {
              'value': 3.46,
              'source': {
                'remoteCall': {
                  'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                  'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                  'service': 'msci.MsciDataService',
                  'address': 'http://localhost:9985/holdings/LU0323240373',
                  'operation': 'getHoldings',
                  'responseTypeName': 'msci.IsinFundHoldings',
                  'method': 'GET',
                  'requestBody': null,
                  'resultCode': 200,
                  'durationMs': 4,
                  'timestamp': '2021-10-12T08:15:53.420409Z',
                  'responseMessageType': 'FULL',
                  'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                  'responseTypeDisplayName': 'IsinFundHoldings',
                  'serviceDisplayName': 'MsciDataService'
                },
                'inputs': [
                  {
                    'parameterName': 'isin',
                    'value': {
                      'typeName': 'msci.FundIsin',
                      'value': 'LU0323240373',
                      'dataSourceId': 'Provided'
                    }
                  }
                ],
                'failedAttempts': [],
                'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                'dataSourceName': 'Operation result'
              },
              'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
              'hashCodeWithDataSource': -2049064836
            },
            {
              'value': 1.62,
              'source': {
                'remoteCall': {
                  'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                  'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                  'service': 'msci.MsciDataService',
                  'address': 'http://localhost:9985/holdings/LU0323240373',
                  'operation': 'getHoldings',
                  'responseTypeName': 'msci.IsinFundHoldings',
                  'method': 'GET',
                  'requestBody': null,
                  'resultCode': 200,
                  'durationMs': 4,
                  'timestamp': '2021-10-12T08:15:53.420409Z',
                  'responseMessageType': 'FULL',
                  'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                  'responseTypeDisplayName': 'IsinFundHoldings',
                  'serviceDisplayName': 'MsciDataService'
                },
                'inputs': [
                  {
                    'parameterName': 'isin',
                    'value': {
                      'typeName': 'msci.FundIsin',
                      'value': 'LU0323240373',
                      'dataSourceId': 'Provided'
                    }
                  }
                ],
                'failedAttempts': [],
                'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                'dataSourceName': 'Operation result'
              },
              'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
              'hashCodeWithDataSource': -2049275884
            },
            {
              'value': 1.57,
              'source': {
                'remoteCall': {
                  'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                  'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                  'service': 'msci.MsciDataService',
                  'address': 'http://localhost:9985/holdings/LU0323240373',
                  'operation': 'getHoldings',
                  'responseTypeName': 'msci.IsinFundHoldings',
                  'method': 'GET',
                  'requestBody': null,
                  'resultCode': 200,
                  'durationMs': 4,
                  'timestamp': '2021-10-12T08:15:53.420409Z',
                  'responseMessageType': 'FULL',
                  'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                  'responseTypeDisplayName': 'IsinFundHoldings',
                  'serviceDisplayName': 'MsciDataService'
                },
                'inputs': [
                  {
                    'parameterName': 'isin',
                    'value': {
                      'typeName': 'msci.FundIsin',
                      'value': 'LU0323240373',
                      'dataSourceId': 'Provided'
                    }
                  }
                ],
                'failedAttempts': [],
                'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                'dataSourceName': 'Operation result'
              },
              'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
              'hashCodeWithDataSource': -2049281619
            },
            {
              'value': 63.88,
              'source': {
                'remoteCall': {
                  'remoteCallId': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                  'responseId': '1b0383c2-70ba-4ed8-9c12-34c0c47e2826',
                  'service': 'msci.MsciDataService',
                  'address': 'http://localhost:9985/holdings/LU0323240373',
                  'operation': 'getHoldings',
                  'responseTypeName': 'msci.IsinFundHoldings',
                  'method': 'GET',
                  'requestBody': null,
                  'resultCode': 200,
                  'durationMs': 4,
                  'timestamp': '2021-10-12T08:15:53.420409Z',
                  'responseMessageType': 'FULL',
                  'operationQualifiedName': 'msci.MsciDataService@@getHoldings',
                  'responseTypeDisplayName': 'IsinFundHoldings',
                  'serviceDisplayName': 'MsciDataService'
                },
                'inputs': [
                  {
                    'parameterName': 'isin',
                    'value': {
                      'typeName': 'msci.FundIsin',
                      'value': 'LU0323240373',
                      'dataSourceId': 'Provided'
                    }
                  }
                ],
                'failedAttempts': [],
                'id': 'e8cc86d6-4dbc-482e-a419-fea2e3dbe7ac',
                'dataSourceName': 'Operation result'
              },
              'typeName': 'nyaya.esg.core.data.FundHoldingPercentage',
              'hashCodeWithDataSource': -2042134662
            }
          ],
          'id': '542b9227-eef2-4192-b93b-82cc16aebdb6',
          'failedAttempts': [],
          'dataSourceName': 'Evaluated expression'
        },
        'typeName': 'nyaya.esg.core.data.TotalPortfolioSize',
        'hashCodeWithDataSource': 1718824914
      }
    ],
    'id': '7cbbe8de-2164-4abd-ae32-7877defaa5c5',
    'failedAttempts': [],
    'dataSourceName': 'Evaluated expression'
  };
