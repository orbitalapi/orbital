import {NgModule, Pipe, PipeTransform} from '@angular/core';
import {ConnectorSummary, ConnectorType} from '../db-connection-editor/db-importer.service';

abstract class ConnectorsFilter implements PipeTransform {
  protected constructor(private connectorType: ConnectorType) {
  }

  transform(connectors: ConnectorSummary[]) {
    return connectors.filter(connector => connector.connectionType === this.connectorType);
  }
}

@Pipe({name: 'databases', standalone: true})
export class DbConnectionsPipe extends ConnectorsFilter {
  constructor() {
    super('JDBC');
  }
}

@Pipe({name: 'messageBrokers', standalone: true})
export class MessageBrokersConnectionsPipe extends ConnectorsFilter {
  constructor() {
    super('MESSAGE_BROKER');
  }
}

@Pipe({name: 'awsConnections', standalone: true})
export class AwsConnectionsPipe extends ConnectorsFilter {
  constructor() {
    super('AWS');
  }
}

