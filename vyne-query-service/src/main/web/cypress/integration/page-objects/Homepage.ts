/// <reference types = "cypress" />
import { matchedText, searchBar } from './Fields';

namespace HomePageObjects {
    export class HomePage {

        search(text: string) {
            cy.get(searchBar).click().focused().clear().should('be.empty').type(text)
            cy.get(matchedText).click();
        }
    }
}
export default HomePageObjects