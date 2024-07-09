import {Component, OnInit} from '@angular/core';
import {MatDialogRef} from '@angular/material/dialog';
import { UiCustomisations } from '../../environments/ui-customisations';

@Component({
  selector: 'app-config-disabled-form',
  template: `
    <h2>
      Downloading test specs is disabled
    </h2>
    <p>
      To create test cases, {{UiCustomisations.productName}} needs to store the responses from services it interacts with. Currently this is
      disabled.
    </p>
    <p>
      To enable, modify the
      <code>vyne.analytics.persistRemoteCallResponses</code> and <code>vyne.analytics.persistResults</code> settings in your server config, setting both values to <code>true</code>.
    </p>
    <p>Once this is done, you'll need to re-start {{UiCustomisations.productName}} and re-run your query.</p>
    <div class="button-row">
      <div class="spacer"></div>
      <button mat-raised-button color="primary" (click)="this.dialogRef.close()">Close</button>
    </div>
  `,
  styleUrls: ['./test-spec-form.component.scss']
})
export class ConfigDisabledFormComponent implements OnInit {

  constructor(public dialogRef: MatDialogRef<ConfigDisabledFormComponent>) {
  }

  ngOnInit() {
  }

  protected readonly UiCustomisations = UiCustomisations;
}
