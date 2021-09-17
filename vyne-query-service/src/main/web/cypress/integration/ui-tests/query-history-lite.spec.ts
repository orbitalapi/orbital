import '@testing-library/cypress/add-commands';
import { homePageUrl, queryBuilder, queryHistory } from '../page-objects/Pages';
import { runButton, queryHistoryButton, queryCancelButton, downloadButton } from '../page-objects/Buttons';
import { choosenSchema, disabledHistoryDisplay, queryHistoryList, queryHistoryProgressbar, queryDone, gather, discoverHistoryFirstItem } from '../page-objects/Fields';
import Action from '../actions/Action'
import QueryWizardPageObjects from '../page-objects/QueryWizard';
import QueryHistoryPageObjects from '../page-objects/QueryHistory';
const action = new Action();
const queryEditorPage = new QueryWizardPageObjects.QueryEditorPage();
const queryHistoryPage = new QueryHistoryPageObjects.QueryHistoryPage();
const queryBuilderPage = new QueryWizardPageObjects.QueryBuilderPage();

describe('Vyne Ui Test Scenario- Query History Lite - Query Builder', () => {
    beforeEach(() => {
        action.goTo(homePageUrl + queryBuilder);
        queryBuilderPage.discover(choosenSchema, gather);
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
        queryHistoryPage.discoverCheck(choosenSchema, 'gather');
    });

});

describe('Query History Lite - Query Editor ', () => {

    beforeEach(() => {
        action.goTo(homePageUrl + queryBuilder);
        action.clickByText('Query Editor');
        queryEditorPage.makeQuery(choosenSchema);
    });

    it('Live queries in query history show progress', () => {
        action.clickButton(queryHistoryButton);
        action.elementVisible(queryHistoryProgressbar);
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

describe('Query History Lite ', () => {
    beforeEach(() => {
        action.goTo(homePageUrl + queryHistory);
    });

    it('History shows scrollbars when lots of queries are present', () => {
        action.ensureScrollable(queryHistoryList);
    });

    it('Display of query results and profile data is disabled', () => {
        action.elementVisible(disabledHistoryDisplay);
    });

    it('Live queries are removed when the query completes', () => {
        action.elementAppear(queryDone);
        queryHistoryPage.queryRecordCheck();
    });
});

