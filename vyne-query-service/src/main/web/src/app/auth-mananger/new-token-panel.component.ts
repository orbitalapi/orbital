import {Component, Inject} from '@angular/core';
import {TypesService} from '../services/types.service';
import {Observable} from 'rxjs/internal/Observable';
import {QualifiedName, Schema, SchemaMember} from '../services/schema';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {
  AuthManagerService,
  AuthToken,
  AuthTokenType,
  authTokenTypeDisplayName,
  NoCredentialsAuthToken
} from './auth-manager.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-new-token-panel',
  template: `
    <h2>Configure a new authentication token</h2>
    <div class="form-container">
      <div class="form-body" [formGroup]="newTokenFormGroup">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Service name</h3>
            <div class="help-text">
              Select the service to apply an authentication token to
            </div>
          </div>
          <app-schema-member-autocomplete appearance="outline" label="Service name" [schema]="schema | async"
                                          schemaMemberType="SERVICE"
                                          [selectedMemberName]="selectedService"
                                          (selectedMemberChange)="serviceSelected($event)"></app-schema-member-autocomplete>
        </div>
        <div class="form-row">

          <div class="form-item-description-container">
            <h3>Authentication type</h3>
            <div class="help-text">
              Choose the authentication scheme this service uses
            </div>
          </div>
          <mat-form-field appearance="outline">
            <mat-label>Authentication type</mat-label>
            <mat-select formControlName="tokenType">
              <mat-option *ngFor="let authenticationType of authenticationTypes" [value]="authenticationType.value">
                {{authenticationType.label}}
              </mat-option>
            </mat-select>
          </mat-form-field>
        </div>
        <div class="form-row">

          <div class="form-item-description-container">
            <h3>Token value</h3>
            <div class="help-text">
              The authentication credentials to use that have been supplied by the service. This value won't be visible
              again after you save this form.
            </div>
          </div>
          <mat-form-field appearance="outline">
            <mat-label>Token value</mat-label>
            <input matInput formControlName="tokenValue">
          </mat-form-field>
        </div>
      </div>
      <div class="error-message-box" *ngIf="errorMessage">
        {{errorMessage}}
      </div>
      <div class="form-buttons">
        <button mat-stroked-button (click)="cancel()">Cancel</button>
        <div class="spacer"></div>
        <button mat-flat-button color="primary" [disabled]="!newTokenFormGroup.valid || saving" (click)="save()">Create
          token
        </button>
      </div>

    </div>
  `,

  styleUrls: ['./new-token-panel.component.scss']
})
export class NewTokenPanelComponent {
  schema: Observable<Schema>;

  authenticationTypes: { label: string; value: AuthTokenType }[] = [
    {label: authTokenTypeDisplayName('AuthorizationBearerHeader'), value: 'AuthorizationBearerHeader'}
  ];

  errorMessage: string = null;
  saving = false;

  newTokenFormGroup = new FormGroup({
    serviceName: new FormControl(null, Validators.required),
    tokenType: new FormControl(null, Validators.required),
    tokenValue: new FormControl(null, Validators.required)
  });

  constructor(private typeService: TypesService,
              private authManagerService: AuthManagerService,
              private snackBar: MatSnackBar,
              public dialogRef: MatDialogRef<NewTokenPanelComponent>,
              @Inject(MAT_DIALOG_DATA) public params: NoCredentialsAuthToken
  ) {
    this.schema = typeService.getTypes();

    if (params) {
      this.newTokenFormGroup.get('serviceName').setValue(params.serviceName);
      this.selectedService = QualifiedName.from(params.serviceName);
      this.newTokenFormGroup.get('tokenType').setValue(params.tokenType);
    }
  }

  selectedService: QualifiedName;

  serviceSelected(member: SchemaMember) {
    if (member) {
      this.newTokenFormGroup.get('serviceName').setValue(member.name.fullyQualifiedName);
    } else {
      this.newTokenFormGroup.get('serviceName').setValue(null);
    }

  }

  cancel() {
    this.dialogRef.close(null);
  }

  save() {
    this.saving = true;
    const formValues = this.newTokenFormGroup.getRawValue();
    const token: AuthToken = {
      tokenType: formValues.tokenType,
      value: formValues.tokenValue
    };
    this.authManagerService.saveToken(formValues.serviceName, token)
      .subscribe(result => {
        this.saving = false;
        this.snackBar.open('Token saved successfully', 'Dismiss', {duration: 3000});
        this.dialogRef.close(result);
      }, error => {
        console.log('Failed to save token: ' + JSON.stringify(error));
        this.saving = false;
        this.errorMessage = error.message;
      });
  }
}
