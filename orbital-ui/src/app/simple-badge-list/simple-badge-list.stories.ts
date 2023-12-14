import { moduleMetadata, storiesOf } from '@storybook/angular';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SimpleBadgeListModule } from './simple-badge-list.module';
import { Badge } from './simple-badge-list.component';

storiesOf('Simple Badge List', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, SimpleBadgeListModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 40px">
<app-simple-badge-list [badges]="badges"></app-simple-badge-list>
    </div>`,
      props: {
        badges: [
          { label: 'Organisation', value: 'com.foo.bar', iconPath: 'assets/img/tabler/affiliate.svg' },
          { label: 'Version', value: '1.2.0', iconPath: 'assets/img/tabler/versions.svg' },
          { label: 'Last published', value: '1.2.0', iconPath: 'assets/img/tabler/clock.svg' },
        ] as Badge[]
      }
    };
  });
