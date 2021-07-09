/// <reference types = "cypress" />
import { runButton, submitQueryButton, findAsArray } from './Buttons';
import { objectViewItem, grid, queryModeSelect, targetToFind, choosenSchema } from './Fields';
import { homePageUrl, apiQueryHistory, apiClient } from './Pages';
import Action from '../actions/Action';

const action = new Action();

namespace QueryWizardPageObjects {

    export class QueryBuilderPage {

        discover(queryType: string, queryMode: string) { // executes query in Query Builder
            action.clickButton(targetToFind).type(queryType, {delay:80})
            action.elementAppear('[class="inline mono-badge"]');
            cy.get('[class="inline mono-badge"]').contains(choosenSchema).first().click();
            action.clickButton(findAsArray);
            action.clickButton(queryModeSelect);
            action.clickButton(queryMode);
            action.clickButton(submitQueryButton);
        }

        downloadAs(fileType: string) {
            action.clickByText('Download');
            action.clickByText(fileType);
        }

        displayLineage() {
            cy.get('div[class="ag-center-cols-container"]') // first item of the query result
                .children('[row-index="0"]').findAllByRole('gridcell').first().click();
        }

        gridCheck() {
            return cy.get(grid).should('be.visible');
        }

        displayLineageCheck() {
            // get first item of the query
            function getFirstItem() {
                return cy.get('div[class="ag-center-cols-container"]')
                    .children('[row-index="0"]').findAllByRole('gridcell').first().invoke('text').as("firstItem");
            }
            // lineage of the first item
            function getLineageItem() {
                return cy.get('h2').invoke('text').as("lineageItem");
            }
            getFirstItem();
            getLineageItem();
            cy.get("body").should(function () {
                expect(this.firstItem).to.eq(this.lineageItem)
            });
        }

        objectViewCheck(gridButton: string) {
            // get the first item of object view
            cy.get(objectViewItem).invoke('text').then(itemText => {
                let objectViewFirst = itemText;
                cy.wrap(objectViewFirst).as('objectViewFirst');

                // go back to grid 
                action.clickButton(gridButton);

                // get first item of the query 
                cy.get('div[class="ag-center-cols-container"]')
                    .children('[row-index="0"]').findAllByRole('gridcell').first().invoke('text').then(spanText => {
                        let firstItem = spanText;
                        cy.wrap(firstItem).as('firstItem');
                        // compare items
                        expect(objectViewFirst).to.eq(firstItem)
                    });
            })
        }

        operationCheck(choosenSchema: string) { // profiler operation
            cy.get('tr[class = "ng-star-inserted"]').children('td').children('div[class = "badges"]').invoke('text').then(text => {
                let divText: string = text
                expect(divText).to.contain(choosenSchema)
            });
        }
    }

    export class QueryEditorPage {

        makeQuery(query: string) {
            cy.get('.view-lines').click().focused().type('{selectall}{backspace}{selectall}{backspace}').should('be.empty'); // .clear() not working as expected
            cy.get('.view-lines').type(`findAll { ${query}[]`) // '}' is not missing, auto creation of curly brackets
                .get(runButton).click();
        }

        queryCancelCheck() {
            cy.request({ method: 'GET', url: homePageUrl + apiQueryHistory }).then(response => {
                const clientId: string = response.body[0]["clientQueryId"];

                cy.request({ method: 'DELETE', url: homePageUrl + apiClient + `${clientId}` }).then(response => {
                    expect(response.status).to.eq(200)
                });
            })
        }
    }
}

export default QueryWizardPageObjects