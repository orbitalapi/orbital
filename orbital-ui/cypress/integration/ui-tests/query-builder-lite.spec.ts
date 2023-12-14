import '@testing-library/cypress/add-commands';
import { homePageUrl, queryBuilder } from '../page-objects/Pages';
import { downloadButton, objectView, gridView } from '../page-objects/Buttons';
import { gather, choosenSchema } from '../page-objects/Fields';
import Action from '../actions/Action'
import QueryWizardPageObjects from '../page-objects/QueryWizard';
import QueryHistoryPageObjects from '../page-objects/QueryHistory';
const action = new Action();
const queryBuilderPage = new QueryWizardPageObjects.QueryBuilderPage();
const queryHistoryPage = new QueryHistoryPageObjects.QueryHistoryPage();

describe('Vyne Ui Test Scenario - Query Builder Lite', () => {
    before(() => {
        action.goTo(homePageUrl + queryBuilder);
        action.onThePage(queryBuilder);
    });

    it('Can build and execute a query', () => {
        queryBuilderPage.discover(choosenSchema, gather);
        action.elementAppear(downloadButton);
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

    it('Query Record Check', () => {
        queryHistoryPage.queryRecordCheck();
    });
});


