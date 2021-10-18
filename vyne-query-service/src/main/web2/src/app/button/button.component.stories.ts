import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ButtonModule} from './button.module';

storiesOf('buttons', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, ButtonModule]
    })
  )
  .add('default', () => {
    return {
      template: `
<div style="padding: 40px; display: flex;">
<app-button type="tertiary" style="margin: 3rem">Tertiary</app-button>
<app-button icon="Check" type="tertiary" style="margin: 3rem">Tertiary with icon</app-button>
<app-button type="tertiary" [enabled]="false" style="margin: 3rem">Tertiary disabled</app-button>
<app-button type="tertiary" size="small" style="margin: 3rem">Tertiary small</app-button>
<app-button type="tertiary" size="small" style="margin: 3rem" icon="Check">Tertiary small withicon</app-button>
<app-button type="tertiary" size="small" [enabled]="false" style="margin: 3rem">Tertiary small disabled</app-button>
</div>
<div style="padding: 40px; display: flex;">
<app-button type="default"  style="margin: 3rem">Default</app-button>
<app-button icon="Check" type="default" style="margin: 3rem">Default with icon</app-button>
<app-button type="default"  [enabled]="false" style="margin: 3rem">Default disabled</app-button>
<app-button type="default"  size="small" style="margin: 3rem">Default small</app-button>
<app-button icon="Check"  size="small" type="default" style="margin: 3rem">Default small with icon</app-button>
<app-button type="default"  size="small" [enabled]="false" style="margin: 3rem">Default small disabled</app-button>
</div>
<div style="padding: 40px; display: flex;">
<app-button type="primary" style="margin: 3rem">Primary</app-button>
<app-button icon="Check" type="primary" style="margin: 3rem">Primary with icon</app-button>
<app-button type="primary" style="margin: 3rem" [enabled]="false">Primary disabled</app-button>
<app-button type="primary" style="margin: 3rem" size="small">Primary small</app-button>
<app-button type="primary" style="margin: 3rem" size="small" icon="Check">Primary small with icon</app-button>
<app-button type="primary" style="margin: 3rem" size="small" [enabled]="false">Primary small disabled</app-button>
</div>

`,
      props: {}
    };
  });
