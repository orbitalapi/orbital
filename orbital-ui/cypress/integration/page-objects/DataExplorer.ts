/// <reference types = "cypress" />
import { dropInput } from './Fields';

namespace DataExplorerObjects {
    export class DataExplorer {

        uploadFile(file: string) {
            return cy.get(dropInput).attachFile({ filePath: file }, { force: true });
        }

        tableItemExist(field: string,) {// field -> row, column
            cy.get(field).each((item) => {
                expect(item).to.exist
            });
        }

        seeContent(field: string, content: string, click: boolean) {
            if (click == true) {
                cy.get(field).first().should('be.visible').click()
                    .invoke('text').should('equal', content);
            }
            else if (click == false) {
                cy.get(field).first().should('be.visible')
                    .invoke('text').should('equal', content);
            }
        }
    }
}
export default DataExplorerObjects