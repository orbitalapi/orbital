import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {PipelinesModule} from '../pipelines/pipelines.module';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {testSchema} from '../object-view/test-schema';
import {InheritanceGraphModule} from './inheritance-graph.module';
import {Inheritable} from './inheritance-graph.component';

storiesOf('Inheritence Graph', module)
  .addDecorator(
    moduleMetadata({
      imports: [InheritanceGraphModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px">
    <app-inheritance-graph [inheritable]="source"></app-inheritance-graph>
    </div>`,
    props: {
      source: {
        name: {
          name: 'Couch',
          namespace: 'house.livingRoom',
          fullyQualifiedName: 'house.livingRoom.Couch',
          parameterizedName: 'house.livingRoom.Couch',
          parameters: [],
          longDisplayName: 'house.livingRoom.Couch',
          shortDisplayName: 'Couch'
        },
        inheritsFrom: {
          name: {
            name: 'Seating',
            namespace: 'house.livingRoom',
            fullyQualifiedName: 'house.livingRoom.Seating',
            parameterizedName: 'house.livingRoom.Seating',
            parameters: [],
            longDisplayName: 'house.livingRoom.Seating',
            shortDisplayName: 'Seating'
          },
          inheritsFrom: {
            name: {
              name: 'Furniture',
              namespace: 'house.livingRoom',
              fullyQualifiedName: 'house.livingRoom.Furniture',
              parameterizedName: 'house.livingRoom.Furniture',
              parameters: [],
              longDisplayName: 'house.livingRoom.Furniture',
              shortDisplayName: 'Furniture'
            },
            inheritsFrom: null
          }
        }
      } as Inheritable
    }
  };
});
