import {Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {Documented, Type} from '../../services/schema';
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
    <div class="wrapper" *ngIf="editable">
      <!--      <div class="info-warning visible-on-changes" [class.has-changes]="hasChanges">-->
      <!--        <img src="assets/img/info.svg">-->
      <!--        <span>You have unsaved changes</span>-->
      <!--      </div>-->
      <div #container></div>
      <div class="button-bar visible-on-changes" [class.has-changes]="hasChanges" *ngIf="showControlBar && hasChanges">
        <button mat-button (click)="cancelEdits.emit()">Cancel</button>
        <button mat-raised-button color="primary" (click)="saveChanges()">Save changes</button>
      </div>
    </div>
    <markdown [data]="documentationSource.typeDoc" *ngIf="!editable" ></markdown>
  `,
  styleUrls: ['./description-editor.component.scss']
})
export class DescriptionEditorComponent implements OnInit, OnDestroy {

  private _containerRef: ElementRef;

  @ViewChild('container', {static: false})
  get containerRef(): ElementRef {
    return this._containerRef;
  }

  set containerRef(value: ElementRef) {
    if (this._containerRef === value) {
      return;
    }
    this._containerRef = value;
    this.resetEditor();
  }

  private _documentationSource: Documented = {
    typeDoc: null
  };

  private changeEventCount: number;
  private lastChangeEvent: ContentSupplier;

  @Input()
    // tslint:disable-next-line:no-inferrable-types
  showControlBar: boolean = true;

  // Editing is disabled by default, as we don't currently have
  // any way of persisting it.
  // Also, when we come to enable editing, we need to consider that the editor
  // is a very heavy-weight component, and don't want to have it for every
  // field on a type.  We'll need to do something where we start with the
  // markdown renderer, and then swap to the rich editor when we enter an
  // edit mode.
  private _editable = false;

  @Input()
  get editable(): boolean {
    return this._editable;
  }

  set editable(value: boolean) {
    const wasEditable = this._editable;
    if (value === wasEditable) {
      return;
    }
    this._editable = value;
    if (this.editable) {
      this.resetEditor();
      this.changes$.subscribe(next => {
        this.changeEventCount++;
        this.valueChanged.emit(next);
        // this.lastChangeEvent = next;
      });
    } else {
      if (wasEditable) {
        this.destroyEditor();
      }
    }
  }

  @Output()
  valueChanged = new EventEmitter<ContentSupplier>();

  @Input()
  placeholder: string;

  get hasChanges(): boolean {
    // We ignore the first change event, as it's the event triggered by
    // setting the initial state
    return this.editable && this.changeEventCount > 1;
  }

  @Output()
  save: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  cancelEdits: EventEmitter<void> = new EventEmitter<void>();

  @Input()
  get documentationSource(): Documented {
    return this._documentationSource;
  }

  set documentationSource(value: Documented) {
    this._documentationSource = value;
    this.resetEditor();
  }

  private changes$ = new BehaviorSubject<ContentSupplier>(() => '');

  constructor() {
  }


  ngOnInit() {
    this.resetEditor();
  }


  private resetEditor() {
    if (!this._editable) {
      return;
    }
    if (!this._containerRef) {
      // console.error('ContainerRef not set - this looks like an angular lifecycle problem');
      return;
    }
    this.changeEventCount = 0;
    ReactEditorWrapper.initialize(this._containerRef,
      {
        changes$: this.changes$,
        initialState: this.initialState,
        placeholder: this.placeholder
      });
  }

  ngOnDestroy() {
    if (this.editable) {
      this.destroyEditor();
    }
  }

  private destroyEditor() {
    if (this._containerRef && this._containerRef.nativeElement) {
      ReactDOM.unmountComponentAtNode(this._containerRef.nativeElement);
    }

  }

  private get initialState(): string {
    if (!this.documentationSource) {
      return 'Type not set.  This shouldn\'t happen';
    } else {
      return this.documentationSource.typeDoc || '';
    }
  }

  saveChanges() {
    this.save.emit(this.lastChangeEvent());
  }
}
