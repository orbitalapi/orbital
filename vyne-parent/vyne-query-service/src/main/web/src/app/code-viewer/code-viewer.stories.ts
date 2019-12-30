import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {CodeViewerComponent} from './code-viewer.component';
import {CovalentHighlightModule} from '@covalent/highlight';
import {CovalentTabSelectModule} from "@covalent/core";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {MatTabsModule} from "@angular/material/tabs";

const code = `type Customer {
     email : CustomerEmailAddress
     id : CustomerId
     name : CustomerName
  }`;

const bigTaxi = `namespace demo {

   type Customer {
      email : CustomerEmailAddress
      id : CustomerId
      name : CustomerName
      postcode : Postcode
   }

   type alias CustomerEmailAddress as String
   type alias CustomerId as Int
   type alias CustomerName as String
   type alias Postcode as String

   enum CurrencyUnit {
      POINTS,
      GBP
   }

   type RewardsAccountBalance {
      balance : RewardsBalance
      cardNumber : RewardsCardNumber
      currencyUnit : CurrencyUnit
   }

   type alias RewardsBalance as Decimal

   type alias RewardsCardNumber as String
}

namespace io.vyne.demos.rewards {

   @ServiceDiscoveryClient(serviceName = "customer-service")
   service CustomerService {
      @HttpOperation(method = "GET" , url = "/customers/email/{demo.CustomerEmailAddress}")
      operation getCustomerByEmail(  demo.CustomerEmailAddress ) : demo.Customer
   }
}`;

const typescript = `import { Injectable } from '@angular/core';
    import { Observable, Subject } from 'rxjs';

    export interface Foo {
       name: string;
    }

    @Injectable()
    export class Service {

      private _sources: {[key : string]: Subject<any>} = {};
      private _observables: {[key: string]: Observable<any>} = {};

      constructor() {

      }

      public register(name) : Observable<any> {
        this._sources[name] = new Subject<any>();
        this._observables[name] = this._sources[name].asObservable();
        return this._observables[name];
      }

      public emit(name: string): void {
        if(this._sources[name]) {
          this._sources[name].next(null);
        }
      }
    }`;
storiesOf('CodeViewer', module)
  .addDecorator(
    moduleMetadata({
      declarations: [CodeViewerComponent],
      imports: [CommonModule, BrowserModule, CovalentHighlightModule, MatTabsModule, BrowserAnimationsModule]
    })
  ).add('default', () => {
  return {
    template: `<app-code-viewer [sources]="sources"></app-code-viewer>`,
    props: {
      sources: [
        {
          name: 'Definition published by CustomerService',
          code: bigTaxi,
          language: 'taxi'
        },
        {
          name: 'Documentation',
          code: typescript,
          language: 'typescript'
        }]
    }
  };
});

