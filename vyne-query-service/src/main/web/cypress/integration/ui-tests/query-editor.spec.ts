import '@testing-library/cypress/add-commands';
import { homePageUrl, queryBuilder } from '../page-objects/Pages';
import { runButton, lineageCloseButton, objectView, callsTabCloseButton, operationListButton, runnerProgressBar, runnerCancelButton, gridView, runnerTimer } from '../page-objects/Buttons';
import { callResult, profilerSchema, choosenSchema } from '../page-objects/Fields';
import Action from '../actions/Action';
import QueryWizardPageObjects from '../page-objects/QueryWizard';
const action = new Action();
const queryBuilderPage = new QueryWizardPageObjects.QueryBuilderPage();
const queryEditorPage = new QueryWizardPageObjects.QueryEditorPage();

describe('Vyne Ui Test Scenario - Query Editor', () => {
    beforeEach(() => {
        action.goTo(homePageUrl + queryBuilder);
        action.onThePage(queryBuilder);
        action.clickByText('Query Editor');
        queryEditorPage.makeQuery(choosenSchema);
    });

    it('Displays progress while query is running', () => {
        action.elementVisible(runnerProgressBar);
    });

    it('Can cancel a running query', () => {
        action.clickButton(runnerCancelButton);
        action.textAppear(runnerTimer, 'Cancelling');
    });

    it('Can build and execute a query', () => {
        action.elementAppear(runButton); // run button appears after data parsing
    });
});

describe('Query Editor', () => {
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
});
