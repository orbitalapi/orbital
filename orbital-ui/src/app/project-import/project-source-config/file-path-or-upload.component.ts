import {TuiInputModule} from "@taiga-ui/legacy";
import {TuiError, TuiNotification} from "@taiga-ui/core";
import {Component, EventEmitter, input, Input, model, Optional, Output, SkipSelf, ViewChild} from '@angular/core';
import {
  AbstractControl,
  ControlContainer,
  FormControl,
  FormsModule,
  NgForm,
  NgModel,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import {Observable, of, switchMap} from 'rxjs';
import {CommonModule} from '@angular/common';
import {TuiFileLike, TuiFiles} from '@taiga-ui/kit';
import {FileExtensionValidatorDirective} from './file-extension-validator.directive';
import {AddProjectWorkflowType} from "./file-config.component";
import {toObservable} from "@angular/core/rxjs-interop";

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
    TuiNotification,
  ],
  template: `
    @if (mode() === 'fileUpload' && editable) {
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
          class="file-ok"
          *ngIf="!rejectedFile && loadedFiles$ | async as file"
          [file]="file"
          [showDelete]="fileDropControl.enabled"
          (remove)="removeFile()"
        ></tui-file>
        <tui-file
          class="file-error"
          *ngIf="rejectedFile | tuiFileRejected: {accept: filesAccepted.join(',')} | async as file"
          state="error"
          [file]="fileDropControl.value"
          [showDelete]="fileDropControl.enabled"
          (remove)="clearRejected()"
        ></tui-file>
      </tui-files>
      <tui-error
        [error]="fileDropControl?.invalid && (fileDropControl.dirty || fileDropControl.touched) ? fileExtensionErrorLabel : null"
      >
      </tui-error>
    } @else {
      <tui-input
        appFileExtensionValidator [appFileExtensionValidator]="filesAccepted"
        required
        name="path"
        [readOnly]="!editable"
        [formControl]="filePathControl"
      >
        Path
        <span class="tui-required"></span>
      </tui-input>
      <tui-notification  size="m" appearance='neutral' class="tui-space_top-2" *ngIf="editable && mode() !== 'git'">
        Using Docker? Enter paths relative to your mounted volume (e.g., /opt/service/workspace )
      </tui-notification>
      <tui-error
        [error]="filePathControl?.invalid && (filePathControl.dirty || filePathControl.touched) ? fileExtensionErrorLabel : null"
      >
      </tui-error>
    }
  `,
})
export class FilePathOrUploadComponent {
  @Input()
  editable: boolean = true;

  @Input()
  readContentsAs: 'string' | 'bytes' = 'string';
  path = model<string>();

  mode = input<AddProjectWorkflowType>()

  /** Deprecated in Taiga v4 (no link or label props on the input anymore)  */
  @Input()
  uploadLabel: string;

  @Input()
  filesAccepted: string[];

  @Input()
  fileExtensionErrorLabel: string;

  // @Output()
  // pathChanged = new EventEmitter<string>();

  @Output()
  fileChanged = new EventEmitter<string>()

  @ViewChild('pathName')
  pathName$: NgModel

  rejectedFile: TuiFileLike;
  errorMessage: string;

  readonly fileIsCorrectType = (control: AbstractControl): ValidationErrors | null => {
    if (control.value === this.rejectedFile || Array.isArray(this.rejectedFile) && this.rejectedFile.includes(control.value)) {
      return {
        'wrongFileType': this.fileExtensionErrorLabel
      }
    } else {
      return null;
    }

  }

  readonly fileDropControl = new FormControl<TuiFileLike | null>(
    null,
    [Validators.required,
      this.fileIsCorrectType
    ]
  );

  readonly filePathControl = new FormControl<string | null>(null, Validators.required);

  readonly loadedFiles$ = this.fileDropControl.valueChanges.pipe(
    switchMap(file => (file ? this.makeRequest(file) : of(null))),
  );


  constructor(
    @Optional() @SkipSelf() private parentFormGroup: NgForm
  ) {
    toObservable(this.mode).subscribe(mode => {
      if (!this.parentFormGroup) {
        return
      }
      if (mode === 'pathToFile') {
        this.parentFormGroup.form.setControl('filePath', this.filePathControl);
        this.parentFormGroup.form.removeControl('fileUpload');
      } else if (mode === 'fileUpload') {
        this.parentFormGroup.form.setControl('fileUpload', this.fileDropControl);
        this.parentFormGroup.form.removeControl('filePath');
      }
    })
    this.filePathControl.valueChanges.subscribe(value => {
      if (value !== this.path()) {
        this.path.set(value);
      }
    })

    // Changes passed in from parent component
    toObservable(this.path).subscribe(path => {
      if (this.filePathControl.getRawValue() !== path) {
        this.filePathControl.setValue(path);
      }
    })
  }

  onReject(file: TuiFileLike | readonly TuiFileLike[]): void {
    if (Array.isArray(file)) {
      if (file.length > 1) {
        console.warn(`Rejected files contains ${file.length} - expected max one`);
      }
      if (file.length > 0) {
        this.rejectedFile = file[0];
      }
    } else {
      this.rejectedFile = file as TuiFileLike;
    }


  }

  makeRequest(file: TuiFileLike): Observable<TuiFileLike | null> {
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
    this.rejectedFile = null;
  }
}
