import '@testing-library/cypress/add-commands';
import { homePageUrl, queryBuilder } from '../page-objects/Pages';
import { runButton, queryHistoryButton, queryCancelButton, lineageCloseButton, downloadButton } from '../page-objects/Buttons';
import { choosenSchema, queryHistoryList, queryHistoryProgressbar, queryDone, queryHistoryFirstItem, profilerSchema, discoverHistoryFirstItem, gather } from '../page-objects/Fields';
import Action from '../actions/Action'
import QueryWizardPageObjects from '../page-objects/QueryWizard';
import QueryHistoryPageObjects from '../page-objects/QueryHistory';
const action = new Action();
const queryBuilderPage = new QueryWizardPageObjects.QueryBuilderPage();
const queryEditorPage = new QueryWizardPageObjects.QueryEditorPage();
const queryHistoryPage = new QueryHistoryPageObjects.QueryHistoryPage();

describe('Vyne Ui Test Scenario - Query History - Enabled - Query Builder', () => {

    beforeEach(() => {
        action.goTo(homePageUrl + queryBuilder);
        queryBuilderPage.discover(choosenSchema, gather)
    });

    it('Live queries in query history show progress', () => {
        action.clickButton(queryHistoryButton);
        action.elementVisible(queryHistoryProgressbar);
        action.elementAppear(queryDone);
    });

    it('Live queries can be cancelled from query history', () => {
        action.clickButton(queryHistoryButton);
        action.clickButton(queryCancelButton);
        queryHistoryPage.queryCancelCheck();
    });

    it('History displays results ', () => {
        action.elementAppear(downloadButton);
        action.clickButton(queryHistoryButton);
        action.elementAppear(discoverHistoryFirstItem);
        action.elementAppear(queryDone);
        queryHistoryPage.discoverCheck(choosenSchema, 'gather');
    });

});

describe('Query History - Enabled - Query Editor', () => {
    beforeEach(() => {
        action.goTo(homePageUrl + queryBuilder);
        action.clickByText('Query Editor');
        queryEditorPage.makeQuery(choosenSchema);
    });

    it('Live queries in query history show progress', () => {
        action.clickButton(queryHistoryButton);
        action.elementVisible(queryHistoryProgressbar);
        action.elementAppear(queryDone);
    });

    it('Live queries can be cancelled from query history', () => {
        action.clickButton(queryHistoryButton);
        action.clickButton(queryCancelButton);
        queryHistoryPage.queryCancelCheck();
    });

    it('History displays results ', () => {
        action.elementAppear(runButton);
        action.clickButton(queryHistoryButton);
        queryHistoryPage.queryCheck();
    });
});

describe('Query History Enabled', () => {

    it('History shows scrollbars when lots of queries are present', () => {
        action.ensureScrollable(queryHistoryList);
    });

    it('Can download results as CSV', () => {
        action.clickButton(queryHistoryFirstItem);
        action.elementAppear(downloadButton);
        queryBuilderPage.downloadAs('as CSV');
        action.downloadStatusCheckAs('CSV')
    });

    it('And I click download as JSON', () => {
        queryBuilderPage.downloadAs('as JSON');
        action.downloadStatusCheckAs('JSON');
    });

    it('Lineage is available in results in query editor', () => {
        queryBuilderPage.displayLineage();
        action.elementAppear('h2');
        queryBuilderPage.displayLineageCheck();
        action.clickButton(lineageCloseButton);
    });

    it('History displays sequence diagrams', () => {
        action.clickByText('Profiler');
        action.elementAppear(profilerSchema);
    });

    it('Live queries are removed when the query completes', () => {
        action.elementAppear(queryDone);
        queryHistoryPage.queryRecordCheck();
    });
});
