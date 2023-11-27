import {ChangeDetectorRef, Component, Inject} from '@angular/core';
import {UntypedFormControl, UntypedFormGroup, Validators} from '@angular/forms';
import {TuiDialogContext} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';
import {PLAUSIBLE_ANALYTICS} from 'src/voyager-app/plausible';
import {SubscriptionResult, VoyagerService} from 'src/voyager-app/voyager.service';

@Component({
  selector: 'app-subscribe-dialog',
  templateUrl: './subscribe-dialog.component.html',
  styleUrls: ['./subscribe-dialog.component.scss']
})
export class SubscribeDialogComponent {

  subscribed = false
  subscriptionResult: SubscriptionResult = SubscriptionResult.UNKNOWN

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<boolean>,
    private voyagerService: VoyagerService,
    @Inject(PLAUSIBLE_ANALYTICS) private plausible: any,
    private ref: ChangeDetectorRef
  ) {}

  subscribeForm = new UntypedFormGroup({
    email: new UntypedFormControl('', [Validators.email, Validators.required]),
    otherCommsCheckbox: new UntypedFormControl(false)
  });

  onSubmit(data) {
    this.plausible.trackEvent("newsletter subscribe")
    this.voyagerService.subscribeToEmails({
      email: data.email,
      otherCommsConsent: data.otherCommsCheckbox
    }).subscribe(response => {
      this.subscribed = true;
      this.subscriptionResult = response.result;
      this.ref.markForCheck();
    }, error => {
      this.subscribed = true;
      this.subscriptionResult = SubscriptionResult.FAILED;
      this.ref.markForCheck();
    });

  }

  closeDialog() {
    this.context.completeWith(false)
  }

  subscribeSuccess() {
    return this.subscriptionResult == SubscriptionResult.SUCCESS;
  }

  alreadySubscribed() {
    return this.subscriptionResult == SubscriptionResult.ALREADY_SUBSCRIBED;
  }
}
