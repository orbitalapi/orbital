import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { TuiDialogContext } from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';
import { PlausibleOptions } from 'plausible-tracker';
import { PLAUSIBLE_ANALYTICS } from 'src/taxi-playground-app/plausible';
import { TaxiPlaygroundService } from 'src/taxi-playground-app/taxi-playground.service';

@Component({
  selector: 'app-subscribe-dialog',
  templateUrl: './subscribe-dialog.component.html',
  styleUrls: ['./subscribe-dialog.component.scss']
})
export class SubscribeDialogComponent {

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<boolean>,
    private taxiPlaygroundService: TaxiPlaygroundService,
    @Inject(PLAUSIBLE_ANALYTICS) private plausible: any
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
    }).subscribe();
  }
}
