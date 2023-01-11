import { Component, Inject, OnInit } from '@angular/core';
import { TypesService } from '../../services/types.service';
import { Metadata, QualifiedName, setOrReplaceMetadata, Type } from '../../services/schema';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CommitMode } from '../type-viewer.component';
import { TypeEditorService } from 'src/app/services/type-editor.service';


export interface EditTagsPanelParams {
  readonly type: Type
  readonly commitMode: CommitMode
}


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
  selectedTags: Metadata[];
  errorMessage: string;

  constructor(private typeService: TypesService,
              private typeEditorService: TypeEditorService,
              private snackBar: MatSnackBar,
              public dialogRef: MatDialogRef<EditTagsPanelContainerComponent>,
              @Inject(MAT_DIALOG_DATA) public params: EditTagsPanelParams) {
    typeService.getAllMetadata()
      .subscribe(metadata => {
        this.availableTags = metadata;
      });
    this.selectedTags = params.type.metadata || [];
  }


  saveTags($event: Metadata[]) {
    this.dialogRef.close(this.params.type);
    this.updateMetadataOnType($event);
    if (this.params.commitMode === 'immediate') {
      this.commitTags($event);
    }
  }

  private updateMetadataOnType($event: Metadata[]) {
    $event.forEach(metadata => setOrReplaceMetadata(this.params.type, metadata));
  }

  private commitTags($event: Metadata[]) {
    this.typeEditorService.setTypeMetadata(this.params.type, this.params.type.metadata)
      .subscribe(result => {
          this.snackBar.open(`Tags for ${this.params.type.name.shortDisplayName} updated successfully`, 'Dismiss', {
            duration: 5000
          });
          this.dialogRef.close();
          // Optimistically update the metadata on the type.  Next time we reload from the schema, this should be there.
          this.updateMetadataOnType($event);
        },
        error => {
          console.error('Failed to save tags: ' + JSON.stringify(error));
          this.errorMessage = error.message;
        });
  }
}
