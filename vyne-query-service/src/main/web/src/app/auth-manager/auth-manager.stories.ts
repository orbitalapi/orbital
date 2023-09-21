import { moduleMetadata, storiesOf } from '@storybook/angular';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AuthManagerModule } from 'src/app/auth-manager/auth-manager.module';
import { of } from 'rxjs';
import { AuthTokenType } from 'src/app/auth-manager/auth-manager.service';

storiesOf('Authentication Manager', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, AuthManagerModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 40px; width: 100%; background-color: #F5F7F9;">
    <app-token-list [tokens$]="tokens" ></app-token-list>
    </div>`,
      props: {
        tokens: of(
          [
            { serviceName: 'Customer service', tokenType: AuthTokenType.Header },
            { serviceName: 'Cards service', tokenType: AuthTokenType.Header },
            { serviceName: 'Accounts service', tokenType: AuthTokenType.Header },
            { serviceName: 'Something service', tokenType: AuthTokenType.Header },
          ]
        )

      }
    };
  });
