import '@testing-library/cypress/add-commands';
import 'cypress-file-upload';
import { cask, dataExplorerUrl, homePageUrl } from '../page-objects/Pages';
import { caskItems, choosenSchema, endDate, startDate } from '../page-objects/Fields';
import Action from '../actions/Action';
import CaskPageObjects from '../page-objects/Cask';
import DataExplorerObjects from '../page-objects/DataExplorer';
import { fixtureDataCsv, selectType, monoBadge, caskStore, storeSuccess } from '../page-objects/Fields';
import { errorSearch } from '../page-objects/Buttons';
const action = new Action();
const caskPage = new CaskPageObjects.CaskPage();
const dataExplorer = new DataExplorerObjects.DataExplorer;

describe('Vyne Ui Test Scenario - Cask', () => {
    before(() => {
        action.goTo(homePageUrl + dataExplorerUrl);
        dataExplorer.uploadFile(fixtureDataCsv);
        action.type(choosenSchema, selectType);
        dataExplorer.seeContent(monoBadge, choosenSchema, true);
        action.clickPanel(caskStore);
        action.clickByText('Send');
        action.textAppear(storeSuccess, 'SUCCESS');
        action.goTo(homePageUrl + cask);
        caskPage.findSchemaByName(choosenSchema);
    })

    it('Displays lists of casks', () => {
        action.goTo(homePageUrl + cask);
        action.elementAppear(caskItems);
        caskPage.findSchemaByName(choosenSchema);// optional
        caskPage.getCaskItem(choosenSchema);
    });

    it('Can view ingestion errors for a cask', () => {
        action.clickByText('Ingestion Errors');
        cy.get(startDate).click().clear().type('5/5/2021');
        cy.get(endDate).click().clear().type('5/10/2021');
        action.clickButton(errorSearch);
        action.clickByText('Ingestion Errors');
    });

    it('Can delete a cask', () => {
        caskPage.deleteCask();
    });

    it('Schema is removed', () => {
        action.goTo(homePageUrl + cask);
        action.elementAppear(caskItems);
        caskPage.schemaRemoved(choosenSchema);
    });

    it('Can post to a cask', () => {
        action.goTo(homePageUrl + dataExplorerUrl);
        dataExplorer.uploadFile(fixtureDataCsv);
        action.type(choosenSchema, selectType);
        dataExplorer.seeContent(monoBadge, choosenSchema, true);
        action.clickPanel(caskStore);
        action.clickByText('Send');
        action.textAppear(storeSuccess, 'SUCCESS');
        action.clickByText('Send');
        action.textAppear(storeSuccess, 'SUCCESS');
        action.clickByText('Send');
        action.textAppear(storeSuccess, 'SUCCESS');
        action.clickByText('Send');
        action.textAppear(storeSuccess, 'SUCCESS');
        action.clickByText('Send');
        action.textAppear(storeSuccess, 'SUCCESS');
        action.goTo(homePageUrl + cask);
        caskPage.findSchemaByName(choosenSchema);
    });
});