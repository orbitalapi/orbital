import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { TuiDialogContext } from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';

@Component({
  selector: 'app-subscribe-dialog',
  templateUrl: './subscribe-dialog.component.html',
  styleUrls: ['./subscribe-dialog.component.scss']
})
export class SubscribeDialogComponent implements OnInit {

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<boolean>) {}

  subscribeForm = new FormGroup({
    emailControl: new FormControl(``),
  });

  ngOnInit(): void {
  }

  onSubmit() {
    console.log("submitted")
  }

}
