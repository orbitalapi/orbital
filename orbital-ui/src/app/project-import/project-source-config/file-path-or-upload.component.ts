import { TuiInputModule } from "@taiga-ui/legacy";
import { TuiError } from "@taiga-ui/core";
import {Component, EventEmitter, Input, OnInit, Optional, Output, SkipSelf, ViewChild} from '@angular/core';
import {
  ControlContainer,
  FormControl,
  FormsModule,
  NgControl,
  NgForm,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {Observable, of, Subject, switchMap} from 'rxjs';
import {CommonModule} from '@angular/common';
import { TuiFileLike, TuiFiles } from '@taiga-ui/kit';
import {FileExtensionValidatorDirective} from './file-extension-validator.directive';

// A component that combines the Tui components required for file upload and a path based input field.
// It also handles the validation that's required for the path based flow.
@Component({
  selector: 'app-file-path-or-upload',
  // Need this rather lengthy bit of Angular magic to cross the bridge between
  // the ngForm (aka template-driven) approach in the parent component and the
  // formControl (aka reactive forms) approach in here to get validation working
  viewProviders: [
    {
      provide: ControlContainer,
      useFactory: (controlContainer: ControlContainer) =>
        controlContainer instanceof NgForm ? controlContainer : null,
      deps: [[new SkipSelf(), ControlContainer]],
    },
  ],
  standalone: true,
  imports: [
    TuiFiles,
    ReactiveFormsModule,
    CommonModule,
    TuiInputModule,
    FormsModule,
    FileExtensionValidatorDirective,
    TuiError,
  ],
  template: `
    @if (mode === 'upload' && editable) {
      <label
        *ngIf="!fileDropControl.value"
        tuiInputFiles
      >
        <input
          tuiInputFiles
          [accept]="filesAccepted.join(',')"
          [formControl]="fileDropControl"
          (reject)="onReject($event)"
        />
      </label>
      <tui-files class="tui-space_top-1">
        <tui-file
          *ngIf="loadedFiles$ | async as file"
          [file]="file"
          [showDelete]="fileDropControl.enabled"
          (remove)="removeFile()"
        ></tui-file>
        <tui-file
          *ngIf="rejectedFiles$ | async as file"
          state="error"
          [file]="file"
          [showDelete]="fileDropControl.enabled"
          (remove)="clearRejected()"
        ></tui-file>
      </tui-files>
    } @else {
      <tui-input
        [ngModel]="path"
        (ngModelChange)="onPathChanged($event)"
        appFileExtensionValidator [appFileExtensionValidator]="filesAccepted"
        required
        name="path"
        [readOnly]="!editable"
        #pathName="ngModel"
      >
        Path
        <span class="tui-required"></span>
      </tui-input>
      <tui-error
        [error]="pathName$?.invalid && (pathName$.dirty || pathName$.touched) ? fileExtensionErrorLabel : null"
      >
      </tui-error>
    }
  `,
})
export class FilePathOrUploadComponent implements OnInit {
  @Input()
  editable: boolean = true;

  @Input()
  readContentsAs: 'string' | 'bytes' = 'string';
  @Input()
  path: string;

  @Input()
  mode: 'path' | 'upload';

  /** Deprecated in Taiga v4 (no link or label props on the input anymore)  */
  @Input()
  uploadLabel: string;

  @Input()
  filesAccepted: string[];

  @Input()
  fileExtensionErrorLabel: string;

  @Output()
  pathChanged = new EventEmitter<string>();

  @Output()
  fileChanged = new EventEmitter<string>()

  @ViewChild('pathName')
  pathName$: NgControl

  errorMessage: string;

  readonly fileDropControl = new FormControl<TuiFileLike | null>(
    null,
    Validators.required
  );

  readonly rejectedFiles$ = new Subject<TuiFileLike | null>();
  readonly loadedFiles$ = this.fileDropControl.valueChanges.pipe(
    switchMap(file => (file ? this.makeRequest(file) : of(null))),
  );

  constructor(
    @Optional() @SkipSelf() private parentFormGroup: NgForm
  ) {}

  ngOnInit() {
    if (this.parentFormGroup) {
      // Add the child control to the parent form dynamically
      this.parentFormGroup.form.addControl('fileUpload', this.fileDropControl);
    }
  }

  onPathChanged(value: string) {
    this.path = value;
    this.pathChanged.emit(value);
  }

  onReject(file: TuiFileLike | readonly TuiFileLike[]): void {
    this.rejectedFiles$.next(file as TuiFileLike);
  }

  makeRequest(file: TuiFileLike): Observable<TuiFileLike | null> {
    this.rejectedFiles$.next(null);
    const fileReader = new FileReader();
    fileReader.onloadend = () => {
      this.fileChanged.emit(fileReader.result as string)
    }
    switch (this.readContentsAs) {
      case "string":
        fileReader.readAsText(file as File);
        break;
      case "bytes":
        fileReader.readAsArrayBuffer(file as File);
        break;
    }
    return of(file)
  }

  removeFile(): void {
    this.fileDropControl.setValue(null);
  }

  clearRejected(): void {
    this.removeFile();
    this.rejectedFiles$.next(null);
  }
}
