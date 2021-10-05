import {Component, Input, OnInit} from '@angular/core';
import {Metadata, Type} from '../../services/schema';
import {DATA_OWNER_FQN, DATA_OWNER_TAG_OWNER_NAME, findDataOwner} from '../../data-catalog/data-catalog.models';
import {MatDialog} from '@angular/material/dialog';
import {EditTagsPanelContainerComponent} from './edit-tags-panel-container.component';
import {EditOwnerPanelContainerComponent} from './edit-owner-panel-container.component';

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
  type: Type;

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
    this.dialogService.open(EditTagsPanelContainerComponent, {
      data: this.type
    });
  }

  editOwner() {
    this.dialogService.open(EditOwnerPanelContainerComponent, {
      data: this.type
    });
  }
}
