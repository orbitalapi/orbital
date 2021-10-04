import {Component, Inject, OnInit} from '@angular/core';
import {TypesService} from '../../services/types.service';
import {QualifiedName, Type} from '../../services/schema';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {NoCredentialsAuthToken} from '../../auth-mananger/auth-manager.service';

@Component({
  selector: 'app-edit-tags-panel-container',
  template: `
    <app-edit-tags-panel [availableTags]="availableTags" (cancel)="dialogRef.close()"
                         (save)="saveTags($event)" [errorMessage]="errorMessage"></app-edit-tags-panel>
  `
})
export class EditTagsPanelContainerComponent {
  availableTags: QualifiedName[];

  errorMessage: string;

  constructor(private typeService: TypesService,
              private snackBar: MatSnackBar,
              public dialogRef: MatDialogRef<EditTagsPanelContainerComponent>,
              @Inject(MAT_DIALOG_DATA) public type: Type) {
    typeService.getAllMetadata()
      .subscribe(metadata => {
        this.availableTags = metadata;
      });
  }


  saveTags($event: QualifiedName[]) {
    this.typeService.setTypeMetadata(this.type, $event)
      .subscribe(result => {
          this.snackBar.open(`Tags for ${this.type.name.shortDisplayName} updated successfully`, 'Dismiss', {
            duration: 5000
          });
        },
        error => {
          console.error('Failed to save tags: ' + JSON.stringify(error));
          this.errorMessage = error.message;
        });
  }

}
