import {Component, Inject, OnInit} from '@angular/core';
import {TypesService} from '../../services/types.service';
import {Metadata, QualifiedName, Type} from '../../services/schema';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {NoCredentialsAuthToken} from '../../auth-mananger/auth-manager.service';

@Component({
  selector: 'app-edit-tags-panel-container',
  template: `
    <app-edit-tags-panel [availableTags]="availableTags" (cancel)="dialogRef.close()"
                         [selectedTags]="selectedTags"
                         (save)="saveTags($event)" [errorMessage]="errorMessage"></app-edit-tags-panel>
  `
})
export class EditTagsPanelContainerComponent {
  availableTags: QualifiedName[];
  selectedTags: QualifiedName[];
  errorMessage: string;

  constructor(private typeService: TypesService,
              private snackBar: MatSnackBar,
              public dialogRef: MatDialogRef<EditTagsPanelContainerComponent>,
              @Inject(MAT_DIALOG_DATA) public type: Type) {
    typeService.getAllMetadata()
      .subscribe(metadata => {
        this.availableTags = metadata;
      });
    this.selectedTags = (type.metadata || []).map(m => m.name);
  }


  saveTags($event: QualifiedName[]) {
    this.typeService.setTypeMetadata(this.type, $event)
      .subscribe(result => {
          this.snackBar.open(`Tags for ${this.type.name.shortDisplayName} updated successfully`, 'Dismiss', {
            duration: 5000
          });
          this.dialogRef.close();
          // Optimistically update the metadata on the type.  Next time we reload from the schema, this should be there.
          this.type.metadata = $event.map(name => {
            return {
              name,
              params: {},
              typeDoc: null
            } as Metadata;
          });
        },
        error => {
          console.error('Failed to save tags: ' + JSON.stringify(error));
          this.errorMessage = error.message;
        });
  }

}
