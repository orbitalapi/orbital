import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Metadata, NamedAndDocumented, Type} from '../../services/schema';
import {DATA_OWNER_FQN, DATA_OWNER_TAG_OWNER_NAME, findDataOwner} from '../../data-catalog/data-catalog.models';
import {MatDialog, MatDialogRef} from '@angular/material/dialog';
import {EditTagsPanelContainerComponent, EditTagsPanelParams} from './edit-tags-panel-container.component';
import {EditOwnerPanelContainerComponent, EditOwnerPanelParams} from './edit-owner-panel-container.component';
import {CommitMode} from '../type-viewer.component';

@Component({
  selector: 'app-tags-section',
  template: `
    <div class="row">
      <div class="title-row">
        <h4>Data owner</h4>
        <mat-icon (click)="editOwner()">edit</mat-icon>
      </div>
      <div>
        <span *ngIf="owner">
            <span class="metadata">{{ owner.params[dataOwnerTagOwnerName] }}</span>
        </span>
        <span *ngIf="!owner" class="subtle">No owner set</span>
      </div>
    </div>
    <div class="row">
      <div class="title-row">
        <h4>Tags</h4>
        <mat-icon (click)="editTags()">edit</mat-icon>
      </div>
      <div>
        <span *ngFor="let tag of otherMetadata" class="metadata">{{ tag.name.shortDisplayName }}</span>
        <span *ngIf="!otherMetadata || otherMetadata.length === 0" class="subtle">No tags</span>
      </div>
    </div>
  `,
  styleUrls: ['./tags-section.component.scss']
})
export class TagsSectionComponent {

  private _metadata: Metadata[];

  // To work around angular template visibility issues
  dataOwnerTagOwnerName = DATA_OWNER_TAG_OWNER_NAME;

  owner: Metadata;
  otherMetadata: Metadata[];

  constructor(private dialogService: MatDialog) {
  }

  @Input()
  commitMode: CommitMode = 'immediate';

  @Input()
  type: Type;

  /**
   * Emitted when the type has been updated, but not committed to the back-end.
   * (ie., when then commitMode = 'explicit')
   */
  @Output()
  updateDeferred = new EventEmitter<Type>();

  @Input()
  get metadata(): Metadata[] {
    return this._metadata;
  }

  set metadata(value: Metadata[]) {
    if (this._metadata === value) {
      return;
    }
    this._metadata = value;
    if (this.metadata) {
      this.owner = findDataOwner(this.metadata);
      this.otherMetadata = value.filter(m => m.name.fullyQualifiedName !== DATA_OWNER_FQN);
    }
  }


  editTags() {
    const dialogRef = this.dialogService.open(EditTagsPanelContainerComponent, {
      data: {
        type: this.type,
        commitMode: this.commitMode
      } as EditTagsPanelParams
    });
    this.subscribeForCloseEvent(dialogRef);
  }

  editOwner() {
    const dialogRef = this.dialogService.open(EditOwnerPanelContainerComponent, {
      data: {
        type: this.type,
        commitMode: this.commitMode
      } as EditOwnerPanelParams
    });
    this.subscribeForCloseEvent(dialogRef);
  }

  /**
   * The dialogs will emit the updated type if their commit mode was 'explicit'.
   */
  private subscribeForCloseEvent(dialogRef: MatDialogRef<any, Type>) {
    dialogRef.afterClosed().subscribe(type => {
      if (type !== null) {
        this.updateDeferred.emit(type);
      }
    })
  }
}
