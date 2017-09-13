import {
  TestBed,
  inject,
  async,
  ComponentFixture,
} from '@angular/core/testing';
import { APP_BASE_HREF } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { XHRBackend, Response, ResponseOptions } from '@angular/http';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MockBackend } from '@angular/http/testing';
import { SharedModule } from '../../shared/shared.module';

import { TdLoadingService } from '@covalent/core';
import { CovalentHttpModule } from '@covalent/http';

import { UsersModule } from '../../users/users.module';
import { UserService, IUser } from '../../users/services/user.service';
import { ProductOverviewComponent } from './overview.component';

import { NgxChartsModule } from '@swimlane/ngx-charts';

import { Observable } from 'rxjs/Observable';

describe('Component: ProductOverview', () => {

  let noop: () => void = () => {
    // noop method
  };

  let generalResponses: Map<string, Response> = new Map<string, Response>();
  generalResponses.set('assets/icons/covalent.svg', new Response(new ResponseOptions({
    status: 200, body: '<svg></svg>',
  })));
  generalResponses.set('assets/icons/github.svg', new Response(new ResponseOptions({
    status: 200, body: '<svg></svg>',
  })));

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ProductOverviewComponent,
      ],
      imports: [
        SharedModule,
        UsersModule,
        CovalentHttpModule.forRoot(),
        RouterTestingModule,
        NgxChartsModule,
        NoopAnimationsModule,
      ],
      providers: [
        MockBackend,
        { provide: APP_BASE_HREF, useValue: '/' },
        TdLoadingService,
        { provide: XHRBackend, useExisting: MockBackend },
      ],
    });
    TestBed.compileComponents();
  }));

  it('should create the component', () => {
    inject([MockBackend], (mockBackend: MockBackend) => {
      let responses: Map<string, Response> = new Map<string, Response>(generalResponses);
      mockBackend.connections.subscribe((connection: any) => {
        connection.mockRespond(responses.get(connection.request.url));
      });
      responses.set('data/items.json', new Response(new ResponseOptions({
        status: 200, body: JSON.stringify([{
          item_id: 1,
          name: 'Suzy Cuningham',
          description: 'updated her account',
          icon: 'account_circle',
          created : '07/13/2016 11:05 AM',
        }, {
          item_id: 2,
          name: 'Bobby Daniels',
          description: 'made a deposit of $25.15',
          icon: 'account_balance_wallet',
          created : '07/01/2016 03:41 PM',
        }]),
      })));
      responses.set('data/users.json', new Response(new ResponseOptions({
        status: 200, body: JSON.stringify([{
          displayName : 'Suzy Cuningham',
          id : 'suzy.cuningham',
          email : 'suzy.cuningham@gmail.com',
          created : '10/01/2015 11:05 AM',
          lastAccess : '12/23/2015 11:05 AM',
          siteAdmin: true,
        }, {
          displayName : 'Bobby Daniels',
          id : 'bobbyD',
          email : 'bobbyD@outlook.com',
          created : '10/01/2015 11:05 AM',
          lastAccess : '12/23/2015 11:05 AM',
          siteAdmin: false,
        }]),
      })));

      let fixture: ComponentFixture<any> = TestBed.createComponent(ProductOverviewComponent);
      let testComponent: ProductOverviewComponent = fixture.componentInstance;
      let element: HTMLElement = fixture.nativeElement;

      expect(element.querySelector('.item-list')).toBeTruthy();
      expect(element.querySelector('.user-list')).toBeTruthy();
      testComponent.ngOnInit();
      fixture.detectChanges();

      expect(element.querySelectorAll('md-nav-list.item-list a[md-list-item]').length)
      .toBe(testComponent.items.length);

      expect(element.querySelectorAll('md-list.user-list md-list-item').length)
        .toBe(testComponent.users.length);
    })();
  });
});
