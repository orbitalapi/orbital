import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {DataCatalogModule} from '../data-catalog.module';
import {SearchResult} from '../../search/search.service';
import {fqn} from '../../services/schema';
import {RouterTestingModule} from '@angular/router/testing';
import {DATA_OWNER_FQN} from '../data-catalog.models';

// tslint:disable-next-line:max-line-length
const lorem = [`Yeah but George, Lorraine wants to go with you. Give her a break. You'll find out in thirty years. A block passed Maple, that's John F. Kennedy Drive. Looks like a airplane, without wings. No no no this sucker's electrical, but I need a nuclear reaction to generate the one point twenty-one gigawatts of electricity that I need.`,
  ``,
  `What a nightmare. Jesus, George, it's a wonder I was ever born. Where? How's your head? Lorenzo, where're you keys?`,
  ``,
  `Doc? Yeah, I'll keep that in mind. C'mon, Mom, make it fast, I'll miss my bus. Hey see you tonight, Pop. Woo, time to change that oil. Wow, you must be rich. Shape up, man. You're a slacker. You wanna be a slacker for the rest of your life?`]
  .join('\n');

const searchResults: SearchResult[] = [
  {
    qualifiedName: fqn('com.foo.EmailAddress'),
    typeDoc: lorem,
    matches: [],
    memberType: 'TYPE',
    metadata: [
      {
        name: fqn(DATA_OWNER_FQN),
        params: {
          name: 'Mickey Stones'
        }
      },
      {
        name: fqn('sensitive'),
        params: {}
      },
      {
        name: fqn('gdpr'),
        params: {}
      },
    ],
    consumers: [fqn('io.vyne.demos.rewards.CustomerService@@getCustomerByEmail'), fqn('com.bar.ServiceB'),],
    producers: [fqn('com.bar.ServiceA'), fqn('com.bar.ServiceB'),]
  },
  {
    qualifiedName: fqn('com.foo.EmailAddress'),
    typeDoc: lorem,
    matches: [],
    memberType: 'TYPE',
    metadata: [],
    consumers: [fqn('com.bar.ServiceA'), fqn('com.bar.ServiceB')],
    producers: [fqn('com.bar.ServiceA'), fqn('com.bar.ServiceB')]
  },
  {
    qualifiedName: fqn('com.foo.EmailAddress'),
    typeDoc: lorem,
    matches: [],
    memberType: 'TYPE',
    metadata: [],
    consumers: [fqn('com.bar.ServiceA'), fqn('com.bar.ServiceB')],
    producers: [fqn('com.bar.ServiceA'), fqn('com.bar.ServiceB')]
  }
];

storiesOf('Data catalog search', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, RouterTestingModule, BrowserAnimationsModule, DataCatalogModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 40px">
<app-data-catalog-search [searchResults]="searchResults"></app-data-catalog-search>
    </div>`,
      props: {
        searchResults
      }
    };
  });
