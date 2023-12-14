import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ExpandingPanelSetModule} from './expanding-panel-set.module';
import {TuiButtonModule} from '@taiga-ui/core';

storiesOf('Expanding panelset', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule,TuiButtonModule, BrowserModule, BrowserAnimationsModule, ExpandingPanelSetModule]
    })
  )
  .add('header', () => {
    return {
      template: `<div style="padding: 40px">
<app-panel-header title="Code">
    <button tuiButton size="s">Run</button>
</app-panel-header>
    </div>`,
      props: {}
    };
  })
  .add('panelset', () => {
    return {
      template: `<div style="padding: 40px">
<app-panelset style="width: 400px;" >
<app-panel title="Catalog" icon="assets/img/tabler/vocabulary.svg">
<div>Hello, from Catalog</div>

</app-panel>
<app-panel title="History" icon="assets/img/tabler/history.svg">
<div>Hello, from History</div>

</app-panel>
</app-panelset>
    </div>`,
      props: {
      }

    }
  })


;
