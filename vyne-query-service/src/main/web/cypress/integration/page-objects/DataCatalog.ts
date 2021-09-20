/// <reference types = "cypress" />
import { markedResult, searchBar } from './Fields';

namespace DataCatalogPageObjects {
    export class DataCatalog {

        search(text: string, execute: boolean) {
            if (execute == true) {
                cy.get(searchBar).click().focused().clear().should('be.empty').type(text);
                cy.get(markedResult).click();
            }
            if (execute == false) {
                cy.get(searchBar).click().focused().clear().should('be.empty').type(text);
            }
            else {
                cy.log('missing parameter');
            }
        }

        checkListItems() {
            cy.get('[class="cdk-virtual-scroll-content-wrapper"]').children('[class="search-result ng-star-inserted"]').its('length').then(size => {
                expect(size).not.to.eq(0);
            })
        }

        checkListItemBy(filterType: string, filterText: string) {// filter type -> namespace, name. filter text -> searched item text
            cy.get(filterType).each((item) => {
                let text: string = item.text().toLowerCase();
                expect(text).to.contain(filterText.toLowerCase());
            })
        }

        documentCheck() {
            cy.get('p').each((item) => {
                let text: string = item.text().toLowerCase()
                expect(text.length).not.to.eq(0);
            })
        }
        getLinks() {
            cy.get('li[class="ng-star-inserted"]').contains('Links').click();
        }

        addLinkGraphItem() {
            cy.get('[class="node ng-star-inserted"]').contains('MEMBER').first().click({ force: true });
            cy.wait(1000);
            cy.get('[class="text-path"]').contains('Has attribute').should('exist');
        }
    }
}
export default DataCatalogPageObjects