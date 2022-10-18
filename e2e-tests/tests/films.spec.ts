import { Page, test } from '@playwright/test';
import { AddDataSourcePage } from './pages/add-data-source.page';
import { DialogPage } from './pages/dialog.page';
import { SchemaImporterPage } from './pages/schema-importer.page';
import { QueryEditorPage } from './pages/query-editor.page';

async function waitForSchemaUpdate(page: Page): Promise<void> {
   await page.waitForTimeout(3000);
}

test.describe('Films demo', () => {
   test('works as described in the tutorial', async ({ page }) => {
      const addDataSourcePage = new AddDataSourcePage(page);
      await addDataSourcePage.goto();

      // 1. Import the database table
      await addDataSourcePage.openDropdown('Select a schema type to import');
      await addDataSourcePage.selectDropdownOption('Database table');

      await addDataSourcePage.openDropdown('Connection name');
      await addDataSourcePage.selectDropdownOption('Add new connection');

      const databaseConnectionDialog = new DialogPage(page);
      await databaseConnectionDialog.fillValue('Connection name', 'films');
      await databaseConnectionDialog.openDropdown('Connection type');
      await databaseConnectionDialog.selectDropdownOption('Postgres');
      await databaseConnectionDialog.fillValue('host', 'postgres');
      await databaseConnectionDialog.fillValue('database', 'pagila');
      await databaseConnectionDialog.fillValue('username', 'root');
      await databaseConnectionDialog.fillValue('password', 'admin');
      await databaseConnectionDialog.clickButton('Create');

      await addDataSourcePage.openDropdown('Table name');
      await addDataSourcePage.selectDropdownOption('film');

      await addDataSourcePage.fillValue('Default namespace', 'io.vyne.demos.films');

      await addDataSourcePage.clickButton('Create');
      const databaseSchemaImporterPage = new SchemaImporterPage(page);
      await databaseSchemaImporterPage.clickButton('Save');
      await databaseSchemaImporterPage.expectNotification('The schema was updated successfully');
      await waitForSchemaUpdate(page);

      // 2. Import the Swagger schema
      await addDataSourcePage.goto();
      await addDataSourcePage.openDropdown('Select a schema type to import');
      await addDataSourcePage.selectDropdownOption('Swagger / OpenAPI');
      await addDataSourcePage.selectTab('Url');
      await addDataSourcePage.fillValue('Swagger / OpenAPI URL', 'http://films-api/v2/api-docs');
      await addDataSourcePage.fillValue('Default namespace', 'io.vyne.demo.films');
      await addDataSourcePage.clickButton('Create');

      const swaggerSchemaImporterPage = new SchemaImporterPage(page);
      await swaggerSchemaImporterPage.openMenuSection('Services');
      await swaggerSchemaImporterPage.chooseMenuItem('getStreamingProvidersForFilmUsingGET');
      await swaggerSchemaImporterPage.selectTypeForParameter('filmId', 'film.types.FilmId');
      await swaggerSchemaImporterPage.clickButton('Save');
      await swaggerSchemaImporterPage.expectNotification('The schema was updated successfully');
      await waitForSchemaUpdate(page);

      // 3. Add Protobuf
      await addDataSourcePage.goto();
      await addDataSourcePage.openDropdown('Select a schema type to import');
      await addDataSourcePage.selectDropdownOption('Protobuf');
      await addDataSourcePage.selectTab('Url');
      await addDataSourcePage.fillValue('Url', 'http://films-api/proto');
      await addDataSourcePage.clickButton('Create');

      const protobufSchemaImporterPage = new SchemaImporterPage(page);
      await protobufSchemaImporterPage.openMenuSection('Models');
      await protobufSchemaImporterPage.chooseMenuItem('NewFilmReleaseAnnouncement');
      // TODO Not working ATM
      // await protobufSchemaImporterPage.selectTypeForParameter('filmId', 'film.types.FilmId');
      await protobufSchemaImporterPage.clickButton('Save');
      await protobufSchemaImporterPage.expectNotification('The schema was updated successfully');
      await waitForSchemaUpdate(page);

      // 4. Add Kafka
      await addDataSourcePage.goto();
      await addDataSourcePage.openDropdown('Select a schema type to import');
      await addDataSourcePage.selectDropdownOption('Kafka topic');

      await addDataSourcePage.openDropdown('Connection name');
      await addDataSourcePage.selectDropdownOption('Add new connection');

      const kafkaConnectionDialog = new DialogPage(page);
      await kafkaConnectionDialog.fillValue('Connection name', 'kafka');
      await kafkaConnectionDialog.fillValue('Broker address', 'kafka:29092');
      await kafkaConnectionDialog.clickButton('Create');

      await addDataSourcePage.setKafkaTopic('releases');
      await addDataSourcePage.openDropdown('Topic offset');
      await addDataSourcePage.selectDropdownOption('LATEST');
      await addDataSourcePage.fillValue('Default namespace', 'io.vyne.demos.announcements');
      await addDataSourcePage.selectOnAutoComplete('Message type', 'NewFilmReleaseAnnouncement');
      await addDataSourcePage.clickButton('Create');

      const kafkaSchemaImporterPage = new SchemaImporterPage(page);
      await kafkaSchemaImporterPage.clickButton('Save');
      await kafkaSchemaImporterPage.expectNotification('The schema was updated successfully');
      await waitForSchemaUpdate(page);

      // 5. Run queries
      const queryEditorPage = new QueryEditorPage(page);
      await queryEditorPage.goto();

      await queryEditorPage.runQuery('find { Film[] }');
      await queryEditorPage.selectResultTab('Table');
      await queryEditorPage.expectTableHeaders([
         'film_id',
         'title',
         'description'
      ]);
      await queryEditorPage.expectTableRow(0, [
         '1',
         'ACADEMY DINOSAUR',
         'A Epic Drama of a Feminist And a Mad Scientist who must Battle a Teacher in The Canadian Rockies'
      ]);

   });

});
