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
      </div>
      <div class="info-warning visible-on-changes" [class.has-changes]="hasChanges">
        <img src="assets/img/info.svg">
        <span>You have unsaved changes</span>
      </div>
      <div *ngIf="isEditModeOn">
        <tui-text-area
            [formControl]="descriptionControl"
            (change)="pushChange($event)"
        >{{placeholder}}</tui-text-area>
      </div>
      <div class="button-bar visible-on-changes" [class.has-changes]="hasChanges" *ngIf="showControlBar && hasChanges">
        <button mat-button (click)="isEditModeOn = false">Cancel</button>
        <button mat-raised-button color="primary" (click)="saveChanges()">Save changes</button>
      </div>
    </div>
    <markdown [data]="documentationSource.typeDoc" *ngIf="!editable"></markdown>
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

  descriptionControl!: FormControl;

  ngOnInit(): void {
    this.changes$.subscribe(value => {
      this.valueChanged.emit(value);
    });
    this.descriptionControl = new FormControl(this.documentationSource?.typeDoc)
  }


  get hasChanges(): boolean {
    return this.editable && this.changes$.value !== '';
  }

  ngOnChanges(): void {
    this.changes$.next('');
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
