import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {fqn, QualifiedName} from '../../services/schema';
import {FormBuilder} from '@angular/forms';

@Component({
  selector: 'app-edit-tags-panel',
  template: `
    <h2>Edit tags</h2>
    <div class="form-container">
      <div class="form-body">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Modify tags</h3>
            <div class="help-text">
              Set the tags to apply
            </div>
          </div>
          <ng-select [searchable]="true" bindLabel="shortDisplayName" labelForId="tags"
                     [addTag]="createNewTag"
                     class="local-material"
                     addTagText="Create a new tag"
                     [(ngModel)]="selectedTags"
                     [multiple]="true" placeholder="Select tags" [items]="availableTags">
          </ng-select>
        </div>
      </div>
      <div class="vertical-spacer"></div>
      <div class="error-message-box" *ngIf="errorMessage">
        {{errorMessage}}
      </div>
      <div class="form-buttons">
        <button mat-stroked-button (click)="cancel.emit()">Cancel</button>
        <div class="spacer"></div>
        <button mat-flat-button color="primary" (click)="save.emit(selectedTags)">Save tags
        </button>
      </div>
    </div>
  `,
  styleUrls: ['./edit-tags-panel.component.scss'],
})
export class EditTagsPanelComponent {

  @Input()
  availableTags: QualifiedName[];

  @Input()
  selectedTags: QualifiedName[];

  @Input()
  errorMessage: string;

  @Output()
  cancel = new EventEmitter();
  @Output()
  save = new EventEmitter<QualifiedName[]>();

  createNewTag(tag: string): QualifiedName {
    return fqn(tag);
  }
}
