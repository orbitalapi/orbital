import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { Documented } from '../../services/schema';
import { BehaviorSubject } from 'rxjs';
import { UntypedFormControl } from '@angular/forms';

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
      <markdown [data]="documentationSource.typeDoc" *ngIf="!isEditModeOn"></markdown>
    </div>

  `,
  styleUrls: ['./description-editor.component.scss'],
})
export class DescriptionEditorComponent implements OnInit, OnChanges {
  @Input()
  showControlBar = true;

  @Input()
  editable: boolean;


  @Input()
  documentationSource: Documented;

  @Input()
  placeholder: string;

  @Output()
  save: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  valueChanged = new EventEmitter<string>();

  isEditModeOn = false;

  descriptionControl!: UntypedFormControl;

  changes$ = new BehaviorSubject<string>('');

  get hasChanges(): boolean {
    return this.editable && this.descriptionControl.value !== this.documentationSource.typeDoc;
  }

  ngOnInit(): void {
    this.descriptionControl = new UntypedFormControl(this.documentationSource.typeDoc);

    this.changes$.subscribe(value => {
      this.valueChanged.emit(value);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.documentationSource && !changes.documentationSource.firstChange && changes.documentationSource.previousValue !== changes.documentationSource.currentValue) {
      this.descriptionControl.reset(this.documentationSource.typeDoc);
    }
  }

  cancelChanges(): void {
    this.changes$.next(this.documentationSource.typeDoc);
    this.isEditModeOn = false;
    this.descriptionControl.reset(this.documentationSource.typeDoc);
  }

  saveChanges() {
    this.save.emit(this.changes$.value);
    this.isEditModeOn = false;
  }

  pushChange($event: any): void {
    this.changes$.next($event.target.value);
  }
}
