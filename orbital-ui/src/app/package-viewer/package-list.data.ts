import { SourcePackageDescription } from './packages.service';

export const PACKAGE_LIST = [
  {
    'identifier': {
      'organisation': 'demo.vyne',
      'name': 'films-demo',
      'version': '0.1.0',
      'unversionedId': 'demo.vyne/films-demo',
      'id': 'demo.vyne/films-demo/0.1.0'
    },
    'health': {
      'status': 'Healthy',
      'message': null,
      'timestamp': '2022-08-11T10:48:02.266985Z'
    },
    'sourceCount': 27,
    'warningCount': 0,
    'errorCount': 0,
    'uriPath': 'demo.vyne:films-demo:0.1.0'
  },
  {
    'identifier': {
      'organisation': 'io.vyne.demos',
      'name': 'films-service',
      'version': '0.0.0',
      'unversionedId': 'io.vyne.demos/films-service',
      'id': 'io.vyne.demos/films-service/0.0.0'
    },
    'health': {
      'status': 'Healthy',
      'message': null,
      'timestamp': '2022-08-11T10:48:02.818390Z'
    },
    'sourceCount': 1,
    'warningCount': 0,
    'errorCount': 0,
    'uriPath': 'io.vyne.demos:films-service:0.0.0'
  },
  {
    'identifier': {
      'organisation': 'io.vyne',
      'name': 'core-types',
      'version': '1.0.0',
      'unversionedId': 'io.vyne/core-types',
      'id': 'io.vyne/core-types/1.0.0'
    },
    'health': {
      'status': 'Healthy',
      'message': null,
      'timestamp': '2022-08-11T10:48:02.831341Z'
    },
    'sourceCount': 10,
    'warningCount': 0,
    'errorCount': 0,
    'uriPath': 'io.vyne:core-types:1.0.0'
  }
] as SourcePackageDescription[]
