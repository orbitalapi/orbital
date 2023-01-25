import { expect, Page, test } from '@playwright/test';
import { AddDataSourcePage } from './pages/add-data-source.page';
import { DialogPage } from './pages/dialog.page';
import { SchemaImporterPage } from './pages/schema-importer.page';
import { QueryEditorPage } from './pages/query-editor.page';
import { doPost } from './helpers/ui';

const useLocalHostNames = process.env.USE_LOCAL_HOSTNAMES === 'true';

async function waitForSchemaUpdate(page: Page): Promise<void> {
   await page.waitForTimeout(3000);
}

const localHostnames = {
   postgres: 'localhost',
   kafka: 'localhost',
   filmsApi: 'localhost:9981'
};

const dockerComposeHostnames = {
   postgres: 'postgres',
   kafka: 'kafka',
   filmsApi: 'films-api'
};

const hostnames = useLocalHostNames ? localHostnames : dockerComposeHostnames;

test.describe('Films demo', () => {
   test('works as described in the tutorial', async ({ page, request }) => {
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
      await databaseConnectionDialog.fillValue('host', hostnames.postgres);
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
      await addDataSourcePage.fillValue('Swagger / OpenAPI URL', `http://${hostnames.filmsApi}/v2/api-docs`);
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
      await addDataSourcePage.fillValue('Url', `http://${hostnames.filmsApi}/proto`);
      await addDataSourcePage.clickButton('Create');

      const protobufSchemaImporterPage = new SchemaImporterPage(page);
      await protobufSchemaImporterPage.openMenuSection('Models');
      await protobufSchemaImporterPage.chooseMenuItem('NewFilmReleaseAnnouncement');
      await protobufSchemaImporterPage.selectTypeForAttribute('FilmId', 'Int', 'film.types.FilmId');
      await protobufSchemaImporterPage.createNewType(
         'String',
         'AnnouncementMessage',
         'lang.taxi.String',
         'Stringlang.taxi.StringA collection of characters.');
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
      await kafkaConnectionDialog.fillValue('Broker address', `${hostnames.kafka}:9092`);
      const currentEpochTime = Math.round((new Date()).getTime() / 1000);
      await kafkaConnectionDialog.fillValue('Group Id', `${currentEpochTime}`);
      await kafkaConnectionDialog.clickButton('Create');

      await addDataSourcePage.setKafkaTopic('releases');
      await addDataSourcePage.openDropdown('Topic offset');
      await addDataSourcePage.selectDropdownOption('EARLIEST');
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

      // Streaming Query that will Join data from Kafka, API and DB.
      await addDataSourcePage.goto();
      await queryEditorPage.goto();
      await queryEditorPage.runQuery('stream { NewFilmReleaseAnnouncement } as {' +
         '    news: {' +
         '        announcement: AnnouncementMessage' +
         '    }' +
         '    film: {' +
         '        name: Title' +
         '        id : FilmId' +
         '        description: Description' +
         '    }' +
         '    productionDetails: {' +
         '        released: ReleaseYear' +
         '    }' +
         '    ' +
         '    providers: StreamingProvider[]' +
         '}[]');

      await queryEditorPage.expectStreamingQueryIsRunning();
      // Wait for the query sets up the kafka subscription, before we post the message to kafka
      // Otherwise, the offset on the subscription will be ahead of the last published message!
      await queryEditorPage.waitFor(5000);
      // push some data to kafka. We push it twice, as the first never been picked up!!
      await doPost(request,
         `http://${hostnames.filmsApi}/kafka/newReleases/1`,
         1,
         'topic',
         'releases')

      await queryEditorPage.selectResultTab('Table');
      await queryEditorPage.expectTableHeaders([
         'news',
         'film',
         'productionDetails',
         'providers'
      ], 5000);
      await queryEditorPage.expectTableRow(0, [
         'View nested structures in tree mode',
         'View nested structures in tree mode',
         'View nested structures in tree mode',
         'View collections in tree mode'
      ]);

      await queryEditorPage.selectResultTab('Tree');

      await queryEditorPage.expectHaveText([
         'Today, Netflix announced the reboot of yet another classic franchise',
         'ACADEMY DINOSAUR',
         'A Epic Drama of a Feminist And a Mad Scientist who must Battle a Teacher in The Canadian Rockies',
         '2006',
         'Disney Plus',
         '7.99',
         'Now TV',
         '13.99'
      ]);
   });
});
