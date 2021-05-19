import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CaskRowComponent} from './cask-row.component';
import {CaskViewerModule} from './cask-viewer.module';
import {SearchModule} from '../search/search.module';
import {RouterTestingModule} from '@angular/router/testing';

storiesOf('Cask Viewer', module)
  .addDecorator(
    moduleMetadata({
      imports: [CaskViewerModule, SearchModule, RouterTestingModule],
    })
  )
  .add('cask viewer', () => {
      return {
        template: `
        <div style="margin: 20px">
          <app-cask-viewer></app-cask-viewer>
        </div>
      `,
        props:
          {
            typeName: 'bank.orders.Order',
            caskConfigs: [{
              tableName: 'Order_2148b0_f29443',
              qualifiedTypeName: 'bank.orders.Order',
              versionHash: 'hash-213123',
              sourceSchemaIds: [],
              sources: [],
              deltaAgainstTableName: 'delta',
              insertedAt: '2012-12-12 12:12:12'
            }, {
              tableName: 'Order_2148b0_f29443',
              qualifiedTypeName: 'bank.orders.Order',
              versionHash: 'hash-213123',
              sourceSchemaIds: [],
              sources: [],
              deltaAgainstTableName: 'delta',
              insertedAt: '2012-12-12 12:12:12'
            }
            ]
          }
      };

    }
  )
  .add('cask rows', () => {
      return {
        template: `
        <div style="margin: 20px">
         <app-cask-row [typeName]="typeName" [caskConfigs]="caskConfigs"></app-cask-row>
         <app-cask-row [typeName]="typeName2" [caskConfigs]="caskConfigs2"></app-cask-row>
        </div>
      `,
        props:
          {
            typeName: 'bank.orders.Order',
            caskConfigs: [{
              tableName: 'Order_2148b0_f29443',
              qualifiedTypeName: 'bank.orders.Order',
              versionHash: 'hash-213123',
              sourceSchemaIds: [],
              sources: [],
              deltaAgainstTableName: 'delta',
              insertedAt: '2012-12-12 12:12:12'
            }, {
              tableName: 'Order_2148b0_f29443',
              qualifiedTypeName: 'bank.orders.Order',
              versionHash: 'hash-213123',
              sourceSchemaIds: [],
              sources: [],
              deltaAgainstTableName: 'delta',
              insertedAt: '2012-12-12 12:12:12'
            }
            ],
            typeName2: 'bank.trades.Trade',
            caskConfigs2: [{
              tableName: 'trade_2fcfaf_a9f3d8',
              qualifiedTypeName: 'bank.trades.Trade',
              versionHash: 'a9f3d8',
              sourceSchemaIds: [],
              sources: [],
              deltaAgainstTableName: 'delta',
              insertedAt: '2012-12-12 12:12:12'
            }
            ]
          }
      };

    }
  )
  .add('cask details', () => {
      return {
        template: `
        <div style="margin: 20px">
          <app-cask-details [caskConfig]="caskConfig"></app-cask-details>
        </div>
      `,
        props:
          {
            caskConfig: {
              tableName: 'Order_2148b0_f29443',
              qualifiedTypeName: 'bank.orders.Order',
              versionHash: 'hash-213123',
              sourceSchemaIds: [
                `bank/CounterParty:3:0:1`
              ],
              sources: [
                `
            import bank.common.party.LegalEntityIdentifier
            import bank.common.party.PartyName

            namespace bank.common.counterparty

            type CounterpartyName inherits PartyName
            type CounterpartyLegalEntityIdentifier inherits LegalEntityIdentifier
            `
              ],
              deltaAgainstTableName: 'delta',
              insertedAt: '2012-12-12 12:12:12',
              details: {
                recordsNumber: 190
              }
            }
          }
      };

    }
  );



