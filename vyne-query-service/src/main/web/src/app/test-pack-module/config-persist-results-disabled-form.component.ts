import {Component, OnInit} from '@angular/core';
import {MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-config-persists-results-disabled-form',
  template: `
    <h2>
      Downloading As Json is disabled
    </h2>
    <p>
      To create Json data, Vyne needs to store the relevant data during query execution. Currently this is
      disabled.
    </p>
    <p>
      To enable, modify the
      <code>vyne.history.persistResults</code> setting in your server config, setting to true.
    </p>
    <p>Once this is done, you'll need to re-start Vyne and re-run your query.</p>
    <div class="button-row">
      <div class="spacer"></div>
      <button mat-raised-button color="primary" (click)="this.dialogRef.close()">Close</button>
    </div>
  `,
  styleUrls: ['./test-spec-form.component.scss']
})
export class ConfigPersistResultsDisabledFormComponent implements OnInit {

  constructor(public dialogRef: MatDialogRef<ConfigPersistResultsDisabledFormComponent>) {
  }

  ngOnInit() {
  }

}
