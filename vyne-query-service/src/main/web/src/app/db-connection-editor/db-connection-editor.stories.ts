import {moduleMetadata, storiesOf} from '@storybook/angular';
import {DbConnectionEditorModule} from './db-connection-editor.module';
import {TableMetadata} from './db-importer.service';
import {QualifiedName} from '../services/schema';
import {testSchema} from '../object-view/test-schema';

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
storiesOf('Db Connection Editor', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [DbConnectionEditorModule]
    })
  ).add('Connection editor', () => {
  return {
    template: `<div style="padding: 40px; width: 100%;">
<app-db-connection-editor></app-db-connection-editor>
</div>`
  };
})
  .add('Table importer', () => {
    return {
      template: `<div style="padding: 40px; width: 100%;">
<app-table-importer [tableMetadata]="table" [schema]="schema"></app-table-importer>
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
  });
