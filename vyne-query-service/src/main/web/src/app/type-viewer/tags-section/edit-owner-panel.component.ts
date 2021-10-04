import {Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation} from '@angular/core';
import {VyneUser} from '../../services/user-info.service';

@Component({
  selector: 'app-edit-owner-panel',
  template: `
    <h2>Edit data owner</h2>
    <div class="form-container">
      <div class="form-body">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Modify owner</h3>
            <div class="help-text">
              A data owner helps users know who to talk to about data sets or attributes.
            </div>
          </div>
          <ng-select [searchable]="true" bindLabel="shortDisplayName" labelForId="tags"
                     [(ngModel)]="selectedOwner"
                     class="local-material"
                     [multiple]="false" placeholder="Select data owner" [items]="availableUsers">
          </ng-select>
        </div>
      </div>
      <div class="error-message-box" *ngIf="errorMessage">
        {{errorMessage}}
      </div>

      <div class="form-buttons">
        <button mat-stroked-button (click)="cancel.emit()">Cancel</button>
        <div class="spacer"></div>
        <button mat-flat-button color="primary" (click)="save.emit(selectedOwner)">Save tags
        </button>
      </div>
    </div>
  `,
  styleUrls: ['./edit-owner-panel.component.scss'],
})
export class EditOwnerPanelComponent  {

  @Input()
  availableUsers: VyneUser[];
  @Input()
  selectedOwner: VyneUser;


  @Input()
  errorMessage: string;

  @Output()
  cancel = new EventEmitter();
  @Output()
  save = new EventEmitter<VyneUser>();

}
