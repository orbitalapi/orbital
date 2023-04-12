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
        <div class="form-row">
        <div class="form-item-description-container">
          <h3>Parameter Name</h3>
          <div class="help-text">
            Only Applicable to Request Header or Query Parameter based tokens. As an example for Header token type, to set the bearer token,
            the value of this field must be Authorization. To set an 'api-key' value with QueryParam token, the value of this field should be api-key.
          </div>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Parameter Name</mat-label>
          <input matInput formControlName="paramName">
        </mat-form-field>
      </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Value Prefix</h3>
            <div class="help-text">
             Optional value that will be prefixed to token value. As an example, when authentication type is set to Header
              and parameter Name is set to Authorization, setting value prefix as Bearer will enable bearer token authentication
              information to be included in request headers.
            </div>
          </div>
          <mat-form-field appearance="outline">
            <mat-label>Value Prefix</mat-label>
            <input matInput formControlName="valuePrefix">
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
    {label: authTokenTypeDisplayName(AuthTokenType.Header), value: AuthTokenType.Header},
    {label: authTokenTypeDisplayName(AuthTokenType.QueryParam), value: AuthTokenType.QueryParam},
    {label: authTokenTypeDisplayName(AuthTokenType.Cookie), value: AuthTokenType.Cookie}
  ];

  errorMessage: string = null;
  saving = false;

  newTokenFormGroup = new FormGroup({
    serviceName: new FormControl(null, Validators.required),
    tokenType: new FormControl(null, Validators.required),
    paramName: new FormControl(null, Validators.required),
    tokenValue: new FormControl(null, Validators.required),
    valuePrefix: new FormControl(null)
  });

  selectedService: QualifiedName;

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

  serviceSelected(member: SchemaMember): void {
    if (member) {
      this.newTokenFormGroup.get('serviceName').setValue(member.name.fullyQualifiedName);
    } else {
      this.newTokenFormGroup.get('serviceName').setValue(null);
    }

  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.saving = true;
    const formValues = this.newTokenFormGroup.getRawValue();
    const token: AuthToken = {
      tokenType: formValues.tokenType,
      value: formValues.tokenValue,
      paramName: formValues.paramName,
      valuePrefix: formValues.valuePrefix
    };
    this.authManagerService.saveToken(formValues.serviceName, token)
      .subscribe(result => {
        this.saving = false;
        this.snackBar.open('Token saved successfully', 'Dismiss', { duration: 3000 });
        this.dialogRef.close(result);
      }, error => {
        console.log('Failed to save token: ' + JSON.stringify(error));
        this.saving = false;
        this.errorMessage = error.message;
      });
  }
}
