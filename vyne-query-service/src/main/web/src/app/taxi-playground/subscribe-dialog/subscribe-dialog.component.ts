import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { TuiDialogContext } from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';
import { PLAUSIBLE_ANALYTICS } from 'src/taxi-playground-app/plausible';
import { SubscribeSuccess, TaxiPlaygroundService } from 'src/taxi-playground-app/taxi-playground.service';

@Component({
  selector: 'app-subscribe-dialog',
  templateUrl: './subscribe-dialog.component.html',
  styleUrls: ['./subscribe-dialog.component.scss']
})
export class SubscribeDialogComponent {

  subscribed = false
  subscriptionResult: SubscribeSuccess = SubscribeSuccess.UNKNOWN

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<boolean>,
    private taxiPlaygroundService: TaxiPlaygroundService,
    @Inject(PLAUSIBLE_ANALYTICS) private plausible: any,
    private ref: ChangeDetectorRef
  ) {}

  subscribeForm = new FormGroup({
    email: new FormControl(''),
    otherCommsCheckbox: new FormControl(false)
  });

  onSubmit(data) {
    this.plausible.trackEvent("newsletter subscribe")
    this.taxiPlaygroundService.subscribeToEmails({
      email: data.email,
      otherCommsConsent: data.otherCommsCheckbox
    }).subscribe(result => {
      this.subscribed = true;
      this.subscriptionResult = result;
      this.ref.markForCheck();
    }, error => {
      this.subscribed = true;
      this.subscriptionResult = SubscribeSuccess.FAILED;
      this.ref.markForCheck();
    }); 
    
  }

  closeDialog() {
    this.context.completeWith(false)
  }

  subscribeSuccess() {
    return this.subscriptionResult == SubscribeSuccess.SUCCESS;
  }
}
