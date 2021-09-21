/// <reference types = "cypress" />
import Action from '../actions/Action';
import CaskPageObjects from '../page-objects/Cask';
import { homePageUrl, queryHistory, cask, apiCask, apiQueryHistory } from './Pages';
import { queryHistoryButton, caskPanelItem, caskPanel } from './Buttons';
import { queryHistoryFirstItem, discoverHistoryFirstItem, choosenSchema, caskItems } from './Fields';
const action = new Action();
const caskPage = new CaskPageObjects.CaskPage();

namespace QueryHistoryPageObjects {

    export class QueryHistoryPage {

        queryRecordCheck() {
            action.goTo(homePageUrl + cask)
            action.clickPanel(caskPanel);
            action.elementAppear(caskItems);
            caskPage.getCaskItem(choosenSchema);
            cy.get('[class="detail mat-card"]').first().invoke('text').then(text => {
                const orderId: string = text.split(' ')[1];
                cy.request({ method: 'GET', url: homePageUrl + apiCask + `${orderId}/details` }).then(response => {
                    const recordsNumber: string = response.body['recordsNumber'];

                    action.clickButton(queryHistoryButton);
                    cy.request({ method: 'GET', url: homePageUrl + apiQueryHistory }).then(response => {
                        const recordCount: string = response.body[0]['recordCount'];
                        expect(recordsNumber).to.eq(recordCount);
                    })
                })

            })

        }

        queryCheck() {
            cy.get(queryHistoryFirstItem).invoke('text').then(text => { // executed query content
                const historyFirstItem: string = text;
                cy.wrap(historyFirstItem).as('historyFirstItem');
                action.clickButton(queryHistoryFirstItem); // after click item , queryId appends to Url
            })

            cy.request({ method: 'GET', url: homePageUrl + apiQueryHistory }).then(response => {
                const queryId: string = response.body[0]["queryId"];
                const executedQuery: string = response.body[0]["taxiQl"];
                const responseStatus: string = response.body[0]['responseStatus'];

                expect(responseStatus).to.eq('COMPLETED')
                cy.get('@historyFirstItem').should('be.equal', executedQuery);
                cy.url().should('be.equal', homePageUrl + queryHistory + queryId);
            })
        }

        discoverCheck(queryType: string, queryMode: string) { // Query mode* : discover, build, gather
            cy.get('[class = "verb"]').first().invoke('text').then((text) => { // check query mode
                expect(queryMode).to.eq(text.replace(' ', ''));

                cy.get('[class = "type-name"]:nth-child(2)').first().invoke('text').then((text) => { // check typename
                    expect(text).to.eq(queryType + '[]')
                });
            });

            cy.request({ method: 'GET', url: homePageUrl + apiQueryHistory }).then((response) => {
                const queryId: string = response.body[0]["queryId"];
                const responseStatus: string = response.body[0]['responseStatus'];
                expect(responseStatus).to.eq('COMPLETED');
                cy.wait(1000);
                action.clickButton(discoverHistoryFirstItem);
                cy.url().should('be.equal', homePageUrl + queryHistory + queryId);
            });
        }

        queryCancelCheck() { // different from QueryWizard.queryCancelCheck
            // url : /api/query/active/clientId/
            cy.request({ method: 'GET', url: homePageUrl + apiQueryHistory }).then(response => {
                const clientId: string = response.body[0]["clientQueryId"];

                cy.request({ method: 'DELETE', url: homePageUrl + '/api/query/active/' + `${clientId}` }).then(response => {
                    expect(response.status).to.eq(200);
                })
            })
        }
    }
}

export default QueryHistoryPageObjects