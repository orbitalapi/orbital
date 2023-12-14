import '@testing-library/cypress/add-commands';
import Action from '../actions/Action';
import { catalogFilter, name } from '../page-objects/Buttons';
import DataCatalogPageObjects from '../page-objects/DataCatalog';
import { dataCatalogList, filterName, line } from '../page-objects/Fields';
import { homePageUrl } from '../page-objects/Pages';
const action = new Action();
const dataCatalog = new DataCatalogPageObjects.DataCatalog();


describe('Vyne Ui Test Scenario - Type Display', () => {
    before(() => {
        action.goTo(homePageUrl);
    })

    it('Can click to see type definition', () => {
        action.clickButton(catalogFilter);
        action.type(filterName, name);
        action.getItemByIndex(dataCatalogList, 2);
    });

    it('Can see the docs', () => {
        dataCatalog.documentCheck();
    });

    it('Can see the links of related types', () => {
        dataCatalog.getLinks();
        action.elementVisible(line);
    });

    it('Clicking on links in the UI adds them to the graph', () => {
        dataCatalog.addLinkGraphItem();
    });
});
