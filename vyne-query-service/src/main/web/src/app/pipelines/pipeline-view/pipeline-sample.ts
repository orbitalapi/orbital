import {RunningPipelineSummary} from '../pipelines.service';

export const pipeline: RunningPipelineSummary = {
  'pipeline': {
    'name': 'test',
    'jobId': '06ab-d250-0b80-0001',
    'spec': {
      'name': 'test',
      'input': {
        'operationName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail',
        'pollSchedule': '*/10 * * * * *',
        'parameterMap': {'since': '$pipeline.lastRunTime'},
        'type': 'taxiOperation',
        'direction': 'INPUT'
      },
      'output': {'operationName': 'io.vyne.demos.rewards.CustomerService@@getCustomerByEmail', 'type': 'taxiOperation', 'direction': 'OUTPUT'},
      'description': 'From Fetch data from operation OrderService / listOrders to Send data to operation StockService / submitOrders',
      'id': 'test@835323834'
    },
    // eslint-disable-next-line max-len
    'dotViz': 'digraph Pipeline {\n\t"Ingest from Fetch data from operation OrderService / listOrders" -> "Transform OrderTransaction[] to StockServiceOrderEvent[] using Vyne";\n\t"Transform OrderTransaction[] to StockServiceOrderEvent[] using Vyne" -> "Write to Send data to operation StockService / submitOrders";\n}',
    'graph': {
      'nodes': [{
        'id': 'IngestfromFetchdatafromoperationOrderServicelistOrders',
        'label': 'Poll operation OrderService / listOrders'
      }, {
        'id': 'TransformOrderTransactiontoStockServiceOrderEventusingVyne',
        'label': 'Transform OrderTransaction[] -> StockServiceOrderEvent[]'
      }, {
        'id': 'WritetoSenddatatooperationStockServicesubmitOrders',
        'label': 'Send to operation StockService / submitOrders'
      }],
      'links': [{
        'source': 'IngestfromFetchdatafromoperationOrderServicelistOrders',
        'target': 'TransformOrderTransactiontoStockServiceOrderEventusingVyne',
        'label': ''
      }, {
        'source': 'TransformOrderTransactiontoStockServiceOrderEventusingVyne',
        'target': 'WritetoSenddatatooperationStockServicesubmitOrders',
        'label': ''
      }]
    },
    'pipelineSpecId': 'test@835323834'
  }, 'status': {
    'name': 'Unnamed job 480709026477244417',
    'id': '06ab-d250-0b80-0001',
    'status': 'RUNNING',
    'submissionTime': new Date(Date.parse('2021-08-19T12:06:03.116Z')),
    'metrics': {
      'receivedCount': [{
        'address': '[192.168.5.73]:5703',
        'measurements': [{'value': 16, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 16,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 15, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 15,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 15, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 15,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 15, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 15,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 122, 'timestamp': '2021-08-19T12:26:25.015Z'}],
        'latestValue': {'value': 244, 'timestamp': '2021-08-19T12:26:25.015Z'}
      }],
      'emittedCount': [{
        'address': '[192.168.5.73]:5703',
        'measurements': [{'value': 122, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 16,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 16, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 15,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 15, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 15,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 15, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 15,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 15, 'timestamp': '2021-08-19T12:26:25.015Z'}],
        'latestValue': {'value': 244, 'timestamp': '2021-08-19T12:26:25.015Z'}
      }],
      'inflight': [{
        'address': '[192.168.5.73]:5703',
        'measurements': [{'value': 16, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 16,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 15, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 15,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 15, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 15,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 15, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 15,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 122, 'timestamp': '2021-08-19T12:26:25.015Z'}],
        'latestValue': {'value': 16, 'timestamp': '2021-08-19T12:26:25.015Z'}
      }],
      'queueSize': [{
        'address': '[192.168.5.73]:5703',
        'measurements': [{'value': 0, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 0,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 0, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 0,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 0, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 0,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 0, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 0,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }, {'value': 0, 'timestamp': '2021-08-19T12:26:25.015Z'}, {
          'value': 0,
          'timestamp': '2021-08-19T12:26:25.015Z'
        }],
        'latestValue': {'value': 0, 'timestamp': '2021-08-19T12:26:25.015Z'}
      }]
    }
  }
};
