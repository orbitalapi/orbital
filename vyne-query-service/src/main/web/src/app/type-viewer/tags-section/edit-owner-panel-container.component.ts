import {Component, Inject, OnInit} from '@angular/core';
import {QualifiedName, Type} from '../../services/schema';
import {TypesService} from '../../services/types.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {UserInfoService, VyneUser} from '../../services/user-info.service';
import {DATA_OWNER_TAG_OWNER_USER_ID, findDataOwner} from '../../data-catalog/data-catalog.models';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-edit-owner-panel-container',
  template: `
    <app-edit-owner-panel [selectedOwner]="selectedUser" [availableUsers]="availableUsers"
                          (save)="saveUser($event)"
                          (cancel)="dialogRef.close()"
    ></app-edit-owner-panel>
  `,
  styleUrls: ['./edit-owner-panel-container.component.scss']
})
export class EditOwnerPanelContainerComponent {

  availableUsers: VyneUser[];
  selectedUser: VyneUser;

  errorMessage: string;

  constructor(private typeService: TypesService,
              private userInfoService: UserInfoService,
              private snackBar: MatSnackBar,
              public dialogRef: MatDialogRef<EditOwnerPanelContainerComponent>,
              @Inject(MAT_DIALOG_DATA) public type: Type) {
    userInfoService.getAllUsers()
      .subscribe(users => {
        this.availableUsers = users;
        const ownerMetadata = findDataOwner(type.metadata);
        if (isNullOrUndefined(ownerMetadata)) {
          this.selectedUser = null;
        } else {
          this.selectedUser = this.availableUsers.find(user => user.userId === ownerMetadata.params[DATA_OWNER_TAG_OWNER_USER_ID]);
        }
      });
  }

  saveUser(user: VyneUser) {
    this.typeService.setTypeDataOwner(this.type, user)
      .subscribe(result => {
          this.snackBar.open(`Data owner for ${this.type.name.shortDisplayName} updated successfully`, 'Dismiss', {
            duration: 5000
          });
        },
        error => {
          console.error('Failed to save data owner: ' + JSON.stringify(error));
          this.errorMessage = error.message;
        });
  }
}
