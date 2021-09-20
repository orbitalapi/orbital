import '@testing-library/cypress/add-commands';
import 'cypress-file-upload';
import Action from '../actions/Action';
import DataExplorerObjects from '../page-objects/DataExplorer';
import CaskPageObjects from '../page-objects/Cask';
import { caskStore, dropzone, choosenSchema, dataSource, drillRelateItem, explorerGrid, fixtureDataCsv, fixtureDataJson, line, monoBadge, selectType, storeSuccess, typeBox, dropData } from '../page-objects/Fields';
import { homePageUrl, dataExplorerUrl, cask } from '../page-objects/Pages';
const action = new Action();
const dataExplorer = new DataExplorerObjects.DataExplorer;
const caskPage = new CaskPageObjects.CaskPage();


describe('Vyne Ui Test Scenario - Data Explorer', () => {
    beforeEach(() => {
        action.goTo(homePageUrl + dataExplorerUrl);
    })
    it('Can drag and drop a file', () => {
        action.dropFile(dropzone, dropData);
        action.elementVisible(dataSource).invoke('text').should('contain', dropData);
    });

    it('Can upload a CSV file, and see the contents', () => {
        dataExplorer.uploadFile(fixtureDataCsv);
        action.type(choosenSchema, selectType);
        dataExplorer.seeContent(monoBadge, choosenSchema, false);
    });

    it('Can post to a cask', () => {
        dataExplorer.uploadFile(fixtureDataCsv);
        action.type(choosenSchema, selectType);
        dataExplorer.seeContent(monoBadge, choosenSchema, true);
        action.clickPanel(caskStore);
        action.clickByText('Send');
        action.textAppear(storeSuccess, 'SUCCESS')
        action.goTo(homePageUrl + cask);
        caskPage.findSchemaByName(choosenSchema);
    });

    it('Can upload a JSON file', () => {
        dataExplorer.uploadFile(fixtureDataJson);
        action.elementVisible(dataSource).invoke('text').should('contain', fixtureDataJson);
    });
});

describe('Data Explorer', () => {
    before(() => {
        action.goTo(homePageUrl + dataExplorerUrl);
    })

    it('Can apply a schema, and see the parsed content', () => {
        dataExplorer.uploadFile(fixtureDataCsv);
        action.type(choosenSchema, selectType);
        dataExplorer.seeContent(monoBadge, choosenSchema, true);
        action.elementVisible(explorerGrid);
    });

    it('Parsed content shows types', () => {
        action.clickByText('Parsed data');
        dataExplorer.tableItemExist('[col-id="entryType"]');
    });

    it('Parsed content is clickable, and shows type-drill-down', () => {
        //queryBuilder.displayLineage();  // grid is broken *
        //queryBuilder.displayLineageCheck();
        cy.get('[col-id="identifierValue"]').first().click({ force: true }); // * IT'S TEMPORARY
        action.elementVisible(typeBox);
    });

    it('Type drill down shows related services', () => {
        action.elementVisible(line);
        action.elementText(drillRelateItem, 'inherits', 'contain');

    });

    it('Can invoke relates services from type drill-down and see results', () => {
        cy.get(typeBox).first().click();
        action.textExist('Data Catalogue');
        action.elementVisible(line);
    });
});




