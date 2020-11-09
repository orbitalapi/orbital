import {moduleMetadata, storiesOf} from '@storybook/angular';
import {ObjectViewComponent} from '../object-view/object-view.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {LineageDisplayModule} from './lineage-display.module';
import {LINEAGE_GRAPH} from './lineage-data';

storiesOf('Lineage display', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, LineageDisplayModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px">
    <app-lineage-display
    [dataSource]="lineageGraph"
    [instance]="typeNamedInstance"
    ></app-lineage-display>
    </div>`,
    props: {
      lineageGraph: LINEAGE_GRAPH,
      typeNamedInstance: {
        'typeName': 'demo.RewardsBalance',
        'value': 2300
      }
    }
  };
});

