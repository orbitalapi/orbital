/// <reference types = "cypress" />
import '@testing-library/cypress/add-commands';

namespace CaskPageObjects {
    export class CaskPage {

        findSchemaByName(choosenSchema: string) {
            return cy.findAllByText(choosenSchema).should('exist');
        }

        getCaskListItem(index: number, orderClick: boolean) {
            if (orderClick = true) {
                cy.get(`[id = "mat-expansion-panel-header-${index - 1}"]`).click();
                cy.get(`[id="cdk-accordion-child-${index - 1}"]`).click();
            }
            else if (orderClick = false) {
                cy.get(`[id = "mat-expansion-panel-header-${index - 1}"]`).click();
            }
            else {
                cy.log('orderClick parameter is missing');
            }
        }

        deleteCask() {
            cy.get('[class="results-actions"]').should('be.visible')
                .click();
            cy.findByText('Confirm').click();
            cy.wait(1000);
        }

        checkListItem(index: number) { // check order details with ID
            cy.get(`[id="cdk-accordion-child-${index - 1}"]`).invoke('text').then(text => {
                const orderText = text.replace(" ", "");

                cy.get('[class="results-details"]').children('[class="detail mat-card"]').first().invoke('text').then(text => {
                    const idText = text.split(' ')[1]
                    expect(idText).to.eq(orderText);
                })
            })
        }

        caskSizeEqualTo(caskSize: number) {
            if (caskSize > 0) {
                cy.get('[class="cask-list"]').children('[class="ng-star-inserted"]').its('length').then(size => {
                    expect(size).to.eq(caskSize);
                })
            }
            else if (caskSize == 0) {
                cy.get('[class="cask-list"]').children('[class="ng-star-inserted"]').should('not.exist');
            }
            else {
                cy.log('cask size cant be negative number ');
            }
        }
        getCaskSize() {
            cy.get('[class="cask-list"]').children('[class="ng-star-inserted"]').its('length').then(size => {
                let caskSize: number = size;
                cy.wrap(caskSize).as('casksize');
                return cy.get('@casksize');
            })
        }
        getCaskItem(choosenSchema: string) {
            let index: number = 0
            cy.get('[class="cask-typename"]').each((item, counter: number = 0) => {
                let text = item.text().split(' ')[0]

                var dict = {};
                dict['key'] = text;
                dict['value'] = counter;
                counter = counter + 1;

                if (dict['key'] == choosenSchema) {
                    index = dict['value'];
                    cy.get(`[id="mat-expansion-panel-header-${index}"]`).click()
                    cy.get(`[id="cdk-accordion-child-${index}"]`).click()
                }
            });
        }

        schemaRemoved(choosenSchema) {
            cy.get('[class="cask-typename"]').each((item) => {
                let text = item.text().split(' ')[0]
                expect(choosenSchema).not.to.equal(text);
            });

        }

    }
}

export default CaskPageObjects