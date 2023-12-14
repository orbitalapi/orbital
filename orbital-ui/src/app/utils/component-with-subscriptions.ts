import {Directive, OnDestroy} from '@angular/core';
import {Subscription} from 'rxjs';

@Directive()
export class ComponentWithSubscriptions implements OnDestroy {
  private subscriptionsToDestroy: Subscription[] = [];

  unsubscribeOnClose(subscription: Subscription): Subscription {
    this.subscriptionsToDestroy.push(subscription);
    return subscription;
  }

  unsubscribeAllNow() {
    this.subscriptionsToDestroy.forEach((sub) => sub.unsubscribe());
    this.subscriptionsToDestroy = [];
  }

  ngOnDestroy() {
    this.unsubscribeAllNow();
  }
}
