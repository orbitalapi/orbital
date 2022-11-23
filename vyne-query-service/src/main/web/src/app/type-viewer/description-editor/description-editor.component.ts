import { Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { Documented } from '../../services/schema';
import { BehaviorSubject } from 'rxjs';
import { FormControl } from '@angular/forms';

@Component({
  selector: 'app-description-editor',
  template: `
    <div class="wrapper" *ngIf="editable">
      <div class="title-row">
        <h4>Description</h4>
        <mat-icon (click)="isEditModeOn = true" *ngIf="!isEditModeOn">edit</mat-icon>
        <div class="spacer"></div>
        <div class="info-warning visible-on-changes" [class.has-changes]="hasChanges">
          <img src="assets/img/tabler/info-circle.svg">
          <span>You have unsaved changes</span>
        </div>
      </div>
      <div *ngIf="isEditModeOn">
        <tui-text-area
          [formControl]="descriptionControl"
          (change)="pushChange($event)"
        ></tui-text-area>
      </div>
      <div class="button-bar" *ngIf="showControlBar && isEditModeOn">
        <button mat-button (click)="cancelChanges()">Cancel</button>
        <button mat-raised-button color="primary" (click)="saveChanges()">Save changes</button>
      </div>
      <markdown [data]="_documentationSource.typeDoc" *ngIf="!isEditModeOn"></markdown>
    </div>

  `,
  styleUrls: ['./description-editor.component.scss'],
})
export class DescriptionEditorComponent implements OnInit {
  @Input()
  showControlBar = true;

  @Input()
  editable: boolean;

  private _documentationSource: Documented;

  @Input()
  get documentationSource(): Documented {
    return this._documentationSource;
  }

  set documentationSource(value: Documented) {
    if (this._documentationSource === value) {
      return;
    }
    this._documentationSource = value;
    this.descriptionControl.reset(this._documentationSource.typeDoc);
  }

  @Input()
  placeholder: string;

  @Output()
  save: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  valueChanged = new EventEmitter<string>();

  isEditModeOn = false;

  descriptionControl!: FormControl;

  ngOnInit(): void {
    this.changes$.subscribe(value => {
      this.valueChanged.emit(value);
    });
    this.descriptionControl = new FormControl(this._documentationSource?.typeDoc)
  }


  get hasChanges(): boolean {
    return this.editable && this.descriptionControl.value !== this._documentationSource.typeDoc;
  }

  cancelChanges(): void {
    this.changes$.next(this._documentationSource.typeDoc);
    this.isEditModeOn = false;
    this.descriptionControl.reset(this._documentationSource.typeDoc);
  }

  changes$ = new BehaviorSubject<string>('');

  saveChanges() {
    this.save.emit(this.changes$.value);
    this.isEditModeOn = false;
  }

  pushChange($event: any): void {
    this.changes$.next($event.target.value);
  }
}
