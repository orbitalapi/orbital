import {Pipe, PipeTransform} from '@angular/core';
import {ConnectorSummary, ConnectorType} from '../../../db-connection-editor/db-importer.service';

abstract class ConnectorsFilter implements PipeTransform {
  protected constructor(private connectorType:ConnectorType) {
  }
  transform(connectors: ConnectorSummary[]) {
    return connectors.filter(connector => connector.type === this.connectorType);
  }
}

@Pipe({name: 'databases'})
export class DbConnectionsPipe extends ConnectorsFilter {
  constructor() {
    super('JDBC');
  }
}

@Pipe({name: 'messageBrokers'})
export class MessageBrokersConnectionsPipe extends ConnectorsFilter {
  constructor() {
    super('MESSAGE_BROKER');
  }
}



