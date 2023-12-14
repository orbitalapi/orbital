/// <reference types = "cypress" />
import { markedResult, matchedText, searchBar } from './Fields';

namespace ServiceExplorerPageObjects {
    export class ServiceExplorer {

        getFirstOperation() {
            cy.get('[class="ng-star-inserted"').children('td').children('a').first().click();
        }

        checkListItems() {
            cy.get('[class="operation-list"]').children('[class="ng-star-inserted"]').its('length').then(size => {
                expect(size).to.not.eq(0);
            })
        }
    }
}
export default ServiceExplorerPageObjects