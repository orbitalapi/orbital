import '@testing-library/cypress/add-commands';
import { homePageUrl, queryBuilder } from '../page-objects/Pages';
import { runButton, objectView, runnerProgressBar, runnerCancelButton, gridView, runnerTimer } from '../page-objects/Buttons';
import { choosenSchema } from '../page-objects/Fields';
import Action from '../actions/Action'
import QueryWizardPageObjects from '../page-objects/QueryWizard';
const action = new Action();
const queryBuilderPage = new QueryWizardPageObjects.QueryBuilderPage();
const queryEditorPage = new QueryWizardPageObjects.QueryEditorPage();

describe('Vyne Ui Test Scenario - Query Editor Lite', () => {
    beforeEach(() => {
        action.goTo(homePageUrl + queryBuilder);
        action.onThePage(queryBuilder);
        action.clickByText('Query Editor');
        queryEditorPage.makeQuery(choosenSchema);
    });

    it('Displays progress while query is running', () => {
        action.elementAppear(runnerProgressBar);
    });

    it('Can cancel a running query', () => {
        action.clickButton(runnerCancelButton);
        action.textAppear(runnerTimer, 'Cancelling')
    });

    it('Can build and execute a query', () => {
        action.elementAppear(runButton); // run button appears after data parsing
    });
});

describe('Query Editor Lite', () => {
    before(() => {
        action.goTo(homePageUrl + queryBuilder);
        action.onThePage(queryBuilder);
        action.clickByText('Query Editor');
        queryEditorPage.makeQuery(choosenSchema);
    });

    it('Can download results as CSV', () => {
        queryBuilderPage.downloadAs('as CSV');
        action.downloadStatusCheckAs('CSV');
    });

    it('Displays results in grid', () => {
        queryBuilderPage.gridCheck();
    });

    it('Displays results in Object view', () => {
        action.elementVisible(objectView);
        action.clickButton(objectView);
        queryBuilderPage.objectViewCheck(gridView);
    });
});
