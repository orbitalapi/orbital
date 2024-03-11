import { Component, DestroyRef, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject } from 'rxjs';
import { Documented } from '../../services/schema';

@Component({
  selector: 'app-description-editor',
  template: `
    <div
      [class.is-editable]="(editable && showHeader) || (editable && !isEditModeOn && !showHeader)"
      (click)="editable && !isEditModeOn ? isEditModeOn = true : null"
      [attr.title]="editable && !isEditModeOn ? 'Click to edit' : null"
    >
      <div *ngIf="showHeader" class="title-row">
        <h4>Description</h4>
        <div class="spacer"></div>
        <div class="info-warning visible-on-changes" [class.has-changes]="hasChanges">
          <img src="assets/img/tabler/info-circle.svg">
          <span>You have unsaved changes</span>
        </div>
      </div>
      <tui-textarea
        *ngIf="isEditModeOn"
        [formControl]="descriptionControl"
        (change)="pushChange($event)"
        [expandable]="true"
        (focusout)="isEditModeOn = false"
        tuiTextfieldCleaner
        tuiTextfieldLabelOutside
        tuiAutoFocus
      ></tui-textarea>
      <div class="button-bar" *ngIf="showControlBar && isEditModeOn">
        <button mat-button (click)="cancelChanges()">Cancel</button>
        <button mat-raised-button color="primary" (click)="saveChanges()">Save changes</button>
      </div>
      <markdown
        *ngIf="!isEditModeOn"
        [data]="descriptionSource.typeDoc ? descriptionSource.typeDoc : editable ? 'No description here yet, click to edit.' : 'No description here yet.'"
        [class.no-description]="!descriptionSource.typeDoc"
      ></markdown>
    </div>
  `,
  styleUrls: ['./description-editor.component.scss'],
})
export class DescriptionEditorComponent implements OnInit, OnChanges {
  @Input()
  showHeader = true;

  @Input()
  showControlBar = true; // implied commitMode = 'implicit'

  @Input()
  editable: boolean;

  @Input()
  descriptionSource: Documented = {typeDoc: ''};

  @Output()
  save: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  valueChanged = new EventEmitter<string>();

  isEditModeOn = false;

  descriptionControl!: UntypedFormControl;

  changes$ = new BehaviorSubject<string>(null);

  constructor(readonly destroyRef: DestroyRef) {}

  get hasChanges(): boolean {
    return this.showControlBar && this.editable && this.descriptionControl.value !== this.descriptionSource.typeDoc;
  }

  ngOnInit(): void {
    this.descriptionControl = new UntypedFormControl(this.descriptionSource.typeDoc);

    this.changes$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        if (value !== null) this.valueChanged.emit(value);
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.descriptionSource && !changes.descriptionSource.firstChange && changes.descriptionSource.previousValue !== changes.descriptionSource.currentValue) {
      this.descriptionControl.reset(this.descriptionSource.typeDoc);
    }
  }

  cancelChanges(): void {
    this.changes$.next(this.descriptionSource.typeDoc);
    this.isEditModeOn = false;
    this.descriptionControl.reset(this.descriptionSource.typeDoc);
  }

  saveChanges() {
    this.save.emit(this.changes$.value);
    this.descriptionSource.typeDoc = this.changes$.value
    this.isEditModeOn = false;
  }

  pushChange($event: any): void {
    this.changes$.next($event.target.value);
  }
}
