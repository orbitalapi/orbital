import {Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {Type} from '../../services/schema';
import * as ReactDOM from 'react-dom';
import {ContentSupplier, ReactEditorWrapper} from './description-editor.react';
import {BehaviorSubject} from 'rxjs';

// Links:
// https://stackoverflow.com/questions/55411994/how-to-use-react-component-in-angular-6-application
// https://stackoverflow.com/questions/54408694/connect-angular-application-with-reactjs-app/54408718#54408718
// https://github.com/qubiack/angular-reactjs

@Component({
  selector: 'app-description-editor',
  template: `
    <div class="wrapper">
<!--      <div class="info-warning visible-on-changes" [class.has-changes]="hasChanges">-->
<!--        <img src="assets/img/info.svg">-->
<!--        <span>You have unsaved changes</span>-->
<!--      </div>-->
      <div #container></div>
      <div class="button-bar visible-on-changes" [class.has-changes]="hasChanges" *ngIf="hasChanges">
        <button mat-button (click)="cancelEdits.emit()">Cancel</button>
        <button mat-raised-button color="primary" (click)="saveChanges()">Save changes</button>
      </div>
    </div>`,
  styleUrls: ['./description-editor.component.scss']
})
export class DescriptionEditorComponent implements OnInit, OnDestroy {
  private _type: Type;

  private changeEventCount: number;
  private lastChangeEvent: ContentSupplier;

  get hasChanges(): boolean {
    // We ignore the first change event, as it's the event triggered by
    // setting the initial state
    return this.changeEventCount > 1;
  }

  @Output()
  save: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  cancelEdits: EventEmitter<void> = new EventEmitter<void>();

  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    this._type = value;
    this.resetEditor();
  }

  private changes$ = new BehaviorSubject<ContentSupplier>(() => '');

  constructor() {
  }

  @ViewChild('container') containerRef: ElementRef;

  ngOnInit() {
    this.resetEditor();
    this.changes$.subscribe(next => {
      console.log('Has changes');
      this.changeEventCount++;
      this.lastChangeEvent = next;
    });
  }

  private resetEditor() {
    if (!this.containerRef) {
      console.error('ContainerREf not set - this looks like an angular lifecycle problem');
      return;
    }
    this.changeEventCount = 0;
    ReactEditorWrapper.initialize(this.containerRef,
      {
        changes$: this.changes$,
        initialState: this.initialState,
        placeholder: `Write something great that describes a ${this.type.name.name}`
      });
  }

  ngOnDestroy() {
    ReactDOM.unmountComponentAtNode(this.containerRef.nativeElement);
  }

  private get initialState(): string {
    if (!this.type) {
      return 'Type not set.  This shouldn\'t happen';
    } else {
      return this.type.typeDoc || '';
    }
  }

  saveChanges() {
    this.save.emit(this.lastChangeEvent());
  }
}
