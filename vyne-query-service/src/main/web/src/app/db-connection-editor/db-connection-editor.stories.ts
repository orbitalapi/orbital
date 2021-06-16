import {moduleMetadata, storiesOf} from '@storybook/angular';
import {DbConnectionEditorModule} from './db-connection-editor.module';
import {DbConnectionService, JdbcDriverConfigOptions, MappedTable, TableMetadata} from './db-importer.service';
import {QualifiedName} from '../services/schema';
import {testSchema} from '../object-view/test-schema';
import {MockBackend} from '@angular/http/testing';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {RouterTestingModule} from '@angular/router/testing';


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
const dbConnectionParams: JdbcDriverConfigOptions[] = [
  {'displayName': 'H2', driverName: 'H2', parameters: []},
  {
    'displayName': 'Postgres',
    driverName: 'POSTGRES',
    'parameters': [{
      'displayName': 'host',
      'dataType': 'STRING',
      'defaultValue': null,
      'sensitive': false,
      'required': true,
      'templateParamName': 'host',
      'allowedValues': []
    }, {
      'displayName': 'port',
      'dataType': 'NUMBER',
      'defaultValue': 5432,
      'sensitive': false,
      'required': true,
      'templateParamName': 'port',
      'allowedValues': []
    }, {
      'displayName': 'database',
      'dataType': 'STRING',
      'defaultValue': null,
      'sensitive': false,
      'required': true,
      'templateParamName': 'database',
      'allowedValues': []
    }, {
      'displayName': 'user',
      'dataType': 'STRING',
      'defaultValue': null,
      'sensitive': false,
      'required': false,
      'templateParamName': 'user',
      'allowedValues': []
    }, {
      'displayName': 'password',
      'dataType': 'STRING',
      'defaultValue': null,
      'sensitive': true,
      'required': false,
      'templateParamName': 'password',
      'allowedValues': []
    }]
  }];

storiesOf('Db Connection Editor', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [DbConnectionEditorModule, HttpClientTestingModule, RouterTestingModule],
      providers: [DbConnectionService]
    })
  ).add('Connection editor', () => {
  return {
    template: `<div style="padding: 40px; width: 100%;">
<app-db-connection-editor [drivers]="drivers"></app-db-connection-editor>
</div>`,
    props: {
      drivers: dbConnectionParams
    }
  };
})
  .add('Table importer', () => {
    return {
      template: `<div style="padding: 40px; width: 100%;">
<app-table-importer
[tableMetadata]="table" [schema]="schema"></app-table-importer>
</div>`,
      props: {
        schema: testSchema,
        table: {
          name: 'people',
          columns: [
            {name: 'id', dbType: 'int', taxiType: intType, nullable: false},
            {name: 'firstName', dbType: 'varchar', taxiType: stringType, nullable: false},
            {name: 'lastName', dbType: 'varchar', taxiType: stringType, nullable: false},
            {name: 'email', dbType: 'varchar', taxiType: stringType, nullable: true}
          ]
        } as TableMetadata
      }
    };
  })
  .add('Connection type selector', () => {
    return {
      template: `<div style="padding: 40px; width: 100%;">
<app-connection-type-selector></app-connection-type-selector>
</div>`
    };
  })
  .add('Table selector', () => {
    return {
      template: `<div style="padding: 40px; width: 100%;">
<app-table-selector [tables]="mappedTables"></app-table-selector>
</div>`,
      props: {
        mappedTables: mappedTables
      }
    };
  })
;
