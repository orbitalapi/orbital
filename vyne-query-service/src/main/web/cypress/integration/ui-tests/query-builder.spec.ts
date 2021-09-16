import '@testing-library/cypress/add-commands';
import { homePageUrl, queryBuilder } from '../page-objects/Pages';
import { operationListButton, lineageCloseButton, callsTabCloseButton, downloadButton, objectView, gridView } from '../page-objects/Buttons';
import { callResult, gather, profilerSchema, choosenSchema } from '../page-objects/Fields';
import Action from '../actions/Action';
import QueryWizardPageObjects from '../page-objects/QueryWizard';
import QueryHistoryPageObjects from '../page-objects/QueryHistory';
const action = new Action();
const queryBuilderPage = new QueryWizardPageObjects.QueryBuilderPage();
const queryHistoryPage = new QueryHistoryPageObjects.QueryHistoryPage();

describe('Vyne Ui Test Scenario - Query Builder', () => {
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

    it('And I click download as JSON', () => {
        queryBuilderPage.downloadAs('as JSON');
        action.downloadStatusCheckAs('JSON');
    });

    it('Displays results in grid', () => {
        queryBuilderPage.gridCheck();
    });

    it('Displays lineage when clicking on an item', () => {
        queryBuilderPage.displayLineage();
        action.elementAppear('h2'); // lineage panel
        queryBuilderPage.displayLineageCheck();
        action.clickButton(lineageCloseButton);
    });

    it('Displays results in Object view', () => {
        action.elementVisible(objectView);
        action.clickButton(objectView);
        queryBuilderPage.objectViewCheck(gridView);
    });

    it('Displays the remote operations', () => {
        action.clickByText('Profiler');
        action.getText(callResult).should('equal', '200'); // Call response check
        action.clickButton(callResult); // Call panel check
        action.clickButton(callsTabCloseButton);
        action.elementVisible(profilerSchema); // Sequence diagram check
        action.clickButton(operationListButton);
        queryBuilderPage.operationCheck(choosenSchema);
    });

    it('Query Record Check', () => {
        queryHistoryPage.queryRecordCheck();
    });
});
