import '@testing-library/cypress/add-commands';
import Action from '../actions/Action'
import { catalogFilter, name } from '../page-objects/Buttons';
import DataCatalogPageObjects from '../page-objects/DataCatalog';
import { filterName, line } from '../page-objects/Fields';
import { homePageUrl, serviceExplorerUrl } from '../page-objects/Pages';
import ServiceExplorerPageObjects from '../page-objects/ServiceExplorer';
const action = new Action();
const serviceExplorer = new ServiceExplorerPageObjects.ServiceExplorer();


describe('Vyne Ui Test Scenario - Service Explorer', () => {
    before(() => {
        action.goTo(homePageUrl + serviceExplorerUrl);
    })

    it('Can see a list of operations', () => {
        serviceExplorer.checkListItems();
    });

    it('Can drill into an operation', () => {
        action.clickByText('getAll');
    });

    it('Can invoke an operation in the browser and see the results', () => {
        action.clickByText('Try it out');
        action.clickByText('Submit');
        // can't view result 
    });
});
