import '@testing-library/cypress/add-commands';
import Action from '../actions/Action';
import { entrType, namespace, type, catalogFilter, name } from '../page-objects/Buttons';
import DataCatalogPageObjects from '../page-objects/DataCatalog';
import { filterName, filterNamespace, link, operationName, operationNamespace, searchResult, searchText, typeName } from '../page-objects/Fields';
import { homePageUrl } from '../page-objects/Pages';
const action = new Action();
const dataCatalog = new DataCatalogPageObjects.DataCatalog();


describe('Vyne Ui Test Scenario - Data catalog', () => {
    beforeEach(() => {
        action.goTo(homePageUrl);
    })
    it('Can see a list of types', () => {
        dataCatalog.checkListItems();
    });

    it('Can filter by namespace and operation type', () => {
        action.clickButton(catalogFilter);
        action.type(filterNamespace, namespace);
        action.chooseItem(entrType, type);
        dataCatalog.checkListItemBy(operationNamespace, filterNamespace);
    });

    it('Can filter by name and operation type', () => {
        action.clickButton(catalogFilter);
        action.type(filterName, name);
        action.chooseItem(entrType, type);
        dataCatalog.checkListItemBy(operationName, filterName);
    });

    it('Can filter by name and namespace', () => {
        action.clickButton(catalogFilter);
        action.type(filterName, name);
        action.type(filterNamespace, namespace);
        dataCatalog.checkListItemBy(operationNamespace, filterNamespace);
        dataCatalog.checkListItemBy(operationName, filterName);
    });

    it('Check search bar results', () => {
        dataCatalog.search(searchText, false);
        dataCatalog.checkListItemBy(searchResult, searchText);
    });

    it('Can search from the search bar', () => {
        dataCatalog.search(searchText, true);
    });

    it('Selecting a search result takes me to the results page', () => {
        dataCatalog.search(searchText, true);
        action.elementAppear(link);
        action.getText(typeName).should('be.equal', searchText);
    });
});






