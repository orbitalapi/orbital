import {Component, OnInit} from '@angular/core';
import {MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-config-disabled-form',
  template: `
    <h2>
      Downloading test specs is disabled
    </h2>
    <p>
      To create test cases, Vyne needs to store the responses from services it interacts with. Currently this is
      disabled.
    </p>
    <p>
      To enable, modify the
      <code>vyne.analytics.persistRemoteCallResponses</code> setting in your server config, setting to true.
      <code>vyne.analytics.persistResults</code> setting in your server config, setting to true.
    </p>
    <p>Once this is done, you'll need to re-start Vyne and re-run your query.</p>
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

}
