/// <reference types = "cypress" />
import '@testing-library/cypress/add-commands';
import { homePageUrl, apiQueryHistory } from '../page-objects/Pages';

export class Action {
    onThePage(page: string) {
        return cy.url().should('be.include', page);
    }

    switchTab(tab: string) {
        return cy.get(tab).click();
    }

    type(text: string, field: string) {
        cy.get(field).click().clear().should('be.empty');
        cy.get(field).click().type(text, { delay: 80 });
    }

    textExist(text: string) {
        cy.findByText(text).should('exist');
    }

    elementText(element: string, text: string, condition: string) {
        cy.get(element).invoke('text').should(condition, text);
    }

    chooseItem(field: string, item: string) {
        cy.get(field).should('be.visible').click();
        cy.get(item).should('be.visible').click();
    }

    elementVisible(button: string) {
        return cy.get(button).should('be.visible');
    }

    goTo(page: string) {
        return cy.visit(page);
    }

    elementAppear(element: string) {
        return cy.get(element, { timeout: 15000 }).should('be.visible');
    }

    textAppear(field: string, text: string) {
        return cy.contains(field, text, { timeout: 12000 });
    }

    clickButton(button: string) {
        return cy.get(button).click();
    }

    clickPanel(button: string) {
        return cy.get(button).click({ force: true });
    }

    getText(item: string) {
        cy.get(item).invoke('text').then(innerText => {
            let itemText: string = innerText
            cy.wrap(itemText).as('itemText')
        });
        return cy.get('@itemText'); // this function can't return the text , it returns an 'alias' which contains the text info
    }

    clickByText(text: string) {
        return cy.findByText(text).click();
    }

    ensureScrollable(item: string) { // vertical only
        return cy.get(item).scrollTo('bottom').scrollTo('top');
    }

    getItemByIndex(list: string, index: number) {
        cy.get(`${list}:nth-child(${index})`).click();
    }

    downloadStatusCheckAs(fileType: string) {  // fileType -> JSON, CSV 
        cy.request({ method: 'GET', url: homePageUrl + apiQueryHistory }).then((response) => {
            expect(response.status).to.eq(200);
            expect(response.body).to.not.be.null;

            // And I get clientQueryId to create file export path 
            const clientQueryId: string = response.body[0]['clientQueryId'];
            const exportPath: string = `/api/query/history/clientId/${clientQueryId}/${fileType}/export`

            // Then I should see download  has been successful
            cy.request({ method: 'GET', url: homePageUrl + exportPath }).then((response) => {
                expect(response.status).to.eq(200);
            })
        })
    }

    dropFile(dropzone: string, fileName: string) {// put all the files required for file-upload-tests inside cypress/fixtures folder 
        cy.fixture(fileName, 'base64')
            .then(Cypress.Blob.base64StringToBlob)
            .then(blob => {

                const testFile = new File([blob], fileName);
                const event = { dataTransfer: { files: [testFile] } };
                return cy.get(dropzone).trigger('drop', event);
            })
    }
}
export default Action