import {moduleMetadata, storiesOf} from '@storybook/angular';
import {DbConnectionEditorModule} from './db-connection-editor.module';
import {DbConnectionService, ConnectionDriverConfigOptions, MappedTable, TableMetadata} from './db-importer.service';
import {QualifiedName} from '../services/schema';
import {testSchema} from '../object-view/test-schema';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {TuiRootModule} from '@taiga-ui/core';


const stringType: QualifiedName = {
  name: 'lang.taxi.String',
  fullyQualifiedName: 'lang.taxi.String',
  longDisplayName: 'lang.taxi.String',
  shortDisplayName: 'String',
  namespace: 'lang.taxi',
  parameterizedName: 'lang.taxi.String',
  parameters: []
};

const intType: QualifiedName = {
  name: 'lang.taxi.Int',
  fullyQualifiedName: 'lang.taxi.Int',
  longDisplayName: 'lang.taxi.Int',
  shortDisplayName: 'Int',
  namespace: 'lang.taxi',
  parameterizedName: 'lang.taxi.Int',
  parameters: []
};

const mappedTables: MappedTable[] = [
  {
    table: {
      tableName: 'Customers',
      schemaName: 'Public'
    }, mappedTo: stringType
  },
  {
    table: {
      tableName: 'Address',
      schemaName: 'Public'
    }, mappedTo: null
  }
];
const dbConnectionParams: ConnectionDriverConfigOptions[] = [
  {displayName: 'H2', driverName: 'H2', parameters: [], connectorType: 'JDBC'},
  {
    displayName: 'Postgres',
    driverName: 'POSTGRES',
    connectorType: 'JDBC',
    parameters: [{
      displayName: 'host',
      dataType: 'STRING',
      defaultValue: null,
      sensitive: false,
      required: true,
      visible: true,
      templateParamName: 'host',
      allowedValues: []
    }, {
      displayName: 'port',
      dataType: 'NUMBER',
      defaultValue: 5432,
      sensitive: false,
      required: true,
      visible: true,
      templateParamName: 'port',
      allowedValues: []
    }, {
      displayName: 'database',
      dataType: 'STRING',
      defaultValue: null,
      sensitive: false,
      required: true,
      visible: true,
      templateParamName: 'database',
      allowedValues: []
    }, {
      displayName: 'user',
      dataType: 'STRING',
      defaultValue: null,
      sensitive: false,
      required: false,
      visible: true,
      templateParamName: 'user',
      allowedValues: []
    }, {
      displayName: 'password',
      dataType: 'STRING',
      defaultValue: null,
      sensitive: true,
      required: false,
      visible: true,
      templateParamName: 'password',
      allowedValues: []
    }]
  }];

storiesOf('Db Connection Editor', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [DbConnectionEditorModule, HttpClientTestingModule, RouterTestingModule, TuiRootModule],
      providers: [DbConnectionService]
    })
  ).add('Connection editor', () => ({
    template: `
<tui-root>
<div style="padding: 40px; width: 100%;">
<app-connection-editor [drivers]="drivers"></app-connection-editor>
</div>
</tui-root>`,
    props: {
      drivers: dbConnectionParams
    }
  }))
  .add('Table importer', () => ({
      template: `<div style="padding: 40px; width: 100%;">
<app-table-importer
[tableMetadata]="table" [schema]="schema"></app-table-importer>
</div>`,
      props: {
        schema: testSchema,
        table: {
          connectionName: 'myDbConnection',
          schemaName: 'TestSchema',
          tableName: 'People',
          mappedType: null,
          columns: [
            { name: 'id', columnSpec: { columnName: 'id', dataType: 'int', nullable: false} , typeSpec: null },
            { name: 'firstName', columnSpec: {columnName: 'firstName', dataType: 'varchar',  nullable: false} , typeSpec: null },
            { name: 'lastName', columnSpec: {columnName: 'lastName', dataType: 'varchar', nullable: false} , typeSpec: null  },
            { name: 'email', columnSpec: {columnName: 'email', dataType: 'varchar',  nullable: true }, typeSpec: null  }
          ]
        } as TableMetadata
      }
    }))
  .add('Connection type selector', () => ({
      template: `<div style="padding: 40px; width: 100%;">
<app-connection-type-selector></app-connection-type-selector>
</div>`
    }))
  .add('Table selector', () => ({
      template: `<div style="padding: 40px; width: 100%;">
<app-table-selector [tables]="mappedTables"></app-table-selector>
</div>`,
      props: {
        mappedTables
      }
    }))
;
